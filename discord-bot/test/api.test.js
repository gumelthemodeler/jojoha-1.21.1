import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openStore } from '../src/store.js';
import { createApi } from '../src/api.js';
import { SUPPORTER_AURA, SUPPORTER_SKINS, normalizeUuid, parseColor } from '../src/rewards.js';

const UUID = '069a79f4-44e9-4726-a5be-fca90e38aaf5';

async function withApi(roles, run) {
  const store = openStore(':memory:');
  const linked = [];
  const server = createApi({
    store,
    rolesOf: async (id) => roles[id] ?? [],
    nameOf: async (id) => `user-${id}`,
    onLinked: (id, name) => linked.push([id, name]),
  });
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await run({ store, base, linked });
  } finally {
    server.close();
    store.close();
  }
}

const post = (base, body) => fetch(`${base}/v1/link`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

test('unlinked players have no perks', async () => {
  await withApi({}, async ({ base }) => {
    const res = await fetch(`${base}/v1/players/${UUID}`);
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), { linked: false, rewards: [], aura: null });
  });
});

test('a code links once and roles become rewards', async () => {
  await withApi({ d1: ['r-kofi'] }, async ({ store, base, linked }) => {
    store.addRoleReward('r-kofi', SUPPORTER_SKINS);
    store.addRoleReward('r-kofi', SUPPORTER_AURA);
    store.addRoleReward('r-other', 'jojoha:other');
    store.setAura('d1', 0xff8800);
    const code = store.issueCode('d1', 60000);

    const res = await post(base, { code: code.toLowerCase(), uuid: UUID.replaceAll('-', ''), name: 'Jotaro' });
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.deepEqual(body.rewards, [SUPPORTER_AURA, SUPPORTER_SKINS]);
    assert.equal(body.aura, 0xff8800);
    assert.equal(body.discord, 'user-d1');
    assert.deepEqual(linked, [['d1', 'Jotaro']]);

    const again = await post(base, { code, uuid: UUID, name: 'Jotaro' });
    assert.equal(again.status, 404);

    const lookup = await (await fetch(`${base}/v1/players/${UUID.toUpperCase()}`)).json();
    assert.deepEqual(lookup, { linked: true, rewards: [SUPPORTER_AURA, SUPPORTER_SKINS], aura: 0xff8800 });
  });
});

test('aura is hidden without the aura reward', async () => {
  await withApi({ d1: ['r-skins'] }, async ({ store, base }) => {
    store.addRoleReward('r-skins', SUPPORTER_SKINS);
    store.setAura('d1', 0x123456);
    store.link('d1', UUID, 'Jotaro');
    const lookup = await (await fetch(`${base}/v1/players/${UUID}`)).json();
    assert.deepEqual(lookup, { linked: true, rewards: [SUPPORTER_SKINS], aura: null });
  });
});

test('relinking moves the account and expired codes fail', async () => {
  await withApi({}, async ({ store, base }) => {
    store.link('d1', UUID, 'Jotaro');
    store.link('d2', UUID, 'Jotaro');
    assert.equal(store.linkOf('d1'), null);
    assert.equal(store.linkByUuid(UUID).discord_id, 'd2');

    const code = store.issueCode('d3', -1);
    const res = await post(base, { code, uuid: UUID, name: 'Jotaro' });
    assert.equal(res.status, 404);
  });
});

test('bad input is rejected and link attempts are rate limited', async () => {
  await withApi({}, async ({ base }) => {
    assert.equal((await fetch(`${base}/v1/players/not-a-uuid`)).status, 400);
    assert.equal((await post(base, { code: 'ABCDEFGH', uuid: UUID, name: 'bad name!' })).status, 400);
    let status = 0;
    for (let i = 0; i < 12; i++) {
      status = (await post(base, { code: 'ABCDEFGH', uuid: UUID, name: 'Jotaro' })).status;
    }
    assert.equal(status, 429);
  });
});

test('parsing helpers', () => {
  assert.equal(normalizeUuid('069A79F444E94726A5BEFCA90E38AAF5'), UUID);
  assert.equal(normalizeUuid('nope'), null);
  assert.equal(parseColor('#ff8800'), 0xff8800);
  assert.equal(parseColor('FF8800'), 0xff8800);
  assert.equal(parseColor('#ff88'), null);
  assert.equal(parseColor(' Rainbow '), 0x1000000);
  assert.equal(parseColor('gold'), 0xffc832);
  assert.equal(parseColor('toString'), null);
});

test('site data carries the live guide and faq', async () => {
  await withApi({}, async ({ store, base }) => {
    store.setSetting('kofi_url', 'https://ko-fi.com/example');
    const res = await fetch(`${base}/v1/site`);
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.deepEqual(body.guide, store.guide());
    assert.deepEqual(body.faq, store.faq());
    assert.equal(body.kofi, 'https://ko-fi.com/example');
  });
});

test('site pages fall back to the app shell', async () => {
  await withApi({}, async ({ base }) => {
    for (const path of ['/', '/wiki/hamon', '/faq']) {
      const res = await fetch(`${base}${path}`);
      assert.equal(res.status, 200);
      assert.match(res.headers.get('content-type'), /text\/html/);
      assert.match(await res.text(), /<main id="app">/);
    }
    const css = await fetch(`${base}/styles.css`);
    assert.match(css.headers.get('content-type'), /text\/css/);
    const gif = await fetch(`${base}/img/stands/the_world.gif`);
    assert.equal(gif.headers.get('content-type'), 'image/gif');
  });
});

test('site files stay inside the site folder', async () => {
  await withApi({}, async ({ base }) => {
    for (const path of ['/..%2Fpackage.json', '/%2e%2e/src/api.js', '/img/..%2F..%2Fsrc%2Fapi.js', '/missing.png', '/%E0%A4%A.png']) {
      const res = await fetch(`${base}${path}`);
      assert.ok(res.status === 404 || res.status === 400, `${path} gave ${res.status}`);
    }
    const unknown = await fetch(`${base}/v1/nothing`);
    assert.equal(unknown.status, 404);
  });
});

test('new wiki pages reach an existing database once, and removed ones stay removed', async () => {
  const { mkdtempSync, rmSync } = await import('node:fs');
  const { join } = await import('node:path');
  const { tmpdir } = await import('node:os');
  const { DatabaseSync } = await import('node:sqlite');
  const { FIRST_PAGES, PAGES } = await import('../src/guide.js');
  const dir = mkdtempSync(join(tmpdir(), 'jjha-'));
  const file = join(dir, 'bot.db');
  try {
    let store = openStore(file);
    store.close();
    const db = new DatabaseSync(file);
    db.exec("DELETE FROM settings WHERE key = 'guide_known'");
    for (const page of PAGES.filter((p) => !FIRST_PAGES.includes(p.id))) db.prepare('DELETE FROM guide WHERE id = ?').run(page.id);
    db.prepare('DELETE FROM guide WHERE id = ?').run('anubis');
    db.close();
    store = openStore(file);
    const ids = store.guide().map((p) => p.id);
    assert.ok(ids.includes('combat') && ids.includes('fortunes'));
    assert.ok(!ids.includes('anubis'));
    store.removeGuide('combat');
    store.close();
    store = openStore(file);
    assert.ok(!store.guide().some((p) => p.id === 'combat'));
    store.close();
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
