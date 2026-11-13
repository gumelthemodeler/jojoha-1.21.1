package org.gumel.jojoha.client.anim;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClipContact {
    private static final float STEP = 0.025F;
    private static final float STILL = (float) Math.toRadians(1.5);
    private static final float RAISED = 0.5F;
    private static final float REACHED = 0.4F;
    private static final float BARELY_MOVES = 0.2F;
    private static final String[] ARMS = {"right_arm", "left_arm"};
    private static final float HELD_DEGREES = 12F;
    private static final float SAME_TIME = 0.002F;

    private static final Map<BedrockAnimation, float[]> CACHE = new WeakHashMap<>();

    private ClipContact() {
    }

    public static float[] of(BedrockAnimation anim) {
        return CACHE.computeIfAbsent(anim, ClipContact::measure);
    }

    public static float clamp(BedrockAnimation anim, float seconds) {
        float[] window = of(anim);
        return Math.max(window[0], Math.min(window[1], seconds));
    }

    private static float[] measure(BedrockAnimation anim) {
        if (anim.marker() >= 0F) {
            return new float[] {anim.marker(), anim.marker()};
        }
        float[] held = held(anim);
        if (held != null) {
            return held;
        }
        float turn = turn(anim);
        if (turn >= 0F) {
            return new float[] {turn, turn};
        }
        return sampled(anim);
    }

    private static float[] held(BedrockAnimation anim) {
        List<List<float[]>> arms = new ArrayList<>();
        for (String arm : ARMS) {
            List<float[]> keys = anim.keys(arm, BedrockAnimation.Channel.ROTATION);
            if (keys.size() > 1) {
                arms.add(keys);
            }
        }
        if (arms.isEmpty()) {
            return null;
        }
        for (float[] key : arms.get(0)) {
            float from = key[0];
            if (from <= SAME_TIME || from >= anim.lengthSeconds() - SAME_TIME) {
                continue;
            }
            float until = Float.MAX_VALUE;
            boolean still = true;
            for (List<float[]> keys : arms) {
                float[] here = at(keys, from);
                float[] next = here == null ? null : after(keys, from);
                if (next == null || next[0] >= anim.lengthSeconds() - SAME_TIME || drift(here, next) > HELD_DEGREES) {
                    still = false;
                    break;
                }
                until = Math.min(until, next[0]);
            }
            if (still) {
                return new float[] {from, until};
            }
        }
        return null;
    }

    private static float turn(BedrockAnimation anim) {
        float best = -1F;
        float most = 0F;
        for (float[] key : anim.keys("torso", BedrockAnimation.Channel.ROTATION)) {
            if (key[0] <= SAME_TIME || key[0] >= anim.lengthSeconds() - SAME_TIME) {
                continue;
            }
            float size = (float) Math.sqrt(key[1] * key[1] + key[2] * key[2] + key[3] * key[3]);
            if (size > most) {
                most = size;
                best = key[0];
            }
        }
        return most > 1F ? best : -1F;
    }

    private static float[] at(List<float[]> keys, float time) {
        for (float[] key : keys) {
            if (Math.abs(key[0] - time) <= SAME_TIME) {
                return key;
            }
        }
        return null;
    }

    private static float[] after(List<float[]> keys, float time) {
        for (float[] key : keys) {
            if (key[0] > time + SAME_TIME) {
                return key;
            }
        }
        return null;
    }

    private static float drift(float[] a, float[] b) {
        return (float) Math.sqrt((a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2])
                + (a[3] - b[3]) * (a[3] - b[3]));
    }

    private static float[] sampled(BedrockAnimation anim) {
        int steps = Math.max(2, (int) (anim.lengthSeconds() / STEP));
        float[] reach = new float[steps + 1];
        float[] speed = new float[steps + 1];
        Vector3f[] rest = new Vector3f[ARMS.length];
        Vector3f[] last = new Vector3f[ARMS.length];
        float peak = 0F;
        int furthest = 0;
        for (int i = 0; i <= steps; i++) {
            float t = Math.min(anim.lengthSeconds(), i * STEP);
            for (int a = 0; a < ARMS.length; a++) {
                Vector3f now = anim.drives(ARMS[a])
                        ? anim.sample(ARMS[a], BedrockAnimation.Channel.ROTATION, t) : null;
                if (now == null) {
                    continue;
                }
                if (rest[a] == null) {
                    rest[a] = new Vector3f(now);
                }
                reach[i] += now.distance(rest[a]);
                if (last[a] != null && i > 0) {
                    speed[i - 1] += now.distance(last[a]);
                }
                last[a] = now;
            }
            if (reach[i] > peak) {
                peak = reach[i];
                furthest = i;
            }
        }
        speed[steps] = Float.MAX_VALUE;
        if (peak < BARELY_MOVES) {
            float quarter = anim.lengthSeconds() * 0.25F;
            return new float[] {quarter, quarter};
        }
        for (int i = 0; i < steps; i++) {
            if (reach[i] >= peak * RAISED && speed[i] <= STILL) {
                int end = i;
                while (end + 1 < steps && speed[end + 1] <= STILL) {
                    end++;
                }
                return new float[] {i * STEP, (end + 1) * STEP};
            }
        }
        for (int i = 1; i < steps; i++) {
            if (reach[i] >= peak * REACHED && speed[i] <= speed[i - 1] && speed[i] <= speed[i + 1]) {
                return new float[] {i * STEP, i * STEP};
            }
        }
        return new float[] {furthest * STEP, furthest * STEP};
    }
}
