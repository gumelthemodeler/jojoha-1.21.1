import { createHash, randomBytes, timingSafeEqual } from 'node:crypto';
import { checkPage, formatSections } from './guide.js';
import { checkEntry } from './faq.js';
import { FILE_TYPES, MAX_CONTENT, MAX_FILE, checkContent, checkUrl, mergeContent } from './site.js';

export const SESSION_COOKIE = 'jjha_session';
const STATE_COOKIE = 'jjha_oauth';
const SESSION_TTL = 7 * 24 * 60 * 60 * 1000;
const STATE_TTL = 10 * 60;
const DISCORD = 'https://discord.com';
const FILE_NAME = /^[A-Za-z0-9._-]{1,80}$/;

const hash = (token) => createHash('sha256').update(token).digest('hex');

export function parseCookies(header) {
  const cookies = {};
  for (const part of String(header ?? '').split(';')) {
    const index = part.indexOf('=');
    if (index > 0) {
      cookies[part.slice(0, index).trim()] = part.slice(index + 1).trim();
    }
  }
  return cookies;
}

export function sniffImage(data) {
  if (data.length < 12) {
    return null;
  }
  if (data[0] === 0x89 && data.toString('latin1', 1, 4) === 'PNG') {
    return 'image/png';
  }
  if (data.toString('latin1', 0, 4) === 'GIF8') {
    return 'image/gif';
  }
  if (data[0] === 0xff && data[1] === 0xd8 && data[2] === 0xff) {
    return 'image/jpeg';
  }
  if (data.toString('latin1', 0, 4) === 'RIFF' && data.toString('latin1', 8, 12) === 'WEBP') {
    return 'image/webp';
  }
  return null;
}

export function siteContent(store) {
  const saved = store.setting('site_content');
  return mergeContent(saved ? JSON.parse(saved) : null);
}

const sameText = (a, b) => {
  const left = Buffer.from(String(a));
  const right = Buffer.from(String(b));
  return left.length === right.length && timingSafeEqual(left, right);
};

export function createAdmin({ store, canManage, clientId, clientSecret, siteUrl, send, readBody, fetchImpl = fetch }) {
  const secure = String(siteUrl).startsWith('https://');
  const redirectUri = `${siteUrl}/auth/callback`;

  const cookie = (name, value, { path = '/', maxAge } = {}) =>
    `${name}=${value}; Path=${path}; HttpOnly; SameSite=Lax${secure ? '; Secure' : ''}${maxAge !== undefined ? `; Max-Age=${maxAge}` : ''}`;

  const redirect = (res, location, cookies = []) => {
    res.writeHead(302, { Location: location, 'Set-Cookie': cookies, 'Cache-Control': 'no-store' });
    res.end();
  };

  const avatarOf = (user) => {
    if (/^(a_)?[0-9a-f]{32}$/.test(user.avatar ?? '') && /^[0-9]{5,25}$/.test(user.id ?? '')) {
      return `https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png?size=64`;
    }
    return '';
  };

  const sessionOf = (req) => {
    const token = parseCookies(req.headers.cookie)[SESSION_COOKIE];
    if (!token) {
      return null;
    }
    const row = store.session(hash(token));
    return row ? { ...row, tokenHash: hash(token) } : null;
  };

  const sameOrigin = (req) => {
    const origin = req.headers.origin;
    if (!origin) {
      return false;
    }
    try {
      return new URL(origin).host === req.headers.host;
    } catch {
      return false;
    }
  };

  const readJson = async (req, max = MAX_CONTENT) => {
    const body = await readBody(req, max);
    if (!body) {
      return null;
    }
    try {
      return JSON.parse(body.toString('utf8'));
    } catch {
      return null;
    }
  };

  const adminState = () => ({
    content: siteContent(store),
    guide: store.guide().map((page) => ({ ...page, sections: formatSections(page.sections) })),
    faq: store.faq().map((entry) => ({ ...entry, related: entry.related.join(', ') })),
    files: store.siteFiles().map((file) => ({ ...file, url: `/files/${file.id}/${file.name}` })),
    kofi: store.setting('kofi_url') ?? '',
  });

  async function login(req, res) {
    if (req.headers.host && req.headers.host !== new URL(siteUrl).host) {
      return redirect(res, `${siteUrl}/auth/login`);
    }
    const id = clientId();
    if (!id || !clientSecret) {
      return redirect(res, '/admin?error=setup');
    }
    const state = randomBytes(16).toString('hex');
    const params = new URLSearchParams({ response_type: 'code', client_id: id, scope: 'identify', state, redirect_uri: redirectUri, prompt: 'none' });
    return redirect(res, `${DISCORD}/oauth2/authorize?${params}`, [cookie(STATE_COOKIE, state, { path: '/auth', maxAge: STATE_TTL })]);
  }

  async function callback(req, res, url) {
    const clear = cookie(STATE_COOKIE, '', { path: '/auth', maxAge: 0 });
    const expected = parseCookies(req.headers.cookie)[STATE_COOKIE];
    const state = url.searchParams.get('state');
    const code = url.searchParams.get('code');
    if (!expected || !state || !code || !sameText(expected, state)) {
      return redirect(res, '/admin?error=state', [clear]);
    }
    const tokenRes = await fetchImpl(`${DISCORD}/api/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ client_id: clientId(), client_secret: clientSecret, grant_type: 'authorization_code', code, redirect_uri: redirectUri }),
    });
    if (!tokenRes.ok) {
      return redirect(res, '/admin?error=discord', [clear]);
    }
    const { access_token: accessToken } = await tokenRes.json();
    const userRes = await fetchImpl(`${DISCORD}/api/users/@me`, { headers: { Authorization: `Bearer ${accessToken}` } });
    if (!userRes.ok) {
      return redirect(res, '/admin?error=discord', [clear]);
    }
    const user = await userRes.json();
    if (!(await canManage(user.id))) {
      return redirect(res, '/admin?error=denied', [clear]);
    }
    const token = randomBytes(32).toString('base64url');
    store.createSession(hash(token), user.id, user.global_name ?? user.username ?? 'Staff', SESSION_TTL, Date.now(), avatarOf(user));
    return redirect(res, '/admin', [clear, cookie(SESSION_COOKIE, token, { maxAge: SESSION_TTL / 1000 })]);
  }

  async function api(req, res, url, parts) {
    const session = sessionOf(req);
    if (!session || !(await canManage(session.discord_id))) {
      return send(res, 401, { error: 'signed_out' });
    }
    if (req.method !== 'GET' && !sameOrigin(req)) {
      return send(res, 403, { error: 'bad_origin' });
    }
    const [, , resource, id] = parts;
    const route = `${req.method} ${resource ?? ''}`;

    if (route === 'GET me') {
      return send(res, 200, { id: session.discord_id, name: session.name, avatar: session.avatar });
    }
    if (route === 'GET state') {
      return send(res, 200, adminState());
    }
    if (route === 'PUT content') {
      const result = checkContent(await readJson(req));
      if (result.error) {
        return send(res, 400, { error: result.error });
      }
      store.setSetting('site_content', JSON.stringify(result.content));
      return send(res, 200, { ok: true });
    }
    if (route === 'PUT kofi') {
      const body = await readJson(req, 4096);
      const link = typeof body?.url === 'string' ? body.url.trim() : '';
      if (link && !checkUrl(link)) {
        return send(res, 400, { error: 'The Ko-fi link must start with https://.' });
      }
      store.setSetting('kofi_url', link || null);
      return send(res, 200, { ok: true });
    }
    if (route === 'PUT guide' && id) {
      const body = await readJson(req, 64 * 1024);
      const result = checkPage({ ...body, id }, store.guide());
      if (result.error) {
        return send(res, 400, { error: result.error });
      }
      store.putGuide(result.page);
      return send(res, 200, { ok: true });
    }
    if (route === 'DELETE guide' && id) {
      return send(res, store.removeGuide(id) ? 200 : 404, { ok: true });
    }
    if (route === 'PUT guide-order' || route === 'PUT faq-order') {
      const body = await readJson(req, 16 * 1024);
      const known = (resource === 'guide-order' ? store.guide() : store.faq()).map((item) => item.id);
      const ids = Array.isArray(body?.ids) ? body.ids : [];
      if (ids.length !== known.length || !known.every((item) => ids.includes(item))) {
        return send(res, 400, { error: 'The order must list every entry once.' });
      }
      (resource === 'guide-order' ? store.orderGuide(ids) : store.orderFaq(ids));
      return send(res, 200, { ok: true });
    }
    if (route === 'PUT faq' && id) {
      const body = await readJson(req, 16 * 1024);
      const result = checkEntry({ ...body, id }, store.faq());
      if (result.error) {
        return send(res, 400, { error: result.error });
      }
      store.putFaq(result.entry);
      return send(res, 200, { ok: true });
    }
    if (route === 'DELETE faq' && id) {
      return send(res, store.removeFaq(id) ? 200 : 404, { ok: true });
    }
    if (route === 'POST files') {
      const name = (url.searchParams.get('name') ?? '').replace(/[^A-Za-z0-9._-]/g, '_').slice(-80);
      if (!FILE_NAME.test(name)) {
        return send(res, 400, { error: 'The file needs a name.' });
      }
      const data = await readBody(req, MAX_FILE);
      if (!data) {
        return send(res, 413, { error: 'Files can be at most 10 MB.' });
      }
      const type = sniffImage(data);
      if (!type || !FILE_TYPES.includes(type)) {
        return send(res, 400, { error: 'Only PNG, GIF, WebP and JPEG images can be uploaded.' });
      }
      const fileId = store.putSiteFile(name, type, data);
      return send(res, 200, { id: fileId, url: `/files/${fileId}/${name}` });
    }
    if (route === 'DELETE files' && /^[1-9][0-9]{0,8}$/.test(id ?? '')) {
      return send(res, store.removeSiteFile(Number(id)) ? 200 : 404, { ok: true });
    }
    return send(res, 404, { error: 'not_found' });
  }

  return async function handle(req, res, url, parts) {
    if (req.method === 'GET' && url.pathname === '/auth/login') {
      await login(req, res);
      return true;
    }
    if (req.method === 'GET' && url.pathname === '/auth/callback') {
      await callback(req, res, url);
      return true;
    }
    if (req.method === 'GET' && url.pathname === '/auth/me') {
      const session = sessionOf(req);
      const staff = session ? await canManage(session.discord_id) : false;
      res.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(staff ? { signedIn: true, name: session.name, avatar: session.avatar } : { signedIn: false }));
      return true;
    }
    if (req.method === 'POST' && url.pathname === '/auth/logout') {
      if (!sameOrigin(req)) {
        send(res, 403, { error: 'bad_origin' });
        return true;
      }
      const session = sessionOf(req);
      if (session) {
        store.endSession(session.tokenHash);
      }
      res.writeHead(200, { 'Content-Type': 'application/json', 'Set-Cookie': cookie(SESSION_COOKIE, '', { maxAge: 0 }), 'Cache-Control': 'no-store' });
      res.end('{"ok":true}');
      return true;
    }
    if (parts[0] === 'admin' && parts[1] === 'api') {
      await api(req, res, url, parts);
      return true;
    }
    if (req.method === 'GET' && url.pathname === '/content.json') {
      send(res, 200, siteContent(store));
      return true;
    }
    if (req.method === 'GET' && parts[0] === 'files' && parts.length === 3) {
      const file = /^[1-9][0-9]{0,8}$/.test(parts[1]) ? store.siteFile(Number(parts[1])) : null;
      if (!file) {
        send(res, 404, { error: 'not_found' });
        return true;
      }
      res.writeHead(200, {
        'Content-Type': file.type,
        'Content-Length': file.data.length,
        'Cache-Control': 'public, max-age=31536000, immutable',
        'X-Content-Type-Options': 'nosniff',
        ETag: `"${file.sha256}"`,
      });
      res.end(file.data);
      return true;
    }
    return false;
  };
}
