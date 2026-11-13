export const SUPPORTER_SKINS = 'jojoha:supporter_skins';
export const SUPPORTER_AURA = 'jojoha:supporter_aura';
export const SUPPORTER_BUILD = 'jojoha:supporter_build';

export const REWARDS = [
  { id: SUPPORTER_SKINS, label: 'Supporter Stand skins' },
  { id: SUPPORTER_AURA, label: 'Custom Stand aura color' },
  { id: SUPPORTER_BUILD, label: 'Supporter build', byRole: true },
];

export const TIERS = ['TIP', 'LIGHT', 'STANDARD', 'KING', 'MAFIA', 'ULTIMATE'];
export const PERK_TIER = 'STANDARD';

const TIER_TAG = /^\s*\[\s*([a-z]+)\s*\]/i;
const TIER_KEY = 'tier:';

export function tierOf(roleName) {
  const match = TIER_TAG.exec(String(roleName ?? ''));
  const name = match?.[1].toUpperCase();
  return name && TIERS.includes(name) ? name : null;
}

export function roleKeys(roles) {
  const keys = [];
  for (const role of roles) {
    keys.push(role.id);
    const tier = tierOf(role.name);
    if (tier) {
      keys.push(TIER_KEY + tier);
    }
  }
  return keys;
}

export function tierRewards(keys) {
  const floor = TIERS.indexOf(PERK_TIER);
  const earned = keys.some((key) => key.startsWith(TIER_KEY) && TIERS.indexOf(key.slice(TIER_KEY.length)) >= floor);
  return earned ? REWARDS.filter((reward) => !reward.byRole).map((reward) => reward.id) : [];
}

export function rewardLabel(id) {
  return REWARDS.find((reward) => reward.id === id)?.label ?? id;
}

const UUID_PATTERN = /^([0-9a-f]{8})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})$/i;

export function normalizeUuid(raw) {
  const match = typeof raw === 'string' ? UUID_PATTERN.exec(raw.trim()) : null;
  return match ? match.slice(1).join('-').toLowerCase() : null;
}

const COLOR_PATTERN = /^#?([0-9a-f]{6})$/i;

export const RAINBOW = 0x1000000;

export const PRESETS = {
  rainbow: RAINBOW,
  red: 0xe0303a,
  orange: 0xff8a1e,
  gold: 0xffc832,
  yellow: 0xfff04a,
  lime: 0x8cf03c,
  green: 0x32c850,
  cyan: 0x3ce6e6,
  sky: 0x6eb4ff,
  blue: 0x3a5af0,
  purple: 0x9a46e6,
  pink: 0xff78c8,
  white: 0xf4f4f4,
  shadow: 0x2a2a36,
};

export function parseColor(raw) {
  if (typeof raw !== 'string') {
    return null;
  }
  const text = raw.trim().toLowerCase();
  if (Object.hasOwn(PRESETS, text)) {
    return PRESETS[text];
  }
  const match = COLOR_PATTERN.exec(text);
  return match ? parseInt(match[1], 16) : null;
}

export function formatColor(colour) {
  const preset = Object.keys(PRESETS).find((name) => PRESETS[name] === colour);
  if (preset) {
    return preset[0].toUpperCase() + preset.slice(1);
  }
  return '#' + colour.toString(16).padStart(6, '0').toUpperCase();
}
