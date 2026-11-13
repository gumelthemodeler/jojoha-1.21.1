import { normalizeUuid } from './rewards.js';

export const NAME_PATTERN = /^[A-Za-z0-9_]{3,16}$/;
const PROFILE_URL = 'https://api.mojang.com/users/profiles/minecraft/';

export async function lookupProfile(name, fetchImpl = fetch) {
  if (!NAME_PATTERN.test(name)) {
    return { error: 'bad_name' };
  }
  let response;
  try {
    response = await fetchImpl(PROFILE_URL + encodeURIComponent(name), { signal: AbortSignal.timeout(8000) });
  } catch {
    return { error: 'unreachable' };
  }
  if (response.status === 204 || response.status === 404) {
    return { error: 'not_found' };
  }
  if (!response.ok) {
    return { error: 'unreachable' };
  }
  const body = await response.json().catch(() => null);
  const uuid = normalizeUuid(body?.id);
  if (!uuid || typeof body?.name !== 'string') {
    return { error: 'unreachable' };
  }
  return { uuid, name: body.name };
}
