import { createServer } from 'node:http';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize, sep } from 'node:path';
import { SUPPORTER_AURA, normalizeUuid } from './rewards.js';
import { CONTENT_TYPES, KINDS, catalog } from './skins.js';
import { SITE_DIR } from './site.js';
import { createAdmin } from './admin.js';

const MAX_BODY = 2048;
const NAME_PATTERN = /^[A-Za-z0-9_]{1,16}$/;
const CODE_PATTERN = /^[A-Z0-9]{8}$/;
const SITE_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
};

export function rateLimiter(limit, windowMs) {
  const hits = new Map();
  return (key, now = Date.now()) => {
    const entry = hits.get(key);
    if (!entry || entry.until <= now) {
      hits.set(key, { count: 1, until: now + windowMs });
      if (hits.size > 10000) {
        for (const [k, v] of hits) {
          if (v.until <= now) hits.delete(k);
        }
      }
      return true;
    }
    entry.count++;
    return entry.count <= limit;
  };
}

export function perksFor(store, link, roles) {
  if (!link) {
    return { linked: false, rewards: [], aura: null };
  }
  const rewards = roles ? store.rewardsForRoles(roles) : [];
  const aura = rewards.includes(SUPPORTER_AURA) ? store.auraOf(link.discord_id) : null;
  return { linked: true, rewards, aura };
}

function send(res, status, body) {
  const text = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(text),
    'Cache-Control': 'no-store',
  });
  res.end(text);
}

function sendFile(res, kind, file) {
  res.writeHead(200, {
    'Content-Type': CONTENT_TYPES[kind],
    'Content-Length': file.data.length,
    'Cache-Control': 'public, max-age=3600',
    ETag: `"${file.sha256}"`,
  });
  res.end(file.data);
}

const assetStamps = new Map();

async function assetStamp(siteDir, path) {
  if (!assetStamps.has(path)) {
    const data = await readFile(join(siteDir, path)).catch(() => null);
    assetStamps.set(path, data ? createHash('sha256').update(data).digest('hex').slice(0, 10) : '');
  }
  return assetStamps.get(path);
}

async function stampAssets(siteDir, html) {
  const paths = [...html.matchAll(/(?:href|src)="(\/[\w./-]+\.(?:js|css))"/g)].map((m) => m[1]);
  for (const path of new Set(paths)) {
    const stamp = await assetStamp(siteDir, path);
    if (stamp) html = html.replaceAll(`"${path}"`, `"${path}?v=${stamp}"`);
  }
  return html;
}

async function sendSite(res, siteDir, pathname) {
  let wanted;
  try {
    const page = pathname === '/admin' || pathname === '/admin/' ? '/admin.html' : '/index.html';
    wanted = decodeURIComponent(extname(pathname) ? pathname : page);
  } catch {
    return send(res, 400, { error: 'bad_path' });
  }
  const file = normalize(join(siteDir, wanted));
  const type = SITE_TYPES[extname(file)];
  if (!type || !file.startsWith(siteDir + sep)) {
    return send(res, 404, { error: 'not_found' });
  }
  let data;
  try {
    data = await readFile(file);
  } catch {
    return send(res, 404, { error: 'not_found' });
  }
  if (extname(file) === '.html') data = Buffer.from(await stampAssets(siteDir, data.toString('utf8')));
  const fresh = ['.html', '.json', '.js', '.css'].includes(extname(file));
  res.writeHead(200, {
    'Content-Type': type,
    'Content-Length': data.length,
    'Cache-Control': fresh ? 'no-cache' : 'public, max-age=3600',
    'X-Content-Type-Options': 'nosniff',
  });
  res.end(data);
}

function readBody(req, max) {
  return new Promise((resolve) => {
    let size = 0;
    const chunks = [];
    const onData = (chunk) => {
      size += chunk.length;
      if (size > max) {
        req.off('data', onData);
        req.resume();
        resolve(null);
        return;
      }
      chunks.push(chunk);
    };
    req.on('data', onData);
    req.on('end', () => resolve(size > max ? null : Buffer.concat(chunks)));
    req.on('error', () => resolve(null));
  });
}

async function readJson(req) {
  const body = await readBody(req, MAX_BODY);
  if (!body) {
    return null;
  }
  try {
    return JSON.parse(body.toString('utf8'));
  } catch {
    return null;
  }
}

export function createApi({
  store, rolesOf, nameOf, onLinked, trustProxy = false, siteDir = SITE_DIR,
  canManage = async () => false, clientId = () => null, clientSecret = null, siteUrl = 'http://localhost:8080', fetchImpl = fetch,
}) {
  const admin = createAdmin({ store, canManage, clientId, clientSecret, siteUrl, send, readBody, fetchImpl });
  const anyLimit = rateLimiter(240, 60 * 1000);
  const linkLimit = rateLimiter(10, 10 * 60 * 1000);

  const clientIp = (req) => {
    if (trustProxy) {
      const forwarded = req.headers['x-forwarded-for'];
      if (typeof forwarded === 'string' && forwarded.length > 0) {
        return forwarded.split(',')[0].trim();
      }
    }
    return req.socket.remoteAddress ?? 'unknown';
  };

  return createServer(async (req, res) => {
    try {
      const ip = clientIp(req);
      if (!anyLimit(ip)) {
        return send(res, 429, { error: 'rate_limited' });
      }
      const url = new URL(req.url ?? '/', 'http://localhost');
      const parts = url.pathname.split('/').filter(Boolean);

      if (req.method === 'GET' && url.pathname === '/health') {
        return send(res, 200, { ok: true });
      }

      if (req.method === 'GET' && parts.length === 3 && parts[0] === 'v1' && parts[1] === 'players') {
        const uuid = normalizeUuid(parts[2]);
        if (!uuid) {
          return send(res, 400, { error: 'bad_uuid' });
        }
        const link = store.linkByUuid(uuid);
        const roles = link ? await rolesOf(link.discord_id) : null;
        return send(res, 200, perksFor(store, link, roles));
      }

      if (req.method === 'GET' && url.pathname === '/v1/site') {
        return send(res, 200, { guide: store.guide(), faq: store.faq(), kofi: store.setting('kofi_url') ?? null });
      }

      if (req.method === 'GET' && url.pathname === '/v1/skins') {
        return send(res, 200, catalog(store.skins()));
      }

      if (req.method === 'GET' && parts.length === 4 && parts[0] === 'v1' && parts[1] === 'skins') {
        const id = /^[1-9][0-9]{0,8}$/.test(parts[2]) ? Number(parts[2]) : null;
        const kind = KINDS.includes(parts[3]) ? parts[3] : null;
        const file = id && kind ? store.skinFile(id, kind) : null;
        if (!file) {
          return send(res, 404, { error: 'not_found' });
        }
        return sendFile(res, kind, file);
      }

      if (req.method === 'POST' && url.pathname === '/v1/link') {
        if (!linkLimit(ip)) {
          return send(res, 429, { error: 'rate_limited' });
        }
        const body = await readJson(req);
        const uuid = normalizeUuid(body?.uuid);
        const name = typeof body?.name === 'string' ? body.name.trim() : '';
        const code = typeof body?.code === 'string' ? body.code.trim().toUpperCase() : '';
        if (!uuid || !NAME_PATTERN.test(name) || !CODE_PATTERN.test(code)) {
          return send(res, 400, { error: 'bad_request' });
        }
        const discordId = store.redeemCode(code);
        if (!discordId) {
          return send(res, 404, { error: 'invalid_code' });
        }
        store.link(discordId, uuid, name);
        const link = store.linkByUuid(uuid);
        const roles = await rolesOf(discordId);
        const discord = await nameOf(discordId);
        onLinked?.(discordId, name);
        return send(res, 200, { ...perksFor(store, link, roles), discord });
      }

      if (await admin(req, res, url, parts)) {
        return;
      }

      if (req.method === 'GET' && parts[0] !== 'v1') {
        return await sendSite(res, siteDir, url.pathname);
      }

      return send(res, 404, { error: 'not_found' });
    } catch (error) {
      console.error('API error', error);
      if (!res.headersSent) {
        send(res, 500, { error: 'internal' });
      }
    }
  });
}
