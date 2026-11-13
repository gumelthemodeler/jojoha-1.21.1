import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { openStore } from '../src/store.js';
import { mergeContent, checkContent } from '../src/site.js';
import { REWARDS, roleKeys, tierOf } from '../src/rewards.js';

const ALL = REWARDS.filter((reward) => !reward.byRole).map((reward) => reward.id).sort();
const role = (id, name) => ({ id, name });

test('Ko-fi tier roles are read from their tag', () => {
  assert.equal(tierOf('[STANDARD] GUMEL\'S CREW'), 'STANDARD');
  assert.equal(tierOf('[King] Gumel\'s Crew'), 'KING');
  assert.equal(tierOf('Gumel\'s Crew [STANDARD]'), null);
  assert.equal(tierOf('[ADMIN] Staff'), null);
  assert.deepEqual(roleKeys([role('1', '[MAFIA] GUMEL\'S CREW'), role('2', 'Member')]), ['1', 'tier:MAFIA', '2']);
});

test('Standard and above get skins and aura, lower tiers do not', () => {
  const store = openStore(':memory:');
  try {
    for (const tier of ['TIP', 'LIGHT']) {
      assert.deepEqual(store.rewardsForRoles(roleKeys([role('r', `[${tier}] GUMEL'S CREW`)])), [], tier);
    }
    for (const tier of ['STANDARD', 'KING', 'MAFIA', 'ULTIMATE']) {
      assert.deepEqual(store.rewardsForRoles(roleKeys([role('r', `[${tier}] GUMEL'S CREW`)])), ALL, tier);
    }
    assert.equal(store.gainedRewards(roleKeys([role('a', '[LIGHT] Crew')]), roleKeys([role('b', '[STANDARD] Crew')])), true);
    assert.equal(store.gainedRewards(roleKeys([role('a', '[TIP] Crew')]), roleKeys([role('b', '[LIGHT] Crew')])), false);
  } finally {
    store.close();
  }
});

test('the supporter page shows the six Ko-fi tiers over an older saved copy', () => {
  const defaults = JSON.parse(readFileSync(new URL('../site/content.json', import.meta.url), 'utf8'));
  assert.deepEqual(defaults.support.tiers.map((t) => t.name), ['Tip', 'Light', 'Standard', 'King', 'Mafia', 'Ultimate']);
  const old = { support: { inGameTitle: 'Every tier gets', inGame: [], tiers: [{ name: 'Tip' }] } };
  assert.equal(mergeContent(old).support.tiers.length, 6);
  const edited = structuredClone(defaults.support);
  edited.tiers[0].price = '$6';
  assert.equal(mergeContent({ support: edited }).support.tiers[0].price, '$6');
  checkContent(mergeContent({}));
});
