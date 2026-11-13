import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export const SITE_DIR = fileURLToPath(new URL('../site', import.meta.url));
export const DEFAULT_CONTENT = JSON.parse(readFileSync(new URL('../site/content.json', import.meta.url), 'utf8'));
export const MAX_CONTENT = 512 * 1024;
export const MAX_FILE = 10 * 1024 * 1024;
export const FILE_TYPES = ['image/png', 'image/gif', 'image/webp', 'image/jpeg'];
export const STATUSES = ['active', 'planned', 'done'];

const SAFE_URL = /^(\/(?!\/)[^\s"'<>\\]*|https:\/\/[^\s"'<>\\]+)$/;
const COLOR = /^#[0-9a-fA-F]{6}$/;
const STAND_ID = /^[a-z0-9_-]{1,40}$/;
const SLUG = /^[a-z0-9-]{1,64}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const MAX_TEXT = 20000;
const MAX_ITEMS = 100;

const isObject = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);

export function roadmapCards(roadmap) {
  if (!Array.isArray(roadmap)) {
    return roadmap;
  }
  for (const column of roadmap) {
    if (!isObject(column)) {
      continue;
    }
    if (!Array.isArray(column.cards)) {
      const items = Array.isArray(column.items) ? column.items : [];
      column.cards = items.map((title) => ({ title, status: column.status, tag: '', image: '', text: '' }));
    }
    delete column.items;
    column.color ??= '';
    column.text ??= '';
  }
  return roadmap;
}

export function mergeContent(saved) {
  const base = structuredClone(DEFAULT_CONTENT);
  if (!isObject(saved)) {
    return base;
  }
  for (const [key, value] of Object.entries(saved)) {
    base[key] = isObject(value) && isObject(base[key]) ? { ...base[key], ...value } : value;
  }
  if (isObject(saved.support) && saved.support.revision !== DEFAULT_CONTENT.support.revision) {
    base.support = structuredClone(DEFAULT_CONTENT.support);
  }
  if (isObject(saved.wikiIcons)) {
    for (const [id, icon] of Object.entries(saved.wikiIcons)) {
      if (icon === '' && DEFAULT_CONTENT.wikiIcons[id]) {
        base.wikiIcons[id] = DEFAULT_CONTENT.wikiIcons[id];
      }
    }
  }
  roadmapCards(base.roadmap);
  if (Array.isArray(base.stands)) {
    for (const stand of base.stands) {
      const defaults = DEFAULT_CONTENT.stands.find((d) => d.id === stand?.id)?.moves ?? [];
      if (isObject(stand) && !Array.isArray(stand.moves)) {
        stand.moves = structuredClone(defaults);
      }
      for (const move of isObject(stand) ? stand.moves : []) {
        if (isObject(move) && move.icon === undefined) {
          move.icon = defaults.find((d) => d.name === move.name)?.icon ?? '';
        }
      }
    }
  }
  return base;
}

function text(value, where, { optional = true, max = MAX_TEXT } = {}) {
  if (typeof value !== 'string') {
    throw new Error(`${where} must be text.`);
  }
  if (!optional && !value.trim()) {
    throw new Error(`${where} can't be empty.`);
  }
  if (value.length > max) {
    throw new Error(`${where} is too long.`);
  }
  return value;
}

function url(value, where, { optional = false } = {}) {
  text(value, where);
  if (optional && value === '') {
    return value;
  }
  if (!SAFE_URL.test(value)) {
    throw new Error(`${where} must be a link starting with https:// or a site path starting with /.`);
  }
  return value;
}

function list(value, where) {
  if (!Array.isArray(value)) {
    throw new Error(`${where} must be a list.`);
  }
  if (value.length > MAX_ITEMS) {
    throw new Error(`${where} can have at most ${MAX_ITEMS} entries.`);
  }
  return value;
}

function record(value, where) {
  if (!isObject(value)) {
    throw new Error(`${where} is missing.`);
  }
  return value;
}

function unique(items, key, where) {
  const seen = new Set();
  for (const item of items) {
    if (seen.has(item[key])) {
      throw new Error(`Two ${where} share the id "${item[key]}".`);
    }
    seen.add(item[key]);
  }
}

function texts(obj, where) {
  for (const [key, value] of Object.entries(obj)) {
    text(value, `${where} ${key}`);
  }
}

export function checkContent(raw) {
  try {
    if (JSON.stringify(raw ?? null).length > MAX_CONTENT) {
      throw new Error('The site content is too large.');
    }
    const c = record(raw, 'The site content');
    url(c.discord, 'The Discord invite', { optional: true });
    list(c.facts, 'Facts').forEach((fact, i) => text(fact, `Fact ${i + 1}`, { max: 80 }));
    texts(record(c.home, 'Home text'), 'Home');
    for (const [id, page] of Object.entries(record(c.pages, 'Page headings'))) {
      record(page, `The ${id} heading`);
      text(page.title, `The ${id} title`, { optional: false, max: 80 });
      text(page.text, `The ${id} subtitle`);
    }
    list(c.stands, 'Stands').forEach((s, i) => {
      const where = `Stand ${i + 1}`;
      record(s, where);
      if (!STAND_ID.test(s.id ?? '')) {
        throw new Error(`${where} needs an id of lowercase letters, digits, _ or -.`);
      }
      text(s.name, `${where} name`, { optional: false, max: 80 });
      url(s.model, `${where} model`);
      url(s.icon, `${where} icon`);
      if (!COLOR.test(s.color ?? '')) {
        throw new Error(`${where} color must look like #a874f0.`);
      }
      for (const key of ['type', 'source', 'signature', 'text']) {
        text(s[key], `${where} ${key}`);
      }
      s.moves ??= [];
      list(s.moves, `${where} moves`).forEach((m, j) => {
        const at = `${where} move ${j + 1}`;
        record(m, at);
        text(m.name, `${at} name`, { optional: false, max: 80 });
        m.icon ??= '';
        url(m.icon, `${at} icon`, { optional: true });
        if (!['Move', 'Passive'].includes(m.kind)) {
          throw new Error(`${at} must be a Move or a Passive.`);
        }
        m.short ??= '';
        text(m.short, `${at} summary`, { max: 200 });
        text(m.text, `${at} details`);
      });
    });
    unique(c.stands, 'id', 'Stands');
    for (const [id, icon] of Object.entries(record(c.wikiIcons, 'Wiki icons'))) {
      url(icon, `The icon for wiki page ${id}`, { optional: true });
    }
    c.wikiGroups ??= [];
    list(c.wikiGroups, 'Wiki groups').forEach((g, i) => {
      const where = `Wiki group ${i + 1}`;
      record(g, where);
      text(g.label, `${where} name`, { optional: false, max: 60 });
      list(g.pages, `${where} pages`).forEach((id) => text(id, `${where} page`, { max: 32 }));
    });
    list(c.roadmap, 'Roadmap');
    roadmapCards(c.roadmap);
    c.roadmap.forEach((p, i) => {
      const where = `Roadmap category ${i + 1}`;
      record(p, where);
      text(p.phase, `${where} label`, { max: 40 });
      text(p.title, `${where} title`, { optional: false, max: 120 });
      text(p.text, `${where} description`, { max: 600 });
      if (!STATUSES.includes(p.status)) {
        throw new Error(`${where} status must be one of ${STATUSES.join(', ')}.`);
      }
      if (p.color !== '' && !COLOR.test(p.color ?? '')) {
        throw new Error(`${where} color must look like #a874f0.`);
      }
      list(p.cards, `${where} cards`).forEach((card, j) => {
        const at = `${where} card ${j + 1}`;
        record(card, at);
        text(card.title, `${at} title`, { optional: false, max: 160 });
        if (!STATUSES.includes(card.status)) {
          throw new Error(`${at} status must be one of ${STATUSES.join(', ')}.`);
        }
        card.tag ??= '';
        card.image ??= '';
        card.text ??= '';
        text(card.tag, `${at} tag`, { max: 40 });
        url(card.image, `${at} picture`, { optional: true });
        text(card.text, `${at} details`, { max: 2000 });
      });
    });
    list(c.projects, 'Projects').forEach((p, i) => {
      record(p, `Project ${i + 1}`);
      text(p.name, `Project ${i + 1} name`, { optional: false, max: 120 });
      for (const key of ['phase', 'when', 'logo']) {
        p[key] ??= '';
      }
      text(p.status, `Project ${i + 1} status`, { max: 60 });
      text(p.phase, `Project ${i + 1} phase`, { max: 60 });
      text(p.when, `Project ${i + 1} date`, { max: 60 });
      url(p.logo, `Project ${i + 1} logo`, { optional: true });
      text(p.text, `Project ${i + 1} text`);
    });
    list(c.staff, 'Staff').forEach((m, i) => {
      record(m, `Staff member ${i + 1}`);
      text(m.name, `Staff member ${i + 1} name`, { optional: false, max: 80 });
      text(m.role, `Staff member ${i + 1} role`, { max: 80 });
      text(m.group, `Staff member ${i + 1} group`, { optional: false, max: 60 });
      url(m.avatar, `Staff member ${i + 1} avatar`, { optional: true });
    });
    list(c.commands, 'Commands').forEach((cmd, i) => {
      record(cmd, `Command ${i + 1}`);
      text(cmd.name, `Command ${i + 1} name`, { optional: false, max: 60 });
      text(cmd.text, `Command ${i + 1} text`, { max: 300 });
    });
    const sup = record(c.supporters, 'Supporter section');
    text(sup.title, 'Supporter title', { max: 120 });
    text(sup.text, 'Supporter text');
    list(sup.steps, 'Supporter steps').forEach((step, i) => {
      record(step, `Supporter step ${i + 1}`);
      text(step.title, `Supporter step ${i + 1} title`, { max: 120 });
      text(step.text, `Supporter step ${i + 1} text`, { max: 600 });
    });
    c.homeLinks ??= [];
    list(c.homeLinks, 'Home tabs').forEach((t, i) => {
      const where = `Home tab ${i + 1}`;
      record(t, where);
      text(t.title, `${where} title`, { optional: false, max: 60 });
      t.text ??= '';
      t.icon ??= '';
      text(t.text, `${where} line`, { max: 120 });
      url(t.icon, `${where} icon`, { optional: true });
      url(t.link, `${where} link`);
    });
    list(c.gallery, 'Gallery').forEach((g, i) => {
      record(g, `Gallery picture ${i + 1}`);
      url(g.image, `Gallery picture ${i + 1}`);
      g.caption ??= '';
      text(g.caption, `Gallery picture ${i + 1} caption`, { max: 200 });
    });
    const support = record(c.support, 'Supporter page');
    text(support.inGameTitle, 'Supporter page perks heading', { max: 120 });
    list(support.inGame, 'Supporter page perks').forEach((perk) => text(perk, 'A supporter perk', { max: 200 }));
    list(support.tiers, 'Tiers').forEach((t, i) => {
      const where = `Tier ${i + 1}`;
      record(t, where);
      text(t.name, `${where} name`, { optional: false, max: 80 });
      text(t.price, `${where} price`, { max: 40 });
      text(t.period, `${where} period`, { max: 40 });
      url(t.logo, `${where} logo`, { optional: true });
      url(t.link, `${where} join link`, { optional: true });
      if (t.color !== '' && !COLOR.test(t.color ?? '')) {
        throw new Error(`${where} color must look like #f6c744.`);
      }
      if (typeof t.featured !== 'boolean') {
        throw new Error(`${where} highlight must be on or off.`);
      }
      list(t.perks, `${where} perks`).forEach((perk) => text(perk, `${where} perk`, { max: 200 }));
      list(t.extras, `${where} extras`).forEach((perk) => text(perk, `${where} extra`, { max: 200 }));
    });
    list(c.blog, 'Blog posts').forEach((post, i) => {
      const where = `Blog post ${i + 1}`;
      record(post, where);
      if (!SLUG.test(post.slug ?? '')) {
        throw new Error(`${where} needs a link name of lowercase letters, digits or -.`);
      }
      if (!DATE.test(post.date ?? '')) {
        throw new Error(`${where} needs a date like 2026-10-23.`);
      }
      text(post.title, `${where} title`, { optional: false, max: 200 });
      for (const key of ['author', 'summary', 'body']) {
        text(post[key], `${where} ${key}`);
      }
    });
    unique(c.blog, 'slug', 'blog posts');
    return { content: c };
  } catch (error) {
    return { error: error.message };
  }
}

export function checkUrl(value) {
  return typeof value === 'string' && SAFE_URL.test(value) && value.startsWith('https://');
}
