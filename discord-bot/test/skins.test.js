import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openStore } from '../src/store.js';
import { createApi } from '../src/api.js';
import { SHARED_BONES, checkAnimations, checkAura, checkModel, checkSlug, checkTexture, sha256 } from '../src/skins.js';

function png(width, height) {
  const buffer = Buffer.alloc(33);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(buffer);
  buffer.writeUInt32BE(13, 8);
  buffer.write('IHDR', 12, 'ascii');
  buffer.writeUInt32BE(width, 16);
  buffer.writeUInt32BE(height, 20);
  return buffer;
}

function geo(bones, version = '1.12.0') {
  return Buffer.from(JSON.stringify({
    format_version: version,
    'minecraft:geometry': [{
      description: { identifier: 'geometry.unknown', texture_width: 128, texture_height: 128 },
      bones: bones.map((name) => ({ name, pivot: [0, 0, 0] })),
    }],
  }));
}

async function withApi(run) {
  const store = openStore(':memory:');
  const server = createApi({ store, rolesOf: async () => [], nameOf: async () => null });
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await run({ store, base });
  } finally {
    server.close();
    store.close();
  }
}

test('skin ids are lowercase words', () => {
  assert.equal(checkSlug('the_world_sukuna'), null);
  assert.ok(checkSlug('The World'));
  assert.ok(checkSlug('ab'));
});

test('textures must be PNGs of a sane size', () => {
  assert.deepEqual(checkTexture(png(128, 128)), { width: 128, height: 128 });
  assert.ok(checkTexture(png(2048, 128)).error);
  assert.ok(checkTexture(Buffer.from('not a png at all, really not')).error);
});

test('models need the shared bones unless allowed', () => {
  const full = checkModel(geo([...SHARED_BONES, 'barrage_rig']));
  assert.equal(full.error, undefined);
  assert.equal(full.barrage, true);
  const missing = checkModel(geo(['Head', 'RightArm']));
  assert.match(missing.error, /bone2/);
  assert.deepEqual(checkModel(geo(['Head']), true).missing, SHARED_BONES.filter((name) => name !== 'Head'));
  assert.ok(checkModel(geo(SHARED_BONES, '1.21.0')).error);
  assert.ok(checkModel(Buffer.from('{')).error);
});

test('animation files need an animations object', () => {
  assert.equal(checkAnimations(Buffer.from('{"animations":{"idle":{}}}')).clips, 1);
  assert.ok(checkAnimations(Buffer.from('{"format_version":"1.8.0"}')).error);
});

test('uploads keep their id and bump the version', async () => {
  await withApi(async ({ store }) => {
    const first = store.putSkin({ slug: 'tw_sukuna', stand: 'jojoha:the_world', name: 'Sukuna', files: { texture: png(128, 128), model: geo(SHARED_BONES) } });
    assert.equal(first.created, true);
    assert.equal(first.version, 1);
    const second = store.putSkin({ slug: 'tw_sukuna', stand: 'jojoha:the_world', name: 'Sukuna', files: { texture: png(64, 64) } });
    assert.equal(second.created, false);
    assert.equal(second.id, first.id);
    assert.equal(second.version, 2);
    assert.deepEqual(second.files.map((file) => file.kind), ['texture']);
    assert.equal(store.removeSkin('tw_sukuna'), true);
    const third = store.putSkin({ slug: 'tw_sukuna', stand: 'jojoha:the_world', name: 'Sukuna', files: { texture: png(64, 64) } });
    assert.notEqual(third.id, first.id);
  });
});

test('the catalog lists fingerprints and files download intact', async () => {
  await withApi(async ({ store, base }) => {
    const texture = png(128, 128);
    const model = geo(SHARED_BONES);
    const saved = store.putSkin({ slug: 'tw_sukuna', stand: 'jojoha:the_world', name: 'Sukuna', files: { texture, model } });

    const listing = await (await fetch(`${base}/v1/skins`)).json();
    assert.deepEqual(listing.skins, [{
      id: saved.id,
      slug: 'tw_sukuna',
      stand: 'jojoha:the_world',
      name: 'Sukuna',
      version: 1,
      files: { model: sha256(model), texture: sha256(texture) },
    }]);

    const res = await fetch(`${base}/v1/skins/${saved.id}/texture?v=1`);
    assert.equal(res.status, 200);
    assert.equal(res.headers.get('content-type'), 'image/png');
    assert.equal(sha256(Buffer.from(await res.arrayBuffer())), sha256(texture));

    assert.equal((await fetch(`${base}/v1/skins/${saved.id}/animations`)).status, 404);
    assert.equal((await fetch(`${base}/v1/skins/${saved.id}/secrets`)).status, 404);
    assert.equal((await fetch(`${base}/v1/skins/abc/texture`)).status, 404);
  });
});

test('skins carry preset aura colors in the catalog', async () => {
  await withApi(async ({ store, base }) => {
    store.putSkin({ slug: 'tw_sukuna', stand: 'jojoha:the_world', name: 'Sukuna', files: { texture: png(128, 128) } });
    assert.equal(store.setSkinAura('missing', 1, 2), null);
    const saved = store.setSkinAura('tw_sukuna', 0xb3121b, 0x000000);
    assert.equal(saved.aura, 0xb3121b);
    assert.equal(saved.aura2, 0);
    const listing = await (await fetch(`${base}/v1/skins`)).json();
    assert.equal(listing.skins[0].aura, 0xb3121b);
    assert.equal(listing.skins[0].aura2, 0);
    store.setSkinAura('tw_sukuna', null, null);
    const cleared = await (await fetch(`${base}/v1/skins`)).json();
    assert.equal('aura' in cleared.skins[0], false);
  });
});

test('aura colors must be real colors', () => {
  const parse = (raw) => (/^#?([0-9a-f]{6})$/i.exec(raw.trim()) ? parseInt(raw.trim().replace('#', ''), 16) : null);
  assert.deepEqual(checkAura('#B3121B', parse), { colour: 0xb3121b });
  assert.deepEqual(checkAura(null, parse), { colour: null });
  assert.ok(checkAura('blurple', parse).error);
});

test('an older database gains the aura columns', async () => {
  const { DatabaseSync } = await import('node:sqlite');
  const { mkdtempSync, rmSync } = await import('node:fs');
  const { join } = await import('node:path');
  const { tmpdir } = await import('node:os');
  const dir = mkdtempSync(join(tmpdir(), 'jojoha-'));
  const path = join(dir, 'old.db');
  const old = new DatabaseSync(path);
  old.exec('CREATE TABLE skins (id INTEGER PRIMARY KEY AUTOINCREMENT, slug TEXT NOT NULL UNIQUE, stand TEXT NOT NULL, name TEXT NOT NULL, version INTEGER NOT NULL, updated_at INTEGER NOT NULL)');
  old.exec("INSERT INTO skins (slug, stand, name, version, updated_at) VALUES ('old_one', 'jojoha:the_world', 'Old', 1, 0)");
  old.close();
  const store = openStore(path);
  try {
    assert.equal(store.skin('old_one').aura, null);
    assert.equal(store.setSkinAura('old_one', 0xff0000, null).aura, 0xff0000);
  } finally {
    store.close();
    rmSync(dir, { recursive: true, force: true });
  }
});
