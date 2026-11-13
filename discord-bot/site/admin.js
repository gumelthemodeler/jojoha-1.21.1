const root = document.getElementById('admin');
const toastEl = document.querySelector('.toast');
const userBar = document.querySelector('.admin-user');

const ERRORS = {
  setup: "Discord sign-in isn't set up yet. The bot needs DISCORD_CLIENT_SECRET in Railway.",
  denied: "That Discord account doesn't have Manage Server in the JoJo: Heaven's Arrow server.",
  state: 'The sign-in took too long or was opened in another tab. Try again.',
  discord: "Discord didn't accept the sign-in. Try again.",
};

const STATUS = ['active', 'planned', 'done'];
const MD_HINT = 'Blank line between paragraphs. Start lines with - for bullets or 1. for steps. **bold**, [link text](https://...), and ![caption](/files/...) on its own line for a picture. Copy picture links from the Images tab.';
const CARD_STATUS = { active: 'In progress', planned: 'Planned', done: 'Done' };

const state = { me: null, data: null, draft: null, kofi: '', dirty: false, tab: 'general', open: new Set(), wikiNew: [], faqNew: [] };

const esc = (text) => String(text ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const clone = (value) => structuredClone(value);
const split = (path) => path.split('.').map((part) => (/^\d+$/.test(part) ? Number(part) : part));
const get = (obj, path) => split(path).reduce((value, key) => value?.[key], obj);
function set(obj, path, value) {
  const keys = split(path);
  const last = keys.pop();
  const parent = keys.reduce((value, key) => value[key], obj);
  parent[last] = value;
}

function toast(message, bad = false) {
  toastEl.textContent = message;
  toastEl.classList.toggle('bad', bad);
  toastEl.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => toastEl.classList.remove('show'), bad ? 6000 : 2500);
}

async function call(method, path, body, headers = {}) {
  const init = { method, credentials: 'same-origin', headers: { ...headers } };
  if (body !== undefined) {
    if (body instanceof Blob) {
      init.body = body;
    } else {
      init.body = JSON.stringify(body);
      init.headers['Content-Type'] = 'application/json';
    }
  }
  const res = await fetch(path, init);
  const data = await res.json().catch(() => ({}));
  if (res.status === 401) {
    gate();
    throw new Error('Signed out');
  }
  if (!res.ok) {
    throw new Error(data.error ?? `Request failed (${res.status})`);
  }
  return data;
}

function gate() {
  userBar.hidden = true;
  const error = new URLSearchParams(location.search).get('error');
  root.innerHTML = `<div class="gate">
    <h1>Site admin</h1>
    <p>Sign in with the Discord account you use in the JoJo: Heaven's Arrow server. Anyone with Manage Server there can edit the site.</p>
    ${error && ERRORS[error] ? `<p class="error">${esc(ERRORS[error])}</p>` : ''}
    <a class="btn discord" href="/auth/login">Sign in with Discord</a>
  </div>`;
}

const field = (path, label, type = 'text', opts = {}) => {
  const value = get(state.draft, path);
  const attrs = `data-path="${esc(path)}" data-type="${type}"${opts.title ? ' data-title="1"' : ''}`;
  let control;
  if (type === 'textarea' || type === 'md') {
    control = `<textarea ${attrs}${type === 'md' ? ' class="tall"' : ''}>${esc(value)}</textarea>`;
  } else if (type === 'lines') {
    control = `<textarea ${attrs}>${esc((value ?? []).join('\n'))}</textarea>`;
  } else if (type === 'select') {
    control = `<select ${attrs}>${opts.options.map(([v, l]) => `<option value="${esc(v)}"${v === value ? ' selected' : ''}>${esc(l)}</option>`).join('')}</select>`;
  } else if (type === 'check') {
    control = `<input type="checkbox" ${attrs}${value ? ' checked' : ''} style="width:auto">`;
  } else if (type === 'color') {
    control = `<div class="color-row"><input type="color" value="${esc(value)}" data-mirror="${esc(path)}"><input ${attrs} value="${esc(value)}" spellcheck="false"></div>`;
  } else if (type === 'image') {
    control = `<div class="img-field"><span class="preview">${value ? `<img src="${esc(value)}" alt="">` : ''}</span><div class="controls"><input ${attrs} value="${esc(value)}" list="imgs" spellcheck="false" placeholder="/img/... or upload"><span><label class="mini">Upload image<input type="file" accept="image/png,image/gif,image/webp,image/jpeg" data-upload="${esc(path)}" hidden></label></span></div></div>`;
  } else {
    control = `<input ${attrs} type="${type === 'date' ? 'date' : type === 'url' ? 'url' : 'text'}" value="${esc(value)}" spellcheck="${type === 'url' ? 'false' : 'true'}">`;
  }
  return `<label class="field"><span>${esc(label)}</span>${control}${opts.hint ? `<small>${esc(opts.hint)}</small>` : ''}</label>`;
};

const SCHEMAS = {
  stands: {
    title: 'name',
    blank: () => ({ id: `stand_${Date.now().toString(36)}`, name: 'New Stand', model: '', icon: '', color: '#a874f0', type: '', source: 'Stand Arrow', signature: '', text: '', moves: [] }),
    thumb: (s) => s.icon,
    swatch: (s) => s.color,
    fields: (p) => `<div class="row2">${field(`${p}.name`, 'Name', 'text', { title: true })}${field(`${p}.id`, 'Id', 'text', { hint: 'Lowercase letters, digits, _ or -. Used in links.' })}</div>
      ${field(`${p}.model`, 'Model (animated GIF)', 'image')}${field(`${p}.icon`, 'Small icon', 'image')}
      <div class="row2">${field(`${p}.type`, 'Type')}${field(`${p}.color`, 'Accent color', 'color')}</div>
      <div class="row2">${field(`${p}.signature`, 'Signature move')}${field(`${p}.source`, 'Found by')}</div>
      ${field(`${p}.text`, 'Description', 'textarea')}
      <span class="label" style="--tint:var(--gold)">Moveset</span>${listEditor(`${p}.moves`, 'move')}`,
  },
  wikiGroups: {
    title: 'label',
    blank: () => ({ label: 'New group', pages: [] }),
    fields: (p) => `${field(`${p}.label`, 'Heading', 'text', { title: true })}${field(`${p}.pages`, 'Pages', 'lines', { hint: `One page id per line, in order. Ids: ${state.data.guide.map((g) => g.id).join(', ')}` })}`,
  },
  'stands.moves': {
    title: 'name',
    blank: () => ({ name: 'New move', kind: 'Move', icon: '', short: '', text: '' }),
    thumb: (m) => m.icon,
    fields: (p) => `<div class="row2">${field(`${p}.name`, 'Name', 'text', { title: true })}${field(`${p}.kind`, 'Kind', 'select', { options: [['Move', 'Move'], ['Passive', 'Passive']] })}</div>
      ${field(`${p}.icon`, 'Icon', 'image', { hint: 'Leave empty to use the Stand icon.' })}${field(`${p}.short`, 'One-line summary')}${field(`${p}.text`, 'Details', 'textarea', { hint: 'Wrap words in ** to make them bold.' })}`,
  },
  roadmap: {
    title: 'title',
    blank: () => ({ phase: 'Later', title: 'New category', status: 'planned', color: '', text: '', cards: [] }),
    swatch: (c) => c.color,
    fields: (p) => `<div class="row2">${field(`${p}.title`, 'Category name', 'text', { title: true })}${field(`${p}.phase`, 'Small label above it')}</div>
      <div class="row2">${field(`${p}.status`, 'Status', 'select', { options: STATUS.map((s) => [s, CARD_STATUS[s]]) })}${field(`${p}.color`, 'Accent color (optional)', 'color')}</div>
      ${field(`${p}.text`, 'Description (optional)', 'textarea')}
      <span class="label" style="--tint:var(--gold)">Cards</span>${listEditor(`${p}.cards`, 'card')}`,
  },
  'roadmap.cards': {
    title: 'title',
    blank: () => ({ title: 'New card', status: 'planned', tag: '', image: '', text: '' }),
    thumb: (c) => c.image,
    fields: (p) => `<div class="row2">${field(`${p}.title`, 'Title', 'text', { title: true })}${field(`${p}.status`, 'Status', 'select', { options: STATUS.map((s) => [s, CARD_STATUS[s]]) })}</div>
      ${field(`${p}.tag`, 'Tag (optional)', 'text', { hint: 'A short label like Part 4 or Fabric.' })}
      ${field(`${p}.image`, 'Picture (optional)', 'image')}
      ${field(`${p}.text`, 'Details (optional)', 'md', { hint: MD_HINT })}`,
  },
  projects: {
    title: 'name',
    blank: () => ({ name: 'New project', phase: 'Coming up', when: 'TBA', status: 'Not announced', logo: '', text: '' }),
    thumb: (p) => p.logo,
    fields: (p) => `<div class="row2">${field(`${p}.name`, 'Name', 'text', { title: true })}${field(`${p}.status`, 'Status')}</div>
      <div class="row2">${field(`${p}.phase`, 'Phase', 'text', { hint: 'Projects with the same phase are grouped, in list order.' })}${field(`${p}.when`, 'Release', 'text', { hint: 'A date, a year, or TBA.' })}</div>
      ${field(`${p}.logo`, 'Title logo', 'image', { hint: 'Wide transparent PNG works best. Without one, the name is shown.' })}
      ${field(`${p}.text`, 'Description', 'textarea')}`,
  },
  staff: {
    title: 'name',
    blank: () => ({ name: 'New member', role: '', group: 'Development', avatar: '' }),
    thumb: (m) => m.avatar,
    fields: (p) => `<div class="row2">${field(`${p}.name`, 'Name', 'text', { title: true })}${field(`${p}.role`, 'Role')}</div>
      ${field(`${p}.group`, 'Group', 'text', { hint: 'Members with the same group are listed together.' })}${field(`${p}.avatar`, 'Avatar', 'image')}`,
  },
  commands: {
    title: 'name',
    blank: () => ({ name: '/command', text: '' }),
    fields: (p) => `${field(`${p}.name`, 'Command', 'text', { title: true })}${field(`${p}.text`, 'What it does', 'textarea')}`,
  },
  'supporters.steps': {
    title: 'title',
    blank: () => ({ title: 'New step', text: '' }),
    fields: (p) => `${field(`${p}.title`, 'Title', 'text', { title: true })}${field(`${p}.text`, 'Text', 'textarea', { hint: 'Wrap commands in `backticks`.' })}`,
  },
  homeLinks: {
    title: 'title',
    blank: () => ({ title: 'New tab', text: '', icon: '', link: '/' }),
    thumb: (t) => t.icon,
    fields: (p) => `<div class="row2">${field(`${p}.title`, 'Title', 'text', { title: true })}${field(`${p}.link`, 'Link', 'text', { hint: 'A site path like /wiki, or a full https:// address.' })}</div>
      ${field(`${p}.text`, 'Short line under the title')}${field(`${p}.icon`, 'Icon', 'image', { hint: 'Small square pixel art works best.' })}`,
  },
  gallery: {
    title: 'caption',
    blank: () => ({ image: '', caption: '' }),
    thumb: (g) => g.image,
    fields: (p) => `${field(`${p}.image`, 'Picture', 'image', { hint: 'Wide screenshots look best. They are cropped to fill the frame.' })}${field(`${p}.caption`, 'Caption (optional)', 'text', { title: true })}`,
  },
  'support.tiers': {
    title: 'name',
    blank: () => ({ name: 'New tier', price: '$', period: 'per month', logo: '', color: '#f6c744', featured: false, link: '', perks: [], extras: [] }),
    thumb: (t) => t.logo,
    swatch: (t) => t.color,
    fields: (p) => `<div class="row2">${field(`${p}.name`, 'Name', 'text', { title: true })}${field(`${p}.color`, 'Accent color', 'color')}</div>
      <div class="row2">${field(`${p}.price`, 'Price')}${field(`${p}.period`, 'Period')}</div>
      ${field(`${p}.logo`, 'Logo', 'image')}
      ${field(`${p}.perks`, 'Main perks', 'lines', { hint: 'One per line. Shown with gold markers.' })}
      ${field(`${p}.extras`, 'Also includes', 'lines', { hint: 'One per line. Shown smaller underneath.' })}
      ${field(`${p}.link`, 'Join link (optional)', 'url', { hint: 'Leave empty to use the Ko-fi page from General.' })}
      ${field(`${p}.featured`, 'Highlight this tier', 'check')}`,
  },
  blog: {
    title: 'title',
    blank: () => ({ slug: `post-${Date.now().toString(36)}`, title: 'New post', date: new Date().toISOString().slice(0, 10), author: state.me?.name ?? '', summary: '', body: '' }),
    fields: (p) => `${field(`${p}.title`, 'Title', 'text', { title: true })}
      <div class="row2">${field(`${p}.date`, 'Date', 'date')}${field(`${p}.author`, 'Author')}</div>
      ${field(`${p}.slug`, 'Link name', 'text', { hint: 'The post lives at /blog/<link name>.' })}
      ${field(`${p}.summary`, 'Summary', 'textarea')}
      ${field(`${p}.body`, 'Post', 'md', { hint: MD_HINT })}`,
  },
};

const schemaOf = (listPath) => SCHEMAS[listPath.replace(/\.\d+(?=\.)/g, '')];

function listEditor(listPath, label) {
  const schema = schemaOf(listPath);
  const items = get(state.draft, listPath) ?? [];
  return `${items.map((item, i) => {
    const p = `${listPath}.${i}`;
    const thumb = schema.thumb?.(item);
    const swatch = schema.swatch?.(item);
    return `<details class="item" data-open-key="${esc(p)}"${state.open.has(p) ? ' open' : ''}>
      <summary>${swatch ? `<span class="sw" style="background:${esc(swatch)}"></span>` : ''}${thumb ? `<img src="${esc(thumb)}" alt="">` : ''}<span class="t" data-title-of="${esc(p)}">${esc(item[schema.title] || '(untitled)')}</span>
        <button type="button" class="mini" data-act="up" data-list="${esc(listPath)}" data-i="${i}"${i === 0 ? ' disabled' : ''}>&uarr;</button>
        <button type="button" class="mini" data-act="down" data-list="${esc(listPath)}" data-i="${i}"${i === items.length - 1 ? ' disabled' : ''}>&darr;</button></summary>
      <div class="body">${schema.fields(p)}<div class="acts"><button type="button" class="mini danger" data-act="del" data-list="${esc(listPath)}" data-i="${i}">Remove</button></div></div>
    </details>`;
  }).join('')}<button type="button" class="mini add" data-act="add" data-list="${esc(listPath)}">+ Add ${esc(label)}</button>`;
}

const TABS = [
  ['Site', [['general', 'General'], ['home', 'Home page'], ['gallery', 'Gallery'], ['support', 'Supporter page'], ['pages', 'Page headings'], ['stands', 'Stands'], ['roadmap', 'Roadmap'], ['projects', 'Projects'], ['staff', 'Staff'], ['discordpage', 'Discord page'], ['blog', 'Blog']]],
  ['Wiki', [['wiki', 'Wiki pages'], ['wikiicons', 'Wiki groups and icons'], ['faq', 'FAQ']]],
  ['Files', [['images', 'Images']]],
];

const PANES = {
  general: () => `<h2>General</h2><p class="hint">Links and the short facts line under the home page hero.</p>
    ${field('discord', 'Discord invite', 'url')}
    <label class="field"><span>Ko-fi page</span><input type="url" data-kofi value="${esc(state.kofi)}" spellcheck="false"><small>Also used by the bot's Ko-fi buttons.</small></label>
    ${field('facts', 'Facts line', 'lines', { hint: 'One per line. Short.' })}`,
  home: () => `<h2>Home page</h2><p class="hint">Text on the home page, top to bottom.</p>
    ${field('home.lead', 'Headline')}${field('home.leadMore', 'Headline, second part (grey)')}${field('home.note', 'Small note under the buttons')}
    <div class="row2">${field('home.roadmapTitle', 'Roadmap box heading')}${field('home.newsTitle', 'Blog box heading')}</div>
    ${field('home.roadmapText', 'Roadmap box note', 'textarea')}
    ${field('home.joinTitle', 'Discord box heading')}${field('home.joinText', 'Discord box text', 'textarea')}
    <div style="height:32px"></div><span class="label" style="--tint:var(--gold)">Small tabs under the boxes</span>${listEditor('homeLinks', 'tab')}`,
  pages: () => `<h2>Page headings</h2><p class="hint">The big title and subtitle at the top of each page.</p>
    ${Object.keys(state.draft.pages).map((id) => `<details class="item" data-open-key="pages.${id}"${state.open.has(`pages.${id}`) ? ' open' : ''}><summary><span class="t">/${esc(id)}</span></summary><div class="body">${field(`pages.${id}.title`, 'Title')}${field(`pages.${id}.text`, 'Subtitle', 'textarea')}</div></details>`).join('')}`,
  gallery: () => `<h2>Gallery</h2><p class="hint">Pictures shown behind the logo at the top of the home page. They fade into each other every 5 seconds and are darkened so the title stays readable. Wide pictures work best.</p>${listEditor('gallery', 'picture')}`,
  support: () => `<h2>Supporter page</h2><p class="hint">The Become a Supporter page. Its title and subtitle are under Page headings, and the "link your account" steps are shared with the Discord page.</p>
    <span class="label" style="--tint:var(--gold)">Tiers</span>${listEditor('support.tiers', 'tier')}
    <div style="height:32px"></div><span class="label" style="--tint:var(--gold)">In-game perks box</span>
    ${field('support.inGameTitle', 'Heading')}${field('support.inGame', 'Perks', 'lines', { hint: 'One per line.' })}`,
  stands: () => `<h2>Stands</h2><p class="hint">Shown in the home page picker and on /stands, in this order.</p>${listEditor('stands', 'Stand')}`,
  roadmap: () => `<h2>Roadmap</h2><p class="hint">Categories show as columns left to right, each with its own cards. In progress is gold and Done is green. The home page shows the first five cards of each category.</p>${listEditor('roadmap', 'category')}`,
  projects: () => `<h2>Projects</h2><p class="hint">Current and future mod projects.</p>${listEditor('projects', 'project')}`,
  staff: () => `<h2>Staff</h2><p class="hint">Without an avatar, the first letter of the name is shown.</p>${listEditor('staff', 'member')}`,
  discordpage: () => `<h2>Discord page</h2><p class="hint">The invite link is under General.</p>
    <span class="label" style="--tint:var(--gold)">Bot commands</span>${listEditor('commands', 'command')}
    <div style="height:32px"></div><span class="label" style="--tint:var(--gold)">Supporter section</span>
    ${field('supporters.title', 'Heading')}${field('supporters.text', 'Text', 'textarea')}${listEditor('supporters.steps', 'step')}`,
  blog: () => `<h2>Blog</h2><p class="hint">Newest date first on the site.</p>${listEditor('blog', 'post')}`,
  wikiicons: () => `<h2>Wiki groups</h2><p class="hint">Headings the wiki pages are sorted under, on the wiki page and in its sidebar. Pages not in any group are listed last under More.</p>
    ${listEditor('wikiGroups', 'group')}
    <div style="height:32px"></div><h2>Wiki icons</h2><p class="hint">The icon beside each wiki page in lists and the sidebar.</p>
    ${state.data.guide.map((page) => {
      if (state.draft.wikiIcons[page.id] === undefined) state.draft.wikiIcons[page.id] = '';
      return field(`wikiIcons.${page.id}`, page.title, 'image');
    }).join('')}`,
  wiki: () => liveList('wiki'),
  faq: () => liveList('faq'),
  images: () => `<h2>Images</h2><p class="hint">Upload PNG, GIF, WebP or JPEG, up to 10 MB each. Copy a path into any image field, or use the Upload button on the field itself.</p>
    <label class="drop" data-drop>Drop images here or click to choose<input type="file" accept="image/png,image/gif,image/webp,image/jpeg" multiple data-upload-many hidden></label>
    <div class="files">${state.data.files.map((f) => `<div class="file"><span class="pic"><img src="${esc(f.url)}" alt="" loading="lazy"></span><code>${esc(f.url)}</code><span class="acts"><button type="button" class="mini" data-copy="${esc(f.url)}">Copy path</button> <button type="button" class="mini danger" data-del-file="${f.id}">Delete</button></span></div>`).join('') || '<p class="empty">No uploads yet.</p>'}</div>`,
};

const LIVE = {
  wiki: {
    title: 'Wiki pages',
    hint: "These are the same pages as the bot's /guide command. Saving one updates Discord and the site at once.",
    items: () => [...state.data.guide, ...state.wikiNew],
    blank: () => ({ id: '', title: 'New page', summary: '', intro: '', sections: '## Heading\n- A line', isNew: true }),
    news: 'wikiNew',
    endpoint: 'guide',
    fields: (item, k) => `${liveField(k, 'id', 'Id', item, { readonly: !item.isNew, hint: item.isNew ? 'Lowercase letters, digits or -. The page lives at /wiki/<id>.' : '' })}
      ${liveField(k, 'title', 'Title', item)}${liveField(k, 'summary', 'Summary (one line)', item)}
      ${liveField(k, 'intro', 'Intro', item, { area: true })}
      ${liveField(k, 'sections', 'Sections', item, { area: 'tall', hint: 'Start each section with a line like ## How to get it. Use - for bullets and 1. for steps.' })}`,
    payload: (v) => ({ title: v.title, summary: v.summary, intro: v.intro, sections: v.sections }),
  },
  faq: {
    title: 'FAQ',
    hint: "The same questions as the bot's /faq command. Saving one updates Discord and the site at once.",
    items: () => [...state.data.faq, ...state.faqNew],
    blank: () => ({ id: '', group: '', question: 'New question', answer: '', related: '', isNew: true }),
    news: 'faqNew',
    endpoint: 'faq',
    fields: (item, k) => `${liveField(k, 'id', 'Id', item, { readonly: !item.isNew, hint: item.isNew ? 'Lowercase letters, digits or -.' : '' })}
      ${liveField(k, 'question', 'Question', item)}${liveField(k, 'group', 'Group', item, { hint: 'Questions with the same group are listed together.' })}
      ${liveField(k, 'answer', 'Answer', item, { area: 'tall' })}
      ${liveField(k, 'related', 'Follow-up question ids', item, { hint: 'Comma separated. Use kofi for the Ko-fi button.' })}`,
    payload: (v) => ({ group: v.group, question: v.question, answer: v.answer, related: v.related }),
  },
};

function liveField(key, name, label, item, { readonly = false, area = false, hint = '' } = {}) {
  const attrs = `data-live="${esc(key)}" data-name="${name}"${readonly ? ' readonly' : ''}`;
  const control = area
    ? `<textarea ${attrs}${area === 'tall' ? ' class="tall"' : ''}>${esc(item[name])}</textarea>`
    : `<input ${attrs} value="${esc(item[name])}" spellcheck="${name === 'id' ? 'false' : 'true'}">`;
  return `<label class="field"><span>${esc(label)}</span>${control}${hint ? `<small>${esc(hint)}</small>` : ''}</label>`;
}

function liveList(kind) {
  const conf = LIVE[kind];
  const items = conf.items();
  const saved = items.filter((item) => !item.isNew).length;
  return `<h2>${esc(conf.title)}</h2><p class="hint">${esc(conf.hint)}</p>
    ${items.map((item, i) => {
      const key = `${kind}:${i}`;
      return `<details class="item" data-open-key="${esc(key)}"${state.open.has(key) ? ' open' : ''}>
        <summary><span class="t">${esc(kind === 'faq' ? item.question : item.title)}${item.isNew ? ' (not saved)' : ''}</span>
          ${item.isNew ? '' : `<button type="button" class="mini" data-live-move="up" data-kind="${kind}" data-i="${i}"${i === 0 ? ' disabled' : ''}>&uarr;</button>
          <button type="button" class="mini" data-live-move="down" data-kind="${kind}" data-i="${i}"${i >= saved - 1 ? ' disabled' : ''}>&darr;</button>`}</summary>
        <div class="body">${conf.fields(item, key)}<div class="acts"><button type="button" class="btn small" data-live-save="${esc(key)}">Save</button> <button type="button" class="mini danger" data-live-del="${esc(key)}">${item.isNew ? 'Discard' : 'Delete'}</button></div></div>
      </details>`;
    }).join('')}<button type="button" class="mini add" data-live-add="${kind}">+ Add ${kind === 'faq' ? 'question' : 'page'}</button>`;
}

function imageOptions() {
  const paths = new Set(state.data.files.map((f) => f.url));
  for (const s of state.draft.stands) {
    paths.add(s.model);
    paths.add(s.icon);
  }
  for (const icon of Object.values(state.draft.wikiIcons)) paths.add(icon);
  for (const m of state.draft.staff) paths.add(m.avatar);
  for (const c of state.draft.roadmap) for (const card of c.cards ?? []) paths.add(card.image);
  for (const s of state.draft.stands) for (const m of s.moves ?? []) paths.add(m.icon);
  for (const g of state.draft.gallery) paths.add(g.image);
  for (const t of state.draft.homeLinks ?? []) paths.add(t.icon);
  for (const t of state.draft.support.tiers) paths.add(t.logo);
  for (const p of state.draft.projects) paths.add(p.logo);
  return [...paths].filter(Boolean).map((p) => `<option value="${esc(p)}">`).join('');
}

function render() {
  const pane = PANES[state.tab]();
  root.innerHTML = `<div class="shell">
    <nav class="tabs">${TABS.map(([group, tabs]) => `<h4>${esc(group.toUpperCase())}</h4>${tabs.map(([id, label]) => `<button type="button" data-tab="${id}" class="${id === state.tab ? 'on' : ''}">${esc(label)}</button>`).join('')}`).join('')}</nav>
    <section class="pane">${pane}</section>
  </div>
  <datalist id="imgs">${imageOptions()}</datalist>
  <div class="savebar${state.dirty ? ' show' : ''}"><div class="inner"><span>You have unsaved changes.</span><button type="button" class="mini" data-discard>Discard</button><button type="button" class="btn small" data-save>Save changes</button></div></div>`;
}

function markDirty() {
  state.dirty = true;
  document.querySelector('.savebar')?.classList.add('show');
}

async function upload(file) {
  const name = file.name.replace(/[^A-Za-z0-9._-]/g, '_').slice(-80) || 'image';
  const result = await call('POST', `/admin/api/files?name=${encodeURIComponent(name)}`, file, { 'Content-Type': file.type || 'application/octet-stream' });
  state.data.files.unshift({ id: result.id, name, url: result.url });
  return result.url;
}

async function refresh() {
  const fresh = await call('GET', '/admin/api/state');
  state.data.guide = fresh.guide;
  state.data.faq = fresh.faq;
  state.data.files = fresh.files;
}

function liveValues(key) {
  const values = {};
  for (const input of root.querySelectorAll(`[data-live="${CSS.escape(key)}"]`)) {
    values[input.dataset.name] = input.value;
  }
  return values;
}

function liveItem(key) {
  const [kind, index] = key.split(':');
  return { kind, index: Number(index), item: LIVE[kind].items()[Number(index)] };
}

root.addEventListener('input', (event) => {
  const el = event.target;
  if (el.dataset.mirror) {
    const text = root.querySelector(`[data-path="${CSS.escape(el.dataset.mirror)}"]`);
    text.value = el.value;
    set(state.draft, el.dataset.mirror, el.value);
    markDirty();
    return;
  }
  if (el.dataset.kofi !== undefined) {
    state.kofi = el.value.trim();
    markDirty();
    return;
  }
  if (!el.dataset.path) return;
  const value = el.dataset.type === 'lines' ? el.value.split('\n').map((line) => line.trim()).filter(Boolean)
    : el.dataset.type === 'check' ? el.checked : el.value;
  set(state.draft, el.dataset.path, value);
  if (el.dataset.title) {
    const item = el.dataset.path.split('.').slice(0, -1).join('.');
    const label = root.querySelector(`[data-title-of="${CSS.escape(item)}"]`);
    if (label) label.textContent = value || '(untitled)';
  }
  if (el.dataset.type === 'image') {
    const preview = el.closest('.img-field').querySelector('.preview');
    preview.innerHTML = value ? `<img src="${esc(value)}" alt="">` : '';
  }
  if (el.dataset.type === 'color') {
    const picker = el.parentElement.querySelector('input[type=color]');
    if (/^#[0-9a-f]{6}$/i.test(value)) picker.value = value;
  }
  markDirty();
});

root.addEventListener('toggle', (event) => {
  const key = event.target.dataset?.openKey;
  if (!key) return;
  if (event.target.open) state.open.add(key);
  else state.open.delete(key);
}, true);

root.addEventListener('change', async (event) => {
  const el = event.target;
  if (el.dataset.upload) {
    const file = el.files[0];
    if (!file) return;
    try {
      const url = await upload(file);
      set(state.draft, el.dataset.upload, url);
      markDirty();
      render();
      toast('Uploaded. Save changes to use it.');
    } catch (error) {
      toast(error.message, true);
    }
  }
  if (el.dataset.uploadMany !== undefined) {
    await uploadMany([...el.files]);
  }
});

async function uploadMany(files) {
  let done = 0;
  for (const file of files) {
    try {
      await upload(file);
      done++;
    } catch (error) {
      toast(`${file.name}: ${error.message}`, true);
    }
  }
  if (done) toast(`Uploaded ${done === 1 ? 'one image' : `${done} images`}.`);
  render();
}

root.addEventListener('dragover', (event) => {
  const drop = event.target.closest('[data-drop]');
  if (!drop) return;
  event.preventDefault();
  drop.classList.add('over');
});
root.addEventListener('dragleave', (event) => event.target.closest('[data-drop]')?.classList.remove('over'));
root.addEventListener('drop', (event) => {
  const drop = event.target.closest('[data-drop]');
  if (!drop) return;
  event.preventDefault();
  uploadMany([...event.dataTransfer.files]);
});

root.addEventListener('click', async (event) => {
  const el = event.target.closest('button, [data-tab]');
  if (!el) return;
  if (el.closest('summary') && el.matches('button')) event.preventDefault();

  if (el.dataset.tab) {
    state.tab = el.dataset.tab;
    render();
    return;
  }

  if (el.dataset.act) {
    const list = get(state.draft, el.dataset.list);
    const i = Number(el.dataset.i);
    const schema = schemaOf(el.dataset.list);
    const key = (n) => `${el.dataset.list}.${n}`;
    if (el.dataset.act === 'add') {
      list.push(schema.blank());
      state.open.add(key(list.length - 1));
    } else if (el.dataset.act === 'del') {
      if (!confirm(`Remove "${list[i][schema.title] || 'this entry'}"?`)) return;
      list.splice(i, 1);
      state.open.clear();
    } else {
      const j = el.dataset.act === 'up' ? i - 1 : i + 1;
      [list[i], list[j]] = [list[j], list[i]];
      const wasOpen = state.open.has(key(i));
      state.open.delete(key(i));
      if (wasOpen) state.open.add(key(j));
    }
    markDirty();
    render();
    return;
  }

  if (el.dataset.save !== undefined) {
    el.disabled = true;
    try {
      await call('PUT', '/admin/api/content', state.draft);
      if (state.kofi !== state.data.kofi) {
        await call('PUT', '/admin/api/kofi', { url: state.kofi });
        state.data.kofi = state.kofi;
      }
      state.data.content = clone(state.draft);
      state.dirty = false;
      render();
      toast('Saved. The site is updated.');
    } catch (error) {
      el.disabled = false;
      toast(error.message, true);
    }
    return;
  }

  if (el.dataset.discard !== undefined) {
    if (!confirm('Throw away your unsaved changes?')) return;
    state.draft = clone(state.data.content);
    state.kofi = state.data.kofi;
    state.dirty = false;
    render();
    return;
  }

  if (el.dataset.liveAdd) {
    const conf = LIVE[el.dataset.liveAdd];
    state[conf.news].push(conf.blank());
    state.open.add(`${el.dataset.liveAdd}:${conf.items().length - 1}`);
    render();
    return;
  }

  if (el.dataset.liveSave) {
    const { kind, item } = liveItem(el.dataset.liveSave);
    const conf = LIVE[kind];
    const values = liveValues(el.dataset.liveSave);
    const id = (item.isNew ? values.id : item.id).trim().toLowerCase();
    el.disabled = true;
    try {
      await call('PUT', `/admin/api/${conf.endpoint}/${encodeURIComponent(id)}`, conf.payload(values));
      if (item.isNew) state[conf.news].splice(state[conf.news].indexOf(item), 1);
      await refresh();
      state.open.clear();
      render();
      toast('Saved. Discord and the site are updated.');
    } catch (error) {
      el.disabled = false;
      toast(error.message, true);
    }
    return;
  }

  if (el.dataset.liveDel) {
    const { kind, item } = liveItem(el.dataset.liveDel);
    const conf = LIVE[kind];
    if (item.isNew) {
      state[conf.news].splice(state[conf.news].indexOf(item), 1);
      state.open.clear();
      render();
      return;
    }
    if (!confirm(`Delete "${kind === 'faq' ? item.question : item.title}" from the site and Discord?`)) return;
    try {
      await call('DELETE', `/admin/api/${conf.endpoint}/${encodeURIComponent(item.id)}`);
      await refresh();
      state.open.clear();
      render();
      toast('Deleted.');
    } catch (error) {
      toast(error.message, true);
    }
    return;
  }

  if (el.dataset.liveMove) {
    const kind = el.dataset.kind;
    const conf = LIVE[kind];
    const ids = conf.items().filter((item) => !item.isNew).map((item) => item.id);
    const i = Number(el.dataset.i);
    const j = el.dataset.liveMove === 'up' ? i - 1 : i + 1;
    [ids[i], ids[j]] = [ids[j], ids[i]];
    try {
      await call('PUT', `/admin/api/${conf.endpoint}-order`, { ids });
      await refresh();
      state.open.clear();
      render();
    } catch (error) {
      toast(error.message, true);
    }
    return;
  }

  if (el.dataset.copy) {
    await navigator.clipboard?.writeText(el.dataset.copy).catch(() => {});
    toast(`Copied ${el.dataset.copy}`);
    return;
  }

  if (el.dataset.delFile) {
    if (!confirm('Delete this image? Anything still using it will show a broken image.')) return;
    try {
      await call('DELETE', `/admin/api/files/${el.dataset.delFile}`);
      state.data.files = state.data.files.filter((f) => String(f.id) !== el.dataset.delFile);
      render();
      toast('Deleted.');
    } catch (error) {
      toast(error.message, true);
    }
  }
});

document.querySelector('.signout').addEventListener('click', async () => {
  if (state.dirty && !confirm('You have unsaved changes. Sign out anyway?')) return;
  await fetch('/auth/logout', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
  state.dirty = false;
  location.href = '/admin';
});

window.addEventListener('beforeunload', (event) => {
  if (state.dirty) event.preventDefault();
});

async function start() {
  try {
    state.me = await call('GET', '/admin/api/me');
  } catch {
    return;
  }
  state.data = await call('GET', '/admin/api/state');
  state.draft = clone(state.data.content);
  state.kofi = state.data.kofi;
  userBar.hidden = false;
  userBar.querySelector('.who').textContent = state.me.name;
  if (location.search) history.replaceState(null, '', '/admin');
  render();
}

start();
