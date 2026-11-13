const app = document.getElementById('app');
const nav = document.querySelector('.nav');
const toggle = document.querySelector('.menu-toggle');

const state = { content: null, live: null };

const esc = (text) => String(text ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

const safeLink = (url) => /^(https:\/\/|\/(?!\/))[^\s<>]*$/.test(url);

const linkTag = (url, label) => (url.startsWith('/')
  ? `<a href="${url}" data-link>${label}</a>`
  : `<a href="${url}" target="_blank" rel="noopener">${label}</a>`);

const inline = (text) => esc(text)
  .replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (all, alt, url) => (safeLink(url) ? `<img class="inline-img" src="${url}" alt="${alt}" loading="lazy">` : all))
  .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (all, label, url) => (safeLink(url) ? linkTag(url, label) : all))
  .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  .replace(/`(.+?)`/g, '<code>$1</code>');

function picture(line) {
  const found = line.trim().match(/^!\[([^\]]*)\]\(([^)\s]+)\)$/);
  if (!found || !safeLink(esc(found[2]))) return null;
  const [, alt, url] = found;
  return `<figure class="md-figure"><img src="${esc(url)}" alt="${esc(alt)}" loading="lazy">${alt ? `<figcaption>${esc(alt)}</figcaption>` : ''}</figure>`;
}

function markdown(text) {
  const blocks = String(text ?? '').replace(/\r/g, '').split(/\n{2,}/);
  return blocks.map((block) => {
    const lines = block.split('\n').filter((line) => line.trim());
    if (lines.length && lines.every((line) => /^\s*\d+\.\s+/.test(line))) {
      return `<ol>${lines.map((line) => `<li>${inline(line.replace(/^\s*\d+\.\s+/, ''))}</li>`).join('')}</ol>`;
    }
    const parts = [];
    let list = null;
    for (const line of lines) {
      const bullet = line.match(/^\s*-\s+(.*)$/);
      if (bullet) {
        list ??= [];
        list.push(`<li>${inline(bullet[1])}</li>`);
      } else {
        if (list) {
          parts.push(`<ul>${list.join('')}</ul>`);
          list = null;
        }
        parts.push(picture(line) ?? `<p>${inline(line)}</p>`);
      }
    }
    if (list) parts.push(`<ul>${list.join('')}</ul>`);
    return parts.join('');
  }).join('');
}

const slug = (text) => String(text).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

const formatDate = (iso) => new Date(`${iso}T12:00:00`).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });

const pad = (n) => String(n + 1).padStart(2, '0');

async function load() {
  const [content, live] = await Promise.all([
    fetch('/content.json').then((res) => res.json()),
    fetch('/v1/site').then((res) => (res.ok ? res.json() : null)).catch(() => null),
  ]);
  state.content = content;
  state.live = live ?? { guide: [], faq: [], kofi: null };
  if (state.live.kofi) {
    document.querySelector('.kofi-link').href = state.live.kofi;
    document.querySelector('.kofi-col').hidden = false;
  }
  if (content.discord) {
    const cta = document.querySelector('[data-invite]');
    cta.href = content.discord;
    cta.target = '_blank';
    cta.rel = 'noopener';
    cta.removeAttribute('data-link');
  }
}

function head(id, opts) {
  const page = state.content.pages?.[id] ?? { title: id, text: '' };
  return pageHead(page.title, page.text, opts);
}

function pageHead(title, text, { tint, crumbs } = {}) {
  return `<section class="page-head"${tint ? ` style="--tint:${tint}"` : ''}><div class="frame">${crumbs ? `<div class="crumbs">${crumbs}</div>` : ''}<h1>${esc(title)}</h1>${text ? `<p>${esc(text)}</p>` : ''}</div></section>`;
}

function inviteButton(label = 'Join Discord') {
  const invite = state.content.discord;
  return invite
    ? `<a class="btn" href="${esc(invite)}" target="_blank" rel="noopener">${esc(label)}</a>`
    : '<span class="note">Invite link coming soon</span>';
}

function wikiIcon(id) {
  const icon = state.content.wikiIcons?.[id];
  return esc(icon || '/img/heavens_arrow.png');
}

function standStats(stand) {
  return `<table class="stats"><tbody>
    <tr><th>Type</th><td>${esc(stand.type)}</td></tr>
    <tr><th>Signature</th><td>${esc(stand.signature)}</td></tr>
    <tr><th>Found by</th><td>${esc(stand.source)}</td></tr>
  </tbody></table>`;
}

function moveCount(stand) {
  const moves = (stand.moves ?? []).filter((m) => m.kind === 'Move').length;
  const passives = (stand.moves ?? []).length - moves;
  return `<span class="arrow-link stand-more">${moves} moves, ${passives} passives &rarr;</span>`;
}

function moveCard(move, stand) {
  return `<div class="move"><span class="slot"><img src="${esc(move.icon || stand.icon)}" alt="" loading="lazy"></span><div><h3>${esc(move.name)}</h3>${move.short ? `<p class="move-short">${esc(move.short)}</p>` : ''}${move.text ? `<p>${inline(move.text)}</p>` : ''}</div></div>`;
}

function standPage(id) {
  const stands = state.content.stands;
  const at = stands.findIndex((s) => s.id === id);
  if (at < 0) return views.missing();
  const s = stands[at];
  const moves = (s.moves ?? []).filter((m) => m.kind === 'Move');
  const passives = (s.moves ?? []).filter((m) => m.kind === 'Passive');
  const prev = stands[(at + stands.length - 1) % stands.length];
  const next = stands[(at + 1) % stands.length];
  return `<section class="band flat stand-hero" style="--c:${esc(s.color)}"><div class="frame">
      <div class="crumbs"><a href="/wiki" data-link>Wiki</a> / <a href="/stands" data-link>Stands</a> / ${esc(s.name)}</div>
      <div class="stand-top">
        <div class="stand-model big"><img src="${esc(s.model)}" alt="${esc(s.name)} model"></div>
        <div class="stand-info"><span class="label">${esc(s.type)}</span><h1>${esc(s.name)}</h1><p>${esc(s.text)}</p>${standStats(s)}
          <div class="stand-jump"><a href="/stands/${esc(prev.id)}" data-link>&larr; ${esc(prev.name)}</a><a href="/stands/${esc(next.id)}" data-link>${esc(next.name)} &rarr;</a></div></div>
      </div>
    </div></section>
    ${moves.length ? `<section class="band flat tight" style="--tint:${esc(s.color)}"><div class="frame"><span class="label">Moveset</span><h2>Moves</h2><div class="moves">${moves.map((m) => moveCard(m, s)).join('')}</div></div></section>` : ''}
    ${passives.length ? `<section class="band flat tight" style="--tint:${esc(s.color)}"><div class="frame"><span class="label">Always on</span><h2>Passives</h2><div class="moves">${passives.map((m) => moveCard(m, s)).join('')}</div></div></section>` : ''}`;
}

function guideEntry(page) {
  return `<a class="entry" href="/wiki/${esc(page.id)}" data-link><span class="slot"><img src="${wikiIcon(page.id)}" alt=""></span><div><h3>${esc(page.title)}</h3><p>${esc(page.summary)}</p></div><span class="go">&rarr;</span></a>`;
}

function postRow(post) {
  return `<a class="post-row" href="/blog/${esc(post.slug)}" data-link><time datetime="${esc(post.date)}">${esc(formatDate(post.date))}</time><div><h3>${esc(post.title)}</h3><p>${esc(post.summary)}</p></div></a>`;
}

const CARD_STATUS = { active: 'In progress', planned: 'Planned', done: 'Done' };

function roadmapCard(card) {
  return `<article class="rm-card ${esc(card.status)}" data-status="${esc(card.status)}">
    ${card.image ? `<img src="${esc(card.image)}" alt="" loading="lazy">` : ''}
    <div class="rm-card-body"><div class="rm-meta"><span class="rm-status">${CARD_STATUS[card.status] ?? ''}</span>${card.tag ? `<span class="rm-tag">${esc(card.tag)}</span>` : ''}</div>
    <h4>${esc(card.title)}</h4>${card.text ? `<div class="rm-text">${markdown(card.text)}</div>` : ''}</div>
  </article>`;
}

function roadmapBoard(columns) {
  const counts = { all: 0, active: 0, planned: 0, done: 0 };
  for (const column of columns) for (const card of column.cards ?? []) { counts.all += 1; counts[card.status] += 1; }
  return `<div class="rm-filter" role="group" aria-label="Filter cards">${['all', 'active', 'planned', 'done'].map((key) => `<button type="button" data-rm-filter="${key}" class="${key === 'all' ? 'on' : ''}">${key === 'all' ? 'All' : CARD_STATUS[key]} <span>${counts[key]}</span></button>`).join('')}</div>
    <div class="rm-board" data-rm-board>${columns.map((column) => `<section class="rm-col ${esc(column.status)}"${column.color ? ` style="--c:${esc(column.color)}"` : ''}>
      <header class="rm-col-head"><div class="when">${esc(column.phase)}</div><h3>${esc(column.title)}</h3>${column.text ? `<p>${esc(column.text)}</p>` : ''}<span class="rm-count">${(column.cards ?? []).length}</span></header>
      <div class="rm-cards">${(column.cards ?? []).map(roadmapCard).join('') || '<p class="rm-empty">Nothing here yet.</p>'}</div>
    </section>`).join('')}</div>`;
}

function projectTimeline(projects) {
  const phases = [];
  for (const project of projects) {
    const name = project.phase || project.status || 'Projects';
    let phase = phases.find((p) => p.name === name);
    if (!phase) phases.push(phase = { name, items: [] });
    phase.items.push(project);
  }
  let n = 0;
  return `<div class="tl">${phases.map((phase) => `<div class="tl-phase"><h3 class="tl-head">${esc(phase.name)}</h3>${phase.items.map((p) => `<article class="tl-item">
    <div class="tl-card">${p.logo ? `<img src="${esc(p.logo)}" alt="${esc(p.name)}" loading="lazy">` : `<span class="tl-name">${esc(p.name)}</span>`}</div>
    <div class="tl-info"><div class="tl-meta"><span class="tl-n">${pad(n++)}</span>${p.when ? `<span class="tl-when">${esc(p.when)}</span>` : ''}${p.status && p.status !== p.when ? `<span class="chip">${esc(p.status)}</span>` : ''}</div><h4>${esc(p.name)}</h4><p>${esc(p.text)}</p></div>
  </article>`).join('')}</div>`).join('')}</div>`;
}

function heroBackdrop(pics) {
  return `<div class="hero-bg" data-gallery>${pics.map((g, i) => `<figure class="slide${i === 0 ? ' on' : ''}"><img src="${esc(g.image)}" alt="${esc(g.caption)}"${i ? ' loading="lazy"' : ''}>${g.caption ? `<figcaption>${esc(g.caption)}</figcaption>` : ''}</figure>`).join('')}
    ${pics.length > 1 ? `<div class="dots">${pics.map((_, i) => `<button type="button" data-slide="${i}" class="${i === 0 ? 'on' : ''}" aria-label="Picture ${i + 1}"></button>`).join('')}</div>` : ''}</div>`;
}

function featureNext() {
  const { home, roadmap } = state.content;
  const current = roadmap.find((p) => p.status === 'active') ?? roadmap[0];
  const cards = (current?.cards ?? []).slice(0, 4);
  return `<article class="feature" style="--tint:var(--magenta)">
    <span class="label">Roadmap</span><h2>${esc(home.roadmapTitle)}</h2>
    ${current ? `<div class="feature-phase"><span>${esc(current.phase)}</span>${esc(current.title)}</div>
    <ul class="feature-list">${cards.map((c) => `<li class="${esc(c.status)}"><span>${CARD_STATUS[c.status] ?? ''}</span>${esc(c.title)}</li>`).join('')}</ul>` : ''}
    <p class="feature-note">${esc(home.roadmapText)}</p>
    <a class="arrow-link" href="/roadmap" data-link>Full roadmap &rarr;</a>
  </article>`;
}

function featureBlog() {
  const [latest, ...older] = sortedPosts();
  return `<article class="feature" style="--tint:var(--teal)">
    <span class="label">News</span><h2>${esc(state.content.home.newsTitle)}</h2>
    ${latest ? `<a class="feature-post" href="/blog/${esc(latest.slug)}" data-link><time datetime="${esc(latest.date)}">${esc(formatDate(latest.date))}</time><h3>${esc(latest.title)}</h3><p>${esc(latest.summary)}</p></a>
    ${older.slice(0, 2).map((p) => `<a class="feature-older" href="/blog/${esc(p.slug)}" data-link><time datetime="${esc(p.date)}">${esc(formatDate(p.date))}</time>${esc(p.title)}</a>`).join('')}` : '<p class="feature-note">No posts yet.</p>'}
    <a class="arrow-link" href="/blog" data-link>All posts &rarr;</a>
  </article>`;
}

function featureDiscord() {
  const home = state.content.home;
  return `<article class="feature feature-discord" style="--tint:var(--blurple)">
    <span class="label">Community</span><h2>${esc(home.joinTitle)}</h2>
    <p class="feature-text">${esc(home.joinText)}</p>
    <div class="feature-cta">${inviteButton()}</div>
    <a class="arrow-link" href="/discord" data-link>Server info &rarr;</a>
  </article>`;
}

function exploreTabs() {
  const links = state.content.homeLinks ?? [];
  if (!links.length) return '';
  return `<section class="band flat tight explore"><div class="frame">
    <span class="label" style="--tint:var(--gold)">Explore</span>
    <div class="home-tabs">${links.map((t) => `<a class="home-tab" href="${esc(t.link)}"${t.link.startsWith('/') ? ' data-link' : ' target="_blank" rel="noopener"'}>${t.icon ? `<img src="${esc(t.icon)}" alt="">` : ''}<span><b>${esc(t.title)}</b><small>${esc(t.text)}</small></span></a>`).join('')}</div>
  </div></section>`;
}

function showSlide(box, index) {
  const slides = box.querySelectorAll('.slide');
  const next = (index + slides.length) % slides.length;
  slides.forEach((slide, i) => slide.classList.toggle('on', i === next));
  box.querySelectorAll('[data-slide]').forEach((dot, i) => dot.classList.toggle('on', i === next));
  box.dataset.at = String(next);
}

let slideTimer = null;

function startSlides() {
  clearInterval(slideTimer);
  const box = document.querySelector('[data-gallery]');
  if (!box || box.querySelectorAll('.slide').length < 2) return;
  slideTimer = setInterval(() => showSlide(box, Number(box.dataset.at ?? 0) + 1), 5000);
}

function tierCard(tier) {
  const link = tier.link || state.live.kofi;
  return `<article class="tier${tier.featured ? ' featured' : ''}"${tier.color ? ` style="--c:${esc(tier.color)}"` : ''}>
    ${tier.featured ? '<span class="tier-flag">Most picked</span>' : ''}
    <div class="tier-top">${tier.logo ? `<img src="${esc(tier.logo)}" alt="">` : ''}<h3>${esc(tier.name)}</h3>
      <div class="tier-price"><b>${esc(tier.price)}</b> <span>${esc(tier.period)}</span></div></div>
    <ul class="tier-perks">${tier.perks.map((perk) => `<li>${esc(perk)}</li>`).join('')}</ul>
    ${tier.extras.length ? `<div class="tier-extras"><span>Also includes</span><ul>${tier.extras.map((perk) => `<li>${esc(perk)}</li>`).join('')}</ul></div>` : ''}
    ${link ? `<a class="btn${tier.featured ? '' : ' line'} tier-join" href="${esc(link)}" target="_blank" rel="noopener">Join ${esc(tier.name)}</a>` : ''}
  </article>`;
}

function sortedPosts() {
  return [...state.content.blog].sort((a, b) => b.date.localeCompare(a.date));
}

function wikiGroups() {
  const pages = state.live.guide;
  const used = new Set();
  const groups = (state.content.wikiGroups ?? []).map((g) => {
    const list = g.pages.map((id) => pages.find((p) => p.id === id)).filter((p) => p && !used.has(p.id));
    for (const p of list) used.add(p.id);
    return { label: g.label, pages: list };
  });
  const rest = pages.filter((p) => !used.has(p.id));
  if (rest.length) groups.push({ label: groups.length ? 'More' : 'Pages', pages: rest });
  return groups.filter((g) => g.pages.length);
}

function wikiSide(active) {
  return `<aside class="side">
    ${wikiGroups().map((g) => `<h4>${esc(g.label.toUpperCase())}</h4>${g.pages.map((p) => `<a href="/wiki/${esc(p.id)}" data-link class="${p.id === active ? 'active' : ''}"><img src="${wikiIcon(p.id)}" alt="">${esc(p.title)}</a>`).join('')}`).join('')}
    <h4>REFERENCE</h4>
    <a href="/stands" data-link class="${active === 'stands-list' ? 'active' : ''}"><img src="/img/star_platinum.png" alt="">All Stands</a>
    <a href="/faq" data-link><img src="/img/hermit_path_hamon_book.png" alt="">FAQ</a>
  </aside>`;
}

const views = {
  home() {
    const { content } = state;
    const home = content.home;
    const pics = content.gallery ?? [];
    return `
      <section class="hero${pics.length ? ' pics' : ''}">${pics.length ? heroBackdrop(pics) : ''}<div class="frame">
        <div class="hero-copy">
          <img class="hero-logo" src="/img/logo.png" alt="JoJo: Heaven's Arrow">
          <p class="hero-lead">${esc(home.lead)} <span>${esc(home.leadMore)}</span></p>
          <div class="hero-actions">${inviteButton()}<a class="btn line" href="/wiki" data-link>Read the wiki</a></div>
          ${home.note ? `<div class="hero-note">${esc(home.note)}</div>` : ''}
        </div>
      </div></section>
      <section class="facts"><div class="frame">${content.facts.map((f) => `<span>${esc(f)}</span>`).join('')}</div></section>
      <section class="features"><div class="frame">${featureNext()}${featureBlog()}${featureDiscord()}</div></section>
      ${exploreTabs()}`;
  },

  stands(id) {
    if (id) return standPage(id);
    return `${head('stands', { tint: 'var(--purple)', crumbs: '<a href="/wiki" data-link>Wiki</a> / Stands' })}
      <section class="band flat tight"><div class="frame"><div class="stand-list">${state.content.stands.map((s) => `<a class="stand-row" id="${esc(s.id)}" href="/stands/${esc(s.id)}" data-link style="--c:${esc(s.color)}">
        <span class="stand-model"><img src="${esc(s.model)}" alt="${esc(s.name)} model" loading="lazy"></span>
        <div class="stand-body"><div><h3>${esc(s.name)}</h3><p>${esc(s.text)}</p>${moveCount(s)}</div>${standStats(s)}</div>
      </a>`).join('')}</div></div></section>`;
  },

  wiki(id) {
    const pages = state.live.guide;
    if (!id) {
      return `${head('wiki', { tint: 'var(--gold)' })}
        ${pages.length ? wikiGroups().map((g, i) => `<section class="band flat tight" style="--tint:${i % 2 ? 'var(--blurple)' : 'var(--gold)'}"><div class="frame">
          <span class="label">${esc(g.label)}</span>
          <div class="entries">${g.pages.map(guideEntry).join('')}</div>
        </div></section>`).join('') : '<section class="band flat tight"><div class="frame"><p class="empty">The wiki could not be loaded right now. Try again in a moment.</p></div></section>'}
        <section class="band flat tight" style="--tint:var(--purple)"><div class="frame">
          <span class="label">Reference</span>
          <div class="entries"><a class="entry" href="/stands" data-link><span class="slot"><img src="/img/star_platinum.png" alt=""></span><div><h3>All Stands</h3><p>Models, types and signature moves.</p></div><span class="go">&rarr;</span></a></div>
        </div></section>`;
    }
    const page = pages.find((p) => p.id === id);
    if (!page) return views.missing();
    return `<section class="band flat"><div class="frame wiki">
        ${wikiSide(id)}
        <article class="article">
          <div class="crumbs"><a href="/wiki" data-link>Wiki</a> / ${esc(page.title)}</div>
          <div class="lede"><span class="slot"><img src="${wikiIcon(page.id)}" alt=""></span><div><h1 style="font-size:clamp(30px,4vw,44px);margin-bottom:8px">${esc(page.title)}</h1><div>${markdown(page.intro)}</div></div></div>
          <div class="toc">${page.sections.map(([name]) => `<a href="#${slug(name)}">${esc(name)}</a>`).join('')}</div>
          ${page.sections.map(([name, body]) => `<h2 id="${slug(name)}">${esc(name)}</h2>${markdown(body)}`).join('')}
        </article>
      </div></section>`;
  },

  roadmap() {
    return `${head('roadmap', { tint: 'var(--magenta)' })}
      <section class="band flat tight"><div class="frame">${roadmapBoard(state.content.roadmap)}</div></section>
      <section class="band flat tight" style="--tint:var(--teal)"><div class="frame">
        <span class="label">Beyond this mod</span>
        <div class="head-row"><h2>Future projects</h2><a class="arrow-link" href="/projects" data-link>All projects &rarr;</a></div>
        ${projectTimeline(state.content.projects)}
      </div></section>`;
  },

  projects() {
    return `${head('projects', { tint: 'var(--teal)' })}
      <section class="band flat tight" style="--tint:var(--teal)"><div class="frame">${projectTimeline(state.content.projects)}</div></section>`;
  },

  blog(postSlug) {
    if (!postSlug) {
      const posts = sortedPosts();
      return `${head('blog', { tint: 'var(--teal)' })}
        <section class="band flat tight" style="--tint:var(--teal)"><div class="frame">${posts.length ? `<div class="posts">${posts.map(postRow).join('')}</div>` : '<p class="empty">No posts yet.</p>'}</div></section>`;
    }
    const post = state.content.blog.find((p) => p.slug === postSlug);
    if (!post) return views.missing();
    return `${pageHead(post.title, '', { tint: 'var(--teal)', crumbs: '<a href="/blog" data-link>Blog</a> / ' + esc(formatDate(post.date)) })}
      <section class="band flat tight"><div class="frame"><article class="article" style="padding-left:0"><p class="post-meta">${esc(formatDate(post.date))} · ${esc(post.author)}</p>${markdown(post.body)}</article></div></section>`;
  },

  faq() {
    const entries = state.live.faq;
    const groups = [];
    for (const entry of entries) {
      let group = groups.find((g) => g.name === entry.group);
      if (!group) groups.push(group = { name: entry.group, entries: [] });
      group.entries.push(entry);
    }
    const byId = Object.fromEntries(entries.map((e) => [e.id, e]));
    const related = (entry) => {
      const links = entry.related.map((id) => {
        if (id === 'kofi') return state.live.kofi ? `<a href="${esc(state.live.kofi)}" target="_blank" rel="noopener">Support on Ko-fi &rarr;</a>` : '';
        return byId[id] ? `<a href="#faq-${esc(id)}" data-open="${esc(id)}">${esc(byId[id].question)}</a>` : '';
      }).filter(Boolean);
      return links.length ? `<div class="related">${links.join('')}</div>` : '';
    };
    return `${head('faq', { tint: 'var(--blurple)' })}
      <section class="band flat tight" style="--tint:var(--blurple)"><div class="frame faq">
        <div class="faq-tools"><input class="search" type="search" placeholder="Search questions" aria-label="Search questions"></div>
        ${groups.length ? groups.map((g) => `<div class="faq-group"><span class="label">${esc(g.name)}</span>${g.entries.map((e) => `<details id="faq-${esc(e.id)}"><summary>${esc(e.question)}</summary><div class="answer">${markdown(e.answer)}${related(e)}</div></details>`).join('')}</div>`).join('') : '<p class="empty">The FAQ could not be loaded right now. Try again in a moment.</p>'}
      </div></section>`;
  },

  staff() {
    const groups = [];
    for (const member of state.content.staff) {
      let group = groups.find((g) => g.name === member.group);
      if (!group) groups.push(group = { name: member.group, members: [] });
      group.members.push(member);
    }
    return `${head('staff', { tint: 'var(--gold)' })}
      ${groups.map((g) => `<section class="band flat tight" style="--tint:var(--gold)"><div class="frame"><span class="label">${esc(g.name)}</span>
        <div class="people">${g.members.map((m) => `<div class="person"><span class="slot">${m.avatar ? `<img src="${esc(m.avatar)}" alt="">` : esc(m.name.charAt(0))}</span><div class="who"><h3>${esc(m.name)}</h3><span class="role">${esc(m.role)}</span></div></div>`).join('')}</div>
      </div></section>`).join('')}`;
  },

  discord() {
    return `${head('discord', { tint: 'var(--blurple)' })}
      <section class="band flat tight" style="--tint:var(--blurple)"><div class="frame">${inviteButton('Join the server')}</div></section>
      <section class="band flat tight" style="--tint:var(--blurple)"><div class="frame">
        <span class="label">Bot commands</span>
        <div class="cmds">${state.content.commands.map((c) => `<div class="cmd"><code>${esc(c.name)}</code><span>${inline(c.text)}</span></div>`).join('')}</div>
      </div></section>
      <section class="band flat tight" style="--tint:var(--gold)"><div class="frame">
        <span class="label">Supporters</span>
        <h2>${esc(state.content.supporters.title)}</h2>
        <p class="sub">${esc(state.content.supporters.text)}</p>
        <div class="steps">${state.content.supporters.steps.map((st) => `<div class="step"><h3>${esc(st.title)}</h3><p>${inline(st.text)}</p></div>`).join('')}</div>
        <p style="margin-top:32px"><a class="btn small" href="/support" data-link>See supporter tiers</a></p>
      </div></section>`;
  },

  support() {
    const sup = state.content.support;
    const steps = state.content.supporters;
    return `${head('support', { tint: 'var(--gold)' })}
      <section class="band flat tight" style="--tint:var(--gold)"><div class="frame"><div class="tiers">${sup.tiers.map(tierCard).join('')}</div></div></section>
      ${sup.inGame.length ? `<section class="band flat tight" style="--tint:var(--gold)"><div class="frame">
        <span class="label">In game and in Discord</span><h2>${esc(sup.inGameTitle)}</h2>
        <ul class="perk-row">${sup.inGame.map((perk) => `<li>${esc(perk)}</li>`).join('')}</ul>
      </div></section>` : ''}
      <section class="band flat tight" style="--tint:var(--blurple)"><div class="frame">
        <span class="label">After you join</span><h2>${esc(steps.title)}</h2><p class="sub">${esc(steps.text)}</p>
        <div class="steps">${steps.steps.map((st) => `<div class="step"><h3>${esc(st.title)}</h3><p>${inline(st.text)}</p></div>`).join('')}</div>
      </div></section>`;
  },

  missing() {
    return `${pageHead('Page not found', 'That page does not exist.')}<section class="band flat tight"><div class="frame"><a class="btn small" href="/" data-link>Back home</a></div></section>`;
  },
};

const titles = { support: 'Become a Supporter', wiki: 'Wiki', stands: 'Stands', roadmap: 'Roadmap', projects: 'Projects', blog: 'Blog', faq: 'FAQ', staff: 'Staff', discord: 'Discord' };

function render() {
  const [section = '', arg] = location.pathname.split('/').filter(Boolean).map(decodeURIComponent);
  const view = section === '' ? views.home : views[section] ?? views.missing;
  app.innerHTML = view(arg);
  startSlides();
  const named = section === 'stands' && arg ? state.content.stands.find((s) => s.id === arg)?.name : null;
  document.title = named ? `${named} · JoJo: Heaven's Arrow` : section && titles[section] ? `${titles[section]} · JoJo: Heaven's Arrow` : "JoJo: Heaven's Arrow";
  for (const link of nav.querySelectorAll('a')) {
    link.classList.toggle('active', section !== '' && link.getAttribute('href') === `/${section}`);
  }
  nav.classList.remove('open');
  toggle.setAttribute('aria-expanded', 'false');
  if (location.hash) {
    const target = document.getElementById(location.hash.slice(1));
    if (target?.tagName === 'DETAILS') target.open = true;
    target?.scrollIntoView();
  } else {
    window.scrollTo(0, 0);
  }
}

const account = document.querySelector('.account');
const accountBtn = account.querySelector('.account-btn');
const accountMenu = account.querySelector('.account-menu');

function showMenu(open) {
  accountMenu.hidden = !open;
  accountBtn.setAttribute('aria-expanded', String(open));
}

async function loadAccount() {
  try {
    const me = await fetch('/auth/me', { credentials: 'same-origin' }).then((r) => r.json());
    if (!me.signedIn) return;
    for (const el of account.querySelectorAll('.account-name')) el.textContent = me.name;
    if (me.avatar) account.querySelector('.account-avatar').src = me.avatar;
    account.hidden = false;
    for (const el of document.querySelectorAll('.staff-signin')) el.hidden = true;
  } catch {
    account.hidden = true;
  }
}

accountBtn.addEventListener('click', (event) => {
  event.stopPropagation();
  showMenu(accountMenu.hidden);
});

account.querySelector('[data-signout]').addEventListener('click', async () => {
  await fetch('/auth/logout', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
  account.hidden = true;
  showMenu(false);
  for (const el of document.querySelectorAll('.staff-signin')) el.hidden = false;
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') showMenu(false);
});

loadAccount();

document.addEventListener('click', (event) => {
  if (!event.target.closest('.account')) showMenu(false);
  const filter = event.target.closest('[data-rm-filter]');
  if (filter) {
    const wanted = filter.dataset.rmFilter;
    for (const button of filter.parentElement.children) button.classList.toggle('on', button === filter);
    for (const card of document.querySelectorAll('[data-rm-board] .rm-card')) card.hidden = wanted !== 'all' && card.dataset.status !== wanted;
    return;
  }
  const dot = event.target.closest('[data-slide]');
  if (dot) {
    const box = dot.closest('[data-gallery]');
    showSlide(box, Number(dot.dataset.slide));
    startSlides();
    return;
  }
  const open = event.target.closest('[data-open]');
  if (open) {
    const target = document.getElementById(`faq-${open.dataset.open}`);
    if (target) target.open = true;
  }
  const link = event.target.closest('a[data-link]');
  if (!link || event.metaKey || event.ctrlKey || event.shiftKey) return;
  event.preventDefault();
  if (link.getAttribute('href') !== location.pathname) {
    history.pushState(null, '', link.getAttribute('href'));
    render();
  }
});

document.addEventListener('input', (event) => {
  if (!event.target.matches('.search')) return;
  const wanted = event.target.value.trim().toLowerCase();
  for (const group of document.querySelectorAll('.faq-group')) {
    let shown = 0;
    for (const item of group.querySelectorAll('details')) {
      const match = !wanted || item.textContent.toLowerCase().includes(wanted);
      item.hidden = !match;
      if (match) shown++;
    }
    group.hidden = shown === 0;
  }
});

toggle.addEventListener('click', () => {
  const open = nav.classList.toggle('open');
  toggle.setAttribute('aria-expanded', String(open));
});

window.addEventListener('popstate', render);

load().then(render).catch(() => {
  app.innerHTML = '<section class="band flat"><div class="frame"><p class="empty">The site could not load. Refresh to try again.</p></div></section>';
});
