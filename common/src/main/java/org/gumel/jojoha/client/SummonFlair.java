package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.fx.ImpactDirector;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.workshop.SummonStyle;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SummonFlair {
    private static final float PX = 1F / 16F;
    private static final int SUMMON_TICKS = 24;
    private static final int UNSUMMON_TICKS = 7;
    private static final int SIGNATURE_AT = 4;
    private static final int SIGNATURE_TICKS = 8;
    private static final int AURA_TICKS = 8;
    private static final int AURA_WISPS = 12;
    private static final int TETHER_TICKS = 6;
    private static final int TETHER_MOTES = 7;
    private static final int ARRIVE_AT = 3;
    private static final int ARRIVE_TICKS = 5;
    private static final int ARRIVE_LINES = 10;
    private static final int RECALL_WISPS = 8;
    private static final float PUSH = 0.035F;
    private static final double RANGE = 48.0;
    private static final int GOLD = 0xFFFFD23F;
    private static final int DUSK = 0xFF2A1F33;
    private static final int RISE_DUST = 18;
    private static final float RISE_DEPTH = 1.1F;
    private static final int FLASH_TICKS = 4;
    private static final float FLASH_SIZE = 1.3F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int STRIDES = 2;
    private static final float STEP_OUT_ALPHA = 0.35F;
    private static final float STEP_TURN_FROM = 0.7F;
    private static final float STEP_BOB = 1.5F * PX;
    private static final float MENACING_ALPHA = 0.08F;
    private static final float MENACING_DARK = 0.12F;
    private static final int WHITE_BODY_TICKS = 2;
    private static final int SCREEN_FLASH_TICKS = 3;
    private static final int SCREEN_FLASH_ALPHA = 0x60;
    private static final int RISE_STEP_DUST = 8;

    private static final ResourceLocation[] RING = frames("fx/circle_large_", 5);
    private static final ResourceLocation[] AURA = frames("particle/stand_aura_", 0, 8);
    private static final ResourceLocation[] MOTE = frames("particle/stand_particle", 1, 12);
    private static final ResourceLocation[] SPARKLE = frames("particle/emerald_sparkle_", 3);
    private static final ResourceLocation STREAK = texture("fx/speed_streak");
    private static final ResourceLocation STAR = texture("fx/impact_star");
    private static final int STREAK_FRAMES = 3;
    private static final int STAR_FRAMES = 4;

    private static final class Flair {
        final int ownerId;
        final int colour;
        final boolean out;
        final SummonStyle style;
        final long born;
        final long seed;
        boolean signed;

        Flair(int ownerId, int cue, boolean out, long born) {
            this.ownerId = ownerId;
            this.colour = 0xFF000000 | cue & 0xFFFFFF;
            this.out = out;
            this.style = out ? SummonStyle.of(cue >>> SummonStyle.CUE_SHIFT) : SummonStyle.CLASSIC;
            this.born = born;
            this.seed = ownerId * 31L + born;
        }
    }

    private record View(Matrix4f pose, MultiBufferSource buffers, Vec3 eye, Vec3 right, Vec3 up, Set<RenderType> used) {
    }

    private static final List<Flair> FLAIRS = new ArrayList<>();
    private static long clock;

    private SummonFlair() {
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/" + path + ".png");
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        return frames(prefix, 1, count);
    }

    private static ResourceLocation[] frames(String prefix, int first, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = texture(prefix + (first + i));
        }
        return textures;
    }

    public static void clear() {
        FLAIRS.clear();
    }

    public static void heard(int ownerId, int cue, boolean out) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !JojohaClientConfig.SUMMON_FLAIR.on()) {
            return;
        }
        boolean mine = minecraft.player != null && minecraft.player.getId() == ownerId;
        if (!mine && !StandViewAlpha.seesStands()) {
            return;
        }
        FLAIRS.removeIf(flair -> flair.ownerId == ownerId);
        Flair flair = new Flair(ownerId, cue, out, clock);
        FLAIRS.add(flair);
        if (out && minecraft.player != null && minecraft.player.getId() == ownerId) {
            ImpactDirector.punch(PUSH);
        }
        if (flair.style == SummonStyle.MENACING) {
            Menacing.heard(ownerId, 0);
        }
    }

    private static Flair emerging(StandEntity stand) {
        Entity owner = stand.getOwner();
        if (owner == null) {
            return null;
        }
        for (Flair flair : FLAIRS) {
            if (flair.out && flair.ownerId == owner.getId()) {
                return flair;
            }
        }
        return null;
    }

    public static float emergeShare(StandEntity stand, float partialTick) {
        Flair flair = emerging(stand);
        if (flair == null) {
            return -1F;
        }
        float share = (clock - flair.born + partialTick) / flair.style.emergeTicks();
        return share >= 1F ? -1F : Math.max(0F, share);
    }

    public static boolean leavesShell(StandEntity stand) {
        Flair flair = emerging(stand);
        return flair != null && flair.style == SummonStyle.STEP_OUT;
    }

    public static float stride(StandEntity stand, float partialTick) {
        Flair flair = emerging(stand);
        if (flair == null || flair.style != SummonStyle.STEP_OUT) {
            return 0F;
        }
        float share = Mth.clamp((clock - flair.born + partialTick) / flair.style.emergeTicks(), 0F, 1F);
        return Mth.sin(share * STRIDES * Mth.PI);
    }

    public static float yaw(StandEntity stand, float partialTick, float yaw) {
        Flair flair = emerging(stand);
        Entity owner = stand.getOwner();
        if (flair == null || flair.style != SummonStyle.STEP_OUT || !(owner instanceof LivingEntity body)) {
            return yaw;
        }
        float share = Mth.clamp((clock - flair.born + partialTick) / flair.style.emergeTicks(), 0F, 1F);
        float turn = Mth.clamp((share - STEP_TURN_FROM) / (1F - STEP_TURN_FROM), 0F, 1F);
        float from = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
        return from + Mth.wrapDegrees(yaw - from) * turn * turn * (3F - 2F * turn);
    }

    public static boolean ghosts(StandEntity stand) {
        Flair flair = emerging(stand);
        return flair == null || flair.style == SummonStyle.CLASSIC || flair.style == SummonStyle.STEP_OUT;
    }

    public static float trail(StandEntity stand, float partialTick) {
        Flair flair = emerging(stand);
        if (flair == null || flair.style != SummonStyle.STEP_OUT) {
            return 1F;
        }
        return walked(Mth.clamp((clock - flair.born + partialTick) / flair.style.emergeTicks(), 0F, 1F));
    }

    private static float walked(float share) {
        float stride = share * STRIDES;
        int done = (int) stride;
        float part = stride - done;
        return Math.min(1F, (done + part * part * (3F - 2F * part)) / STRIDES);
    }

    private static float stepShare(Flair flair) {
        return Mth.clamp((float) (clock - flair.born) / flair.style.emergeTicks(), 0F, 1F);
    }

    public static float bodyAlpha(StandEntity stand) {
        Flair flair = emerging(stand);
        if (flair == null) {
            return 1F;
        }
        float share = stepShare(flair);
        return switch (flair.style) {
            case STEP_OUT -> STEP_OUT_ALPHA + (1F - STEP_OUT_ALPHA) * share;
            case MENACING -> MENACING_ALPHA + (1F - MENACING_ALPHA) * share;
            default -> 1F;
        };
    }

    public static float bodyShade(StandEntity stand) {
        Flair flair = emerging(stand);
        if (flair == null || flair.style != SummonStyle.MENACING) {
            return 1F;
        }
        float share = stepShare(flair);
        return MENACING_DARK + (1F - MENACING_DARK) * share * share;
    }

    public static float white(StandEntity stand) {
        Flair flair = emerging(stand);
        return flair != null && flair.style == SummonStyle.FLASH && clock - flair.born < WHITE_BODY_TICKS ? 1F : 0F;
    }

    public static void screen(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        for (Flair flair : FLAIRS) {
            int age = (int) (clock - flair.born);
            if (flair.out && flair.style == SummonStyle.FLASH && flair.ownerId == minecraft.player.getId()
                    && age < SCREEN_FLASH_TICKS) {
                int alpha = SCREEN_FLASH_ALPHA * (SCREEN_FLASH_TICKS - age) / SCREEN_FLASH_TICKS;
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
            }
        }
    }

    public static Vec3 offset(StandEntity stand, float partialTick) {
        Flair flair = emerging(stand);
        if (flair == null || flair.style != SummonStyle.RISE && flair.style != SummonStyle.STEP_OUT) {
            return Vec3.ZERO;
        }
        float share = Mth.clamp((clock - flair.born + partialTick) / flair.style.emergeTicks(), 0F, 1F);
        if (flair.style == SummonStyle.RISE) {
            float left = 1F - share;
            double depth = left * left * stand.getBbHeight() * RISE_DEPTH;
            return new Vec3(0.0, -Math.ceil(depth / PX) * PX, 0.0);
        }
        Entity owner = stand.getOwner();
        if (owner == null) {
            return Vec3.ZERO;
        }
        return owner.getPosition(partialTick).subtract(stand.getPosition(partialTick)).scale(1F - walked(share))
                .add(0.0, Math.abs(Mth.sin(share * STRIDES * Mth.PI)) * STEP_BOB, 0.0);
    }

    public static int emergeColour(StandEntity stand) {
        Flair flair = emerging(stand);
        return flair == null ? WHITE : mix(flair.colour, WHITE, 0.4F);
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        clock++;
        for (Iterator<Flair> it = FLAIRS.iterator(); it.hasNext(); ) {
            Flair flair = it.next();
            int age = (int) (clock - flair.born);
            Entity owner = level.getEntity(flair.ownerId);
            if (owner == null || age > (flair.out ? SUMMON_TICKS : UNSUMMON_TICKS)) {
                it.remove();
                continue;
            }
            if (!flair.out) {
                continue;
            }
            if (!flair.signed && age >= SIGNATURE_AT) {
                flair.signed = true;
                StandEntity stand = standOf(level, owner);
                if (stand != null) {
                    sign(level, stand);
                }
            }
            styleCue(level, flair, owner, age);
        }
    }

    private static StandEntity standOf(ClientLevel level, Entity owner) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && !stand.isManifesting()
                    && stand.getOwnerUuid().map(owner.getUUID()::equals).orElse(false)) {
                return stand;
            }
        }
        return null;
    }

    private static void styleCue(ClientLevel level, Flair flair, Entity owner, int age) {
        int half = flair.style.emergeTicks() / STRIDES;
        switch (flair.style) {
            case CLASSIC -> {
                if (age == 1) {
                    sound(level, chest(owner), SoundEvents.PHANTOM_SWOOP, 0.3F, 1.7F);
                }
            }
            case STEP_OUT -> {
                if (age == 1 || age == 1 + half) {
                    BlockState ground = level.getBlockState(owner.blockPosition().below());
                    if (!ground.isAir()) {
                        sound(level, owner.position(), ground.getSoundType().getStepSound(), 0.5F, 0.8F);
                    }
                }
            }
            case RISE -> {
                StandEntity stand = age == 1 || age == 1 + half ? standOf(level, owner) : null;
                if (stand != null) {
                    dust(level, stand, age == 1 ? RISE_DUST : RISE_STEP_DUST, age == 1 ? 0.7F : 0.55F);
                }
            }
            case FLASH -> {
                if (age == 1) {
                    sound(level, chest(owner), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.8F);
                }
            }
            case MENACING -> {
                if (age == 1) {
                    sound(level, chest(owner), SoundEvents.PORTAL_AMBIENT, 0.35F, 0.5F);
                }
            }
        }
    }

    private static void dust(ClientLevel level, StandEntity stand, int count, float pitch) {
        BlockState ground = level.getBlockState(stand.blockPosition().below());
        if (ground.isAir()) {
            return;
        }
        RandomSource random = level.random;
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            double radius = 0.3 + random.nextDouble() * 0.3;
            level.addParticle(particle, stand.getX() + Math.cos(angle) * radius, stand.getY() + 0.1,
                    stand.getZ() + Math.sin(angle) * radius, Math.cos(angle) * 0.15, 0.25, Math.sin(angle) * 0.15);
        }
        sound(level, stand.position(), ground.getSoundType().getBreakSound(), 0.6F, pitch);
    }

    private static Vec3 chest(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    private static void sign(ClientLevel level, StandEntity stand) {
        ResourceLocation id = stand.getStandType().id();
        Vec3 at = chest(stand);
        RandomSource random = level.random;
        if (StandTypes.THE_WORLD_ID.equals(id)) {
            sound(level, at, SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.9F);
        } else if (StandTypes.SILVER_CHARIOT_ID.equals(id)) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.6,
                        random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.6);
            }
            sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.5F);
        } else if (StandTypes.MAGICIANS_RED_ID.equals(id)) {
            FlameFx.burst(at, 22, 0.25);
            sound(level, at, SoundEvents.BLAZE_SHOOT, 0.4F, 0.8F);
        } else if (StandTypes.SECRET_THE_WORLD_ID.equals(id)) {
            for (int i = 0; i < 12; i++) {
                double angle = Mth.TWO_PI * i / 12;
                level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, Math.cos(angle) * 0.08, 0.02,
                        Math.sin(angle) * 0.08);
            }
        }
    }

    private static void sound(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || FLAIRS.isEmpty()) {
            return;
        }
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        View view = new View(poseStack.last().pose(), buffers, camera.getPosition(),
                new Vec3(-leftF.x(), -leftF.y(), -leftF.z()), new Vec3(upF.x(), upF.y(), upF.z()),
                new LinkedHashSet<>());

        for (Flair flair : FLAIRS) {
            Entity owner = level.getEntity(flair.ownerId);
            if (owner == null || owner.position().distanceToSqr(view.eye) > RANGE * RANGE) {
                continue;
            }
            int age = (int) (clock - flair.born);
            Vec3 feet = owner.getPosition(partialTick).add(0, 0.06, 0);
            if (flair.out) {
                summon(view, level, flair, owner, feet, age);
            } else {
                unsummon(view, flair, owner, feet, age);
            }
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            for (RenderType type : view.used) {
                source.endBatch(type);
            }
        }
    }

    private static void summon(View view, ClientLevel level, Flair flair, Entity owner, Vec3 feet, int age) {
        int light = mix(flair.colour, WHITE, 0.5F);
        float height = owner.getBbHeight();
        Vec3 chest = feet.add(0, height * 0.6, 0);
        RandomSource random = RandomSource.create(flair.seed);
        SummonStyle style = flair.style;
        int wisp = style == SummonStyle.MENACING ? mix(flair.colour, DUSK, 0.6F) : flair.colour;

        if (age < AURA_TICKS && (style == SummonStyle.CLASSIC || style == SummonStyle.MENACING)) {
            for (int i = 0; i < AURA_WISPS; i++) {
                double angle = Mth.TWO_PI * i / AURA_WISPS + random.nextFloat() * 0.6;
                double radius = 0.34 + random.nextFloat() * 0.14;
                double from = random.nextFloat() * height * 0.75;
                double rise = (0.1 + random.nextFloat() * 0.06) * age;
                int frame = Math.min(AURA.length - 1, age + (i & 1));
                float size = (6 + random.nextInt(4)) * PX * (1F - (float) age / (AURA_TICKS + 3));
                int tint = i % 3 == 0 ? light : wisp;
                Vec3 at = feet.add(Math.cos(angle) * radius, from + rise, Math.sin(angle) * radius);
                billboard(view, AURA[frame], 0F, 1F, at, size, size, tint);
            }
        }

        StandEntity stand = age >= 1 ? standOf(level, owner) : null;
        if (stand == null) {
            return;
        }
        Vec3 body = chest(stand);

        if (style == SummonStyle.FLASH) {
            if (age < FLASH_TICKS) {
                billboard(view, STAR, (float) age / STAR_FRAMES, (float) (age + 1) / STAR_FRAMES, body,
                        FLASH_SIZE, FLASH_SIZE, WHITE);
                int frame = Math.min(RING.length - 1, age);
                billboard(view, RING[frame], 0F, 1F, body, 0.6F + 0.4F * age, 0.6F + 0.4F * age, light);
            }
        }
        if (style == SummonStyle.RISE && age < RING.length) {
            Vec3 ground = stand.position().add(0, 0.05, 0);
            billboard(view, RING[age], 0F, 1F, ground, 0.9F + 0.15F * age, 0.25F + 0.05F * age, light);
        }

        if (age <= TETHER_TICKS + 2 && style == SummonStyle.STEP_OUT) {
            Vec3 path = body.subtract(chest);
            Vec3 side = path.cross(view.eye.subtract(chest));
            side = side.lengthSqr() < 1.0E-6 ? view.right : side.normalize();
            for (int k = 0; k < TETHER_MOTES; k++) {
                float share = (age - k * 0.45F) / TETHER_TICKS;
                if (share <= 0F || share >= 1F) {
                    continue;
                }
                double wobble = Math.sin(share * Math.PI) * (k % 2 == 0 ? 0.18 : -0.18);
                Vec3 at = chest.add(path.scale(share)).add(side.scale(wobble));
                float size = (5 - k / 3) * PX;
                billboard(view, MOTE[(age + k) % MOTE.length], 0F, 1F, at, size, size, k == 0 ? WHITE : light);
            }
        }

        int arrive = age - ARRIVE_AT;
        if (arrive >= 0 && arrive < ARRIVE_TICKS && style == SummonStyle.CLASSIC) {
            int frame = Math.min(STREAK_FRAMES - 1, arrive / 2);
            int alpha = arrive < 2 ? 0xFF : 0xFF - (arrive - 1) * 0x38;
            for (int i = 0; i < ARRIVE_LINES; i++) {
                double angle = Mth.TWO_PI * i / ARRIVE_LINES + random.nextFloat() * 0.3;
                Vec3 out = view.right.scale(Math.cos(angle)).add(view.up.scale(Math.sin(angle)));
                Vec3 across = view.right.scale(-Math.sin(angle)).add(view.up.scale(Math.cos(angle)));
                double reach = 0.42 + (0.2 + random.nextFloat() * 0.1) * arrive;
                Vec3 centre = body.add(out.scale(reach));
                sprite(view, STREAK, (float) frame / STREAK_FRAMES, (float) (frame + 1) / STREAK_FRAMES, centre,
                        across.scale(3 * PX), out.scale((10 + random.nextInt(5)) * PX), alpha << 24 | (light & 0xFFFFFF));
            }
        }

        int signed = age - SIGNATURE_AT;
        if (signed < 0 || signed >= SIGNATURE_TICKS) {
            return;
        }
        ResourceLocation id = stand.getStandType().id();
        if (StandTypes.STAR_PLATINUM_ID.equals(id)) {
            if (signed < STAR_FRAMES) {
                billboard(view, STAR, (float) signed / STAR_FRAMES, (float) (signed + 1) / STAR_FRAMES,
                        body.add(0, 0.5, 0), 0.7F, 0.7F, WHITE);
            }
        } else if (StandTypes.THE_WORLD_ID.equals(id)) {
            if (signed < RING.length) {
                int frame = RING.length - 1 - signed;
                billboard(view, RING[frame], 0F, 1F, body, 1.6F - 0.25F * signed, 1.6F - 0.25F * signed, GOLD);
            }
        } else if (StandTypes.HIEROPHANT_GREEN_ID.equals(id)) {
            for (int i = 0; i < 6; i++) {
                double angle = Mth.TWO_PI * i / 6 + random.nextFloat();
                Vec3 at = body.add(view.right.scale(Math.cos(angle) * (0.4 + 0.08 * signed)))
                        .add(view.up.scale(Math.sin(angle) * (0.5 + 0.08 * signed)));
                billboard(view, SPARKLE[(signed + i) % SPARKLE.length], 0F, 1F, at, 4 * PX, 4 * PX, WHITE);
            }
        }
    }

    private static void unsummon(View view, Flair flair, Entity owner, Vec3 feet, int age) {
        Vec3 chest = feet.add(0, owner.getBbHeight() * 0.6, 0);
        int light = mix(flair.colour, WHITE, 0.5F);
        RandomSource random = RandomSource.create(flair.seed);
        float share = (float) age / UNSUMMON_TICKS;
        for (int i = 0; i < RECALL_WISPS; i++) {
            double angle = Mth.TWO_PI * i / RECALL_WISPS + random.nextFloat() * 0.5 + share * 1.2;
            double radius = (0.9 + random.nextFloat() * 0.3) * (1F - share);
            double lift = (random.nextFloat() - 0.5) * 0.8 * (1F - share);
            Vec3 at = chest.add(Math.cos(angle) * radius, lift, Math.sin(angle) * radius);
            int frame = Math.max(0, AURA.length - 1 - age);
            float size = (5 + random.nextInt(3)) * PX;
            billboard(view, AURA[frame], 0F, 1F, at, size, size, i % 2 == 0 ? light : flair.colour);
        }
        if (age >= UNSUMMON_TICKS - 2) {
            billboard(view, MOTE[age % MOTE.length], 0F, 1F, chest, 6 * PX, 6 * PX, WHITE);
        }
    }

    private static void billboard(View view, ResourceLocation texture, float u0, float u1, Vec3 centre,
                                  float halfW, float halfH, int argb) {
        sprite(view, texture, u0, u1, centre, view.right.scale(halfW), view.up.scale(halfH), argb);
    }

    private static void sprite(View view, ResourceLocation texture, float u0, float u1, Vec3 centre, Vec3 right,
                               Vec3 up, int argb) {
        RenderType type = RenderType.text(texture);
        view.used.add(type);
        VertexConsumer out = view.buffers.getBuffer(type);

        Vec3 bl = centre.subtract(right).subtract(up);
        Vec3 br = centre.add(right).subtract(up);
        Vec3 tr = centre.add(right).add(up);
        Vec3 tl = centre.subtract(right).add(up);

        vertex(view, out, bl, argb, u0, 1F);
        vertex(view, out, br, argb, u1, 1F);
        vertex(view, out, tr, argb, u1, 0F);
        vertex(view, out, tl, argb, u0, 0F);

        vertex(view, out, tl, argb, u0, 0F);
        vertex(view, out, tr, argb, u1, 0F);
        vertex(view, out, br, argb, u1, 1F);
        vertex(view, out, bl, argb, u0, 1F);
    }

    private static void vertex(View view, VertexConsumer out, Vec3 at, int argb, float u, float v) {
        out.addVertex(view.pose, (float) (at.x - view.eye.x), (float) (at.y - view.eye.y), (float) (at.z - view.eye.z))
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static int mix(int from, int to, float share) {
        int r = Math.round(((from >> 16) & 0xFF) * (1F - share) + ((to >> 16) & 0xFF) * share);
        int g = Math.round(((from >> 8) & 0xFF) * (1F - share) + ((to >> 8) & 0xFF) * share);
        int b = Math.round((from & 0xFF) * (1F - share) + (to & 0xFF) * share);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
