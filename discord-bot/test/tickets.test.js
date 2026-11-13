import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openStore } from '../src/store.js';
import { COMMANDS } from '../src/bot.js';
import {
  TICKETS,
  TICKET_KINDS,
  answersBackup,
  applicationEmbed,
  applicationModal,
  channelName,
  checkApplication,
  decisionModal,
  ticketButtons,
  ticketPanel,
} from '../src/tickets.js';

const GOOD = {
  server: { server: 'Bizarre SMP, play.example.net', invite: 'discord.gg/bizarre', modpack: 'Bizarre Pack 1.2', channels: 'https://discord.com/channels/1/2', extra: '' },
  dev: { portfolio: 'https://github.com/example', experience: '3', age: '19', time: '10 hours a week', specialization: 'Animation' },
  youtuber: { channel: 'youtube.com/@example', subscribers: '12.4k', plans: 'A survival series', example: '' },
  partner: { server: 'Crusaders Hub', invite: 'https://discord.gg/hub', members: '2,500', about: 'JoJo fans', why: '' },
};

test('every application form fits Discord\'s limits', () => {
  for (const kind of TICKET_KINDS) {
    const ticket = TICKETS[kind];
    assert.ok(ticket.fields.length >= 1 && ticket.fields.length <= 5, kind);
    for (const field of ticket.fields) {
      assert.ok(field.label.length <= 45, field.label);
      assert.ok(field.placeholder.length <= 100, field.placeholder);
      assert.ok(field.max <= 4000);
    }
    const modal = applicationModal(kind).toJSON();
    assert.equal(modal.custom_id, `ticket:submit:${kind}`);
    assert.ok(modal.title.length <= 45);
  }
  assert.equal(decisionModal(4, 'accepted').toJSON().custom_id, 'ticket:decide:4:accepted');
  const panel = ticketPanel();
  assert.deepEqual(panel.components[0].components.map((b) => b.custom_id), TICKET_KINDS.map((kind) => `ticket:open:${kind}`));
  assert.ok(panel.embeds[0].description.length <= 4096);
});

test('any answers pass as long as the required boxes are filled', () => {
  for (const kind of TICKET_KINDS) {
    assert.equal(checkApplication(kind, GOOD[kind]).error, undefined, kind);
  }
  const anything = checkApplication('youtuber', { channel: 'twitch.tv/me', subscribers: '300', plans: 'Shorts', example: '' });
  assert.equal(anything.error, undefined);
  assert.equal(anything.answers.subscribers, '300');
  assert.equal(checkApplication('partner', { ...GOOD.partner, invite: 'ask me', members: 'small' }).error, undefined);
  assert.equal(checkApplication('dev', { ...GOOD.dev, age: 'twenty' }).error, undefined);
  assert.equal(checkApplication('server', { ...GOOD.server, extra: '' }).error, undefined);
  assert.match(checkApplication('server', { ...GOOD.server, channels: '  ' }).error, /rules, IP and modpack/);
  assert.match(checkApplication('nope', {}).error, /no longer exists/);
});

test('the forms ask for no set numbers', () => {
  const text = JSON.stringify(TICKETS) + JSON.stringify(ticketPanel());
  assert.doesNotMatch(text, /5,000|5k|at least|minimum/i);
});

test('channel names are safe and say what they are for', () => {
  assert.equal(channelName('dev', 'Cool.User_99'), 'dev-cool-user-99');
  assert.equal(channelName('youtuber', '___'), 'yt-applicant');
  assert.ok(channelName('partner', 'x'.repeat(200)).length <= 90);
});

test('a rejected form gives the answers back', () => {
  const text = answersBackup('dev', { ...GOOD.dev, portfolio: 'a ``` b' });
  assert.match(text, /Portfolio:\na ''' b/);
  assert.ok(answersBackup('dev', { ...GOOD.dev, portfolio: 'x'.repeat(5000) }).length < 2000);
});

test('a ticket opens, is decided once, and closes', () => {
  const store = openStore(':memory:');
  try {
    const answers = checkApplication('dev', GOOD.dev).answers;
    const ticket = store.openTicket({ kind: 'dev', userId: 'u1', channelId: 'c1', answers }, 1000);
    assert.equal(ticket.status, 'open');
    assert.deepEqual(ticket.answers, answers);
    assert.equal(store.activeTicket('u1', 'dev').id, ticket.id);
    assert.equal(store.activeTicket('u1', 'partner'), null);
    assert.equal(store.ticketInChannel('c1').id, ticket.id);

    const denied = store.decideTicket(ticket.id, 'denied', 'staff', 'Not right now', 2000);
    assert.equal(denied.status, 'denied');
    assert.equal(denied.note, 'Not right now');
    assert.equal(store.decideTicket(ticket.id, 'accepted', 'staff2', '', 3000), null);

    assert.equal(store.closeTicket(ticket.id, 4000), true);
    assert.equal(store.closeTicket(ticket.id, 5000), false);
    assert.equal(store.activeTicket('u1', 'dev'), null);
    assert.equal(store.ticketInChannel('c1'), null);
    assert.equal(store.ticket(ticket.id).closedAt, 4000);

    const embed = applicationEmbed(denied);
    assert.equal(embed.fields.at(-2).value, 'Denied by <@staff>');
    assert.equal(embed.fields.at(-1).value, 'Not right now');
    assert.deepEqual(ticketButtons(denied)[0].components.map((b) => Boolean(b.disabled)), [true, true, false]);
  } finally {
    store.close();
  }
});

test('the ticket commands are registered', () => {
  const panel = COMMANDS.find((command) => command.name === 'panel');
  assert.ok(panel.options.some((sub) => sub.name === 'tickets'));
  const setup = COMMANDS.find((command) => command.name === 'setup');
  const tickets = setup.options.find((sub) => sub.name === 'tickets');
  assert.deepEqual(tickets.options.map((opt) => [opt.name, Boolean(opt.required)]), [['category', true], ['staff', true], ['log', false]]);
  assert.ok(setup.options.some((sub) => sub.name === 'ticketrole'));
  const boost = setup.options.find((sub) => sub.name === 'boostrole');
  assert.deepEqual(boost.options.map((opt) => [opt.name, Boolean(opt.required)]), [['role', false]]);
});
