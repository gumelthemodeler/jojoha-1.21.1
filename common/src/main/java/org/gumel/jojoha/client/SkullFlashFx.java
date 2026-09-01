package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.SkullFlashPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The skull inside a crushed head: it glows through during the grab, and comes apart on the punch.
 *
 * <h2>Why there is no model file</h2>
 *
 * <p>Vanilla already ships one and it is reachable. {@code SkullBlockRenderer.createSkullRenderers}
 * hands back a model per skull type from the game's own model set, and {@code renderSkull} draws one
 * with a render type of your choosing. The skeleton skull is borrowed rather than rebuilt - no
 * texture, no geometry, and it looks like the game it is in.
 *
 * <h2>Where this is drawn from</h2>
 *
 * <p>Not from a world render pass but from inside the victim's own render, before their body is
 * drawn - see {@code SkullCrushRendererMixin}, which is also what makes them see-through. The skull
 * is solid; it is the body around it that fades.
 *
 * <p>Two earlier attempts got this wrong in instructive ways. The first drew the skull additively
 * with {@code RenderType.eyes}, which sums its colour with whatever is behind it - a pale texture
 * over a lit head came out as a white blob with no bone in it. The second drew it solidly but from a
 * world pass after the entity, where the head had already written depth and the skull inside it was
 * rejected by the depth test and never appeared at all.
 *
 * <p>Order is the fix, not the render type: drawn first, the skull writes depth and the translucent
 * body then blends over the top of it.
 *
 * <h2>Lighting</h2>
 *
 * <p>Lit at full brightness rather than by the block it stands in. It is inside a body, in whatever
 * gloom the fight is happening in, seen through a skin - three things taking light off it before
 * anyone sees any bone. Full brightness on a solid, properly textured model is not a glow; it is
 * simply legible, which the earlier version was not.
 */
public final class SkullFlashFx {
    /** The skull's own texture, wanted directly because the render type is not vanilla's. */
    private static final ResourceLocation SKULL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    /**
     * How tall the skull model is at scale one, in blocks.
     *
     * <p>Half a block - a skull block fills the bottom half of the block it sits in, which is where
     * this number comes from and why it is not a guess. It is also, conveniently, exactly the height
     * of a player's head.
     */
    private static final float SKULL_HEIGHT = 0.5F;

    /**
     * Slightly under head-sized, so it rests inside rather than wearing the face as a mask.
     *
     * <p>At one it would be exactly a head and would z-fight with the skin it is sitting in.
     */
    private static final float SKULL_SCALE = 0.92F;

    /**
     * How far above the eyes the middle of a head actually is.
     *
     * <p>Eye height is the closest thing to a head-centre every living entity agrees on, but eyes sit
     * a little below the middle of the head they are in - three centimetres of it on a player, whose
     * head runs 1.40 to 1.90 with eyes at 1.62. Without this the skull hangs out of the chin.
     *
     * <p>Set higher than that geometric middle on purpose. Dead centre reads as low, because a skull
     * is read by its cranium and not by its jaw, and the jaw is the half sitting below the eyes.
     */
    private static final float HEAD_LIFT = 0.08F;

    /**
     * How see-through the victim becomes, from the moment they are caught to the moment they are hit.
     *
     * <p>Down to a sixth. It stopped at 0.28 before, which sounds transparent and is not: a skin at
     * a quarter strength over a bone texture still hides most of it, and the skull was there the
     * whole time behind a body nobody could see past. If the point is to look inside someone, the
     * outside has to nearly go.
     *
     * <p>Never all the way to nothing, though - a body that vanishes entirely reads as a despawn.
     */
    /**
     * How see-through the body goes at the one moment it goes see-through at all.
     *
     * <p>Low, because it is only there for a fifth of a second and has one job in that time: let
     * the skull be seen clearly enough to watch it go. A gentle fade needs to stay mild so the
     * halfway frames still read; a snap has no halfway frames to protect.
     */
    private static final float BODY_ALPHA_OPEN = 0.22F;

    /**
     * When the body snaps shut again, as a share of the crush.
     *
     * <p>Deliberately the same figure as {@link #POP_OUT}: the body is open for exactly as long as
     * there is a skull inside it to look at, and shuts on the frame the skull finishes imploding.
     * The sequence is solid, snap open on the implosion, snap shut, then thrown - so the last thing
     * seen before the launch is the mob itself rather than a ghost of it.
     */
    private static final float BODY_RETURN_AT = 0.8F;

    /**
     * The break, which happens inside the head rather than out of it.
     *
     * <p>There used to be seven of them - the same model at a third of the size, spun and pushed
     * apart. Two things were wrong with that. A skull made of smaller skulls is a thing the eye
     * refuses to read as breaking, because every piece is still recognisably the whole; and seven
     * model draws a frame for a fifth of a second was the dearest part of the move by a distance.
     *
     * <p>What replaced it is one skull being pressed flat, and particles for the rest. The model
     * does the part a model is good at - a solid thing losing its shape - and the burst does the
     * part particles are good at, which is being debris. See SkullCrusherSkill.burst.
     */
    private static final float POP_OUT = org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill.POP_AT;

    /**
     * How much of the crush passes before the skull starts to come apart.
     *
     * <p>None of it. There used to be a beat of the skull sitting whole after the fist landed, on the
     * theory that it made the break read as a consequence rather than the same event. In practice it
     * did the opposite - a punch that lands and then waits has no impact in it at all. The moment of
     * contact and the moment of breaking are the same moment, and the flash is what separates them.
     */
    private static final float BREAK_AT = 0F;

    /**
     * The pop, measured in ticks rather than as a share of the crush.
     *
     * <p>That is the whole correction. Spreading the collapse across a proportion of the beat meant
     * it stretched every time the beat got longer - at twenty-two ticks of crush it was taking over
     * half a second to shrink, which is not a pop, it is a deflation. Three ticks is a pop, and it
     * stays three ticks whatever else changes around it.
     *
     * <p>So the skull now holds at full size for the whole of its time on screen and then goes in
     * one movement: a frame of swelling, which is the anticipation the collapse needs in order to be
     * a change from something, and two frames of it crashing to nothing.
     *
     * <p>Uniform on every axis, which is the other half of it. The moment one axis moves differently
     * from another it stops being an implosion and starts being a squash, and a squash reads as the
     * model being deformed rather than as the skull failing.
     */
    private static final float POP_TICKS =
            org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill.POP_TICKS;
    private static final float POP_SWELL = 1.3F;
    private static final float POP_SWELL_AT = 0.3F;

    /**
     * The shudder before it goes, in blocks, and how fast it buzzes.
     *
     * <p>Small enough to stay inside the head it is in - the point of the move is that the crushing
     * happens in there, and a skull that wanders outside the skin is a skull that has escaped.
     *
     * <p>It grows as the pop approaches rather than running flat, so it is a build and not a
     * vibration. A rattle at a constant strength says something is broken already; one that tightens
     * says something is about to give, which is the only thing this beat has to say.
     *
     * <p>The three axes run at deliberately unrelated rates. Matched ones would beat together into a
     * single clean oscillation, which reads as a bob - the thing this is specifically not.
     */
    private static final float RATTLE_MAX = 0.032F;
    private static final float RATTLE_FREQ = 3.1F;

    /** How hard the puncher's own camera shakes at the peak of the wind-up, in degrees. */
    private static final float SHAKE_YAW = 0.85F;
    private static final float SHAKE_PITCH = 0.55F;

    /**
     * The kick on the frame the fist lands, in degrees, and how fast it dies.
     *
     * <p>Four times the wind-up tremble and gone inside a third of a second. A shake that stays at
     * one strength for a second is a rumble; an impact is a single hard displacement that recovers,
     * and the recovery is the part that sells the hit.
     */
    private static final float KICK_DEGREES = 3.6F;
    private static final int KICK_TICKS = 5;

    /**
     * The white frame on contact.
     *
     * <p>Three ticks. The whole trick of an impact frame is that it is over before it registers as a
     * thing in its own right - long enough to punctuate, short enough that what you remember is the
     * hit rather than the flash.
     */
    private static final int FLASH_TICKS = 3;
    private static final float FLASH_ALPHA = 0.78F;

    private static final List<Flash> ACTIVE = new ArrayList<>();

    private static Map<SkullBlock.Type, SkullModelBase> models;

    /**
     * The body below the neck, baked once.
     *
     * <p>A plain {@code HumanoidModel} over the skeleton layer rather than a {@code SkeletonModel}.
     * The two are the same geometry - the subclass exists to pose a bow - and the subclass is
     * generic over {@code Mob & RangedAttackMob}, which a player is not. Posing it by hand costs
     * nothing here and takes the type problem away entirely.
     */
    private static HumanoidModel<LivingEntity> bones;

    private SkullFlashFx() {
    }

    /**
     * How transparent this entity's body should be right now, or -1 if it is not involved.
     *
     * <p>Asked by the renderer mixin on every living entity every frame, so it answers from a short
     * list and allocates nothing.
     */
    public static float bodyAlpha(LivingEntity entity) {
        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.victimId != entity.getId()) {
                continue;
            }

            // Solid for the whole seize. Not "nearly solid" - untouched, which is what returning a
            // negative means to the renderer: no translucent render type, no alpha, and nothing
            // drawn inside either, because the skull and the skeleton are both gated on this value.
            // A body you cannot see into does not need its insides drawn, and this is the cheaper
            // half of the move as a result.
            if (flash.phase == SkullFlashPacket.WINDUP) {
                return -1F;
            }

            // And then it is a switch, thrown twice.
            //
            // Both edges used to be ramps, and a ramp is the wrong shape for this. Fading in over a
            // few ticks announces itself - you watch the body dissolving, so by the time the skull
            // is visible the interesting part has already been given away, and the same in reverse
            // on the way out. What lands instead is the cut: solid, solid, solid, and then the flesh
            // is simply not there any more and there is a skull inside it. Nothing is interpolated,
            // deliberately - the value is read off a whole tick, so it changes between two frames
            // and never sits at a value in between.
            float progress = 1F - flash.share();
            return progress < BODY_RETURN_AT ? BODY_ALPHA_OPEN : -1F;
        }
        return -1F;
    }

    /** Called when a packet lands. A new phase for the same victim replaces the old one. */
    public static void begin(int victimId, int attackerId, int phase, int ticks) {
        ACTIVE.removeIf(flash -> flash.victimId == victimId);
        ACTIVE.add(new Flash(victimId, attackerId, phase, ticks));

        // The skull giving way is what drains the colour out of the world. Asked for here rather
        // than from the move itself because this is the moment the client learns it happened, and
        // an impact frame that arrives a tick late is not an impact frame.
        //
        // Both ends of the punch see it, and nobody else. For the two people in it, it is the hit
        // registering; for a bystander across the courtyard the world going monochrome with no
        // explanation is a graphical fault.
        if (phase == SkullFlashPacket.SHATTER) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && (minecraft.player.getId() == attackerId
                        || minecraft.player.getId() == victimId)) {
                ImpactFrame.begin(1F);
            }
        }
    }

    public static void tick() {
        Iterator<Flash> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    /**
     * How hard the screen should be dimmed for the local player, from zero to one.
     *
     * <p>Only for the person throwing the punch, and only while there is something to look at. It
     * climbs through the hold, sits at full through the crush, and lets go as the victim is thrown -
     * so the picture opens back up on the same beat they leave.
     */
    public static float focus(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0F;
        }

        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.attackerId != minecraft.player.getId()) {
                continue;
            }

            // Interpolated inside the tick, not sampled at its edge - this is read once a frame
            // and a value that only changes twenty times a second is the whole of the choppiness.
            float progress = 1F - flash.share(partialTick);
            if (flash.phase == SkullFlashPacket.WINDUP) {
                return progress;
            }
            return progress < BODY_RETURN_AT ? 1F
                    : 1F - (progress - BODY_RETURN_AT) / (1F - BODY_RETURN_AT);
        }
        return 0F;
    }

    /**
     * The white impact frame, from one down to zero, for the puncher only.
     *
     * <p>Zero for everyone else. A screen-filling flash on a bystander's monitor with no explanation
     * is not a punch landing, it is a graphical fault.
     */
    public static float flash(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0F;
        }

        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.attackerId != minecraft.player.getId()
                    || flash.phase != SkullFlashPacket.SHATTER) {
                continue;
            }

            float elapsedTicks = (1F - flash.share(partialTick)) * flash.ticks;
            if (elapsedTicks >= FLASH_TICKS) {
                return 0F;
            }

            // Squared, so it is at its brightest on the first frame and mostly gone by the second -
            // a linear fade over three ticks reads as a slow white wash rather than a snap.
            float left = 1F - elapsedTicks / FLASH_TICKS;
            return left * left;
        }
        return 0F;
    }

    /**
     * How much the local player's camera should shake, or null.
     *
     * <p>Only the puncher's. Everyone else sees the skull and the shards; the jolt belongs to the
     * person whose fist is doing it, the same way the awakening shake belongs to the person waking.
     */
    public static float[] cameraShake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        for (Flash flash : ACTIVE) {
            if (flash.attackerId != minecraft.player.getId()) {
                continue;
            }

            float time = minecraft.player.tickCount + flash.remaining;

            if (flash.phase == SkullFlashPacket.WINDUP) {
                // A tremble that grows as the fist draws back.
                float weight = 1F - flash.share();
                return new float[]{
                        Mth.sin(time * 2.7F) * SHAKE_YAW * weight,
                        Mth.cos(time * 3.4F) * SHAKE_PITCH * weight};
            }

            // And on contact, a kick rather than a shake: hard on the first frames, gone in a third
            // of a second, with the sign alternating each tick so it snaps back and forth instead of
            // sliding one way.
            float elapsedTicks = (1F - flash.share()) * flash.ticks;
            if (elapsedTicks >= KICK_TICKS) {
                return null;
            }

            float decay = 1F - elapsedTicks / KICK_TICKS;
            float bite = decay * decay;
            float sign = (flash.remaining % 2 == 0) ? 1F : -1F;
            return new float[]{
                    sign * KICK_DEGREES * bite,
                    sign * KICK_DEGREES * 0.45F * bite};
        }
        return null;
    }

    /**
     * Draws the skull inside one victim, in the pose their own renderer is already standing in.
     *
     * <p>That pose is at their feet, unrotated and camera-relative, which is why there is no
     * interpolating or camera maths here any more - the renderer has done all of it.
     */
    public static void renderInside(LivingEntity victim, PoseStack poseStack,
                                    MultiBufferSource buffers, int light, float partialTick) {
        Flash flash = null;
        for (int i = 0; i < ACTIVE.size(); i++) {
            if (ACTIVE.get(i).victimId == victim.getId()) {
                flash = ACTIVE.get(i);
                break;
            }
        }
        if (flash == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Built once, from the game's own model set, and only when something actually needs it.
        if (models == null) {
            models = SkullBlockRenderer.createSkullRenderers(minecraft.getEntityModels());
        }

        SkullModelBase model = models.get(SkullBlock.Types.SKELETON);
        if (model == null) {
            return;
        }

        // Solid. The fading is the body's job, not the skull's - the whole point is that what you
        // see through the head is a real skull and not a stain on it.
        RenderType bone = RenderType.entitySolid(SKULL_TEXTURE);

        // Half a turn, and this one is derived rather than tried.
        //
        // SkullBlock.getStateForPlacement stores the placer's own yaw as the rotation, and renderSkull
        // is handed that back as degrees. But a placed skull looks *at* whoever placed it: a player at
        // yaw 0 faces south, puts the skull down south of them, and it looks back north - which is yaw
        // 180. So the face ends up pointing along (degrees + 180).
        //
        // Wanting the face along the mob's own head yaw therefore means handing over headYaw - 180,
        // which is the same angle as headYaw + 180.
        float yaw = Mth.rotLerp(partialTick, victim.yHeadRotO, victim.yHeadRot) + 180F;

        // Eye height is the middle of a head on very nearly everything that has one, and the skull
        // is placed by its base - so it starts half its own height below that.
        float base = victim.getEyeHeight() + HEAD_LIFT - SKULL_HEIGHT * SKULL_SCALE / 2F;

        poseStack.pushPose();
        poseStack.translate(0F, base, 0F);

        if (flash.phase == SkullFlashPacket.WINDUP) {
            windup(poseStack, buffers, model, bone, yaw, flash, partialTick, light);
        } else {
            shatter(poseStack, buffers, model, bone, yaw, flash, partialTick, light);
        }

        poseStack.popPose();
    }

    /**
     * The grab: the skull sits inside the head, and stays there.
     *
     * <p>It used to shiver, and grow as the fist wound back. Both had to go. The shiver moved it
     * relative to the head it is supposed to be inside, which is exactly what reads as bobbing - a
     * thing inside a skull is anchored to the skull, and anything that breaks that reads as two
     * objects rather than one. The growth did the same over a longer period.
     *
     * <p>What conveys the pressure now is the body clearing away around it, which does not move the
     * skull at all.
     */
    private static void windup(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                               RenderType through, float yaw, Flash flash, float partialTick,
                               int light) {
        draw(poseStack, buffers, model, through, yaw, SKULL_SCALE, light);
    }

    /**
     * The punch: the skull caves in where it stands.
     *
     * <p>One skull, pressed flat and gone. It loses height and gains width at the same time,
     * because the inside of a thing being crushed has to go somewhere, and it fades out over the
     * back half so that it hands over to the burst rather than simply vanishing on a frame.
     *
     * <p>The pieces are particles now and are thrown by the server - see SkullCrusherSkill.burst.
     * That is the right split: the model is a solid object losing its shape, which particles cannot
     * do, and the debris is a hundred small things going in a hundred directions, which a model
     * cannot do without being drawn a hundred times.
     */
    private static void shatter(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                                RenderType through, float yaw, Flash flash, float partialTick,
                                int light) {
        float progress = 1F - flash.share(partialTick);
        if (progress >= POP_OUT) {
            // Gone. The burst is what is on screen by now.
            return;
        }

        // Worked in ticks, not in shares of the beat - see POP_TICKS. The skull sits at full size
        // until the last three ticks it has, then goes.
        float elapsed = progress * flash.ticks;
        float popAt = POP_OUT * flash.ticks - POP_TICKS;

        float pop;
        if (elapsed < popAt) {
            pop = 1F;

            // Building toward the pop - see RATTLE_MAX. Squared, so nearly all of it happens in the
            // last moments rather than the skull trembling gently for half a second.
            if (popAt > 0F) {
                float build = elapsed / popAt;
                float shake = RATTLE_MAX * build * build;
                poseStack.translate(
                        Mth.sin(elapsed * RATTLE_FREQ) * shake,
                        Mth.sin(elapsed * RATTLE_FREQ * 1.37F) * shake * 0.55F,
                        Mth.cos(elapsed * RATTLE_FREQ * 1.11F) * shake);
            }
        } else {
            float u = Math.min(1F, (elapsed - popAt) / POP_TICKS);
            pop = u < POP_SWELL_AT
                    ? Mth.lerp(u / POP_SWELL_AT, 1F, POP_SWELL)
                    : Mth.lerp((u - POP_SWELL_AT) / (1F - POP_SWELL_AT), POP_SWELL, 0F);
        }

        if (pop <= 0.02F) {
            return;
        }

        // One number on all three axes. See POP_SWELL.
        draw(poseStack, buffers, model, through, yaw, SKULL_SCALE * pop, light);
    }

    /**
     * Places one skull, the way the game places a skull block.
     *
     * <p>{@code renderSkull} wants a pose sitting at a block's corner, the right way up, in world
     * orientation - that is exactly how {@code SkullBlockRenderer} calls it, with no setup of its own
     * beyond being at the block. Given that, it fills the bottom half of the block, centred. So the
     * only thing needed here is to scale, and to take back the half block it adds internally to
     * centre itself.
     *
     * <p>This replaces a hand-rolled transform borrowed from {@code CustomHeadLayer}, which was the
     * mistake underneath all of it. That transform is written for <em>model</em> space, where Y runs
     * downward because the entity renderer has already flipped it - and it was being applied here, in
     * world space, where Y runs up. The result was an upside-down skull half a block from where it
     * belonged, and no amount of adjusting the numbers was going to fix a sign error in the space
     * they were being applied to.
     */
    private static void draw(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                             RenderType through, float yaw, float scale, int light) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        // renderSkull moves half a block along both horizontal axes to centre itself in its block;
        // this is that, taken back, so the skull ends up centred on the point it was handed.
        poseStack.translate(-0.5F, 0F, -0.5F);

        // The victim's own light is handed in and deliberately not used - see the note on lighting.
        SkullBlockRenderer.renderSkull(null, yaw, 0F, poseStack, buffers,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, model, through);
        poseStack.popPose();
    }

    /** One victim's current phase. */
    private static final class Flash {
        private final int victimId;
        private final int attackerId;
        private final int phase;
        private final int ticks;
        private int remaining;

        private Flash(int victimId, int attackerId, int phase, int ticks) {
            this.victimId = victimId;
            this.attackerId = attackerId;
            this.phase = phase;
            this.ticks = Math.max(1, ticks);
            this.remaining = this.ticks;
        }

        /** How much of this phase is left, from one down to zero. */
        private float share() {
            return remaining / (float) ticks;
        }

        private float share(float partialTick) {
            return Mth.clamp((remaining - partialTick) / ticks, 0F, 1F);
        }
    }
    /**
     * The rest of the skeleton, wearing whatever pose the mob it is inside is already in.
     *
     * <p>The first version of this posed a skeleton from scratch - limbs at rest, head yaw taken off
     * the entity - and it was wrong in a way that only shows up on a mob doing something. A zombie
     * holds its arms straight out in front of it. The skeleton inside was standing to attention, so
     * the arms you could see through the flesh were not the arms the flesh was in.
     *
     * <p>Re-deriving the pose was never going to work, because the pose is not a function of the
     * entity - it is a function of the entity plus whatever that particular model does with it, and
     * every mob does something different. So it is copied instead. The live model has just been
     * through {@code setupAnim} for this exact entity on this exact frame, and every joint is
     * already sitting where the mob is holding it; {@code ModelPart.copyFrom} lifts the whole
     * transform across in one call per limb.
     *
     * <h2>Where this is called from, and why it moved</h2>
     *
     * <p>From inside the wrap on {@code renderToBuffer} rather than at the head of {@code render}.
     * That change is what makes the copy possible at all: at the head of the method vanilla has not
     * called {@code setupAnim} yet, so the model still holds the pose of whichever entity was drawn
     * before this one - a shared instance carrying stale limbs. By the time the body is about to be
     * drawn it holds the right ones.
     *
     * <p>It also disposes of the transform this used to do by hand. The pose stack at that point has
     * already been turned, flipped and dropped by vanilla, which is the space the model expects, so
     * the skeleton goes in with no transform of its own and cannot drift out of alignment with the
     * body around it.
     *
     * <p>Still drawn before the body, which is what keeps it visible: a translucent surface writes
     * depth, so anything solid behind it has to be in the buffer first.
     *
     * <h2>Cutout, not solid</h2>
     *
     * <p>The skull gets away with a solid render type because every texel it touches is opaque. A
     * humanoid body does not: the skeleton texture leaves the outer column of each limb clear, and a
     * solid type has no alpha test, so every one of those clear texels came out as a black quad -
     * black slabs down the arms and legs. Cutout discards them, which is what the vanilla skeleton
     * renderer uses and for the same reason.
     */
    public static void renderSkeleton(HumanoidModel<?> source, PoseStack poseStack,
                                      MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (bones == null) {
            bones = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.SKELETON));

            // The skull is drawn separately, from the eye height, where it actually sits inside the
            // head. The model has a head of its own in roughly the same place, and drawing both puts
            // two overlapping skulls in one skull-sized space, which z-fights.
            bones.head.visible = false;
            bones.hat.visible = false;
        }

        bones.body.copyFrom(source.body);
        bones.rightArm.copyFrom(source.rightArm);
        bones.leftArm.copyFrom(source.leftArm);
        bones.rightLeg.copyFrom(source.rightLeg);
        bones.leftLeg.copyFrom(source.leftLeg);

        // Not a transform, so it does not come across with the rest: it is a flag the parent class
        // reads inside renderToBuffer to decide whether to draw the whole thing baby-sized.
        bones.young = source.young;

        bones.renderToBuffer(poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(SKULL_TEXTURE)),
                light, overlay, 0xFFFFFFFF);
    }

}
