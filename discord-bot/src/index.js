import { openStore } from './store.js';
import { createApi } from './api.js';
import { createBot } from './bot.js';

function required(name) {
  const value = process.env[name];
  if (!value) {
    console.error(`Missing environment variable ${name}. See .env.example.`);
    process.exit(1);
  }
  return value;
}

const token = required('DISCORD_TOKEN');
const guildId = required('GUILD_ID');
const port = Number(process.env.PORT ?? 8080);
const dbPath = process.env.DB_PATH ?? './data/jojoha.db';
const trustProxy = process.env.TRUST_PROXY === '1';
const clientSecret = process.env.DISCORD_CLIENT_SECRET ?? null;
const siteUrl = (process.env.SITE_URL ?? 'https://jojoha-supporter-bot-production.up.railway.app').replace(/\/+$/, '');

const store = openStore(dbPath);
const bot = createBot({ token, guildId, store });
const api = createApi({
  store, rolesOf: bot.rolesOf, nameOf: bot.nameOf, onLinked: bot.onLinked, trustProxy,
  canManage: bot.canManage, clientId: bot.clientId, clientSecret, siteUrl,
});

api.listen(port, () => console.log(`Supporter API listening on port ${port}`));
await bot.start();

const shutdown = () => {
  api.close();
  store.close();
  process.exit(0);
};
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
