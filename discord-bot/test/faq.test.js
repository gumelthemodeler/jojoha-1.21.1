import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openStore } from '../src/store.js';
import { KOFI, SEED, checkEntry, findEntry, httpsUrl, searchEntries } from '../src/faq.js';
import { existsSync } from 'node:fs';
import { LOGO, READ_BUTTON, faqAnswer, faqPanel, faqReminder, guidePage, guideSelect, perksPanel, questionsNotice, status, welcome } from '../src/views.js';
import { PAGES, checkPage, findPage, formatSections, parseSections } from '../src/guide.js';
import { COMMANDS, entryModal } from '../src/bot.js';
import { SUPPORTER_AURA, SUPPORTER_SKINS } from '../src/rewards.js';

const LINKS = { kofi: 'https://ko-fi.com/example', earlyAccess: 'https://ko-fi.com/post/example' };
const NONE = { kofi: null, earlyAccess: null };

const buttons = (payload) => payload.components.flatMap((row) => row.components);

function withStore(run) {
  const store = openStore(':memory:');
  try {
    run(store);
  } finally {
    store.close();
  }
}

test('the first FAQ is seeded once, in its groups', () => {
  withStore((store) => {
    const entries = store.faq();
    assert.deepEqual(entries.map((entry) => entry.id), SEED.map((entry) => entry.id));
    assert.deepEqual([...new Set(entries.map((entry) => entry.group))], ['Release', 'Platform', 'Getting access', 'Gameplay']);
    for (const entry of entries) {
      assert.equal(checkEntry({ ...entry, related: entry.related.join(',') }, entries).error, undefined);
    }
    for (const entry of entries) {
      store.removeFaq(entry.id);
    }
    assert.equal(store.faq().length, 0);
  });
});

test('new questions join their group and edits keep their place', () => {
  withStore((store) => {
    assert.equal(store.putFaq({ id: 'forge', group: 'Platform', question: 'Forge?', answer: 'No.', related: [] }), true);
    const ids = store.faq().map((entry) => entry.id);
    assert.equal(ids.indexOf('forge'), ids.indexOf('platform') + 1);
    assert.equal(store.putFaq({ id: 'release', group: 'Release', question: 'When?', answer: 'Later.', related: [KOFI] }), false);
    const first = store.faq()[0];
    assert.equal(first.question, 'When?');
    assert.deepEqual(first.related, [KOFI]);
    assert.equal(store.removeFaq('forge'), true);
    assert.equal(store.removeFaq('forge'), false);
  });
});

test('entries are checked before they are saved', () => {
  withStore((store) => {
    const entries = store.faq();
    const base = { id: 'new-one', group: 'Gameplay', question: 'Q?', answer: 'A.', related: '' };
    assert.deepEqual(checkEntry({ ...base, related: 'Stands, kofi stands' }, entries).entry.related, ['stands', KOFI]);
    assert.match(checkEntry({ ...base, id: 'Bad Id!' }, entries).error, /id/);
    assert.match(checkEntry({ ...base, id: KOFI }, entries).error, /id/);
    assert.match(checkEntry({ ...base, related: 'nope' }, entries).error, /nope/);
    assert.match(checkEntry({ ...base, id: 'stands', related: 'stands' }, entries).error, /stands/);
    assert.match(checkEntry({ ...base, question: 'x'.repeat(101) }, entries).error, /question/);
    assert.match(checkEntry({ ...base, answer: '' }, entries).error, /answer/);
    const full = Array.from({ length: 25 }, (_, i) => ({ id: `q${i}` }));
    assert.match(checkEntry(base, full).error, /at most/);
    assert.equal(checkEntry({ ...base, id: 'q3' }, full).error, undefined);
  });
});

test('questions are found by id, wording or part of either', () => {
  withStore((store) => {
    const entries = store.faq();
    assert.equal(findEntry(entries, 'PLATFORM').id, 'platform');
    assert.equal(findEntry(entries, 'what is the public beta?').id, 'public-beta');
    assert.equal(findEntry(entries, 'nothing'), null);
    assert.deepEqual(searchEntries(entries, 'beta').map((entry) => entry.id), ['public-beta']);
    assert.equal(searchEntries(entries, '').length, entries.length);
  });
});

test('an answer offers its follow-ups, and Ko-fi only once a page is set', () => {
  withStore((store) => {
    const entries = store.faq();
    const early = findEntry(entries, 'early-access');
    assert.match(early.answer, /membership/);
    const withLink = buttons(faqAnswer(early, entries, LINKS));
    assert.deepEqual(withLink.map((b) => b.custom_id ?? b.url), ['faq:show:supporters', LINKS.kofi]);
    assert.deepEqual(buttons(faqAnswer(early, entries, NONE)).map((b) => b.custom_id), ['faq:show:supporters']);
    assert.deepEqual(faqAnswer(findEntry(entries, 'platform'), entries, LINKS).components, []);
    store.removeFaq('supporters');
    assert.deepEqual(buttons(faqAnswer(early, store.faq(), NONE)), []);
  });
});

test('the panels list every question and say a membership is needed', () => {
  withStore((store) => {
    const panel = faqPanel(store.faq(), LINKS);
    const text = panel.embeds[0].description;
    assert.equal(panel.embeds[0].title, 'Gumel\'s Corner FAQ');
    assert.equal(panel.embeds[0].image.url, 'attachment://' + LOGO);
    assert.ok(existsSync(new URL('../src/' + LOGO, import.meta.url)));
    assert.ok(text.includes('**【01.】When does the mod release?**\nThere are no ETAs'));
    assert.ok(text.includes('\n\n**【07.】How are Stands obtainable'));
    assert.equal(panel.components[0].components[0].custom_id, 'guide:pick');
    assert.deepEqual(panel.components[0].components[0].options.map((option) => option.value), PAGES.map((page) => page.id));
    assert.equal(panel.components[1].components[0].url, LINKS.kofi);
    assert.equal(faqPanel(store.faq(), NONE).components.length, 1);
    const long = Array.from({ length: 25 }, (_, i) => ({ id: 'q' + i, group: 'G', question: 'Q' + i, answer: 'x'.repeat(2000), related: [] }));
    const crowded = faqPanel(long, NONE).embeds[0].description;
    assert.ok(crowded.length <= 4096);
    assert.match(crowded, /More answers/);
    const perks = perksPanel(LINKS);
    assert.match(perks.embeds[0].description, /membership/);
    assert.deepEqual(buttons(perks).map((b) => b.custom_id ?? b.url), [LINKS.kofi, 'perks:link', 'perks:status']);
    assert.deepEqual(buttons(perksPanel(NONE)).map((b) => b.custom_id), ['perks:link', 'perks:status']);
  });
});

test('every guide page fits in a Discord embed', () => {
  assert.equal(new Set(PAGES.map((page) => page.id)).size, PAGES.length);
  assert.ok(PAGES.length <= 25);
  assert.equal(findPage('nope'), null);
  for (const page of PAGES) {
    const embed = guidePage(page).embeds[0];
    assert.ok(embed.fields.length <= 25);
    for (const field of embed.fields) {
      assert.ok(field.name.length <= 256 && field.value.length <= 1024);
    }
    assert.ok(JSON.stringify(embed).length < 5000);
    const picked = guideSelect('guide:turn', page.id).components[0].options.filter((option) => option.default);
    assert.deepEqual(picked.map((option) => option.value), [page.id]);
  }
});

test('the welcome and the status say what to do next', () => {
  const hello = welcome('42', LINKS);
  assert.match(hello.content, /^<@42>/);
  assert.deepEqual(buttons(hello).map((b) => b.custom_id ?? b.url), ['perks:link', LINKS.earlyAccess]);
  assert.deepEqual(buttons(welcome('42', NONE)).map((b) => b.custom_id), ['perks:link']);

  const outsider = status({ link: null, rewards: [], aura: null }, LINKS);
  assert.match(outsider.embeds[0].description, /membership/);
  assert.deepEqual(buttons(outsider).map((b) => b.url), [LINKS.kofi]);
  assert.deepEqual(status({ link: null, rewards: [], aura: null }, NONE).components, []);

  const unlinked = status({ link: null, rewards: [SUPPORTER_SKINS, SUPPORTER_AURA], aura: null }, LINKS);
  assert.deepEqual(buttons(unlinked).map((b) => b.custom_id ?? b.url), ['perks:link', LINKS.earlyAccess]);

  const linked = status({ link: { mc_name: 'Jotaro' }, rewards: [SUPPORTER_AURA], aura: 0xffc832 }, NONE);
  assert.deepEqual(linked.components, []);
  assert.deepEqual(linked.embeds[0].fields.map((field) => field.value).slice(1), ['`Jotaro`', 'Gold']);
});

test('a welcome follows a first reward, once per cooldown', () => {
  withStore((store) => {
    store.addRoleReward('r-kofi', SUPPORTER_SKINS);
    assert.equal(store.gainedRewards(['r-a'], ['r-a', 'r-kofi']), true);
    assert.equal(store.gainedRewards(['r-kofi'], ['r-kofi', 'r-a']), false);
    assert.equal(store.gainedRewards(['r-kofi'], []), false);
    assert.equal(store.gainedRewards([], ['r-a']), false);
    assert.equal(store.markWelcomed('d1', 1000, 5000), true);
    assert.equal(store.markWelcomed('d1', 1000, 5500), false);
    assert.equal(store.markWelcomed('d1', 1000, 6000), true);
    assert.equal(store.markWelcomed('d2', 1000, 6000), true);
  });
});

test('settings are kept and cleared, and only https links pass', () => {
  withStore((store) => {
    assert.equal(store.setting('kofi_url'), null);
    store.setSetting('kofi_url', LINKS.kofi);
    assert.equal(store.setting('kofi_url'), LINKS.kofi);
    store.setSetting('kofi_url', null);
    assert.equal(store.setting('kofi_url'), null);
  });
  assert.equal(httpsUrl(' https://ko-fi.com/example '), 'https://ko-fi.com/example');
  assert.equal(httpsUrl('http://ko-fi.com/example'), null);
  assert.equal(httpsUrl('javascript:alert(1)'), null);
  assert.equal(httpsUrl('ko-fi.com/example'), null);
});

test('the commands and the edit form are ones Discord accepts', () => {
  const names = COMMANDS.map((command) => command.name);
  assert.deepEqual(names, ['link', 'unlink', 'whitelist', 'perks', 'aura', 'faq', 'guide', 'guideedit', 'faqedit', 'panel', 'setup', 'reward', 'skin', 'Answer with FAQ']);
  assert.equal(new Set(names).size, names.length);
  withStore((store) => {
    const added = entryModal(null).toJSON();
    assert.equal(added.custom_id, 'faq:edit:new');
    assert.equal(added.components.length, 5);
    const edited = entryModal(findEntry(store.faq(), 'early-access')).toJSON();
    assert.equal(edited.custom_id, 'faq:edit:early-access');
    assert.deepEqual(edited.components.map((row) => row.components[0].custom_id), ['group', 'question', 'answer', 'related']);
    assert.equal(edited.components[3].components[0].value, 'supporters, kofi');
    for (const row of [...added.components, ...edited.components]) {
      assert.ok(row.components[0].label.length <= 45);
    }
  });
});

test('the FAQ panel shows the read button only while a questions channel needs it', () => {
  withStore((store) => {
    const open = faqPanel(store.faq(), LINKS);
    assert.equal(buttons(open).some((b) => b.custom_id === 'faq:read'), false);
    const locked = faqPanel(store.faq(), { ...LINKS, questions: '123' });
    const read = buttons(locked).find((b) => b.custom_id === 'faq:read');
    assert.equal(read.label, READ_BUTTON);
    assert.match(locked.embeds[0].footer.text, /before posting in the questions channel/);
    assert.ok(buttons(locked).some((b) => b.url === LINKS.kofi));
  });
});

test('the questions note links back to the FAQ panel', () => {
  const note = questionsNotice('https://discord.com/channels/1/2/3');
  assert.equal(buttons(note)[0].url, 'https://discord.com/channels/1/2/3');
  assert.match(note.embeds[0].description, new RegExp(READ_BUTTON));
  assert.equal(questionsNotice(null).components.length, 0);
  assert.match(note.embeds[0].description, /removed/);
  const reminder = faqReminder('42', 'https://discord.com/channels/1/2/3');
  assert.match(reminder.content, /^<@42>, read the FAQ/);
  assert.equal(buttons(reminder)[0].url, 'https://discord.com/channels/1/2/3');
  assert.equal(faqReminder('42', null).components.length, 0);
});

test('setup can turn the questions filter on and off', () => {
  const setup = COMMANDS.find((command) => command.name === 'setup');
  const subs = setup.options.map((option) => option.name);
  assert.ok(subs.includes('questions') && subs.includes('unlockquestions'));
});

test('guide pages are seeded once and edited from Discord', () => {
  withStore((store) => {
    assert.deepEqual(store.guide().map((page) => page.id), PAGES.map((page) => page.id));
    for (const page of store.guide()) {
      const checked = checkPage({ ...page, sections: formatSections(page.sections) }, store.guide());
      assert.equal(checked.error, undefined, page.id);
      assert.deepEqual(checked.page, page);
    }
    const pillar = findPage('pillar-man', store.guide());
    store.putGuide({ ...pillar, intro: 'Changed.' });
    assert.equal(findPage('pillar-man', store.guide()).intro, 'Changed.');
    assert.deepEqual(store.guide().map((page) => page.id), PAGES.map((page) => page.id));
    assert.ok(store.removeGuide('anubis'));
    assert.equal(findPage('anubis', store.guide()), null);
    const panel = faqPanel(store.faq(), NONE, store.guide());
    assert.ok(!panel.components[0].components[0].options.some((option) => option.value === 'anubis'));
  });
});

test('guide sections need a heading and fit an embed field', () => {
  assert.equal(parseSections('text before any heading'), null);
  assert.deepEqual(parseSections('## A\n- one\n\n## B\ntwo'), [['A', '- one'], ['B', 'two']]);
  const base = { id: 'x', title: 'X', summary: 'S', intro: 'I' };
  assert.match(checkPage({ ...base, sections: 'no heading' }, PAGES).error, /heading/);
  assert.match(checkPage({ ...base, sections: '## Empty' }, PAGES).error, /1-1024/);
  assert.match(checkPage({ ...base, sections: '## Long\n' + 'a'.repeat(1025) }, PAGES).error, /1-1024/);
  assert.equal(checkPage({ ...base, sections: '## Fine\nok' }, PAGES).error, undefined);
});

test('the Pillar Man page describes the Progenitor Mask, not the removed Sealed Pillar', () => {
  const page = findPage('pillar-man');
  const text = [page.summary, page.intro, ...page.sections.flat()].join(' ');
  assert.match(text, /Progenitor Mask/);
  assert.match(text, /catacomb vault/);
  assert.doesNotMatch(text, /Sealed Pillar/);
});

test('wiki pictures and site links become full links in Discord', async () => {
  const { forDiscord } = await import('../src/views.js');
  const text = forDiscord('![Shot](/files/3/a.png) [wiki](/wiki/combat) [out](https://x.com)');
  assert.match(text, /^\[Shot\]\(https:\/\/[^)]+\/files\/3\/a\.png\) \[wiki\]\(https:\/\/[^)]+\/wiki\/combat\) \[out\]\(https:\/\/x\.com\)$/);
  assert.equal(forDiscord('x'.repeat(2000), 1024).length, 1024);
});
