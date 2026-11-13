package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.combat.BarrageBank;
import org.gumel.jojoha.combat.BarrageBlow;
import org.gumel.jojoha.combat.ComboDecay;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Exposed;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.BarrageDemolition;
import org.gumel.jojoha.stand.combat.BarrageGenerator;
import org.gumel.jojoha.stand.combat.BarragePattern;
import org.gumel.jojoha.stand.combat.PunchType;
import org.gumel.jojoha.stand.combat.StandCombatProfile;
import org.gumel.jojoha.stand.combat.StandCombatProfiles;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.combat.StandFists;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.BarrageSkill;
import org.gumel.jojoha.stand.skill.moves.KickFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.KnifeBarrageSkill;
import org.gumel.jojoha.stand.skill.moves.SwordFlurrySkill;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.RawAnimation;
import java.util.ArrayList;
import java.util.List;

final class StandBarrage {
    private final StandEntity stand;

    StandBarrage(StandEntity stand) {
        this.stand = stand;
    }

    static RawAnimation[] knifeVolleys() {
        RawAnimation[] volleys = new RawAnimation[KnifeBarrageSkill.KNIVES];
        for (int count = 1; count <= volleys.length; count++) {
            RawAnimation chain = RawAnimation.begin();
            for (int knife = 0; knife < count; knife++) {
                chain = chain.then("knives_startup_variant", Animation.LoopType.PLAY_ONCE)
                        .then("knivesthrow_variant", Animation.LoopType.PLAY_ONCE);
            }
            volleys[count - 1] = chain;
        }
        return volleys;
    }

    private boolean flurryShown;

    public static final double COMMANDED_BARRAGE_REACH = 3.5;

    public static double barrageReach() {
        return COMMANDED_BARRAGE_REACH + StandEntity.BARRAGE_PULL_IN;
    }

    private static final double PILOTED_BARRAGE_REACH = StandAim.DEFAULT_REACH;

    private static final double BARRAGE_LANE_HALF_WIDTH = 0.9;

    private int pendingExtraFists;

    private float pendingExtraBlow;

    private static final float PRESS_IN_PER_TICK = 0.02F;

    private static final float PRESS_OUT_PER_TICK = 0.12F;

    private static final int HELD_BARRAGE_MAX_TICKS = 180;

    public static final float BARRAGE_VICTIM_LEAST = 18.0F;

    public static final float BARRAGE_VICTIM_MOST = 24.0F;

    public static final float BARRAGE_PERK_ROOM = 1.2F;

    public static final int BARRAGE_IDLE_TICKS = 20;

    public static final float BARRAGE_LEAST_COOLDOWN = 0.4F;

    public static float barrageVictimCap(int power) {
        return BARRAGE_VICTIM_LEAST + (BARRAGE_VICTIM_MOST - BARRAGE_VICTIM_LEAST) * StatEffects.climb(power);
    }

    private float barrageVictimCap() {
        return (stand.getOwner() instanceof ServerPlayer owner
                ? barrageVictimCap(PlayerDataAccess.get(owner).stand.power()) : BARRAGE_VICTIM_LEAST)
                * barrageTune(stand.barrageStyle);
    }

    private static float barrageTune(StandEntity.BarrageStyle style) {
        return StandTuning.damage(style.tuningKey, style.damage) / style.damage;
    }

    private float budgetBlow(Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return 0F;
        }
        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        return barrageVictimCap(data.stand.power()) * barrageTune(stand.barrageStyle)
                / Math.max(1.0E-3F, data.stand.powerScale());
    }

    private int rushBeats() {
        return stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES && !isHeldBarrageRunning()
                ? stand.barrageStyle.hits - 1 : stand.barrageStyle.hits;
    }

    private float beatBlow(Player owner) {
        float rush = stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES ? 1F - BarrageGenerator.finisherShare(combatProfile()) : 1F;
        return budgetBlow(owner) * rush / Math.max(1, rushBeats());
    }

    private float closingFinisher(Player owner, float done) {
        return budgetBlow(owner) * BarrageGenerator.FINISHER_SHARE / BarrageGenerator.FINISHER_SCALE
                * Mth.clamp(done, 0F, 1F);
    }

    private float underBarrageCap(LivingEntity victim, float dealt) {
        float cap = barrageVictimCap() * BARRAGE_PERK_ROOM;
        float so = stand.barrageDealt.getOrDefault(victim.getId(), 0F);
        float allowed = Math.max(0F, Math.min(dealt, cap - so));
        stand.barrageDealt.put(victim.getId(), so + allowed);
        if (so + allowed >= cap) {
            barrageCapped = true;
        }
        return allowed;
    }

    private boolean barrageCapped;

    void tickBarrageCap() {
        if (!barrageCapped || stand.level().isClientSide()) {
            return;
        }
        barrageCapped = false;
        StandEntity.BarrageStyle style = stand.barrageStyle;
        if (isHeldBarrageRunning()) {
            endHeldBarrage();
        } else if (stand.barrageHitsRemaining > 0) {
            stand.barrageHitsRemaining = 0;
            stand.barrageHitInterval = 0;
            pendingExtraFists = 0;
            stopFlurryAnimation();
            coolBarrage(style);
        }
    }

    void stopFlurryAnimation() {
        if (flurryShown) {
            flurryShown = false;
            stand.stopTriggeredAnim(StandEntity.CONTROLLER, isKickBarrage() ? "kick_barrage" : "sword_flurry");
            stand.sync(StandEntity.DATA_KICKING, false);
        }
        if (stand.barrageStyle != StandEntity.BarrageStyle.SWORD) {
            stand.stopTriggeredAnim(StandEntity.CONTROLLER, "barrage");
        }
    }

    private int flurryInterval() {
        return stand.barrageStyle == StandEntity.BarrageStyle.SWORD && stand.armorOff()
                ? Math.max(1, stand.barrageStyle.interval - Chariot.UNARMORED_FLURRY_QUICKER)
                : stand.barrageStyle.interval;
    }

    private void coolBarrage(StandEntity.BarrageStyle style) {
        if (stand.level().isClientSide()) {
            return;
        }
        if (stand.getOwner() instanceof ServerPlayer owner) {
            List<StandSkill> skills = style == StandEntity.BarrageStyle.KICKS
                    ? List.of(KickFlurrySkill.INSTANCE)
                    : style == StandEntity.BarrageStyle.SWORD
                    ? List.of(SwordFlurrySkill.INSTANCE,
                            BarrageSkill.INSTANCE)
                    : List.of(BarrageSkill.INSTANCE);
            JojohaPlayerData data = PlayerDataAccess.get(owner);
            float landed = 0F;
            for (float dealt : stand.barrageDealt.values()) {
                landed = Math.max(landed, dealt);
            }
            float rush = barrageVictimCap(data.stand.power()) * barrageTune(style)
                    * (style == StandEntity.BarrageStyle.PUNCHES ? 1F - BarrageGenerator.finisherShare(combatProfile()) : 1F);
            float paid = Mth.clamp(landed / Math.max(1.0E-3F, rush), BARRAGE_LEAST_COOLDOWN, 1F);
            if (landed <= 0F && !data.standMode.handlesItems()) {
                Exposed.open(owner);
            }
            for (StandSkill skill : skills) {
                data.setMoveCooldown(skill.id(), owner.level().getGameTime(),
                        Math.round(StandSkills.scaledCooldown(skill, data) * paid));
            }
            PlayerDataAccess.set(owner, data);
            PlayerDataAccess.sync(owner);
        }
    }

    private int heldBarrageTicks = 0;

    Vec3 nextBarrageSway(Player owner, StandMovementProfile profile) {
        boolean barraging = stand.synced(StandEntity.DATA_BARRAGING)
                && stand.synced(StandEntity.DATA_FRONT_STANCE);

        stand.swayStrength = barraging
                ? Math.min(1F, stand.swayStrength + PRESS_IN_PER_TICK)
                : Math.max(0F, stand.swayStrength - PRESS_OUT_PER_TICK);

        if (stand.swayStrength <= 0F) {
            return Vec3.ZERO;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

        return forward.scale(profile.barragePush() * stand.swayStrength);
    }

    private void dealBarrageHit(Player owner, float damage) {
        LivingEntity target = stand.pursuitTarget;
        if (target == null || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float scale = Math.max(1.0E-3F, data.stand.powerScale());
        float allowed = underBarrageCap(target, StandPassives.scaleOutgoing(
                serverOwner, data, target, damage * scale));
        if (allowed > 0F) {
            BarrageBlow.within(owner, () -> ComboDecay.uncounted(() -> stand.dealHit(owner, allowed / scale, true)));
        }
    }

    void tickBarrage(Player owner) {
        stand.fists.tick(stand, owner, (victim, contact, heading, blow, finisher) -> landFist(owner, victim, contact, heading, blow, finisher));
        tickClosing(owner);

        if (stand.barrageHitsRemaining <= 0 && flurryShown && !isHeldBarrageRunning() && stand.pendingStrike == null) {
            stopFlurryAnimation();
        }
        if (stand.barrageHitsRemaining <= 0) {
            pendingExtraFists = 0;
            return;
        }
        if (windingUp()) {
            pendingExtraFists = 0;
        }
        for (int i = 0; i < BarrageGenerator.EXTRAS_PER_TICK
                && pendingExtraFists > 0 && stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES; i++) {
            pendingExtraFists--;
            throwShot(owner, pendingExtraBlow, true, false);
        }

        if (--stand.barrageHitInterval <= 0) {
            stand.barrageHitInterval = flurryInterval() * (windingUp() ? BarrageGenerator.WINDUP_PACE : 1);
            if (!BarrageStruggle.in(owner)) {
                stand.barrageHitsRemaining--;
            }
            float blow = beatBlow(owner);
            if (stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES) {
                blow *= BarrageGenerator.thudWeight(stand.barrageStyle.hits - stand.barrageHitsRemaining - 1, rushBeats());
            }
            breakThrough(owner);
            if (stand.barrageStyle == StandEntity.BarrageStyle.SWORD) {
                Chariot.thrust(stand, owner);
            }
            if (stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES) {
                throwBeat(owner, blow);
            } else if (stand.pursuitTarget != null) {
                stand.pursuitTarget.invulnerableTime = 0;
                dealBarrageHit(owner, blow);
                styleShove(stand.pursuitTarget);
            } else {
                sweepBarrage(owner, blow);
            }

            if (stand.barrageHitsRemaining <= 0
                    && !(stand.barrageStyle != StandEntity.BarrageStyle.PUNCHES && isHeldBarrageRunning())) {
                stopFlurryAnimation();
            }
        }
    }

    boolean windingUp() {
        return stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES
                && (stand.level().getGameTime() - stand.fists.lastBeat() > BarrageGenerator.NEW_BARRAGE_GAP
                || stand.fists.beats() < BarrageGenerator.WINDUP_BEATS);
    }

    private void breakThrough(Player owner) {
        if (!(stand.level() instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }
        if (!PlayerDataAccess.get(serverOwner).standMode.handlesItems()) {
            return;
        }
        Vec3 chest = stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        int broken = BarrageDemolition.strike(serverLevel, serverOwner, chest,
                owner.getLookAngle(), barrageReach(), BARRAGE_LANE_HALF_WIDTH);
        if (broken > 0) {
            spendBarrageEnergy(serverOwner, PlayerDataAccess.get(serverOwner));
        }
    }

    private void styleShove(LivingEntity victim) {
        if (victim == null || (stand.barrageStyle.push <= 0.0 && stand.barrageStyle.lift <= 0.0)) {
            return;
        }
        Vec3 away = victim.position().subtract(stand.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(stand.barrageStyle.push);
        Vec3 motion = victim.getDeltaMovement();
        victim.setDeltaMovement(motion.x + push.x, Math.max(motion.y, stand.barrageStyle.lift),
                motion.z + push.z);
        victim.hurtMarked = true;
    }

    void impactRings(ServerLevel level, LivingEntity target, float damage, int count) {
        impactRings(level, target, null, damage, count);
    }

    void impactRings(ServerLevel level, LivingEntity target, Vec3 at, float damage, int count) {
        Vec3 heading = target.position().subtract(stand.position());

        ImpactSpec spec = count > 1
                ? ImpactSpec.FLURRY
                : ImpactSpec.MEDIUM
                        .scaledBy(Mth.clamp(damage / HEAVY_BLOW_DAMAGE, 0.35F, 1F));

        Impacts.landed(level, target, at, heading, spec,
                StandShadowForm.auraColourFor(stand));
    }

    private static final float HEAVY_BLOW_DAMAGE = 8F;

    private void throwBeat(Player owner, float blow) {
        boolean counted = !isHeldBarrageRunning();
        if (counted && stand.barrageHitsRemaining <= 0) {
            throwFinisher(owner, closingFinisher(owner, 1F));
            return;
        }
        boolean rushing = counted && stand.barrageHitsRemaining <= BarrageGenerator.RUSH_BEATS;
        int extras = phantomsFollow() ? BarrageGenerator.extras(stand.fists.momentum(), rushing,
                combatProfile()) : 0;
        float share = 1F + extras * BarrageGenerator.EXTRA_DAMAGE_SHARE;
        blow /= share;
        throwShot(owner, blow, beatBlow(owner) / share, false, false);
        pendingExtraFists = extras;
        pendingExtraBlow = blow * BarrageGenerator.EXTRA_DAMAGE_SHARE;
    }

    private boolean phantomsFollow() {
        int beats = stand.level().getGameTime() - stand.fists.lastBeat() > BarrageGenerator.NEW_BARRAGE_GAP
                ? 1 : stand.fists.beats() + 1;
        return beats >= BarrageGenerator.WINDUP_BEATS;
    }

    private void throwFinisher(Player owner, float blow) {
        pendingExtraFists = 0;
        throwShot(owner, BarrageGenerator.finisherBlow(blow, combatProfile()), false, true);
    }

    private void throwShot(Player owner, float blow, boolean extra, boolean finisher) {
        throwShot(owner, blow, blow, extra, finisher);
    }

    private void throwShot(Player owner, float blow, float heft, boolean extra, boolean finisher) {
        if (!(stand.level() instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }
        List<LivingEntity> bodies = flurryBodies(owner);
        LivingEntity pursued = stand.pursuitTarget != null && stand.pursuitTarget.isAlive() ? stand.pursuitTarget : null;
        if (pursued != null && !bodies.contains(pursued)) {
            bodies.add(pursued);
        }
        LivingEntity target = bodies.isEmpty() ? null
                : pursued != null && bodies.size() == 1 ? pursued
                : bodies.get(stand.getRandom().nextInt(bodies.size()));
        double speed = target == null ? 0.0 : target.getDeltaMovement().horizontalDistance();
        BarragePattern pattern =
                BarrageGenerator.pattern(bodies.size(), speed, stand.fists.momentum());
        PunchType type = finisher
                ? PunchType.STRAIGHT : pattern.pick(stand.getRandom(), combatProfile());

        Vec3 chest = stand.position().add(0, stand.getBbHeight() * 0.6, 0);
        double strength = heft * PlayerDataAccess.get(serverOwner).stand.powerScale() * combatProfile().clashStrength();
        stand.fists.throwFist(stand, new StandFists.Shot(target, type, pattern, chest,
                owner.getLookAngle(), barrageReach(), blow, strength, extra, finisher)).ifPresent(fist ->
                NetworkHandler.broadcastStandFist(serverLevel, stand, fist));
    }

    public StandCombatProfile combatProfile() {
        return StandCombatProfiles.of(stand.getStandType().id());
    }

    public StandFists fists() {
        return stand.fists;
    }

    public void fistKnockedBack(StandFist fist) {
        if (stand.level() instanceof ServerLevel serverLevel) {
            NetworkHandler.broadcastStandFist(serverLevel, stand, fist);
        }
    }

    public void fistsClashed(StandEntity other, Vec3 at, Vec3 heading, boolean perfect, StandEntity through) {
        if (!(stand.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int colour = through == null ? Impacts.PLAYER : StandShadowForm.auraColourFor(through);
        long now = serverLevel.getGameTime();
        Player mine = stand.getOwner(), theirs = other.getOwner();
        if (BarrageStruggle.between(mine, theirs) || BarrageStruggle.begin(mine, theirs, at)) {
            lastBigClash = now;
            other.barrage.lastBigClash = now;
            return;
        }
        boolean big = perfect || now - lastBigClash >= BIG_CLASH_EVERY_TICKS;
        if (big) {
            lastBigClash = now;
            other.barrage.lastBigClash = now;
            Impacts.landed(serverLevel, at, heading,
                    ImpactSpec.CLASH, colour);
        } else {
            Impacts.landed(serverLevel, at, heading,
                    ImpactSpec.MEDIUM, colour);
        }
    }

    private long lastBigClash = Long.MIN_VALUE / 2;

    private static final int BIG_CLASH_EVERY_TICKS = 8;

    private List<LivingEntity> flurryBodies(Player owner) {
        Vec3 look = owner.getLookAngle();
        if (stand.isPiloted()) {
            LivingEntity aimed = StandAim.lookTarget(owner, PILOTED_BARRAGE_REACH, true);
            if (aimed != null) {
                return new ArrayList<>(List.of(aimed));
            }
        }
        double reach = barrageReach();
        Vec3 centre = stand.position().add(0, stand.getBbHeight() * 0.5, 0).add(look.scale(reach * 0.5));
        AABB area = new AABB(centre, centre).inflate(reach * 0.5);
        return new ArrayList<>(stand.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && StandHits.hittable(owner, entity)
                        && inFrontOf(owner, look, entity)));
    }

    private boolean inFrontOf(Player owner, Vec3 look, LivingEntity victim) {
        return Hitbox.inLane(stand.position().add(0, stand.getBbHeight() * 0.5, 0), look,
                barrageReach(), BARRAGE_LANE_HALF_WIDTH, victim);
    }

    static Vec3 struck(LivingEntity victim, Vec3 fist, Vec3 heading) {
        AABB body = victim.getBoundingBox();
        return body.clip(fist.subtract(heading), fist).orElseGet(() -> new Vec3(
                Mth.clamp(fist.x, body.minX, body.maxX),
                Mth.clamp(fist.y, body.minY, body.maxY),
                Mth.clamp(fist.z, body.minZ, body.maxZ)));
    }

    private static final double FINISHER_SHOVE = 1.1;

    private static final double FINISHER_LIFT = 0.55;

    private static final float FINISHER_STING_VOLUME = 0.9F;

    private static final float FINISHER_STING_PITCH = 0.7F;

    public static final float BARRAGE_ENERGY_PER_HIT = 0.23F;

    private void spendBarrageEnergy(ServerPlayer owner, JojohaPlayerData data) {
        if (Charms.freeUtility(owner, data)) {
            return;
        }
        if (data.standEnergy <= 0F) {
            if (isHeldBarrageRunning()) {
                endHeldBarrage();
            }
            return;
        }
        data.standEnergy = Math.max(0F, data.standEnergy - BARRAGE_ENERGY_PER_HIT
                * Heaven.costScale(data));
        PlayerDataAccess.set(owner, data);
        if (data.standEnergy <= 0F) {
            PlayerDataAccess.sync(owner);
            if (isHeldBarrageRunning()) {
                endHeldBarrage();
            }
        }
    }

    private StandFists.Landing landFist(Player owner, LivingEntity victim, Vec3 contact,
                                                                       Vec3 heading, float blow, boolean finisher) {
        if (!(stand.level() instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer serverOwner)) {
            return StandFists.Landing.REFUSED;
        }
        if (BarrageStruggle.in(serverOwner)) {
            return StandFists.Landing.REFUSED;
        }
        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        LivingEntity body = StandHits.bodyOf(victim);
        float dealt = StandPassives.scaleOutgoing(serverOwner, data, body,
                blow * data.stand.powerScale());
        if (finisher) {
            dealt = Math.min(dealt, BarrageGenerator.FINISHER_CEILING);
        }
        boolean guarded = body instanceof ServerPlayer defender
                && PlayerDataAccess.get(defender).standGuarding;
        dealt = underBarrageCap(body, dealt);
        if (dealt <= 0F) {
            BarrageBank.keep(body, owner);
            spendBarrageEnergy(serverOwner, data);
            impactRings(serverLevel, victim, struck(victim, contact, heading), 0.5F, StandEntity.BARRAGE_RING_CLUSTER);
            return guarded ? StandFists.Landing.GUARDED : StandFists.Landing.HIT;
        }
        spendBarrageEnergy(serverOwner, data);
        float onVictim = dealt;
        victim.invulnerableTime = 0;
        boolean hurt = BarrageBlow.hurt(victim,
                serverLevel.damageSources().playerAttack(owner), finisher ? DamageScale.stand(owner, victim, onVictim)
                        : ComboDecay.uncounted(() -> DamageScale.stand(owner, victim, onVictim)));
        Vec3 struck = struck(victim, contact, heading);
        if (finisher) {
            BarrageBank.letGo(victim);
            if (hurt) {
                Vec3 away = victim.position().subtract(stand.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                if (flat.lengthSqr() > 1.0E-6) {
                    Vec3 shove = flat.normalize().scale(FINISHER_SHOVE);
                    Vec3 launch = new Vec3(shove.x, FINISHER_LIFT, shove.z);
                    victim.setDeltaMovement(launch);
                    ThrownBodies.launch(victim, launch);
                }
                Impacts.landed(serverLevel, victim, struck, away,
                        ImpactSpec.FINISHER, StandShadowForm.auraColourFor(stand));
                ComboDecay.reset(body);
                serverLevel.playSound(null, struck.x, struck.y, struck.z, ModSounds.GUARD_BREAK.get(),
                        SoundSource.PLAYERS, FINISHER_STING_VOLUME, FINISHER_STING_PITCH);
            }
        } else if (hurt) {
            thudImpact(serverLevel, victim, struck, blow / Math.max(1.0E-4F, beatBlow(owner)));
        } else {
            impactRings(serverLevel, victim, struck, dealt, StandEntity.BARRAGE_RING_CLUSTER);
        }
        victim.invulnerableTime = 0;
        victim.hurtMarked = true;
        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(serverOwner, data);
        return hurt ? StandFists.Landing.HIT
                : guarded ? StandFists.Landing.GUARDED
                : StandFists.Landing.REFUSED;
    }

    private void thudImpact(ServerLevel level, LivingEntity victim, Vec3 struck, float heft) {
        if (stand.barrageStyle != StandEntity.BarrageStyle.PUNCHES || heft < BarrageGenerator.THUD_LEAST_HEFT) {
            impactRings(level, victim, struck, heft, StandEntity.BARRAGE_RING_CLUSTER);
            return;
        }
        int beats = rushBeats();
        ImpactSpec spec = heft >= BarrageGenerator.heavyHeft(beats)
                ? ImpactSpec.HEAVY.scaledBy(BarrageGenerator.thudStrength(heft, beats))
                : ImpactSpec.THUD.scaledBy(BarrageGenerator.thudStrength(heft, beats) + THUD_STRENGTH_FLOOR);
        Impacts.landed(level, victim, struck, victim.position().subtract(stand.position()), spec,
                StandShadowForm.auraColourFor(stand));
    }

    private static final float THUD_STRENGTH_FLOOR = 0.3F;

    void playFlurryAnimation() {
        if (stand.barrageStyle == StandEntity.BarrageStyle.SWORD) {
            if (!flurryShown) {
                flurryShown = true;
                stand.triggerAnim(StandEntity.CONTROLLER, "sword_flurry");
            }
        } else if (stand.barrageStyle == StandEntity.BarrageStyle.KICKS) {
            if (!flurryShown) {
                flurryShown = true;
                stand.sync(StandEntity.DATA_KICKING, true);
                stand.triggerAnim(StandEntity.CONTROLLER, "kick_barrage");
            }
        }
    }

    public boolean isKickBarrage() {
        return stand.synced(StandEntity.DATA_KICKING);
    }

    public boolean atPeak() {
        return stand.synced(StandEntity.DATA_PEAK);
    }

    public static final int CLOSING_TICKS = 10;

    private static final int FLOW_STEPS = 100;

    private float closingBlow;

    int flow() {
        if (stand.barrageStyle != StandEntity.BarrageStyle.PUNCHES) {
            return 0;
        }
        int momentum = (int) Math.round(Mth.clamp(stand.fists.momentum(), 0.0, 1.0) * FLOW_STEPS);
        int toFinisher = stand.closingTicks > 0 ? stand.closingTicks
                : !isHeldBarrageRunning() && stand.barrageHitsRemaining > 0
                        ? (stand.barrageHitsRemaining - 1) * flurryInterval() + Math.max(0, stand.barrageHitInterval)
                        : Integer.MAX_VALUE;
        int closing = toFinisher >= CLOSING_TICKS ? 0
                : Math.round((1F - toFinisher / (float) CLOSING_TICKS) * FLOW_STEPS);
        return momentum + closing * (FLOW_STEPS + 1);
    }

    public float barrageMomentum() {
        return Math.floorMod(stand.synced(StandEntity.DATA_FLOW), FLOW_STEPS + 1) / (float) FLOW_STEPS;
    }

    public float barrageClosing() {
        return Math.floorDiv(stand.synced(StandEntity.DATA_FLOW), FLOW_STEPS + 1) / (float) FLOW_STEPS;
    }

    private void tickClosing(Player owner) {
        if (stand.closingTicks <= 0 || --stand.closingTicks > 0) {
            return;
        }
        if (owner != null) {
            throwFinisher(owner, closingBlow);
        }
    }

    public void hasten(int ticks) {
        stand.hasteTicks = Math.max(stand.hasteTicks, ticks);
        stand.sync(StandEntity.DATA_HASTE, true);
    }

    public boolean hastened() {
        return stand.synced(StandEntity.DATA_HASTE);
    }

    private void sweepBarrage(Player owner, float damage) {
        if (!(stand.level() instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverOwner);
        float dealt = damage * data.stand.powerScale();

        Vec3 look = owner.getLookAngle();
        double reach = barrageReach();
        Vec3 chest = stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        Vec3 centre = chest.add(look.scale(reach * 0.5));
        AABB area = new AABB(centre, centre).inflate(reach * 0.5);

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && StandHits.hittable(owner, entity))) {
            if (!Hitbox.inLane(chest, look, reach, BARRAGE_LANE_HALF_WIDTH, victim)) {
                continue;
            }

            LivingEntity body = StandHits.bodyOf(victim);
            float onVictim = underBarrageCap(body,
                    StandPassives.scaleOutgoing(serverOwner, data, body, dealt));
            if (onVictim <= 0F) {
                continue;
            }
            BarrageBlow.hurt(victim,
                    serverLevel.damageSources().playerAttack(owner),
                    ComboDecay.uncounted(() -> DamageScale.stand(owner, victim, onVictim)));
            impactRings(serverLevel, victim, onVictim, StandEntity.BARRAGE_RING_CLUSTER);
            victim.invulnerableTime = 0;
            victim.hurtMarked = true;
            styleShove(victim);
        }

        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(serverOwner, data);
    }

    public void beginCommandedBarrage() {
        if (stand.pursuitTarget != null || stand.returningFromPursuit) {
            stand.endEngagement();
            stand.returningFromPursuit = false;
        }

        stand.stationaryBarrageTicks = StandEntity.STRIKE_WAIT_TIMEOUT_TICKS
                + stand.barrageStyle.hits * stand.barrageStyle.interval + StandEntity.PUNCH_STANCE_TICKS;
        stand.queueStrike(StandEntity.PendingStrike.BARRAGE);
    }

    public void beginHeldBarrage() {
        beginHeldBarrage(stand.isChariot() ? StandEntity.BarrageStyle.SWORD : StandEntity.BarrageStyle.PUNCHES);
    }

    public void beginHeldBarrage(StandEntity.BarrageStyle style) {
        stand.barrageStyle = style;
        setHeldBarrage(true);
        stand.closingTicks = 0;
        heldBarrageTicks = 0;
        heldIdleTicks = 0;
        heldArmed = false;
        stand.barrageDealt.clear();
        barrageCapped = false;
        beginCommandedBarrage();
    }

    public void endHeldBarrage() {
        setHeldBarrage(false);
        coolBarrage(stand.barrageStyle);

        Player owner = stand.getOwner();
        if (!quietEnd && stand.barrageStyle == StandEntity.BarrageStyle.PUNCHES && owner != null && stand.fists.anyLive()
                && stand.fists.momentum() >= BarrageGenerator.PEAK_AT) {
            stand.closingTicks = CLOSING_TICKS;
            closingBlow = closingFinisher(owner, heldArmed
                    ? (stand.barrageStyle.hits - stand.barrageHitsRemaining) / (float) stand.barrageStyle.hits : 0F);
        }
        pendingExtraFists = 0;

        if (stand.pendingStrike == StandEntity.PendingStrike.BARRAGE) {
            stand.pendingStrike = null;
        }

        if (stand.barrageHitsRemaining > 0 || stand.barrageStyle != StandEntity.BarrageStyle.PUNCHES) {
            stand.barrageHitsRemaining = 0;
            stand.barrageHitInterval = 0;
            stopFlurryAnimation();
        }
        stand.barrageStyle = StandEntity.BarrageStyle.PUNCHES;

        stand.stationaryBarrageTicks = Math.min(stand.stationaryBarrageTicks, StandEntity.PUNCH_STANCE_TICKS);
        stand.takeAttackStance();
    }

    public void endHeldBarrageQuietly() {
        quietEnd = true;
        try {
            endHeldBarrage();
        } finally {
            quietEnd = false;
        }
        stand.closingTicks = 0;
        stand.fists.clear();
        pendingExtraFists = 0;
    }

    private boolean quietEnd;

    private boolean heldArmed;

    private int heldIdleTicks;

    private boolean idleBarrage() {
        Player owner = stand.getOwner();
        if (!(owner instanceof ServerPlayer serverOwner) || BarrageStruggle.in(owner)
                || PlayerDataAccess.get(serverOwner).standMode.handlesItems()
                || stand.pursuitTarget != null || !flurryBodies(owner).isEmpty()) {
            heldIdleTicks = 0;
            return false;
        }
        return ++heldIdleTicks >= BARRAGE_IDLE_TICKS;
    }

    public boolean isHeldBarrageRunning() {
        return stand.synced(StandEntity.DATA_HELD_BARRAGE);
    }

    void setHeldBarrage(boolean held) {
        stand.sync(StandEntity.DATA_HELD_BARRAGE, held);
    }

    void tickHeldBarrage() {
        if (!isHeldBarrageRunning()) {
            return;
        }
        if (!BarrageStruggle.in(stand.getOwner()) && ++heldBarrageTicks >= HELD_BARRAGE_MAX_TICKS) {
            endHeldBarrage();
            return;
        }
        if (stand.barrageHitsRemaining <= 0 && stand.pendingStrike == null) {
            if (heldArmed) {
                endHeldBarrage();
                return;
            }
            heldArmed = true;
            stand.barrageHitsRemaining = stand.barrageStyle.hits;
            stand.barrageHitInterval = stand.barrageStyle.interval;
            playFlurryAnimation();
        }
        if (heldArmed && idleBarrage()) {
            endHeldBarrage();
            return;
        }
        stand.stationaryBarrageTicks = Math.max(stand.stationaryBarrageTicks, stand.barrageStyle.interval * 2);
    }

    void releaseCommandedBarrage() {
        if (!isHeldBarrageRunning()) {
            stand.barrageDealt.clear();
            barrageCapped = false;
        } else {
            heldArmed = true;
        }
        stand.barrageHitsRemaining = stand.barrageStyle.hits;
        stand.barrageHitInterval = stand.barrageStyle.interval;
        playFlurryAnimation();
    }
}
