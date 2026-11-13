package org.gumel.jojoha.stand;

import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.skill.moves.KnifeBarrageSkill;
import org.gumel.jojoha.stand.workshop.RestPose;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import java.util.List;

final class StandAnimations {
    private final StandEntity stand;

    StandAnimations(StandEntity stand) {
        this.stand = stand;
    }

    private static final RawAnimation AWAKENING = RawAnimation.begin().thenLoop("awakening");

    private static final RawAnimation IDLE_PARTIAL = RawAnimation.begin().thenLoop("idle_partial");

    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("spawn");

    private static final RawAnimation JAB = RawAnimation.begin().thenPlay("punch");

    private static final RawAnimation JAB2 = RawAnimation.begin().thenPlay("punch2");

    private static final RawAnimation STAR_FINGER = RawAnimation.begin().thenPlay("star_finger");

    private static final RawAnimation INHALE = RawAnimation.begin().thenPlayAndHold("inhale");

    private static final RawAnimation TIMESTOP_1 = RawAnimation.begin().thenPlay("timestop_1");

    private static final RawAnimation TIMESTOP_2 = RawAnimation.begin().thenPlay("timestop_2");

    private static final RawAnimation CHARGE_LOOP = RawAnimation.begin().thenLoop("charge_loop");

    private static final RawAnimation GUARDBROKEN = RawAnimation.begin().thenPlay("guardbroken");

    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("idle2");

    private static final RawAnimation COMBAT_IDLE = RawAnimation.begin().thenLoop("combat");

    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("falling");

    private static final RawAnimation STAND_JUMP = RawAnimation.begin().thenPlay("stand_jump");

    private static final RawAnimation CATCH = RawAnimation.begin().thenPlay("catch");

    private static final RawAnimation GRAB = RawAnimation.begin().thenPlayAndHold("grab");

    private static final RawAnimation GRAB2 = RawAnimation.begin().thenPlayAndHold("grab2");

    private static final RawAnimation GRAB_IDLE = RawAnimation.begin().thenLoop("grab_idle");

    private static final RawAnimation GRAB2_IDLE = RawAnimation.begin().thenLoop("grab2_idle");

    private static final RawAnimation FETCH_IDLE = RawAnimation.begin().thenLoop("fetch");

    private static final RawAnimation TW_SPAWN = RawAnimation.begin().thenPlay("tw_spawn");

    private static final RawAnimation GROUND_BREAKER = RawAnimation.begin().thenPlay("ground_breaker");

    private static final RawAnimation BEATDOWN = RawAnimation.begin().thenLoop("beatdown");

    private static final RawAnimation KNIFE_THROW = RawAnimation.begin().thenPlay("knife_throw");

    private static final RawAnimation THROW = RawAnimation.begin().then("throw", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation KNIFE_THROW_HELD =
            RawAnimation.begin().thenPlay("knife_throw_held");

    private static final RawAnimation KICK_BARRAGE = RawAnimation.begin().thenLoop("kick_barrage");

    private static final RawAnimation CHARIOT_IDLE = RawAnimation.begin().thenLoop("idle_pose");

    private static final RawAnimation CHARIOT_GUARD =
            RawAnimation.begin().then("guard_start", Animation.LoopType.PLAY_ONCE).thenLoop("guard_loop");

    private static final RawAnimation CHARIOT_GUARD_BREAK = RawAnimation.begin().then("guard_break", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation SWORD_FLURRY =
            RawAnimation.begin().then("sword_flurry_start", Animation.LoopType.PLAY_ONCE).thenLoop("sword_flurry_loop");

    private static final RawAnimation ARMOR_RELEASE = RawAnimation.begin().then("armor_release", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation AFTERIMAGE = RawAnimation.begin().then("afterimage", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation BLADE_LAUNCH = RawAnimation.begin().then("blade_launch", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation CHARIOT_HIT = RawAnimation.begin().then("hit", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation CHARIOT_HIT_FOLLOWUP = RawAnimation.begin().then("hit_followup", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation CHARIOT_HIT_FOLLOWUP2 = RawAnimation.begin().then("hit_followup2", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation SABRE_CYCLE = RawAnimation.begin().then("sabre_cycle", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation DEFLECTION = RawAnimation.begin().then("deflection", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation BALESTRA = RawAnimation.begin().then("balestra", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation SABRE_CHARGE_HELD = RawAnimation.begin().thenPlayAndHold("sabre_cycle_charge");

    private static final RawAnimation DEFLECTION_HELD = RawAnimation.begin().thenPlayAndHold("deflection");

    private static final RawAnimation RIPOSTE_HELD = RawAnimation.begin().thenPlayAndHold("guard_start");

    private static final RawAnimation MR_CONTROLLED_BLAZE = RawAnimation.begin().then("controlled_blaze", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_SWEEP_KICK = RawAnimation.begin().then("sweep_kick", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_CROSS_FIRE_HURRICANE = RawAnimation.begin().then("cross_fire_hurricane", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_CROSS_FIRE_HURRICANE_SPECIAL = RawAnimation.begin().then("cross_fire_hurricane_special", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_INFERNO = RawAnimation.begin().then("inferno", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_LIFE_SENSE = RawAnimation.begin().then("life_sense", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation MR_INFINITE_INFERNO_FIRE = RawAnimation.begin().then("infinite_inferno_fire", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation HG_SHOTGUN = RawAnimation.begin().then("hg_shotgun_remake", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation HG_SPIKE = RawAnimation.begin().then("hg_spike_alt", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation HG_MINE = RawAnimation.begin().then("hg_mine_placement_remake", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation HG_RADIUS = RawAnimation.begin().then("hg_chudsplash", Animation.LoopType.PLAY_ONCE);

    private static final RawAnimation HG_POSSESS = RawAnimation.begin().then("hg_possession_remake", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation HG_POSSESS_ALT = RawAnimation.begin().then("hg_possession_alt", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation HG_POSSESS_EXIT = RawAnimation.begin().then("hg_possession_exit_remake", Animation.LoopType.PLAY_ONCE);

    private static final int CHARIOT_HIT_HOLD_TICKS = 12;

    private int chariotHeldSince = -1;

    private static final int RESTING_BLEND_TICKS = 4;

    private static final int LIGHT_BLEND_TICKS = 3;

    private static final int HEAVY_BLEND_TICKS = 9;

    private static final int MOOD_BLEND_TICKS = 10;

    private static final int HEAVY_HOLD_TICKS = 5;

    private static final List<RawAnimation> HEAVY_MOVES = List.of(StandEntity.UPPERCUT, StandEntity.KICK, GROUND_BREAKER, BALESTRA,
            SABRE_CYCLE, BLADE_LAUNCH, MR_CROSS_FIRE_HURRICANE, MR_CROSS_FIRE_HURRICANE_SPECIAL, MR_INFERNO,
            GUARDBROKEN);

    private static final List<RawAnimation> LIGHT_MOVES = List.of(StandEntity.PUNCH, StandEntity.PUNCH2, JAB, JAB2, CHARIOT_HIT,
            CHARIOT_HIT_FOLLOWUP, CHARIOT_HIT_FOLLOWUP2);

    private RawAnimation lastMove;

    private int restingSince = -1;

    private int heavyHeldSince = -1;

    private int restingBlend() {
        if (lastMove == null) {
            return MOOD_BLEND_TICKS;
        }
        if (HEAVY_MOVES.contains(lastMove)) {
            return HEAVY_BLEND_TICKS;
        }
        return LIGHT_MOVES.contains(lastMove) ? LIGHT_BLEND_TICKS : RESTING_BLEND_TICKS;
    }

    private static boolean isKnifeVolley(RawAnimation playing) {
        for (RawAnimation volley : StandEntity.KNIFE_VOLLEYS) {
            if (playing == volley) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAttackAnimation(RawAnimation playing) {
        return playing == StandEntity.PUNCH || playing == StandEntity.PUNCH2 || playing == StandEntity.UPPERCUT || playing == StandEntity.KICK
                || playing == GRAB || playing == GRAB2;
    }

    private static boolean isJab(RawAnimation playing) {
        return playing == JAB || playing == JAB2;
    }

    private static boolean isChariotHit(RawAnimation playing) {
        return playing == CHARIOT_HIT || playing == CHARIOT_HIT_FOLLOWUP || playing == CHARIOT_HIT_FOLLOWUP2;
    }

    void register(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<StandEntity> animator =
                new AnimationController<>(stand, StandEntity.CONTROLLER, 0, state -> {
                    AnimationController<StandEntity> controller = state.getController();

                    if (stand.isManifesting()) {
                        if (!StandManifestation.posed(stand.tickCount + state.getPartialTick())) {
                            return state.setAndContinue(AWAKENING);
                        }
                        controller.transitionLength(StandManifestation.POSE_SNAP_TICKS);
                        return state.setAndContinue(stand.isChariot() || stand.isMagician() ? CHARIOT_IDLE : StandEntity.IDLE);
                    }

                    controller.transitionLength(
                            controller.getTriggeredAnimation() == null ? restingBlend() : 0);

                    if (controller.getTriggeredAnimation() != null) {
                        lastMove = controller.getTriggeredAnimation();
                        restingSince = -1;
                        boolean paused = controller.getAnimationState() == AnimationController.State.PAUSED;
                        if (paused && isChariotHit(controller.getTriggeredAnimation())
                                && stand.synced(StandEntity.DATA_HOLD_POSE) == 0) {
                            if (chariotHeldSince < 0) {
                                chariotHeldSince = stand.tickCount;
                            }
                            return stand.tickCount - chariotHeldSince > CHARIOT_HIT_HOLD_TICKS
                                    ? PlayState.STOP
                                    : PlayState.CONTINUE;
                        }
                        chariotHeldSince = -1;
                        if (paused && HEAVY_MOVES.contains(controller.getTriggeredAnimation())) {
                            if (heavyHeldSince < 0) {
                                heavyHeldSince = stand.tickCount;
                            }
                            return stand.tickCount - heavyHeldSince > HEAVY_HOLD_TICKS
                                    ? PlayState.STOP
                                    : PlayState.CONTINUE;
                        }
                        heavyHeldSince = -1;
                        return paused ? PlayState.STOP : PlayState.CONTINUE;
                    }
                    chariotHeldSince = -1;
                    heavyHeldSince = -1;
                    if (restingSince < 0) {
                        restingSince = stand.tickCount;
                    } else if (stand.tickCount - restingSince > HEAVY_BLEND_TICKS) {
                        lastMove = null;
                    }
                    if (stand.getTrustTier().isPartialManifestation()) {
                        return state.setAndContinue(IDLE_PARTIAL);
                    }
                    byte hold = stand.synced(StandEntity.DATA_HOLD_POSE);
                    if (hold == StandEntity.HOLD_GRAB2) {
                        return state.setAndContinue(GRAB2_IDLE);
                    }
                    if (hold == StandEntity.HOLD_GRAB) {
                        return state.setAndContinue(GRAB_IDLE);
                    }
                    if (hold == StandEntity.HOLD_FETCH) {
                        return state.setAndContinue(FETCH_IDLE);
                    }
                    if (hold == StandEntity.HOLD_SABRE) {
                        return state.setAndContinue(SABRE_CHARGE_HELD);
                    }
                    if (hold == StandEntity.HOLD_DEFLECT) {
                        return state.setAndContinue(DEFLECTION_HELD);
                    }
                    if (hold == StandEntity.HOLD_RIPOSTE) {
                        return state.setAndContinue(RIPOSTE_HELD);
                    }
                    if (stand.isChariot() || stand.isMagician()) {
                        return state.setAndContinue(CHARIOT_IDLE);
                    }
                    if (stand.inCombatFrame()) {
                        return state.setAndContinue(COMBAT_IDLE);
                    }
                    if (stand.synced(StandEntity.DATA_FALLING)) {
                        return state.setAndContinue(FALLING);
                    }
                    return state.setAndContinue(stand.restPose() == RestPose.READY ? IDLE2 : StandEntity.IDLE);
                }).receiveTriggeredAnimations()
                .triggerableAnim("spawn", SPAWN)
                .triggerableAnim("punch", StandEntity.PUNCH)
                .triggerableAnim("punch2", StandEntity.PUNCH2)
                .triggerableAnim("jab", JAB)
                .triggerableAnim("jab2", JAB2)
                .triggerableAnim("barrage", StandEntity.BARRAGE)
                .triggerableAnim("star_finger", STAR_FINGER)
                .triggerableAnim("inhale", INHALE)
                .triggerableAnim("timestop_1", TIMESTOP_1)
                .triggerableAnim("timestop_2", TIMESTOP_2)
                .triggerableAnim("grab", GRAB)
                .triggerableAnim("grab2", GRAB2)
                .triggerableAnim("uppercut", StandEntity.UPPERCUT)
                .triggerableAnim("kick", StandEntity.KICK)
                .triggerableAnim("block", StandEntity.BLOCK)
                .triggerableAnim("guardbroken", GUARDBROKEN)
                .triggerableAnim("tw_spawn", TW_SPAWN)
                .triggerableAnim("tw_timestop", StandEntity.TW_TIMESTOP)
                .triggerableAnim("charge_loop", CHARGE_LOOP)
                .triggerableAnim("ground_breaker", GROUND_BREAKER)
                .triggerableAnim("beatdown", BEATDOWN)
                .triggerableAnim("knife_throw", KNIFE_THROW)
                .triggerableAnim("throw", THROW)
                .triggerableAnim("knife_throw_held", KNIFE_THROW_HELD)
                .triggerableAnim("chariot_guard", CHARIOT_GUARD)
                .triggerableAnim("chariot_guard_break", CHARIOT_GUARD_BREAK)
                .triggerableAnim("sword_flurry", SWORD_FLURRY)
                .triggerableAnim("armor_release", ARMOR_RELEASE)
                .triggerableAnim("afterimage", AFTERIMAGE)
                .triggerableAnim("blade_launch", BLADE_LAUNCH)
                .triggerableAnim("hit", CHARIOT_HIT)
                .triggerableAnim("hit_followup", CHARIOT_HIT_FOLLOWUP)
                .triggerableAnim("hit_followup2", CHARIOT_HIT_FOLLOWUP2)
                .triggerableAnim("sabre_cycle", SABRE_CYCLE)
                .triggerableAnim("deflection", DEFLECTION)
                .triggerableAnim("balestra", BALESTRA)
                .triggerableAnim("controlled_blaze", MR_CONTROLLED_BLAZE)
                .triggerableAnim("sweep_kick", MR_SWEEP_KICK)
                .triggerableAnim("cross_fire_hurricane", MR_CROSS_FIRE_HURRICANE)
                .triggerableAnim("cross_fire_hurricane_special", MR_CROSS_FIRE_HURRICANE_SPECIAL)
                .triggerableAnim("red_bind", StandEntity.MR_RED_BIND)
                .triggerableAnim("inferno", MR_INFERNO)
                .triggerableAnim("life_sense", MR_LIFE_SENSE)
                .triggerableAnim("infinite_inferno_fire", MR_INFINITE_INFERNO_FIRE)
                .triggerableAnim(HierophantClips.SHOTGUN, HG_SHOTGUN)
                .triggerableAnim(HierophantClips.SPIKE, HG_SPIKE)
                .triggerableAnim(HierophantClips.MINE, HG_MINE)
                .triggerableAnim(HierophantClips.RADIUS, HG_RADIUS)
                .triggerableAnim(HierophantClips.POSSESS, HG_POSSESS)
                .triggerableAnim(HierophantClips.POSSESS_ALT, HG_POSSESS_ALT)
                .triggerableAnim(HierophantClips.POSSESS_EXIT, HG_POSSESS_EXIT)
                .triggerableAnim("kick_barrage", KICK_BARRAGE)
                .triggerableAnim("stand_jump", STAND_JUMP)
                .triggerableAnim("catch", CATCH);
        for (int count = 1; count <= StandEntity.KNIFE_VOLLEYS.length; count++) {
            animator.triggerableAnim("knife_volley_" + count, StandEntity.KNIFE_VOLLEYS[count - 1]);
        }

        animator.setAnimationSpeedHandler(
                animated -> (isJab(animator.getTriggeredAnimation()) ? StandEntity.JAB_ANIM_SPEED
                        : isAttackAnimation(animator.getTriggeredAnimation()) ? StandEntity.ATTACK_ANIM_SPEED
                        : isChariotHit(animator.getTriggeredAnimation()) ? StandEntity.CHARIOT_HIT_ANIM_SPEED
                        : isKnifeVolley(animator.getTriggeredAnimation())
                        ? KnifeBarrageSkill.VOLLEY_ANIM_SPEED : 1.0) * (stand.hastened() ? StandEntity.LINK_HASTE : 1.0));

        controllers.add(animator);
    }

    public void holdAnimation(int ticks) {
        if (ticks <= 0) {
            return;
        }
        if (stand.tickCount >= stand.animationHeldUntil) {
            stand.animationHeldAt = stand.tickCount - stand.animationHeldTotal + StandEntity.renderPartialTick.getAsDouble();
            stand.animationHeldUntil = stand.tickCount;
        }
        stand.animationHeldUntil += ticks;
        stand.animationHeldTotal += ticks;
    }
}
