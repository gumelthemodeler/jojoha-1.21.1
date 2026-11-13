import { createHash } from 'node:crypto';

export const KINDS = ['texture', 'model', 'animations'];

export const CONTENT_TYPES = {
  texture: 'image/png',
  model: 'application/json',
  animations: 'application/json',
};

export const MAX_BYTES = {
  texture: 1024 * 1024,
  model: 2 * 1024 * 1024,
  animations: 2 * 1024 * 1024,
};

export const MAX_SIDE = 1024;

export const STANDS = [
  ['jojoha:star_platinum', 'Star Platinum'],
  ['jojoha:the_world', 'The World'],
  ['jojoha:hermit_purple', 'Hermit Purple'],
  ['jojoha:hierophant_green', 'Hierophant Green'],
  ['jojoha:silver_chariot', 'Silver Chariot'],
  ['jojoha:magicians_red', "Magician's Red"],
  ['jojoha:anubis', 'Anubis'],
  ['jojoha:the_world_over_heaven', 'The World Over Heaven'],
];

export const SHARED_BONES = ['Head', 'RightArm', 'bone2', 'LeftArm', 'bone3', 'RightLeg', 'bone', 'LeftLeg', 'bone4'];

const SLUG_PATTERN = /^[a-z0-9_]{3,40}$/;
const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

export function sha256(buffer) {
  return createHash('sha256').update(buffer).digest('hex');
}

export function standLabel(id) {
  return STANDS.find(([stand]) => stand === id)?.[1] ?? id;
}

export function checkSlug(slug) {
  return SLUG_PATTERN.test(slug) ? null : 'The id must be 3 to 40 lowercase letters, numbers or underscores.';
}

export function checkTexture(buffer) {
  if (buffer.length > MAX_BYTES.texture) {
    return { error: 'The texture is over 1 MB.' };
  }
  if (buffer.length < 24 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) {
    return { error: 'The texture must be a PNG file.' };
  }
  const width = buffer.readUInt32BE(16);
  const height = buffer.readUInt32BE(20);
  if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE) {
    return { error: `The texture must be at most ${MAX_SIDE}×${MAX_SIDE} pixels.` };
  }
  return { width, height };
}

function parseJson(buffer) {
  try {
    return JSON.parse(buffer.toString('utf8').replace(/^﻿/, ''));
  } catch {
    return null;
  }
}

export function checkModel(buffer, allowMissing = false) {
  if (buffer.length > MAX_BYTES.model) {
    return { error: 'The model is over 2 MB.' };
  }
  const json = parseJson(buffer);
  const geometry = json?.['minecraft:geometry']?.[0];
  if (!geometry || !Array.isArray(geometry.bones)) {
    return { error: 'The model must be a GeckoLib `.geo.json` export (Blockbench → Export → GeckoLib model).' };
  }
  if (json.format_version !== '1.12.0') {
    return { error: 'The model must use format version 1.12.0. Re-export it from Blockbench as a GeckoLib model.' };
  }
  const names = new Set(geometry.bones.map((bone) => bone?.name));
  const missing = SHARED_BONES.filter((name) => !names.has(name));
  if (missing.length > 0 && !allowMissing) {
    return {
      error: `The model is missing shared bones, so the Stand's animations would break: ${missing.join(', ')}. `
        + 'Rename the bones, or upload again with `allow_missing_bones:True` if this Stand is not humanoid.',
    };
  }
  const width = geometry.description?.texture_width;
  const height = geometry.description?.texture_height;
  return { bones: names.size, missing, width, height, barrage: names.has('barrage_rig') };
}

export function checkAnimations(buffer) {
  if (buffer.length > MAX_BYTES.animations) {
    return { error: 'The animations file is over 2 MB.' };
  }
  const json = parseJson(buffer);
  if (!json || typeof json.animations !== 'object' || json.animations === null) {
    return { error: 'The animations file must be a Blockbench `.animation.json` export.' };
  }
  return { clips: Object.keys(json.animations).length };
}

export function checkAura(raw, parse) {
  if (raw === null || raw === undefined || raw.trim() === '') {
    return { colour: null };
  }
  const colour = parse(raw);
  if (colour === null || colour < 0 || colour > 0xffffff) {
    return { error: `"${raw}" isn't a colour. Use a hex color like #B3121B or a preset name (not rainbow).` };
  }
  return { colour };
}

export function hex(colour) {
  return `#${colour.toString(16).padStart(6, '0').toUpperCase()}`;
}

export function catalog(skins) {
  return {
    skins: skins.map((skin) => ({
      id: skin.id,
      slug: skin.slug,
      stand: skin.stand,
      name: skin.name,
      version: skin.version,
      ...(skin.aura !== null && skin.aura !== undefined ? { aura: skin.aura } : {}),
      ...(skin.aura2 !== null && skin.aura2 !== undefined ? { aura2: skin.aura2 } : {}),
      files: Object.fromEntries(skin.files.map((file) => [file.kind, file.sha256])),
    })),
  };
}
