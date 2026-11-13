export const KOFI = 'kofi';
export const MAX_ENTRIES = 25;
export const MAX_RELATED = 4;

const ID_PATTERN = /^[a-z0-9-]{1,32}$/;

export const SEED = [
  {
    id: 'release',
    group: 'Release',
    question: 'When does the mod release?',
    answer: 'There are no ETAs for release until a public beta is finalized. An announcement will be published around that time.',
    related: ['public-beta'],
  },
  {
    id: 'public-beta',
    group: 'Release',
    question: 'What is the public beta?',
    answer: 'The public beta is a planned CurseForge and Modrinth release that will drop with Parts 1-3 content.\n\n'
      + 'The official release will then be worked on for a substantial period of time and will release with Part 4 and Part 5 content, alongside a Part 7 content drop.',
    related: ['release'],
  },
  {
    id: 'platform',
    group: 'Platform',
    question: 'Is this Java or Bedrock?',
    answer: 'This is a Java 1.21.1 mod for Fabric and NeoForge. A Forge port is not currently planned, and a Bedrock port will never exist.',
    related: [],
  },
  {
    id: 'tester',
    group: 'Getting access',
    question: 'How do I become a tester?',
    answer: 'Apply when applications are open.',
    related: ['early-access'],
  },
  {
    id: 'early-access',
    group: 'Getting access',
    question: 'How do I obtain early access?',
    answer: 'Early access comes with supporter benefits, which need a Ko-fi membership. One-off donations are appreciated but don\'t include it.',
    related: ['supporters', KOFI],
  },
  {
    id: 'supporters',
    group: 'Getting access',
    question: 'What do supporters get?',
    answer: 'Ko-fi members get custom Stand skins and a custom Stand aura color in game, early access sneak peeks and versions, and priority community voice.',
    related: ['early-access', KOFI],
  },
  {
    id: 'stands',
    group: 'Gameplay',
    question: 'How are Stands obtainable in JJ:HA?',
    answer: 'Stands can be obtained through Stand Arrows or Stand Arrow shards found in structures throughout the world. '
      + 'You must have the appropriate Worthiness level for a Stand to stick.\n\nOn official release, more ways to obtain them will be added.',
    related: [],
  },
];

export function parseRelated(raw) {
  return [...new Set(String(raw ?? '').toLowerCase().split(/[\s,]+/).filter(Boolean))];
}

export function checkEntry(raw, entries) {
  const id = String(raw.id ?? '').trim().toLowerCase();
  const group = String(raw.group ?? '').trim();
  const question = String(raw.question ?? '').trim();
  const answer = String(raw.answer ?? '').trim();
  const related = parseRelated(raw.related);
  const known = new Set(entries.map((entry) => entry.id));
  if (!ID_PATTERN.test(id) || id === KOFI) {
    return { error: 'The id must be 1-32 lowercase letters, digits or dashes, such as `public-beta`.' };
  }
  if (!known.has(id) && entries.length >= MAX_ENTRIES) {
    return { error: `There can be at most ${MAX_ENTRIES} questions. Remove one first.` };
  }
  if (!group || group.length > 40) {
    return { error: 'The group needs 1-40 characters.' };
  }
  if (!question || question.length > 100) {
    return { error: 'The question needs 1-100 characters.' };
  }
  if (!answer || answer.length > 2000) {
    return { error: 'The answer needs 1-2000 characters.' };
  }
  const unknown = related.filter((other) => other !== KOFI && (other === id || !known.has(other)));
  if (unknown.length > 0) {
    return { error: `Unknown follow-ups: ${unknown.join(', ')}. Use question ids, or \`${KOFI}\` for the Ko-fi button.` };
  }
  if (related.filter((other) => other !== KOFI).length > MAX_RELATED) {
    return { error: `A question can have at most ${MAX_RELATED} follow-ups.` };
  }
  return { entry: { id, group, question, answer, related } };
}

export function findEntry(entries, text) {
  const wanted = String(text ?? '').trim().toLowerCase();
  return entries.find((entry) => entry.id === wanted)
    ?? entries.find((entry) => entry.question.toLowerCase() === wanted)
    ?? null;
}

export function searchEntries(entries, text) {
  const wanted = String(text ?? '').trim().toLowerCase();
  return entries.filter((entry) => entry.id.includes(wanted) || entry.question.toLowerCase().includes(wanted));
}

export function httpsUrl(raw) {
  try {
    const url = new URL(String(raw ?? '').trim());
    return url.protocol === 'https:' ? url.toString() : null;
  } catch {
    return null;
  }
}
