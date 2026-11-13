import { ActionRowBuilder, ButtonBuilder, ButtonStyle, EmbedBuilder, StringSelectMenuBuilder } from 'discord.js';
import { KOFI } from './faq.js';
import { PAGES } from './guide.js';
import { SUPPORTER_AURA, formatColor, rewardLabel } from './rewards.js';

const COLOUR = 0x9a46e6;
export const LOGO = 'logo.png';
const PANEL_ROOM = 3900;
const MEMBERSHIP = 'Stand skins and the aura color come with the Standard membership or higher on Ko-fi. One-off donations are appreciated but don\'t include them.';

const SITE = (process.env.SITE_URL ?? 'https://jojoheavensarrow.dev').replace(/\/+$/, '');

export const forDiscord = (text, max) => {
  const out = String(text ?? '')
    .replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (all, alt, url) => `[${alt || 'Picture'}](${url.startsWith('/') ? SITE + url : url})`)
    .replace(/\[([^\]]+)\]\((\/(?!\/)[^)\s]*)\)/g, (all, label, url) => `[${label}](${SITE}${url})`);
  return max ? clip(out, max) : out;
};

const clip = (text, max) => (text.length > max ? text.slice(0, max - 1) + '...' : text);

const linkButton = (label, url) => new ButtonBuilder().setStyle(ButtonStyle.Link).setLabel(label).setURL(url);

const button = (id, label) => new ButtonBuilder().setStyle(ButtonStyle.Secondary).setCustomId(id).setLabel(label);

export const READ_BUTTON = 'I\'ve read the FAQ';

const rows = (...buttons) => {
  const present = buttons.filter(Boolean);
  return present.length === 0 ? [] : [new ActionRowBuilder().addComponents(present).toJSON()];
};

export function faqSelect(entries, customId, placeholder) {
  const menu = new StringSelectMenuBuilder()
    .setCustomId(customId)
    .setPlaceholder(placeholder)
    .addOptions(entries.map((entry) => ({
      label: clip(entry.question, 100),
      description: clip(entry.group, 100),
      value: entry.id,
    })));
  return new ActionRowBuilder().addComponents(menu).toJSON();
}

export function guideSelect(customId, current, pages = PAGES) {
  const menu = new StringSelectMenuBuilder()
    .setCustomId(customId)
    .setPlaceholder('Mod guide: how to get each power')
    .addOptions(pages.map((page) => ({
      label: page.title,
      description: clip(page.summary, 100),
      value: page.id,
      default: page.id === current,
    })));
  return new ActionRowBuilder().addComponents(menu).toJSON();
}

export function guidePage(page, pages = PAGES) {
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setTitle(page.title)
    .setDescription(forDiscord(page.intro, 4096))
    .addFields(page.sections.map(([name, value]) => ({ name, value: forDiscord(value, 1024) })))
    .setFooter({ text: `Mod guide · page ${pages.findIndex((other) => other.id === page.id) + 1} of ${pages.length}` });
  return { embeds: [embed.toJSON()] };
}

export function faqPanel(entries, settings, pages = PAGES) {
  const parts = ['Read these for quick questions and answers. Please read this channel before asking any questions in our related channels.'];
  let room = PANEL_ROOM - parts[0].length;
  let shown = 0;
  for (const entry of entries) {
    const part = `**【${String(shown + 1).padStart(2, '0')}.】${entry.question}**\n${forDiscord(entry.answer)}`;
    if (part.length + 2 > room) {
      break;
    }
    parts.push(part);
    room -= part.length + 2;
    shown++;
  }
  if (shown < entries.length) {
    parts.push('More answers: type `/faq`.');
  }
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setTitle('Gumel\'s Corner FAQ')
    .setDescription(parts.join('\n\n'))
    .setImage(`attachment://${LOGO}`)
    .setFooter({ text: settings.questions
      ? `Use the menu below for the mod guide. Done reading? Press "${READ_BUTTON}" before posting in the questions channel.`
      : 'Use the menu below for the mod guide.' });
  return {
    embeds: [embed.toJSON()],
    components: [guideSelect('guide:pick', null, pages), ...rows(
      settings.questions ? button('faq:read', READ_BUTTON).setStyle(ButtonStyle.Success) : null,
      settings.kofi ? linkButton('Support on Ko-fi', settings.kofi) : null,
    )],
  };
}

export function questionsNotice(faqUrl) {
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setTitle('Read the FAQ first')
    .setDescription('Read the FAQ before posting here. Most questions are already answered there.\n\n'
      + `At the bottom of the FAQ, press **${READ_BUTTON}**. Until then, messages you post here are removed.`);
  return { embeds: [embed.toJSON()], components: rows(faqUrl ? linkButton('Open the FAQ', faqUrl) : null) };
}

export function faqReminder(userId, faqUrl) {
  return {
    content: `<@${userId}>, read the FAQ before posting here. Press **${READ_BUTTON}** at the bottom of it, then post again.`,
    components: rows(faqUrl ? linkButton('Open the FAQ', faqUrl) : null),
  };
}

export function faqAnswer(entry, entries, settings) {
  const embed = new EmbedBuilder().setColor(COLOUR).setTitle(clip(entry.question, 256)).setDescription(forDiscord(entry.answer, 4096));
  const buttons = [];
  for (const id of entry.related) {
    if (id === KOFI) {
      continue;
    }
    const other = entries.find((candidate) => candidate.id === id);
    if (other) {
      buttons.push(button(`faq:show:${other.id}`, clip(other.question, 80)));
    }
  }
  if (entry.related.includes(KOFI) && settings.kofi) {
    buttons.push(linkButton('Support on Ko-fi', settings.kofi));
  }
  return { embeds: [embed.toJSON()], components: rows(...buttons.slice(0, 5)) };
}

export function perksPanel(settings) {
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setTitle('Supporter benefits')
    .setDescription(MEMBERSHIP)
    .addFields(
      { name: 'Stand skins and aura', value: 'Standard and above: supporter-only Stand skins and your own aura color in game.', inline: true },
      { name: 'Early access', value: 'Sneak peeks and early versions of the mod.', inline: true },
      { name: 'Priority voice', value: 'Your feedback is heard first.', inline: true },
    );
  return {
    embeds: [embed.toJSON()],
    components: rows(
      settings.kofi ? linkButton('Support on Ko-fi', settings.kofi) : null,
      button('perks:link', 'Link my account'),
      button('perks:status', 'My status'),
    ),
  };
}

export function welcome(userId, settings) {
  const embed = new EmbedBuilder()
    .setColor(COLOUR)
    .setDescription([
      '**1.** Press **Get my code**, or run `/link` here',
      '**2.** In Minecraft, run `/supporter link <code>`',
      '**3.** Pick an aura color with `/aura set`',
    ].join('\n'));
  return {
    content: `<@${userId}> thanks for backing the mod. Three steps to claim your perks:`,
    embeds: [embed.toJSON()],
    components: rows(
      button('perks:link', 'Get my code'),
      settings.earlyAccess ? linkButton('Early access builds', settings.earlyAccess) : null,
    ),
  };
}

export function status({ link, rewards, aura }, settings) {
  const embed = new EmbedBuilder().setColor(COLOUR);
  if (rewards.length === 0) {
    embed
      .setTitle('You\'re not a supporter yet')
      .setDescription('Supporters get custom Stand skins and a custom aura color in game, early access sneak peeks and versions, '
        + 'and priority community voice.\n\n' + MEMBERSHIP);
    if (link) {
      embed.addFields({ name: 'Minecraft account', value: `\`${link.mc_name}\`` });
    }
    return { embeds: [embed.toJSON()], components: rows(settings.kofi ? linkButton('Support on Ko-fi', settings.kofi) : null) };
  }
  embed
    .setTitle('You\'re a supporter')
    .addFields({ name: 'Perks', value: rewards.map(rewardLabel).join('\n') })
    .addFields({
      name: 'Minecraft account',
      value: link ? `\`${link.mc_name}\`` : 'Not linked yet. Press **Link my account** to get a code.',
    });
  if (rewards.includes(SUPPORTER_AURA)) {
    embed.addFields({ name: 'Aura color', value: aura === null ? 'Your skin\'s own color. Change it with `/aura set`.' : formatColor(aura) });
  }
  if (link) {
    embed.setFooter({ text: 'Perks refresh in game when you join, or within 10 minutes.' });
  }
  return {
    embeds: [embed.toJSON()],
    components: rows(
      link ? null : button('perks:link', 'Link my account'),
      settings.earlyAccess ? linkButton('Early access builds', settings.earlyAccess) : null,
    ),
  };
}
