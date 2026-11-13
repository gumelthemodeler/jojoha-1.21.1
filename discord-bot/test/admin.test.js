import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { createServer } from 'node:http';
import { openStore } from '../src/store.js';
import { createApi } from '../src/api.js';
import { DEFAULT_CONTENT, checkContent, mergeContent } from '../src/site.js';

const STAFF = '111111111111111111';
const PNG = Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), Buffer.alloc(32)]);

const freePort = () => new Promise((resolve) => {
  const probe = createServer().listen(0, () => {
    const { port } = probe.address();
    probe.close(() => resolve(port));
  });
});

async function withAdmin(run, { staff = [STAFF], fetchImpl } = {}) {
  const port = await freePort();
  const store = openStore(':memory:');
  const server = createApi({
    store,
    rolesOf: async () => [],
    nameOf: async () => null,
    canManage: async (id) => staff.includes(id),
    clientId: () => '999',
    clientSecret: 'secret',
    siteUrl: `http://127.0.0.1:${port}`,
    fetchImpl,
  });
  await new Promise((resolve) => server.listen(port, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${port}`;
  const login = (id = STAFF) => {
    const token = `token-${id}`;
    store.createSession(createHash('sha256').update(token).digest('hex'), id, 'Tester', 60000);
    return { cookie: `jjha_session=${token}`, origin: base };
  };
  try {
    await run({ store, base, login });
  } finally {
    server.close();
    store.close();
  }
}

const json = (method, body, headers) => ({ method, headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(body) });

test('the bundled content passes its own checks', () => {
  assert.deepEqual(checkContent(structuredClone(DEFAULT_CONTENT)).error, undefined);
});

test('content checks reject unsafe links and colors', () => {
  const bad = structuredClone(DEFAULT_CONTENT);
  bad.stands[0].model = 'javascript:alert(1)';
  assert.match(checkContent(bad).error, /model/);
  const color = structuredClone(DEFAULT_CONTENT);
  color.stands[0].color = 'red;background:url(x)';
  assert.match(checkContent(color).error, /color/);
  const proto = structuredClone(DEFAULT_CONTENT);
  proto.discord = '//evil.example';
  assert.match(checkContent(proto).error, /invite/);
  const status = structuredClone(DEFAULT_CONTENT);
  status.roadmap[0].status = 'x" onclick="';
  assert.match(checkContent(status).error, /status/);
});

test('the admin page and content are served', async () => {
  await withAdmin(async ({ base }) => {
    const page = await fetch(`${base}/admin`);
    assert.match(await page.text(), /id="admin"/);
    const home = await (await fetch(`${base}/`)).text();
    assert.match(home, /src="\/app\.js\?v=[0-9a-f]{10}"/);
    assert.match(home, /href="\/styles\.css\?v=[0-9a-f]{10}"/);
    const content = await (await fetch(`${base}/content.json`)).json();
    assert.equal(content.discord, DEFAULT_CONTENT.discord);
  });
});

test('the admin api needs a staff session', async () => {
  await withAdmin(async ({ base, login }) => {
    assert.equal((await fetch(`${base}/admin/api/state`)).status, 401);
    const outsider = login('222222222222222222');
    assert.equal((await fetch(`${base}/admin/api/state`, { headers: outsider })).status, 401);
    const staff = login();
    const res = await fetch(`${base}/admin/api/state`, { headers: staff });
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.ok(body.guide.length > 0);
    assert.match(body.guide[0].sections, /^## /);
  });
});

test('saving content updates the public site', async () => {
  await withAdmin(async ({ base, login }) => {
    const staff = login();
    const content = structuredClone(DEFAULT_CONTENT);
    content.home.lead = 'Edited headline';
    const noOrigin = await fetch(`${base}/admin/api/content`, json('PUT', content, { cookie: staff.cookie }));
    assert.equal(noOrigin.status, 403);
    const saved = await fetch(`${base}/admin/api/content`, json('PUT', content, staff));
    assert.equal(saved.status, 200);
    const live = await (await fetch(`${base}/content.json`)).json();
    assert.equal(live.home.lead, 'Edited headline');
    content.staff[0].avatar = 'javascript:alert(1)';
    const rejected = await fetch(`${base}/admin/api/content`, json('PUT', content, staff));
    assert.equal(rejected.status, 400);
    assert.equal((await (await fetch(`${base}/content.json`)).json()).staff[0].avatar, '');
  });
});

test('wiki pages and faq entries save through the bot validators', async () => {
  await withAdmin(async ({ base, store, login }) => {
    const staff = login();
    const page = await fetch(`${base}/admin/api/guide/new-page`, json('PUT', { title: 'New', summary: 'One line', intro: 'Hello', sections: '## Part\n- A line' }, staff));
    assert.equal(page.status, 200);
    assert.equal(store.guide().at(-1).id, 'new-page');
    const bad = await fetch(`${base}/admin/api/guide/new-page`, json('PUT', { title: 'New', summary: 'x', intro: 'x', sections: 'no heading' }, staff));
    assert.equal(bad.status, 400);
    const ids = store.guide().map((p) => p.id).reverse();
    assert.equal((await fetch(`${base}/admin/api/guide-order`, json('PUT', { ids }, staff))).status, 200);
    assert.deepEqual(store.guide().map((p) => p.id), ids);
    const faq = await fetch(`${base}/admin/api/faq/new-q`, json('PUT', { group: 'Misc', question: 'Q?', answer: 'A.', related: 'kofi' }, staff));
    assert.equal(faq.status, 200);
    assert.ok(store.faq().some((e) => e.id === 'new-q'));
    assert.equal((await fetch(`${base}/admin/api/faq/new-q`, { method: 'DELETE', headers: staff })).status, 200);
  });
});

test('uploads accept real images only and are served back', async () => {
  await withAdmin(async ({ base, login }) => {
    const staff = login();
    const html = await fetch(`${base}/admin/api/files?name=x.png`, { method: 'POST', headers: { ...staff, 'Content-Type': 'image/png' }, body: '<script>alert(1)</script>' });
    assert.equal(html.status, 400);
    const ok = await fetch(`${base}/admin/api/files?name=pic.png`, { method: 'POST', headers: { ...staff, 'Content-Type': 'image/png' }, body: PNG });
    assert.equal(ok.status, 200);
    const { url } = await ok.json();
    const served = await fetch(`${base}${url}`);
    assert.equal(served.headers.get('content-type'), 'image/png');
    assert.equal(Buffer.from(await served.arrayBuffer()).length, PNG.length);
  });
});

test('discord sign-in checks state and staff permission', async () => {
  const fakeDiscord = (userId) => async (url) => {
    if (String(url).endsWith('/oauth2/token')) {
      return new Response(JSON.stringify({ access_token: 'abc' }), { status: 200 });
    }
    return new Response(JSON.stringify({ id: userId, username: 'tester' }), { status: 200 });
  };
  await withAdmin(async ({ base }) => {
    const start = await fetch(`${base}/auth/login`, { redirect: 'manual' });
    assert.equal(start.status, 302);
    const location = new URL(start.headers.get('location'));
    assert.equal(location.host, 'discord.com');
    const state = location.searchParams.get('state');
    const stateCookie = start.headers.get('set-cookie').split(';')[0];
    const wrong = await fetch(`${base}/auth/callback?code=c&state=nope`, { redirect: 'manual', headers: { cookie: stateCookie } });
    assert.match(wrong.headers.get('location'), /error=state/);
    const good = await fetch(`${base}/auth/callback?code=c&state=${state}`, { redirect: 'manual', headers: { cookie: stateCookie } });
    assert.equal(good.headers.get('location'), '/admin');
    const session = good.headers.getSetCookie().find((c) => c.startsWith('jjha_session=')).split(';')[0];
    assert.equal((await fetch(`${base}/admin/api/me`, { headers: { cookie: session } })).status, 200);
  }, { fetchImpl: fakeDiscord(STAFF) });

  await withAdmin(async ({ base }) => {
    const start = await fetch(`${base}/auth/login`, { redirect: 'manual' });
    const state = new URL(start.headers.get('location')).searchParams.get('state');
    const stateCookie = start.headers.get('set-cookie').split(';')[0];
    const denied = await fetch(`${base}/auth/callback?code=c&state=${state}`, { redirect: 'manual', headers: { cookie: stateCookie } });
    assert.match(denied.headers.get('location'), /error=denied/);
  }, { fetchImpl: fakeDiscord('333333333333333333') });
});

test('sign-in starts on the configured site address', async () => {
  const store = openStore(':memory:');
  const server = createApi({ store, rolesOf: async () => [], nameOf: async () => null, clientId: () => '999', clientSecret: 's', siteUrl: 'https://jojoheavensarrow.dev' });
  await new Promise((resolve) => server.listen(0, resolve));
  try {
    const res = await fetch(`http://127.0.0.1:${server.address().port}/auth/login`, { redirect: 'manual' });
    assert.equal(res.headers.get('location'), 'https://jojoheavensarrow.dev/auth/login');
  } finally {
    server.close();
    store.close();
  }
});

test('projects saved before logos existed still pass', () => {
  const content = structuredClone(DEFAULT_CONTENT);
  content.projects = [{ name: 'Old', status: 'Not announced', text: 'Saved earlier.' }];
  assert.equal(checkContent(content).error, undefined);
  content.projects[0].logo = 'javascript:alert(1)';
  assert.match(checkContent(content).error, /logo/);
});

test('roadmap phases saved as plain lists become cards', () => {
  const old = structuredClone(DEFAULT_CONTENT);
  old.roadmap = [{ phase: 'Now', title: 'Testing', status: 'active', items: ['Bug fixing'] }];
  const merged = mergeContent({ roadmap: structuredClone(old.roadmap) });
  assert.deepEqual(merged.roadmap[0].cards, [{ title: 'Bug fixing', status: 'active', tag: '', image: '', text: '' }]);
  const checked = checkContent(old);
  assert.equal(checked.error, undefined);
  const bad = structuredClone(DEFAULT_CONTENT);
  bad.roadmap[0].cards[0].status = 'soon';
  assert.match(checkContent(bad).error, /status/);
});

test('the site can tell who is signed in', async () => {
  await withAdmin(async ({ base, login }) => {
    assert.deepEqual(await (await fetch(`${base}/auth/me`)).json(), { signedIn: false });
    const outsider = login('222222222222222222');
    assert.deepEqual(await (await fetch(`${base}/auth/me`, { headers: outsider })).json(), { signedIn: false });
    const staff = await (await fetch(`${base}/auth/me`, { headers: login() })).json();
    assert.equal(staff.signedIn, true);
    assert.equal(staff.name, 'Tester');
  });
});
