import { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder, ModalBuilder, TextInputBuilder, TextInputStyle } from 'discord.js';

const COLOUR = 0x9a46e6;
const ACCEPTED_COLOUR = 0x3fb97a;
const DENIED_COLOUR = 0xd9534f;

const short = (id, label, placeholder, max = 100, required = true) => ({ id, label, placeholder, max, required, style: TextInputStyle.Short });
const long = (id, label, placeholder, max = 1000, required = true) => ({ id, label, placeholder, max, required, style: TextInputStyle.Paragraph });

export const TICKETS = {
  server: {
    name: 'Server Owner',
    prefix: 'server',
    blurb: 'Run a Minecraft server with the mod. Your Discord needs its own rules, IP and modpack channels.',
    fields: [
      short('server', 'Server name and IP', 'Minecraft Server, play.example.net'),
      short('invite', 'Discord invite', 'Discord Server invite link'),
      short('modpack', 'Modpack name, link and version', 'Bizarre Pack 1.2 on CurseForge, link'),
      long('channels', 'Links to your rules, IP and modpack channels', 'Right click each channel, Copy Link, and paste it here. All three are needed.'),
      long('extra', 'Anything else', 'Player count, how long the server has run, what makes it different', 1000, false),
    ],
  },
  dev: {
    name: 'Development Team',
    prefix: 'dev',
    blurb: 'Help build the mod. Show us your past work.',
    fields: [
      long('portfolio', 'Portfolio', 'Links to your past work: GitHub, ArtStation, Sketchfab, videos'),
      short('experience', 'Years of experience', 'How long you have been doing this', 100),
      short('age', 'Age', 'Your age', 20),
      short('time', 'Time you can give', '10 hours a week', 100),
      short('specialization', 'Specialization', 'Java, models, animation, textures, sounds, building'),
    ],
  },
  youtuber: {
    name: 'YouTuber',
    prefix: 'yt',
    blurb: 'Make videos about the mod.',
    fields: [
      short('channel', 'YouTube channel link', 'https://youtube.com/@yourchannel', 200),
      short('subscribers', 'Subscriber count', 'Your current subscriber count', 40),
      long('plans', 'What would you make about the mod?', 'Series, showcases, PvP, tutorials'),
      short('example', 'A video you are proud of (optional)', 'https://youtube.com/watch?v=...', 200, false),
    ],
  },
  partner: {
    name: 'Partner',
    prefix: 'partner',
    blurb: 'Partner your server with ours. If it interests us, we partner.',
    fields: [
      short('server', 'Server name', 'Discord Server'),
      short('invite', 'Discord invite', 'Discord Server invite link'),
      short('members', 'Member count', 'Your current member count', 40),
      long('about', 'What is your server about?', 'Who it is for and what happens there'),
      long('why', 'Why partner with us?', 'What both servers get out of it', 1000, false),
    ],
  },
};

export const TICKET_KINDS = Object.keys(TICKETS);

export function checkApplication(kind, raw) {
  const ticket = TICKETS[kind];
  if (!ticket) {
    return { error: 'That application type no longer exists.' };
  }
  const answers = {};
  for (const field of ticket.fields) {
    answers[field.id] = String(raw[field.id] ?? '').trim();
    if (field.required && !answers[field.id]) {
      return { error: `Fill in "${field.label}".` };
    }
  }
  return { answers };
}

export function channelName(kind, username) {
  const clean = String(username).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'applicant';
  return `${TICKETS[kind].prefix}-${clean}`.slice(0, 90);
}

const clip = (text, max) => (text.length > max ? text.slice(0, max - 1) + '...' : text);

export function ticketPanel() {
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setTitle('Applications')
    .setDescription([
      'Press a button below and fill in the form. A private channel opens where staff review your application and can ask you questions.',
      '',
      ...TICKET_KINDS.map((kind) => `**${TICKETS[kind].name}**\n${TICKETS[kind].blurb}`),
      '',
      'You can have one open application of each type.',
    ].join('\n'));
  const row = new ActionRowBuilder().addComponents(TICKET_KINDS.map((kind) => new ButtonBuilder()
    .setStyle(ButtonStyle.Secondary)
    .setCustomId(`ticket:open:${kind}`)
    .setLabel(TICKETS[kind].name)));
  return { embeds: [embed.toJSON()], components: [row.toJSON()] };
}

export function applicationModal(kind) {
  const ticket = TICKETS[kind];
  return new ModalBuilder()
    .setCustomId(`ticket:submit:${kind}`)
    .setTitle(`${ticket.name} application`)
    .addComponents(ticket.fields.map((field) => new ActionRowBuilder().addComponents(new TextInputBuilder()
      .setCustomId(field.id)
      .setLabel(field.label)
      .setPlaceholder(field.placeholder)
      .setStyle(field.style)
      .setRequired(field.required)
      .setMaxLength(field.max))));
}

export function decisionModal(id, outcome) {
  return new ModalBuilder()
    .setCustomId(`ticket:decide:${id}:${outcome}`)
    .setTitle(outcome === 'accepted' ? 'Accept application' : 'Deny application')
    .addComponents(new ActionRowBuilder().addComponents(new TextInputBuilder()
      .setCustomId('note')
      .setLabel('Message to the applicant (optional)')
      .setStyle(TextInputStyle.Paragraph)
      .setRequired(false)
      .setMaxLength(1000)));
}

const OUTCOME = { open: 'Waiting for review', accepted: 'Accepted', denied: 'Denied' };

export function applicationEmbed(ticket) {
  const kind = TICKETS[ticket.kind];
  const embed = new EmbedBuilder()
    .setColor(ticket.status === 'accepted' ? ACCEPTED_COLOUR : ticket.status === 'denied' ? DENIED_COLOUR : COLOUR)
    .setTitle(`${kind.name} application`)
    .setDescription(`From <@${ticket.userId}>`)
    .addFields(kind.fields.map((field) => ({ name: field.label, value: clip(ticket.answers[field.id] || '-', 1024) })))
    .addFields({ name: 'Status', value: ticket.status === 'open' ? OUTCOME.open : `${OUTCOME[ticket.status]} by <@${ticket.decidedBy}>` })
    .setFooter({ text: `Application #${ticket.id}` })
    .setTimestamp(ticket.createdAt);
  if (ticket.note) {
    embed.addFields({ name: 'Note', value: clip(ticket.note, 1024) });
  }
  return embed.toJSON();
}

export function ticketButtons(ticket) {
  const decided = ticket.status !== 'open';
  return [new ActionRowBuilder().addComponents(
    new ButtonBuilder().setStyle(ButtonStyle.Success).setCustomId(`ticket:accept:${ticket.id}`).setLabel('Accept').setDisabled(decided),
    new ButtonBuilder().setStyle(ButtonStyle.Danger).setCustomId(`ticket:deny:${ticket.id}`).setLabel('Deny').setDisabled(decided),
    new ButtonBuilder().setStyle(ButtonStyle.Secondary).setCustomId(`ticket:close:${ticket.id}`).setLabel('Close'),
  ).toJSON()];
}

export function decisionMessage(ticket, roleNote) {
  const name = TICKETS[ticket.kind].name;
  const lines = [ticket.status === 'accepted'
    ? `<@${ticket.userId}>, your ${name} application was **accepted**.`
    : `<@${ticket.userId}>, your ${name} application was **denied**.`];
  if (ticket.note) {
    lines.push('', ticket.note);
  }
  if (roleNote) {
    lines.push('', roleNote);
  }
  lines.push('', 'Staff will close this channel when you are done here.');
  return lines.join('\n');
}

export function directMessage(ticket, guildName) {
  const name = TICKETS[ticket.kind].name;
  const lines = [`Your ${name} application in ${guildName} was ${ticket.status === 'accepted' ? 'accepted' : 'denied'}.`];
  if (ticket.note) {
    lines.push('', ticket.note);
  }
  return lines.join('\n');
}

export function answersBackup(kind, raw) {
  const text = TICKETS[kind].fields
    .map((field) => `${field.label}:\n${String(raw[field.id] ?? '').trim() || '-'}`)
    .join('\n\n')
    .replace(/```/g, "'''");
  return `Your answers, so you don't have to type them again:\n\`\`\`\n${clip(text, 1600)}\n\`\`\``;
}
