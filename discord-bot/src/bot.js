import {
  ActionRowBuilder,
  ApplicationCommandType,
  ButtonBuilder,
  ButtonStyle,
  ChannelType,
  Client,
  ContextMenuCommandBuilder,
  Events,
  GatewayIntentBits,
  MessageFlags,
  ModalBuilder,
  PermissionFlagsBits,
  SlashCommandBuilder,
  TextInputBuilder,
  TextInputStyle,
} from 'discord.js';
import { PERK_TIER, PRESETS, REWARDS, SUPPORTER_AURA, SUPPORTER_BUILD, TIERS, formatColor, parseColor, rewardLabel, roleKeys } from './rewards.js';
import { checkEntry, findEntry, httpsUrl, searchEntries } from './faq.js';
import { lookupProfile } from './mojang.js';
import { fileURLToPath } from 'node:url';
import { checkPage, findPage, formatSections, searchPages } from './guide.js';
import { KINDS, MAX_BYTES, STANDS, checkAnimations, checkAura, checkModel, checkSlug, checkTexture, hex, standLabel } from './skins.js';
import { LOGO, READ_BUTTON, faqAnswer, faqPanel, faqSelect, faqReminder, guidePage, guideSelect, perksPanel, questionsNotice, status, welcome } from './views.js';
import {
  TICKETS,
  TICKET_KINDS,
  answersBackup,
  applicationEmbed,
  applicationModal,
  channelName,
  checkApplication,
  decisionMessage,
  decisionModal,
  directMessage,
  ticketButtons,
  ticketPanel,
} from './tickets.js';

const CODE_TTL_MS = 10 * 60 * 1000;
const WELCOME_COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;
const ANSWER_COMMAND = 'Answer with FAQ';
const GATE_ROLE_NAME = 'Read the FAQ';
const LOCKED = ['SendMessages', 'SendMessagesInThreads', 'CreatePublicThreads', 'CreatePrivateThreads'];
const GATE_PERMISSIONS = [PermissionFlagsBits.ManageRoles, PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ManageMessages];
const TICKET_PERMISSIONS = [PermissionFlagsBits.ManageRoles, PermissionFlagsBits.ManageChannels];
const REMIND_COOLDOWN_MS = 30 * 1000;
const REMINDER_LIFETIME_MS = 15 * 1000;
const TICKET_ACCESS = [
  PermissionFlagsBits.ViewChannel,
  PermissionFlagsBits.SendMessages,
  PermissionFlagsBits.ReadMessageHistory,
  PermissionFlagsBits.AttachFiles,
  PermissionFlagsBits.EmbedLinks,
];
const CLOSE_DELAY_MS = 5000;
const ticketChoices = TICKET_KINDS.map((kind) => ({ name: TICKETS[kind].name, value: kind }));
const LOGO_FILE = { attachment: fileURLToPath(new URL(`./${LOGO}`, import.meta.url)), name: LOGO };

const rewardChoices = REWARDS.map((reward) => ({ name: reward.label, value: reward.id }));
const standChoices = STANDS.map(([value, name]) => ({ name, value }));
const skinOption = (opt) => opt.setName('id').setDescription('The skin id').setRequired(true).setAutocomplete(true);

const pageOption = (opt) => opt.setName('page').setDescription('The page').setRequired(true).setAutocomplete(true);

const topicOption = (opt) => opt.setName('topic').setDescription('The question').setRequired(true).setAutocomplete(true);

export const COMMANDS = [
  new SlashCommandBuilder()
    .setName('link')
    .setDescription('Get a code to link your Minecraft account for supporter perks'),
  new SlashCommandBuilder()
    .setName('unlink')
    .setDescription('Unlink your Minecraft account'),
  new SlashCommandBuilder()
    .setName('whitelist')
    .setDescription('Whitelist your Minecraft account for the supporter build')
    .addStringOption((opt) => opt.setName('name').setDescription('Your Minecraft username').setRequired(true)
      .setMinLength(3).setMaxLength(16)),
  new SlashCommandBuilder()
    .setName('perks')
    .setDescription('Show what supporters get, your linked account and your perks'),
  new SlashCommandBuilder()
    .setName('aura')
    .setDescription('Choose your Stand aura color')
    .addSubcommand((sub) => sub
      .setName('set')
      .setDescription('Set your Stand aura color')
      .addStringOption((opt) => opt
        .setName('color')
        .setDescription('A preset such as rainbow or gold, or a hex color like #FF8800')
        .setRequired(true)
        .setAutocomplete(true)))
    .addSubcommand((sub) => sub
      .setName('reset')
      .setDescription('Go back to your skin\'s own aura color')),
  new SlashCommandBuilder()
    .setName('faq')
    .setDescription('Answer a common question about the mod')
    .addStringOption(topicOption),
  new SlashCommandBuilder()
    .setName('guide')
    .setDescription('Show a page of the mod guide, such as how to get a Stand')
    .addStringOption(pageOption),
  new SlashCommandBuilder()
    .setName('guideedit')
    .setDescription('Add, change or remove mod guide pages')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Add a guide page'))
    .addSubcommand((sub) => sub
      .setName('edit')
      .setDescription('Change a guide page')
      .addStringOption(pageOption))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Remove a guide page')
      .addStringOption(pageOption)),
  new SlashCommandBuilder()
    .setName('faqedit')
    .setDescription('Add, change or remove FAQ answers')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Add a question'))
    .addSubcommand((sub) => sub
      .setName('edit')
      .setDescription('Change a question or its answer')
      .addStringOption(topicOption))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Remove a question')
      .addStringOption(topicOption)),
  new SlashCommandBuilder()
    .setName('panel')
    .setDescription('Post a panel in this channel')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('faq')
      .setDescription('Post the FAQ panel here'))
    .addSubcommand((sub) => sub
      .setName('perks')
      .setDescription('Post the supporter benefits panel here'))
    .addSubcommand((sub) => sub
      .setName('tickets')
      .setDescription('Post the applications panel here')),
  new SlashCommandBuilder()
    .setName('setup')
    .setDescription('Set the bot\'s links and channels')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('kofi')
      .setDescription('Set the Ko-fi page the support buttons open')
      .addStringOption((opt) => opt.setName('url').setDescription('An https link').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('earlyaccess')
      .setDescription('Set the Ko-fi post that holds the early access builds')
      .addStringOption((opt) => opt.setName('url').setDescription('An https link').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('welcome')
      .setDescription('Choose the channel where new supporters are welcomed')
      .addChannelOption((opt) => opt.setName('channel').setDescription('The channel').setRequired(true)
        .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)))
    .addSubcommand((sub) => sub
      .setName('questions')
      .setDescription('Only let members post in a questions channel after they read the FAQ')
      .addChannelOption((opt) => opt.setName('channel').setDescription('The questions channel').setRequired(true)
        .addChannelTypes(ChannelType.GuildText, ChannelType.GuildForum))
      .addRoleOption((opt) => opt.setName('role').setDescription(`The role given for reading the FAQ. Leave empty to use "${GATE_ROLE_NAME}"`)))
    .addSubcommand((sub) => sub
      .setName('unlockquestions')
      .setDescription('Let anyone post in the questions channel again'))
    .addSubcommand((sub) => sub
      .setName('tickets')
      .setDescription('Set where applications open and who reviews them')
      .addChannelOption((opt) => opt.setName('category').setDescription('The category new application channels go in').setRequired(true)
        .addChannelTypes(ChannelType.GuildCategory))
      .addRoleOption((opt) => opt.setName('staff').setDescription('The role that sees and reviews applications').setRequired(true))
      .addChannelOption((opt) => opt.setName('log').setDescription('A staff channel that gets a record of each closed application')
        .addChannelTypes(ChannelType.GuildText)))
    .addSubcommand((sub) => sub
      .setName('ticketrole')
      .setDescription('Give a role when an application is accepted. Leave the role empty to stop')
      .addStringOption((opt) => opt.setName('type').setDescription('The application type').setRequired(true).addChoices(...ticketChoices))
      .addRoleOption((opt) => opt.setName('role').setDescription('The role to give')))
    .addSubcommand((sub) => sub
      .setName('boostrole')
      .setDescription('Give a role to members only while they boost the server. Leave the role empty to stop')
      .addRoleOption((opt) => opt.setName('role').setDescription('The role to give boosters')))
    .addSubcommand((sub) => sub
      .setName('show')
      .setDescription('Show the current settings')),
  new SlashCommandBuilder()
    .setName('reward')
    .setDescription('Map Discord roles to supporter rewards')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Give a reward to everyone with a role')
      .addRoleOption((opt) => opt.setName('role').setDescription('The role').setRequired(true))
      .addStringOption((opt) => opt.setName('reward').setDescription('The reward').setRequired(true)
        .addChoices(...rewardChoices)))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Stop a role giving a reward')
      .addRoleOption((opt) => opt.setName('role').setDescription('The role').setRequired(true))
      .addStringOption((opt) => opt.setName('reward').setDescription('The reward').setRequired(true)
        .addChoices(...rewardChoices)))
    .addSubcommand((sub) => sub
      .setName('list')
      .setDescription('List which roles give which rewards')),
  new SlashCommandBuilder()
    .setName('skin')
    .setDescription('Manage supporter Stand skins')
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
    .setDMPermission(false)
    .addSubcommand((sub) => sub
      .setName('upload')
      .setDescription('Add a supporter skin, or update one with the same id')
      .addStringOption((opt) => opt.setName('id').setDescription('Lowercase id, e.g. the_world_sukuna').setRequired(true).setMaxLength(40))
      .addStringOption((opt) => opt.setName('stand').setDescription('The Stand it is for').setRequired(true).addChoices(...standChoices))
      .addStringOption((opt) => opt.setName('name').setDescription('The name shown in game').setRequired(true).setMaxLength(40))
      .addAttachmentOption((opt) => opt.setName('texture').setDescription('The skin texture (.png)').setRequired(true))
      .addAttachmentOption((opt) => opt.setName('model').setDescription('Its own model (.geo.json). Leave empty to use the Stand\'s model'))
      .addAttachmentOption((opt) => opt.setName('animations').setDescription('Its own clips (.animation.json). Leave empty to use the Stand\'s clips'))
      .addStringOption((opt) => opt.setName('aura').setDescription('Aura color, e.g. #B3121B. With aura2 it is the color at the feet'))
      .addStringOption((opt) => opt.setName('aura2').setDescription('Second aura color at the top, e.g. #000000'))
      .addBooleanOption((opt) => opt.setName('replace_all').setDescription('Drop an earlier model or animations file that you did not attach this time'))
      .addBooleanOption((opt) => opt.setName('allow_missing_bones').setDescription('Accept a model without the shared humanoid bones')))
    .addSubcommand((sub) => sub
      .setName('aura')
      .setDescription('Set a skin\'s preset aura colors. Leave both empty to use the Stand\'s own')
      .addStringOption(skinOption)
      .addStringOption((opt) => opt.setName('aura').setDescription('Aura color, e.g. #B3121B. With aura2 it is the color at the feet'))
      .addStringOption((opt) => opt.setName('aura2').setDescription('Second aura color at the top, e.g. #000000')))
    .addSubcommand((sub) => sub
      .setName('list')
      .setDescription('List the supporter skins'))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Remove a supporter skin. Anyone wearing it goes back to the default')
      .addStringOption(skinOption)),
  new ContextMenuCommandBuilder()
    .setName(ANSWER_COMMAND)
    .setType(ApplicationCommandType.Message)
    .setDefaultMemberPermissions(PermissionFlagsBits.ManageMessages)
    .setDMPermission(false),
].map((command) => command.toJSON());

export function entryModal(entry) {
  const field = (id, label, style, value, required, max) => {
    const input = new TextInputBuilder().setCustomId(id).setLabel(label).setStyle(style).setRequired(required).setMaxLength(max);
    if (value) {
      input.setValue(value);
    }
    return new ActionRowBuilder().addComponents(input);
  };
  const modal = new ModalBuilder()
    .setCustomId(`faq:edit:${entry ? entry.id : 'new'}`)
    .setTitle(entry ? 'Edit question' : 'Add question');
  if (!entry) {
    modal.addComponents(field('id', 'Short id, such as public-beta', TextInputStyle.Short, '', true, 32));
  }
  modal.addComponents(
    field('group', 'Group, such as Release or Gameplay', TextInputStyle.Short, entry?.group, true, 40),
    field('question', 'Question', TextInputStyle.Short, entry?.question, true, 100),
    field('answer', 'Answer', TextInputStyle.Paragraph, entry?.answer, true, 2000),
    field('related', 'Follow-up ids (kofi adds the button)', TextInputStyle.Short, entry?.related.join(', '), false, 200),
  );
  return modal;
}

export function pageModal(page) {
  const field = (id, label, style, value, max) => {
    const input = new TextInputBuilder().setCustomId(id).setLabel(label).setStyle(style).setRequired(true).setMaxLength(max);
    if (value) {
      input.setValue(value);
    }
    return new ActionRowBuilder().addComponents(input);
  };
  const modal = new ModalBuilder()
    .setCustomId(`guide:edit:${page ? page.id : 'new'}`)
    .setTitle(page ? 'Edit guide page' : 'Add guide page');
  if (!page) {
    modal.addComponents(field('id', 'Short id, such as pillar-man', TextInputStyle.Short, '', 32));
  }
  modal.addComponents(
    field('title', 'Title', TextInputStyle.Short, page?.title, 100),
    field('summary', 'One-line summary for the menu', TextInputStyle.Short, page?.summary, 100),
    field('intro', 'Intro', TextInputStyle.Paragraph, page?.intro, 1000),
    field('sections', 'Sections: start each with ## Heading', TextInputStyle.Paragraph, page ? formatSections(page.sections) : '', 4000),
  );
  return modal;
}

export function createBot({ token, guildId, store }) {
  const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers, GatewayIntentBits.GuildMessages] });

  const guild = () => client.guilds.cache.get(guildId) ?? null;

  const rolesOf = async (discordId) => {
    const g = guild();
    if (!g) {
      return null;
    }
    try {
      const member = await g.members.fetch(discordId);
      return roleKeys(member.roles.cache.values());
    } catch {
      return [];
    }
  };

  const syncBooster = async (member) => {
    const roleId = store.setting('boost_role');
    if (!roleId || member.user.bot) {
      return false;
    }
    const boosting = Boolean(member.premiumSinceTimestamp);
    const has = member.roles.cache.has(roleId);
    if (boosting && !has) {
      await member.roles.add(roleId, 'Boosting the server');
      return true;
    }
    if (!boosting && has) {
      await member.roles.remove(roleId, 'Stopped boosting the server');
      return true;
    }
    return false;
  };

  const syncBoosters = async (g) => {
    if (!store.setting('boost_role')) {
      return 0;
    }
    let changed = 0;
    for (const member of g.members.cache.values()) {
      const done = await syncBooster(member).catch((error) => {
        console.error('booster role failed', error);
        return false;
      });
      changed += done ? 1 : 0;
    }
    return changed;
  };

  const canManage = async (discordId) => {
    const g = guild();
    if (!g || !discordId) {
      return false;
    }
    try {
      const member = await g.members.fetch(discordId);
      return member.permissions.has(PermissionFlagsBits.ManageGuild);
    } catch {
      return false;
    }
  };

  const nameOf = async (discordId) => {
    try {
      const user = await client.users.fetch(discordId);
      return user.globalName ?? user.username;
    } catch {
      return null;
    }
  };

  const reply = (interaction, content) =>
    interaction.reply({ content, flags: MessageFlags.Ephemeral, allowedMentions: { parse: [] } });

  const download = async (attachment, kind) => {
    if (!attachment) {
      return { data: null };
    }
    if (attachment.size > MAX_BYTES[kind]) {
      return { error: `The ${kind} file is too large.` };
    }
    const response = await fetch(attachment.url);
    if (!response.ok) {
      return { error: `I couldn't download the ${kind} file. Please try again.` };
    }
    return { data: Buffer.from(await response.arrayBuffer()) };
  };

  const describeSkin = (skin) => {
    const kinds = skin.files.map((file) => file.kind);
    const extras = [kinds.includes('model') && 'own model', kinds.includes('animations') && 'own clips'].filter(Boolean);
    const aura = skin.aura === null || skin.aura === undefined ? null
      : skin.aura2 === null || skin.aura2 === undefined ? `aura ${hex(skin.aura)}` : `aura ${hex(skin.aura)} to ${hex(skin.aura2)}`;
    return `**${skin.name}** (\`${skin.slug}\`) for ${standLabel(skin.stand)}, version ${skin.version}`
      + (extras.length > 0 ? `, ${extras.join(', ')}` : '') + (aura ? `, ${aura}` : '');
  };

  const whisper = (interaction, payload) =>
    interaction.reply({ ...payload, flags: MessageFlags.Ephemeral, allowedMentions: { parse: [] } });

  const settings = () => ({
    kofi: store.setting('kofi_url'),
    earlyAccess: store.setting('early_access_url'),
    questions: store.setting('questions_channel'),
  });

  const faqUrl = () => {
    const placed = store.setting('panel_faq');
    if (!placed) {
      return null;
    }
    const [channelId, messageId] = placed.split(':');
    return `https://discord.com/channels/${guildId}/${channelId}/${messageId}`;
  };

  const gateRole = async (g, chosen) => {
    if (chosen) {
      return chosen;
    }
    const saved = store.setting('questions_role');
    const existing = saved ? await g.roles.fetch(saved).catch(() => null) : null;
    return existing ?? g.roles.create({ name: GATE_ROLE_NAME, permissions: [], mentionable: false, reason: 'Lets members post in the questions channel' });
  };

  const removeOldLock = async (g) => {
    const channelId = store.setting('questions_channel');
    if (!channelId || store.setting('questions_mode') === 'filter') {
      return true;
    }
    try {
      const channel = await client.channels.fetch(channelId);
      await channel.permissionOverwrites.edit(g.roles.everyone,
        Object.fromEntries(LOCKED.map((flag) => [flag, null])), { reason: 'Old FAQ lock removed' });
      const roleId = store.setting('questions_role');
      if (roleId) {
        await channel.permissionOverwrites.delete(roleId, 'Old FAQ lock removed').catch(() => {});
      }
      return true;
    } catch {
      return false;
    }
  };

  const gateQuestions = async (interaction) => {
    const g = interaction.guild;
    const channel = interaction.options.getChannel('channel', true);
    const me = g.members.me ?? await g.members.fetchMe();
    if (!me.permissions.has(GATE_PERMISSIONS)) {
      await reply(interaction, 'I need the **Manage Roles**, **Manage Channels** and **Manage Messages** permissions for this. '
        + 'In Server Settings → Roles, give them to my role, then run this again.');
      return;
    }
    if (channel.type === ChannelType.GuildForum && !me.permissions.has(PermissionFlagsBits.ManageThreads)) {
      await reply(interaction, 'For a forum I also need **Manage Threads**, so I can remove posts. Give it to my role, then run this again.');
      return;
    }
    const chosen = interaction.options.getRole('role');
    if (chosen && (!chosen.editable || chosen.managed || chosen.id === g.id)) {
      await reply(interaction, `I can't give out <@&${chosen.id}>. Pick a normal role below my own role in Server Settings → Roles.`);
      return;
    }
    await interaction.deferReply({ flags: MessageFlags.Ephemeral });
    const unlocked = await removeOldLock(g);
    const role = await gateRole(g, chosen);
    await channel.permissionOverwrites.edit(me, { ViewChannel: true, SendMessages: true, EmbedLinks: true, ReadMessageHistory: true },
      { reason: 'Lets the bot post FAQ reminders' });
    store.setSetting('questions_channel', channel.id);
    store.setSetting('questions_role', role.id);
    store.setSetting('questions_mode', 'filter');
    let noticed;
    if (channel.type === ChannelType.GuildText) {
      noticed = await channel.send(questionsNotice(faqUrl())).then(
        (message) => message.pin().then(() => ' I posted and pinned a note there.', () => ' I posted a note there. Pin it so it stays at the top.'),
        () => ' I couldn\'t post a note there.');
    } else {
      noticed = ' Add a line to the forum\'s post guidelines telling members to read the FAQ first.';
    }
    const refreshed = await refreshPanel('faq');
    await interaction.editReply({
      content: `Members can type in <#${channel.id}>, but I remove posts from anyone without <@&${role.id}> and point them to the FAQ. `
        + `The **${READ_BUTTON}** button on the FAQ panel gives the role. Staff with Manage Messages can always post.`
        + noticed + (unlocked ? '' : ' I couldn\'t remove the old Send Messages lock from the previous questions channel, so remove it there by hand.')
        + (refreshed === null ? ' Post the FAQ panel with `/panel faq` so members can find the button.' : panelNote(refreshed, 'faq')),
      allowedMentions: { parse: [] },
    });
  };

  const ungateQuestions = async (interaction) => {
    const channelId = store.setting('questions_channel');
    if (!channelId) {
      await reply(interaction, 'No questions channel needs the FAQ right now.');
      return;
    }
    await interaction.deferReply({ flags: MessageFlags.Ephemeral });
    const unlocked = await removeOldLock(interaction.guild);
    store.setSetting('questions_channel', null);
    store.setSetting('questions_mode', null);
    const refreshed = await refreshPanel('faq');
    await interaction.editReply(`Anyone can post in <#${channelId}> again. The role is kept; delete it in Server Settings if you no longer want it.`
      + (unlocked ? '' : ' I couldn\'t change the channel\'s permissions, so remove the Send Messages lock there by hand.')
      + panelNote(refreshed, 'faq'));
  };

  const reminded = new Map();

  const filterQuestion = async (message) => {
    const channelId = store.setting('questions_channel');
    const roleId = store.setting('questions_role');
    if (!channelId || !roleId || store.setting('questions_mode') !== 'filter') {
      return;
    }
    if (!message.inGuild() || message.guildId !== guildId || message.author.bot || message.system) {
      return;
    }
    const thread = message.channel.isThread() ? message.channel : null;
    if (message.channelId !== channelId && thread?.parentId !== channelId) {
      return;
    }
    const member = message.member ?? await message.guild.members.fetch(message.author.id).catch(() => null);
    if (!member || member.roles.cache.has(roleId) || member.permissions.has(PermissionFlagsBits.ManageMessages)) {
      return;
    }
    const starter = Boolean(thread) && message.id === thread.id;
    const removed = starter
      ? await thread.delete('Posted before reading the FAQ').then(() => true, () => false)
      : await message.delete().then(() => true, () => false);
    if (!removed) {
      console.error(`Couldn't remove a post in the questions channel. Give the bot Manage Messages${thread ? ' and Manage Threads' : ''} there.`);
      return;
    }
    const now = Date.now();
    if (now - (reminded.get(message.author.id) ?? 0) < REMIND_COOLDOWN_MS) {
      return;
    }
    if (reminded.size > 5000) {
      reminded.clear();
    }
    reminded.set(message.author.id, now);
    const payload = { ...faqReminder(message.author.id, faqUrl()), allowedMentions: { users: [message.author.id] } };
    if (starter) {
      await message.author.send(payload).catch(() => {});
      return;
    }
    const sent = await message.channel.send(payload).catch(() => null);
    if (sent) {
      setTimeout(() => sent.delete().catch(() => {}), REMINDER_LIFETIME_MS);
    }
  };

  const panels = {
    faq: () => faqPanel(store.faq(), settings(), store.guide()),
    perks: () => perksPanel(settings()),
    tickets: () => ticketPanel(),
  };

  const ticketSetup = () => {
    const category = store.setting('ticket_category');
    const staff = store.setting('ticket_staff');
    return category && staff ? { category, staff, log: store.setting('ticket_log') } : null;
  };

  const isStaff = (member) => Boolean(member?.permissions?.has?.(PermissionFlagsBits.ManageGuild)
    || member?.roles?.cache?.has(store.setting('ticket_staff')));

  const ticketBlock = async (userId, kind) => {
    const active = store.activeTicket(userId, kind);
    if (active) {
      const channel = await client.channels.fetch(active.channelId).catch(() => null);
      if (channel) {
        return `You already have an open ${TICKETS[kind].name} application in <#${channel.id}>.`;
      }
      store.closeTicket(active.id);
    }
    return null;
  };

  const openTicketFor = async (interaction, ticket) => {
    if (!ticket) {
      await reply(interaction, 'That application is no longer here.');
      return null;
    }
    if (ticket.closedAt) {
      await reply(interaction, 'That application is already closed.');
      return null;
    }
    return ticket;
  };

  const logTicket = async (ticket, closedBy, withdrawn) => {
    const logId = store.setting('ticket_log');
    if (!logId) {
      return;
    }
    try {
      const channel = await client.channels.fetch(logId);
      await channel.send({
        content: `${withdrawn ? 'Withdrawn' : 'Closed'} by <@${closedBy}>`,
        embeds: [applicationEmbed(ticket)],
        allowedMentions: { parse: [] },
      });
    } catch (error) {
      console.error('ticket log failed', error);
    }
  };

  const refreshPanel = async (name) => {
    const placed = store.setting(`panel_${name}`);
    if (!placed) {
      return null;
    }
    const [channelId, messageId] = placed.split(':');
    try {
      const channel = await client.channels.fetch(channelId);
      await channel.messages.edit(messageId, name === 'faq'
        ? { ...panels[name](), files: [LOGO_FILE], attachments: [] }
        : panels[name]());
      return true;
    } catch {
      return false;
    }
  };

  const panelNote = (refreshed, name) => {
    if (refreshed === null) {
      return '';
    }
    return refreshed ? ' The panel is updated.' : ` I couldn't update the panel, so post it again with \`/panel ${name}\`.`;
  };

  const linkLines = (discordId) => {
    const current = store.linkOf(discordId);
    const code = store.issueCode(discordId, CODE_TTL_MS);
    const lines = [
      `Your link code is **${code}**`,
      `In Minecraft, run \`/supporter link ${code}\` within 10 minutes.`,
    ];
    if (current) {
      lines.push(`This replaces your current link to \`${current.mc_name}\`.`);
    }
    return lines.join('\n');
  };

  const statusOf = async (discordId) => {
    const rewards = store.rewardsForRoles((await rolesOf(discordId)) ?? []);
    return status({
      link: store.linkOf(discordId),
      rewards,
      aura: rewards.includes(SUPPORTER_AURA) ? store.auraOf(discordId) : null,
    }, settings());
  };

  const handlers = {
    async link(interaction) {
      await reply(interaction, linkLines(interaction.user.id));
    },

    async unlink(interaction) {
      const removed = store.unlink(interaction.user.id);
      await reply(interaction, removed ? 'Your Minecraft account is unlinked.' : 'You have no linked Minecraft account.');
    },

    async whitelist(interaction) {
      const rewards = store.rewardsForRoles((await rolesOf(interaction.user.id)) ?? []);
      if (!rewards.includes(SUPPORTER_BUILD)) {
        await reply(interaction, 'The supporter build is for Ko-fi supporters. Run `/perks` to see what supporters get.');
        return;
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const profile = await lookupProfile(interaction.options.getString('name', true).trim());
      if (profile.error) {
        await interaction.editReply(profile.error === 'unreachable'
          ? 'I couldn\'t reach Minecraft\'s account service. Please try again in a minute.'
          : 'I couldn\'t find a Minecraft account with that name. Check the spelling.');
        return;
      }
      const owner = store.linkByUuid(profile.uuid);
      if (owner && owner.discord_id !== interaction.user.id) {
        await interaction.editReply(`\`${profile.name}\` is already whitelisted by another member. Ask staff if this is your account.`);
        return;
      }
      const current = store.linkOf(interaction.user.id);
      store.link(interaction.user.id, profile.uuid, profile.name);
      const replaced = current && current.mc_uuid !== profile.uuid ? ` This replaces \`${current.mc_name}\`.` : '';
      await interaction.editReply(`\`${profile.name}\` is whitelisted. Start the supporter build, or press Retry on its lock screen.${replaced}`);
    },

    async perks(interaction) {
      await whisper(interaction, await statusOf(interaction.user.id));
    },

    async aura(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'reset') {
        store.clearAura(interaction.user.id);
        await reply(interaction, 'Your aura is back to your skin\'s own color.');
        return;
      }
      const rewards = store.rewardsForRoles((await rolesOf(interaction.user.id)) ?? []);
      if (!rewards.includes(SUPPORTER_AURA)) {
        await reply(interaction, 'A custom aura color is a supporter perk. Run `/perks` to see what supporters get.');
        return;
      }
      const colour = parseColor(interaction.options.getString('color', true));
      if (colour === null) {
        await reply(interaction, 'Use a preset (' + Object.keys(PRESETS).join(', ') + ') or a hex color like `#FF8800`.');
        return;
      }
      store.setAura(interaction.user.id, colour);
      const linked = store.linkOf(interaction.user.id);
      await reply(interaction, `Aura color set to ${formatColor(colour)}.`
        + (linked ? '' : ' Link your Minecraft account with `/link` to see it in game.'));
    },

    async faq(interaction) {
      const entries = store.faq();
      const entry = findEntry(entries, interaction.options.getString('topic', true));
      if (!entry) {
        await reply(interaction, 'I don\'t have that question. Pick one from the list that appears as you type.');
        return;
      }
      await interaction.reply({ ...faqAnswer(entry, entries, settings()), allowedMentions: { parse: [] } });
    },

    async guide(interaction) {
      const pages = store.guide();
      const page = findPage(interaction.options.getString('page', true), pages);
      if (!page) {
        await reply(interaction, 'I don\'t have that page. Pick one from the list that appears as you type.');
        return;
      }
      await interaction.reply({ ...guidePage(page, pages), allowedMentions: { parse: [] } });
    },

    async guideedit(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'add') {
        await interaction.showModal(pageModal(null));
        return;
      }
      const pages = store.guide();
      const page = findPage(interaction.options.getString('page', true), pages);
      if (!page) {
        await reply(interaction, 'I don\'t have that page.');
        return;
      }
      if (sub === 'edit') {
        await interaction.showModal(pageModal(page));
        return;
      }
      if (pages.length === 1) {
        await reply(interaction, 'That is the last guide page, so it can\'t be removed.');
        return;
      }
      store.removeGuide(page.id);
      await reply(interaction, `Removed "${page.title}".` + panelNote(await refreshPanel('faq'), 'faq'));
    },

    async faqedit(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'add') {
        await interaction.showModal(entryModal(null));
        return;
      }
      const entry = findEntry(store.faq(), interaction.options.getString('topic', true));
      if (!entry) {
        await reply(interaction, 'I don\'t have that question.');
        return;
      }
      if (sub === 'edit') {
        await interaction.showModal(entryModal(entry));
        return;
      }
      store.removeFaq(entry.id);
      await reply(interaction, `Removed "${entry.question}".` + panelNote(await refreshPanel('faq'), 'faq'));
    },

    async panel(interaction) {
      const name = interaction.options.getSubcommand();
      const payload = name === 'faq' ? { ...panels[name](), files: [LOGO_FILE] } : panels[name]();
      let message = null;
      try {
        message = await interaction.channel.send(payload);
      } catch {
        await interaction.reply(payload);
        message = await interaction.fetchReply();
      }
      if (!interaction.replied) {
        await reply(interaction, 'Posted. Pin it so it stays easy to find.');
      }
      store.setSetting(`panel_${name}`, `${message.channelId}:${message.id}`);
    },

    async setup(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'show') {
        const channel = store.setting('welcome_channel');
        const questions = store.setting('questions_channel');
        const tickets = ticketSetup();
        await reply(interaction, [
          `Ko-fi page: ${store.setting('kofi_url') ?? 'not set'}`,
          `Early access post: ${store.setting('early_access_url') ?? 'not set'}`,
          `Welcome channel: ${channel ? `<#${channel}>` : 'not set, so new supporters get a direct message'}`,
          `Questions channel: ${questions ? `<#${questions}>, posting needs <@&${store.setting('questions_role')}>` : 'anyone can post'}`,
          `Applications: ${tickets ? `open in <#${tickets.category}>, reviewed by <@&${tickets.staff}>, ${tickets.log ? `logged in <#${tickets.log}>` : 'not logged'}` : 'not set up'}`,
          ...TICKET_KINDS.map((kind) => {
            const role = store.setting(`ticket_role_${kind}`);
            return `Accepted ${TICKETS[kind].name}: ${role ? `gets <@&${role}>` : 'no role'}`;
          }),
          `Boosters: ${store.setting('boost_role') ? `get <@&${store.setting('boost_role')}> while boosting` : 'no role'}`,
        ].join('\n'));
        return;
      }
      if (sub === 'tickets') {
        const g = interaction.guild;
        const me = g.members.me ?? await g.members.fetchMe();
        if (!me.permissions.has(TICKET_PERMISSIONS)) {
          await reply(interaction, 'I need the **Manage Roles** and **Manage Channels** permissions to open application channels. '
            + 'In Server Settings → Roles, give them to my role, then run this again.');
          return;
        }
        const category = interaction.options.getChannel('category', true);
        const staff = interaction.options.getRole('staff', true);
        const log = interaction.options.getChannel('log');
        if (log && !log.permissionsFor?.(me)?.has([PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks])) {
          await reply(interaction, `I can't post in <#${log.id}>. Give me View Channel, Send Messages and Embed Links there, then try again.`);
          return;
        }
        store.setSetting('ticket_category', category.id);
        store.setSetting('ticket_staff', staff.id);
        store.setSetting('ticket_log', log?.id ?? null);
        await reply(interaction, [
          `Applications open as private channels in **${category.name}**. Only the applicant, <@&${staff.id}> and admins can see them.`,
          log ? `Closed applications are recorded in <#${log.id}>.` : 'Closed applications are not recorded. Add a `log` channel to keep a record.',
          'Post the panel with `/panel tickets`. To give a role on acceptance, use `/setup ticketrole`.',
        ].join('\n'));
        return;
      }
      if (sub === 'ticketrole') {
        const kind = interaction.options.getString('type', true);
        const role = interaction.options.getRole('role');
        if (role && (!role.editable || role.managed || role.id === interaction.guild.id)) {
          await reply(interaction, `I can't give out <@&${role.id}>. Pick a normal role below my own role in Server Settings → Roles.`);
          return;
        }
        store.setSetting(`ticket_role_${kind}`, role?.id ?? null);
        await reply(interaction, role
          ? `Accepted ${TICKETS[kind].name} applicants now get <@&${role.id}>.`
          : `Accepted ${TICKETS[kind].name} applicants no longer get a role.`);
        return;
      }
      if (sub === 'boostrole') {
        const role = interaction.options.getRole('role');
        if (role && (!role.editable || role.managed || role.id === interaction.guild.id)) {
          await reply(interaction, `I can't give out <@&${role.id}>. Pick a normal role below my own role in Server Settings → Roles. `
            + 'Discord\'s own Server Booster role can\'t be used here.');
          return;
        }
        if (!role) {
          store.setSetting('boost_role', null);
          await reply(interaction, 'I no longer manage a booster role. Members keep whatever role they have now.');
          return;
        }
        await interaction.deferReply({ flags: MessageFlags.Ephemeral });
        store.setSetting('boost_role', role.id);
        const changed = await syncBoosters(interaction.guild);
        await interaction.editReply(`Members get <@&${role.id}> while they boost and lose it when they stop. `
          + `Updated just now: ${changed === 1 ? 'one member' : `${changed} members`}.`);
        return;
      }
      if (sub === 'questions') {
        await gateQuestions(interaction);
        return;
      }
      if (sub === 'unlockquestions') {
        await ungateQuestions(interaction);
        return;
      }
      if (sub === 'welcome') {
        const channel = interaction.options.getChannel('channel', true);
        const allowed = channel.permissionsFor?.(client.user)?.has([
          PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.EmbedLinks]);
        if (!allowed) {
          await reply(interaction, `I can't post in <#${channel.id}>. Give me View Channel, Send Messages and Embed Links there, then try again.`);
          return;
        }
        store.setSetting('welcome_channel', channel.id);
        await reply(interaction, `New supporters will be welcomed in <#${channel.id}>.`);
        return;
      }
      const url = httpsUrl(interaction.options.getString('url', true));
      if (!url) {
        await reply(interaction, 'That isn\'t a link I can use. It has to start with `https://`.');
        return;
      }
      if (sub === 'kofi') {
        store.setSetting('kofi_url', url);
        const refreshed = [await refreshPanel('perks'), await refreshPanel('faq')];
        await reply(interaction, 'The support buttons now open that page.'
          + panelNote(refreshed.includes(false) ? false : refreshed.includes(true) ? true : null, 'faq` and `/panel perks'));
      } else {
        store.setSetting('early_access_url', url);
        await reply(interaction, 'The early access buttons now open that post.');
      }
    },

    async reward(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'list') {
        const rows = store.roleRewards();
        const tiers = TIERS.slice(TIERS.indexOf(PERK_TIER)).map((tier) => `[${tier}]`).join(', ');
        await reply(interaction, [
          `Roles named ${tiers} give every reward automatically.`,
          ...(rows.length === 0
            ? ['No other roles give rewards. Use `/reward add`.']
            : rows.map((row) => `<@&${row.role_id}> → ${rewardLabel(row.reward)}`)),
        ].join('\n'));
        return;
      }
      const role = interaction.options.getRole('role', true);
      const reward = interaction.options.getString('reward', true);
      if (sub === 'add') {
        const added = store.addRoleReward(role.id, reward);
        await reply(interaction, added
          ? `<@&${role.id}> now gives ${rewardLabel(reward)}.`
          : `<@&${role.id}> already gives ${rewardLabel(reward)}.`);
      } else {
        const removed = store.removeRoleReward(role.id, reward);
        await reply(interaction, removed
          ? `<@&${role.id}> no longer gives ${rewardLabel(reward)}.`
          : `<@&${role.id}> didn't give ${rewardLabel(reward)}.`);
      }
    },

    async skin(interaction) {
      const sub = interaction.options.getSubcommand();
      if (sub === 'list') {
        const skins = store.skins();
        await reply(interaction, skins.length === 0
          ? 'There are no supporter skins yet. Add one with `/skin upload`.'
          : skins.map((skin) => `- ${describeSkin(skin)}`).join('\n'));
        return;
      }
      const slug = interaction.options.getString('id', true).trim().toLowerCase();
      if (sub === 'remove') {
        await reply(interaction, store.removeSkin(slug)
          ? `Removed \`${slug}\`. Players wearing it go back to the default skin within 10 minutes.`
          : `There is no skin \`${slug}\`.`);
        return;
      }
      const auras = [];
      for (const name of ['aura', 'aura2']) {
        const checked = checkAura(interaction.options.getString(name), parseColor);
        if (checked.error) {
          await reply(interaction, checked.error);
          return;
        }
        auras.push(checked.colour);
      }
      if (auras[0] === null && auras[1] !== null) {
        await reply(interaction, 'Set `aura` too. `aura2` is the second color of a two-color aura.');
        return;
      }
      if (sub === 'aura') {
        const saved = store.setSkinAura(slug, auras[0], auras[1]);
        await reply(interaction, saved
          ? `Saved. ${describeSkin(saved)}. Players see it within 10 minutes.`
          : `There is no skin \`${slug}\`.`);
        return;
      }
      const badSlug = checkSlug(slug);
      if (badSlug) {
        await reply(interaction, badSlug);
        return;
      }
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const answer = (content) => interaction.editReply({ content, allowedMentions: { parse: [] } });
      const stand = interaction.options.getString('stand', true);
      const name = interaction.options.getString('name', true).trim();
      const files = {};
      for (const kind of KINDS) {
        const got = await download(interaction.options.getAttachment(kind), kind);
        if (got.error) {
          await answer(got.error);
          return;
        }
        files[kind] = got.data;
      }
      const texture = checkTexture(files.texture);
      if (texture.error) {
        await answer(texture.error);
        return;
      }
      const notes = [];
      const existing = store.skin(slug);
      const keepOld = existing && !interaction.options.getBoolean('replace_all');
      for (const kind of ['model', 'animations']) {
        if (!files[kind] && keepOld && existing.files.some((file) => file.kind === kind)) {
          files[kind] = store.skinFile(existing.id, kind).data;
          notes.push(`Kept the earlier ${kind} file.`);
        }
      }
      if (files.model) {
        const model = checkModel(files.model, interaction.options.getBoolean('allow_missing_bones') ?? false);
        if (model.error) {
          await answer(model.error);
          return;
        }
        if (model.width && model.height && (model.width !== texture.width || model.height !== texture.height)) {
          notes.push(`The model expects a ${model.width}×${model.height} texture but the PNG is ${texture.width}×${texture.height}.`);
        }
        if (!model.barrage) {
          notes.push('The model has no `barrage_rig` bone, so barrages show no extra arms.');
        }
        if (model.missing.length > 0) {
          notes.push(`Missing shared bones: ${model.missing.join(', ')}.`);
        }
      }
      if (files.animations) {
        const clips = checkAnimations(files.animations);
        if (clips.error) {
          await answer(clips.error);
          return;
        }
      }
      if (existing && existing.stand !== stand) {
        notes.push(`It moved from ${standLabel(existing.stand)}, so anyone wearing it goes back to the default skin.`);
      }
      let saved = store.putSkin({ slug, stand, name, files });
      if (auras[0] !== null) {
        saved = { ...store.setSkinAura(slug, auras[0], auras[1]), created: saved.created };
      }
      await answer([
        `${saved.created ? 'Added' : 'Updated'} ${describeSkin(saved)}.`,
        'Players see it the next time they join a world, or within 10 minutes.',
        ...notes,
      ].join('\n'));
    },

    async [ANSWER_COMMAND](interaction) {
      const entries = store.faq();
      if (entries.length === 0) {
        await reply(interaction, 'There are no FAQ answers yet. Add one with `/faqedit add`.');
        return;
      }
      const target = interaction.targetMessage;
      await whisper(interaction, {
        content: 'Which answer?',
        components: [faqSelect(entries, `faq:answer:${target.id}:${target.author.id}`, 'Pick the answer to post')],
      });
    },
  };

  const showAnswer = async (interaction, id) => {
    const entries = store.faq();
    const entry = findEntry(entries, id);
    if (!entry) {
      await reply(interaction, 'That question has been removed.');
      return;
    }
    await whisper(interaction, faqAnswer(entry, entries, settings()));
  };

  const components = {
    async 'faq:pick'(interaction) {
      await showAnswer(interaction, interaction.values[0]);
    },

    async 'guide:pick'(interaction) {
      const pages = store.guide();
      const page = findPage(interaction.values[0], pages) ?? pages[0];
      await whisper(interaction, { ...guidePage(page, pages), components: [guideSelect('guide:turn', page.id, pages)] });
    },

    async 'guide:turn'(interaction) {
      const pages = store.guide();
      const page = findPage(interaction.values[0], pages) ?? pages[0];
      await interaction.update({ ...guidePage(page, pages), components: [guideSelect('guide:turn', page.id, pages)] });
    },

    async 'guide:edit'(interaction, id) {
      const pages = store.guide();
      const existing = id === 'new' ? null : findPage(id, pages);
      if (id !== 'new' && !existing) {
        await reply(interaction, 'That page has been removed.');
        return;
      }
      const text = (name) => interaction.fields.getTextInputValue(name);
      const checked = checkPage({
        id: existing ? existing.id : text('id'),
        title: text('title'),
        summary: text('summary'),
        intro: text('intro'),
        sections: text('sections'),
      }, pages);
      if (checked.error) {
        await reply(interaction, checked.error);
        return;
      }
      if (!existing && pages.some((page) => page.id === checked.page.id)) {
        await reply(interaction, `The id \`${checked.page.id}\` is taken. Use \`/guideedit edit\` to change that page.`);
        return;
      }
      store.putGuide(checked.page);
      await reply(interaction, (existing ? 'Saved' : 'Added') + ` "${checked.page.title}".` + panelNote(await refreshPanel('faq'), 'faq'));
    },

    async 'faq:show'(interaction, id) {
      await showAnswer(interaction, id);
    },

    async 'faq:answer'(interaction, messageId, authorId) {
      const entries = store.faq();
      const entry = findEntry(entries, interaction.values[0]);
      if (!entry) {
        await interaction.update({ content: 'That question has been removed.', components: [] });
        return;
      }
      await interaction.deferUpdate();
      const payload = faqAnswer(entry, entries, settings());
      try {
        await interaction.channel.send({
          ...payload,
          reply: { messageReference: messageId, failIfNotExists: false },
          allowedMentions: { parse: [], repliedUser: true },
        });
      } catch {
        await interaction.followUp({ ...payload, content: `<@${authorId}>`, allowedMentions: { users: [authorId] } });
      }
      await interaction.editReply({ content: 'Answered.', components: [] });
    },

    async 'faq:edit'(interaction, id) {
      const entries = store.faq();
      const existing = id === 'new' ? null : findEntry(entries, id);
      if (id !== 'new' && !existing) {
        await reply(interaction, 'That question has been removed.');
        return;
      }
      const text = (name) => interaction.fields.getTextInputValue(name);
      const checked = checkEntry({
        id: existing ? existing.id : text('id'),
        group: text('group'),
        question: text('question'),
        answer: text('answer'),
        related: text('related'),
      }, entries);
      if (checked.error) {
        await reply(interaction, checked.error);
        return;
      }
      if (!existing && entries.some((entry) => entry.id === checked.entry.id)) {
        await reply(interaction, `The id \`${checked.entry.id}\` is taken. Use \`/faqedit edit\` to change that question.`);
        return;
      }
      store.putFaq(checked.entry);
      await reply(interaction, (existing ? 'Saved' : 'Added') + ` "${checked.entry.question}".` + panelNote(await refreshPanel('faq'), 'faq'));
    },

    async 'faq:read'(interaction) {
      const channelId = store.setting('questions_channel');
      const roleId = store.setting('questions_role');
      if (!channelId || !roleId || !interaction.member) {
        await reply(interaction, 'Thanks for reading. No channel needs it right now.');
        return;
      }
      if (interaction.member.roles.cache.has(roleId)) {
        await reply(interaction, `You can already post in <#${channelId}>.`);
        return;
      }
      try {
        await interaction.member.roles.add(roleId, 'Read the FAQ');
      } catch {
        await reply(interaction, 'I couldn\'t give you the role. Please let a moderator know.');
        return;
      }
      await reply(interaction, `Thanks for reading. You can now post in <#${channelId}>.`);
    },

    async 'perks:link'(interaction) {
      await reply(interaction, linkLines(interaction.user.id));
    },

    async 'perks:status'(interaction) {
      await whisper(interaction, await statusOf(interaction.user.id));
    },

    async 'ticket:open'(interaction, kind) {
      if (!TICKETS[kind]) {
        await reply(interaction, 'That application type no longer exists.');
        return;
      }
      if (!ticketSetup()) {
        await reply(interaction, 'Applications aren\'t set up yet. Please let staff know.');
        return;
      }
      const blocked = await ticketBlock(interaction.user.id, kind);
      if (blocked) {
        await reply(interaction, blocked);
        return;
      }
      await interaction.showModal(applicationModal(kind));
    },

    async 'ticket:submit'(interaction, kind) {
      const fields = TICKETS[kind]?.fields ?? [];
      const raw = Object.fromEntries(fields.map((field) => [field.id, interaction.fields.getTextInputValue(field.id) ?? '']));
      const setup = ticketSetup();
      if (!setup) {
        await reply(interaction, 'Applications aren\'t set up yet. Please let staff know.\n\n' + answersBackup(kind, raw));
        return;
      }
      const blocked = await ticketBlock(interaction.user.id, kind);
      if (blocked) {
        await reply(interaction, blocked);
        return;
      }
      const checked = checkApplication(kind, raw);
      if (checked.error) {
        await reply(interaction, `${checked.error}\n\n${answersBackup(kind, raw)}`);
        return;
      }
      await interaction.deferUpdate();
      const g = interaction.guild;
      const me = g.members.me ?? await g.members.fetchMe();
      let channel;
      try {
        channel = await g.channels.create({
          name: channelName(kind, interaction.user.username),
          type: ChannelType.GuildText,
          parent: setup.category,
          topic: `${TICKETS[kind].name} application from ${interaction.user.username}`,
          permissionOverwrites: [
            { id: g.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
            { id: interaction.user.id, allow: TICKET_ACCESS },
            { id: setup.staff, allow: TICKET_ACCESS },
            { id: me.id, allow: [...TICKET_ACCESS, PermissionFlagsBits.ManageChannels] },
          ],
          reason: `${TICKETS[kind].name} application`,
        });
      } catch (error) {
        console.error('ticket channel failed', error);
        await interaction.followUp({ content: `I couldn't open a channel for your application. Please let staff know.\n\n${answersBackup(kind, raw)}`, flags: MessageFlags.Ephemeral });
        return;
      }
      const ticket = store.openTicket({ kind, userId: interaction.user.id, channelId: channel.id, answers: checked.answers });
      await channel.send({
        content: `<@${interaction.user.id}>, thanks for applying. <@&${setup.staff}> will review it here and may ask you questions.`,
        embeds: [applicationEmbed(ticket)],
        components: ticketButtons(ticket),
        allowedMentions: { users: [interaction.user.id], roles: [setup.staff] },
      });
    },

    async 'ticket:accept'(interaction, id) {
      await askDecision(interaction, id, 'accepted');
    },

    async 'ticket:deny'(interaction, id) {
      await askDecision(interaction, id, 'denied');
    },

    async 'ticket:decide'(interaction, id, outcome) {
      if (!isStaff(interaction.member)) {
        await reply(interaction, 'Only staff can decide applications.');
        return;
      }
      if (!(await openTicketFor(interaction, store.ticket(Number(id))))) {
        return;
      }
      const note = interaction.fields.getTextInputValue('note')?.trim() ?? '';
      const ticket = store.decideTicket(Number(id), outcome === 'accepted' ? 'accepted' : 'denied', interaction.user.id, note);
      if (!ticket) {
        await reply(interaction, 'Someone already decided this application.');
        return;
      }
      let roleNote = '';
      let staffNote = '';
      const roleId = ticket.status === 'accepted' ? store.setting(`ticket_role_${ticket.kind}`) : null;
      if (roleId) {
        try {
          const member = await interaction.guild.members.fetch(ticket.userId);
          await member.roles.add(roleId, `${TICKETS[ticket.kind].name} application accepted`);
          roleNote = `You now have the <@&${roleId}> role.`;
        } catch {
          staffNote = `I couldn't give <@&${roleId}>. Check that the applicant is still here and the role is below mine.`;
        }
      }
      await interaction.message?.edit({ embeds: [applicationEmbed(ticket)], components: ticketButtons(ticket) }).catch(() => {});
      await interaction.reply({ content: decisionMessage(ticket, roleNote), allowedMentions: { users: [ticket.userId] } });
      if (staffNote) {
        await interaction.followUp({ content: staffNote, flags: MessageFlags.Ephemeral, allowedMentions: { parse: [] } });
      }
      await client.users.send(ticket.userId, directMessage(ticket, interaction.guild.name)).catch(() => {});
    },

    async 'ticket:close'(interaction, id) {
      const ticket = await openTicketFor(interaction, store.ticket(Number(id)));
      if (!ticket) {
        return;
      }
      if (!isStaff(interaction.member) && interaction.user.id !== ticket.userId) {
        await reply(interaction, 'Only staff or the applicant can close this.');
        return;
      }
      const withdraw = ticket.status === 'open' && interaction.user.id === ticket.userId && !isStaff(interaction.member);
      await whisper(interaction, {
        content: withdraw
          ? 'Withdraw your application? This channel will be deleted.'
          : 'Close this application? This channel will be deleted.',
        components: [new ActionRowBuilder().addComponents(new ButtonBuilder()
          .setStyle(ButtonStyle.Danger)
          .setCustomId(`ticket:shut:${ticket.id}`)
          .setLabel(withdraw ? 'Withdraw and delete' : 'Close and delete')).toJSON()],
      });
    },

    async 'ticket:shut'(interaction, id) {
      const ticket = await openTicketFor(interaction, store.ticket(Number(id)));
      if (!ticket) {
        return;
      }
      const staff = isStaff(interaction.member);
      if (!staff && interaction.user.id !== ticket.userId) {
        await reply(interaction, 'Only staff or the applicant can close this.');
        return;
      }
      if (!store.closeTicket(ticket.id)) {
        await reply(interaction, 'That application is already closed.');
        return;
      }
      await logTicket(ticket, interaction.user.id, !staff && ticket.status === 'open');
      await interaction.update({ content: 'Closing.', components: [] });
      const channel = await client.channels.fetch(ticket.channelId).catch(() => null);
      if (channel) {
        await channel.send(`Closed by <@${interaction.user.id}>. This channel will be deleted in ${CLOSE_DELAY_MS / 1000} seconds.`).catch(() => {});
        setTimeout(() => channel.delete('Application closed').catch(() => {}), CLOSE_DELAY_MS);
      }
    },
  };

  const askDecision = async (interaction, id, outcome) => {
    if (!isStaff(interaction.member)) {
      await reply(interaction, 'Only staff can decide applications.');
      return;
    }
    const ticket = await openTicketFor(interaction, store.ticket(Number(id)));
    if (!ticket) {
      return;
    }
    if (ticket.status !== 'open') {
      await reply(interaction, 'This application has already been decided.');
      return;
    }
    await interaction.showModal(decisionModal(ticket.id, outcome));
  };

  const route = (interaction) => {
    if (interaction.isChatInputCommand() || interaction.isMessageContextMenuCommand()) {
      const handler = handlers[interaction.commandName];
      return handler ? [interaction.commandName, () => handler(interaction)] : null;
    }
    if (interaction.isMessageComponent() || interaction.isModalSubmit()) {
      const [scope, action, ...rest] = interaction.customId.split(':');
      const handler = components[`${scope}:${action}`];
      return handler ? [`${scope}:${action}`, () => handler(interaction, ...rest)] : null;
    }
    return null;
  };

  client.once(Events.ClientReady, async (ready) => {
    console.log(`Logged in as ${ready.user.tag}`);
    const g = guild();
    if (!g) {
      console.error(`The bot is not in the guild ${guildId}. Invite it, then restart.`);
      return;
    }
    await g.commands.set(COMMANDS);
    await g.members.fetch();
    await syncBoosters(g);
    await refreshPanel('faq');
    console.log(`Commands registered in ${g.name}; ${g.members.cache.size} members cached`);
  });

  client.on(Events.InteractionCreate, async (interaction) => {
    if (interaction.isAutocomplete()) {
      const typed = interaction.options.getFocused().trim().toLowerCase();
      const choices = interaction.commandName === 'aura'
        ? Object.keys(PRESETS).filter((name) => name.startsWith(typed)).map((name) => ({ name, value: name }))
        : interaction.commandName === 'guide' || interaction.commandName === 'guideedit'
          ? searchPages(store.guide(), typed).slice(0, 25).map((page) => ({ name: page.title, value: page.id }))
          : interaction.commandName === 'skin'
            ? store.skins().filter((skin) => skin.slug.includes(typed) || skin.name.toLowerCase().includes(typed))
              .slice(0, 25).map((skin) => ({ name: `${skin.name} (${skin.slug})`, value: skin.slug }))
          : searchEntries(store.faq(), typed).slice(0, 25).map((entry) => ({ name: entry.question, value: entry.id }));
      await interaction.respond(choices).catch(() => {});
      return;
    }
    if (interaction.guildId !== null && interaction.guildId !== guildId) {
      return;
    }
    const routed = route(interaction);
    if (!routed) {
      return;
    }
    const [name, run] = routed;
    try {
      await run();
    } catch (error) {
      console.error(`${name} failed`, error);
      if (!interaction.replied && !interaction.deferred) {
        await reply(interaction, 'Something went wrong. Please try again.').catch(() => {});
      }
    }
  });

  client.on(Events.GuildMemberUpdate, async (before, after) => {
    if (after.guild.id === guildId) {
      await syncBooster(after).catch((error) => console.error('booster role failed', error));
    }
    if (after.guild.id !== guildId || before.partial || after.user.bot) {
      return;
    }
    if (!store.gainedRewards(roleKeys(before.roles.cache.values()), roleKeys(after.roles.cache.values()))) {
      return;
    }
    if (!store.markWelcomed(after.id, WELCOME_COOLDOWN_MS)) {
      return;
    }
    const payload = { ...welcome(after.id, settings()), allowedMentions: { users: [after.id] } };
    const channelId = store.setting('welcome_channel');
    try {
      if (!channelId) {
        throw new Error('no welcome channel');
      }
      const channel = await client.channels.fetch(channelId);
      await channel.send(payload);
    } catch {
      await after.send(payload).catch(() => {});
    }
  });

  client.on(Events.MessageCreate, (message) => {
    filterQuestion(message).catch((error) => console.error('questions filter failed', error));
  });

  client.on(Events.ChannelDelete, (channel) => {
    const ticket = store.ticketInChannel(channel.id);
    if (ticket) {
      store.closeTicket(ticket.id);
    }
  });

  const onLinked = (discordId, name) => {
    client.users.send(discordId, `Your Discord is now linked to the Minecraft account \`${name}\`.`).catch(() => {});
  };

  return {
    start: () => client.login(token),
    rolesOf,
    nameOf,
    onLinked,
    canManage,
    clientId: () => client.application?.id ?? null,
  };
}
