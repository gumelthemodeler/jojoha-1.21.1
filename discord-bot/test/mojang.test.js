import { test } from 'node:test';
import assert from 'node:assert/strict';
import { lookupProfile } from '../src/mojang.js';
import { openStore } from '../src/store.js';
import { SUPPORTER_BUILD, roleKeys } from '../src/rewards.js';

const answer = (status, body) => async () => new Response(body === undefined ? null : JSON.stringify(body), { status });

test('a Minecraft name resolves to a dashed uuid', async () => {
  const profile = await lookupProfile('Gumel', answer(200, { id: '069a79f444e94726a5befca90e38aaf5', name: 'Gumel' }));
  assert.deepEqual(profile, { uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5', name: 'Gumel' });
});

test('unknown names, bad names and outages are told apart', async () => {
  assert.equal((await lookupProfile('Nobody', answer(204))).error, 'not_found');
  assert.equal((await lookupProfile('Nobody', answer(404, { error: 'x' }))).error, 'not_found');
  assert.equal((await lookupProfile('no spaces', answer(200, {}))).error, 'bad_name');
  assert.equal((await lookupProfile('Gumel', answer(503))).error, 'unreachable');
  assert.equal((await lookupProfile('Gumel', async () => { throw new Error('down'); })).error, 'unreachable');
});

test('the supporter build comes only from roles that are given it', () => {
  const store = openStore(':memory:');
  try {
    const tiered = roleKeys([{ id: 'r', name: '[ULTIMATE] Crew' }]);
    assert.equal(store.rewardsForRoles(tiered).includes(SUPPORTER_BUILD), false);
    store.addRoleReward('r', SUPPORTER_BUILD);
    assert.equal(store.rewardsForRoles(tiered).includes(SUPPORTER_BUILD), true);
  } finally {
    store.close();
  }
});
