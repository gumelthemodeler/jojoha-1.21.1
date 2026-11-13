import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { randomInt } from 'node:crypto';
import { SEED } from './faq.js';
import { FIRST_PAGES, PAGES } from './guide.js';
import { sha256 } from './skins.js';
import { tierRewards } from './rewards.js';

const CODE_ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 8;

export function newCode() {
  let code = '';
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += CODE_ALPHABET[randomInt(CODE_ALPHABET.length)];
  }
  return code;
}

export function openStore(path) {
  if (path !== ':memory:') {
    mkdirSync(dirname(path), { recursive: true });
  }
  const db = new DatabaseSync(path);
  db.exec(`
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS links (
      discord_id TEXT PRIMARY KEY,
      mc_uuid TEXT NOT NULL UNIQUE,
      mc_name TEXT NOT NULL,
      linked_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS link_codes (
      code TEXT PRIMARY KEY,
      discord_id TEXT NOT NULL UNIQUE,
      expires_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS role_rewards (
      role_id TEXT NOT NULL,
      reward TEXT NOT NULL,
      PRIMARY KEY (role_id, reward)
    );
    CREATE TABLE IF NOT EXISTS auras (
      discord_id TEXT PRIMARY KEY,
      colour INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS faq (
      id TEXT PRIMARY KEY,
      grp TEXT NOT NULL,
      question TEXT NOT NULL,
      answer TEXT NOT NULL,
      related TEXT NOT NULL,
      position INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS guide (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      summary TEXT NOT NULL,
      intro TEXT NOT NULL,
      sections TEXT NOT NULL,
      position INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS skins (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      slug TEXT NOT NULL UNIQUE,
      stand TEXT NOT NULL,
      name TEXT NOT NULL,
      version INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS skin_files (
      skin_id INTEGER NOT NULL,
      kind TEXT NOT NULL,
      data BLOB NOT NULL,
      sha256 TEXT NOT NULL,
      PRIMARY KEY (skin_id, kind)
    );
    CREATE TABLE IF NOT EXISTS welcomed (
      discord_id TEXT PRIMARY KEY,
      at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS site_files (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      type TEXT NOT NULL,
      data BLOB NOT NULL,
      sha256 TEXT NOT NULL,
      uploaded_at INTEGER NOT NULL
    );
    CREATE TABLE IF NOT EXISTS tickets (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      kind TEXT NOT NULL,
      user_id TEXT NOT NULL,
      channel_id TEXT NOT NULL,
      answers TEXT NOT NULL,
      status TEXT NOT NULL,
      decided_by TEXT,
      note TEXT NOT NULL DEFAULT '',
      created_at INTEGER NOT NULL,
      decided_at INTEGER,
      closed_at INTEGER
    );
    CREATE TABLE IF NOT EXISTS sessions (
      token_hash TEXT PRIMARY KEY,
      discord_id TEXT NOT NULL,
      name TEXT NOT NULL,
      expires_at INTEGER NOT NULL
    );
  `);

  const sessionColumns = new Set(db.prepare('PRAGMA table_info(sessions)').all().map((column) => column.name));
  if (!sessionColumns.has('avatar')) {
    db.exec("ALTER TABLE sessions ADD COLUMN avatar TEXT NOT NULL DEFAULT ''");
  }

  const skinColumns = new Set(db.prepare('PRAGMA table_info(skins)').all().map((column) => column.name));
  for (const column of ['aura', 'aura2']) {
    if (!skinColumns.has(column)) {
      db.exec(`ALTER TABLE skins ADD COLUMN ${column} INTEGER`);
    }
  }

  const sql = {
    purgeCodes: db.prepare('DELETE FROM link_codes WHERE expires_at <= ?'),
    putCode: db.prepare('INSERT OR REPLACE INTO link_codes (code, discord_id, expires_at) VALUES (?, ?, ?)'),
    dropCodeOf: db.prepare('DELETE FROM link_codes WHERE discord_id = ?'),
    findCode: db.prepare('SELECT discord_id, expires_at FROM link_codes WHERE code = ?'),
    dropCode: db.prepare('DELETE FROM link_codes WHERE code = ?'),
    dropLinkByUuid: db.prepare('DELETE FROM links WHERE mc_uuid = ?'),
    putLink: db.prepare('INSERT OR REPLACE INTO links (discord_id, mc_uuid, mc_name, linked_at) VALUES (?, ?, ?, ?)'),
    dropLink: db.prepare('DELETE FROM links WHERE discord_id = ?'),
    linkOf: db.prepare('SELECT discord_id, mc_uuid, mc_name, linked_at FROM links WHERE discord_id = ?'),
    linkByUuid: db.prepare('SELECT discord_id, mc_uuid, mc_name, linked_at FROM links WHERE mc_uuid = ?'),
    putReward: db.prepare('INSERT OR IGNORE INTO role_rewards (role_id, reward) VALUES (?, ?)'),
    dropReward: db.prepare('DELETE FROM role_rewards WHERE role_id = ? AND reward = ?'),
    allRewards: db.prepare('SELECT role_id, reward FROM role_rewards ORDER BY reward, role_id'),
    putAura: db.prepare('INSERT OR REPLACE INTO auras (discord_id, colour) VALUES (?, ?)'),
    dropAura: db.prepare('DELETE FROM auras WHERE discord_id = ?'),
    auraOf: db.prepare('SELECT colour FROM auras WHERE discord_id = ?'),
    faqAll: db.prepare('SELECT id, grp, question, answer, related, position FROM faq ORDER BY position'),
    faqGet: db.prepare('SELECT position FROM faq WHERE id = ?'),
    faqTop: db.prepare('SELECT COALESCE(MAX(position), 0) AS top FROM faq'),
    faqPut: db.prepare('INSERT OR REPLACE INTO faq (id, grp, question, answer, related, position) VALUES (?, ?, ?, ?, ?, ?)'),
    faqDrop: db.prepare('DELETE FROM faq WHERE id = ?'),
    guideAll: db.prepare('SELECT id, title, summary, intro, sections FROM guide ORDER BY position'),
    guideGet: db.prepare('SELECT position FROM guide WHERE id = ?'),
    guideTop: db.prepare('SELECT COALESCE(MAX(position), 0) AS top FROM guide'),
    guidePut: db.prepare('INSERT OR REPLACE INTO guide (id, title, summary, intro, sections, position) VALUES (?, ?, ?, ?, ?, ?)'),
    guideDrop: db.prepare('DELETE FROM guide WHERE id = ?'),
    setting: db.prepare('SELECT value FROM settings WHERE key = ?'),
    putSetting: db.prepare('INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)'),
    dropSetting: db.prepare('DELETE FROM settings WHERE key = ?'),
    welcomedAt: db.prepare('SELECT at FROM welcomed WHERE discord_id = ?'),
    putWelcomed: db.prepare('INSERT OR REPLACE INTO welcomed (discord_id, at) VALUES (?, ?)'),
    skinAll: db.prepare('SELECT id, slug, stand, name, version, updated_at, aura, aura2 FROM skins ORDER BY id'),
    skinBySlug: db.prepare('SELECT id, slug, stand, name, version, updated_at, aura, aura2 FROM skins WHERE slug = ?'),
    skinAura: db.prepare('UPDATE skins SET aura = ?, aura2 = ?, updated_at = ? WHERE id = ?'),
    skinFilesOf: db.prepare('SELECT kind, sha256, length(data) AS size FROM skin_files WHERE skin_id = ? ORDER BY kind'),
    skinFile: db.prepare('SELECT data, sha256 FROM skin_files WHERE skin_id = ? AND kind = ?'),
    skinInsert: db.prepare('INSERT INTO skins (slug, stand, name, version, updated_at) VALUES (?, ?, ?, 1, ?)'),
    skinUpdate: db.prepare('UPDATE skins SET stand = ?, name = ?, version = version + 1, updated_at = ? WHERE id = ?'),
    skinDrop: db.prepare('DELETE FROM skins WHERE id = ?'),
    skinFilesDrop: db.prepare('DELETE FROM skin_files WHERE skin_id = ?'),
    skinFilePut: db.prepare('INSERT INTO skin_files (skin_id, kind, data, sha256) VALUES (?, ?, ?, ?)'),
    guideMove: db.prepare('UPDATE guide SET position = ? WHERE id = ?'),
    faqMove: db.prepare('UPDATE faq SET position = ? WHERE id = ?'),
    siteFileAll: db.prepare('SELECT id, name, type, sha256, length(data) AS size, uploaded_at FROM site_files ORDER BY id DESC'),
    siteFile: db.prepare('SELECT id, name, type, data, sha256 FROM site_files WHERE id = ?'),
    siteFilePut: db.prepare('INSERT INTO site_files (name, type, data, sha256, uploaded_at) VALUES (?, ?, ?, ?, ?)'),
    siteFileDrop: db.prepare('DELETE FROM site_files WHERE id = ?'),
    sessionPut: db.prepare('INSERT INTO sessions (token_hash, discord_id, name, expires_at, avatar) VALUES (?, ?, ?, ?, ?)'),
    sessionGet: db.prepare('SELECT discord_id, name, avatar, expires_at FROM sessions WHERE token_hash = ?'),
    sessionDrop: db.prepare('DELETE FROM sessions WHERE token_hash = ?'),
    sessionPurge: db.prepare('DELETE FROM sessions WHERE expires_at <= ?'),
    ticketPut: db.prepare("INSERT INTO tickets (kind, user_id, channel_id, answers, status, created_at) VALUES (?, ?, ?, ?, 'open', ?)"),
    ticketGet: db.prepare('SELECT * FROM tickets WHERE id = ?'),
    ticketByChannel: db.prepare('SELECT * FROM tickets WHERE channel_id = ? AND closed_at IS NULL'),
    ticketActive: db.prepare('SELECT * FROM tickets WHERE user_id = ? AND kind = ? AND closed_at IS NULL ORDER BY id DESC LIMIT 1'),
    ticketDecide: db.prepare("UPDATE tickets SET status = ?, decided_by = ?, note = ?, decided_at = ? WHERE id = ? AND status = 'open'"),
    ticketClose: db.prepare('UPDATE tickets SET closed_at = ? WHERE id = ? AND closed_at IS NULL'),
  };

  const ticketRow = (row) => row && ({
    id: Number(row.id),
    kind: row.kind,
    userId: row.user_id,
    channelId: row.channel_id,
    answers: JSON.parse(row.answers),
    status: row.status,
    decidedBy: row.decided_by,
    note: row.note,
    createdAt: row.created_at,
    decidedAt: row.decided_at,
    closedAt: row.closed_at,
  });

  const putFaq = (entry) => {
    const existing = sql.faqGet.get(entry.id);
    const position = existing ? existing.position : sql.faqTop.get().top + 1;
    sql.faqPut.run(entry.id, entry.group, entry.question, entry.answer, entry.related.join(','), position);
    return !existing;
  };

  const putGuide = (page) => {
    const existing = sql.guideGet.get(page.id);
    const position = existing ? existing.position : sql.guideTop.get().top + 1;
    sql.guidePut.run(page.id, page.title, page.summary, page.intro, JSON.stringify(page.sections), position);
    return !existing;
  };

  if (!sql.setting.get('guide_seeded')) {
    for (const page of PAGES) {
      putGuide(page);
    }
    sql.putSetting.run('guide_seeded', '1');
  }

  const knownSaved = sql.setting.get('guide_known');
  const knownPages = new Set(knownSaved ? JSON.parse(knownSaved.value)
    : [...FIRST_PAGES, ...sql.guideAll.all().map((row) => row.id)]);
  for (const page of PAGES) {
    if (!knownPages.has(page.id)) {
      putGuide(page);
      knownPages.add(page.id);
    }
  }
  sql.putSetting.run('guide_known', JSON.stringify([...knownPages]));

  if (!sql.setting.get('faq_seeded')) {
    for (const entry of SEED) {
      putFaq(entry);
    }
    sql.putSetting.run('faq_seeded', '1');
  }

  const withFiles = (row) => row && ({ ...row, files: sql.skinFilesOf.all(row.id) });

  return {
    skins() {
      return sql.skinAll.all().map(withFiles);
    },

    skin(slug) {
      return withFiles(sql.skinBySlug.get(slug));
    },

    skinFile(id, kind) {
      const row = sql.skinFile.get(id, kind);
      return row ? { data: Buffer.from(row.data), sha256: row.sha256 } : null;
    },

    putSkin({ slug, stand, name, files }, now = Date.now()) {
      db.exec('BEGIN');
      try {
        const existing = sql.skinBySlug.get(slug);
        let id;
        if (existing) {
          id = existing.id;
          sql.skinUpdate.run(stand, name, now, id);
        } else {
          id = Number(sql.skinInsert.run(slug, stand, name, now).lastInsertRowid);
        }
        sql.skinFilesDrop.run(id);
        for (const [kind, data] of Object.entries(files)) {
          if (data) {
            sql.skinFilePut.run(id, kind, data, sha256(data));
          }
        }
        db.exec('COMMIT');
        return { ...withFiles(sql.skinBySlug.get(slug)), created: !existing };
      } catch (error) {
        db.exec('ROLLBACK');
        throw error;
      }
    },

    setSkinAura(slug, aura, aura2, now = Date.now()) {
      const existing = sql.skinBySlug.get(slug);
      if (!existing) {
        return null;
      }
      sql.skinAura.run(aura ?? null, aura2 ?? null, now, existing.id);
      return withFiles(sql.skinBySlug.get(slug));
    },

    removeSkin(slug) {
      const existing = sql.skinBySlug.get(slug);
      if (!existing) {
        return false;
      }
      sql.skinFilesDrop.run(existing.id);
      sql.skinDrop.run(existing.id);
      return true;
    },

    faq() {
      const rows = sql.faqAll.all();
      const order = [];
      for (const row of rows) {
        if (!order.includes(row.grp)) {
          order.push(row.grp);
        }
      }
      return rows
        .sort((a, b) => order.indexOf(a.grp) - order.indexOf(b.grp) || a.position - b.position)
        .map((row) => ({
          id: row.id,
          group: row.grp,
          question: row.question,
          answer: row.answer,
          related: row.related ? row.related.split(',') : [],
        }));
    },

    putFaq,

    guide() {
      return sql.guideAll.all().map((row) => ({
        id: row.id,
        title: row.title,
        summary: row.summary,
        intro: row.intro,
        sections: JSON.parse(row.sections),
      }));
    },

    putGuide,

    removeGuide(id) {
      return sql.guideDrop.run(id).changes > 0;
    },

    removeFaq(id) {
      return sql.faqDrop.run(id).changes > 0;
    },

    orderGuide(ids) {
      ids.forEach((id, index) => sql.guideMove.run(index + 1, id));
    },

    orderFaq(ids) {
      ids.forEach((id, index) => sql.faqMove.run(index + 1, id));
    },

    siteFiles() {
      return sql.siteFileAll.all();
    },

    siteFile(id) {
      return sql.siteFile.get(id) ?? null;
    },

    putSiteFile(name, type, data, now = Date.now()) {
      return Number(sql.siteFilePut.run(name, type, data, sha256(data), now).lastInsertRowid);
    },

    removeSiteFile(id) {
      return sql.siteFileDrop.run(id).changes > 0;
    },

    createSession(tokenHash, discordId, name, ttlMs, now = Date.now(), avatar = '') {
      sql.sessionPurge.run(now);
      sql.sessionPut.run(tokenHash, discordId, name, now + ttlMs, avatar);
    },

    session(tokenHash, now = Date.now()) {
      const row = sql.sessionGet.get(tokenHash);
      return row && row.expires_at > now ? row : null;
    },

    endSession(tokenHash) {
      sql.sessionDrop.run(tokenHash);
    },

    openTicket({ kind, userId, channelId, answers }, now = Date.now()) {
      const { lastInsertRowid } = sql.ticketPut.run(kind, userId, channelId, JSON.stringify(answers), now);
      return ticketRow(sql.ticketGet.get(lastInsertRowid));
    },

    ticket(id) {
      return ticketRow(sql.ticketGet.get(id)) ?? null;
    },

    ticketInChannel(channelId) {
      return ticketRow(sql.ticketByChannel.get(channelId)) ?? null;
    },

    activeTicket(userId, kind) {
      return ticketRow(sql.ticketActive.get(userId, kind)) ?? null;
    },

    decideTicket(id, status, decidedBy, note, now = Date.now()) {
      return sql.ticketDecide.run(status, decidedBy, note, now, id).changes > 0 ? this.ticket(id) : null;
    },

    closeTicket(id, now = Date.now()) {
      return sql.ticketClose.run(now, id).changes > 0;
    },

    setting(key) {
      return sql.setting.get(key)?.value ?? null;
    },

    setSetting(key, value) {
      if (value === null) {
        sql.dropSetting.run(key);
      } else {
        sql.putSetting.run(key, String(value));
      }
    },

    gainedRewards(rolesBefore, rolesAfter) {
      return this.rewardsForRoles(rolesBefore).length === 0 && this.rewardsForRoles(rolesAfter).length > 0;
    },

    markWelcomed(discordId, cooldownMs, now = Date.now()) {
      const last = sql.welcomedAt.get(discordId);
      if (last && now - last.at < cooldownMs) {
        return false;
      }
      sql.putWelcomed.run(discordId, now);
      return true;
    },

    issueCode(discordId, ttlMs, now = Date.now()) {
      sql.purgeCodes.run(now);
      sql.dropCodeOf.run(discordId);
      const code = newCode();
      sql.putCode.run(code, discordId, now + ttlMs);
      return code;
    },

    redeemCode(code, now = Date.now()) {
      const row = sql.findCode.get(code);
      if (!row) {
        return null;
      }
      sql.dropCode.run(code);
      return row.expires_at > now ? row.discord_id : null;
    },

    link(discordId, uuid, name, now = Date.now()) {
      sql.dropLinkByUuid.run(uuid);
      sql.putLink.run(discordId, uuid, name, now);
    },

    unlink(discordId) {
      return sql.dropLink.run(discordId).changes > 0;
    },

    linkOf(discordId) {
      return sql.linkOf.get(discordId) ?? null;
    },

    linkByUuid(uuid) {
      return sql.linkByUuid.get(uuid) ?? null;
    },

    addRoleReward(roleId, reward) {
      return sql.putReward.run(roleId, reward).changes > 0;
    },

    removeRoleReward(roleId, reward) {
      return sql.dropReward.run(roleId, reward).changes > 0;
    },

    roleRewards() {
      return sql.allRewards.all();
    },

    rewardsForRoles(roleIds) {
      const held = new Set(roleIds);
      const rewards = new Set(tierRewards(roleIds));
      for (const row of sql.allRewards.all()) {
        if (held.has(row.role_id)) {
          rewards.add(row.reward);
        }
      }
      return [...rewards].sort();
    },

    setAura(discordId, colour) {
      sql.putAura.run(discordId, colour);
    },

    clearAura(discordId) {
      return sql.dropAura.run(discordId).changes > 0;
    },

    auraOf(discordId) {
      return sql.auraOf.get(discordId)?.colour ?? null;
    },

    close() {
      db.close();
    },
  };
}
