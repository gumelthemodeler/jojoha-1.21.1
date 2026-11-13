package org.gumel.jojoha.stand.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Idealogue;
import org.gumel.jojoha.item.ArrowStrike;
import org.gumel.jojoha.item.StandArrowItem;
import org.gumel.jojoha.level.Continents;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.anubis.Bearer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MobStands {
    public static final TagKey<EntityType<?>> CAN_HOLD =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "can_hold_stand"));
    public static final TagKey<EntityType<?>> SPAWNS_WITH =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "spawns_with_stand"));

    public static final float SPAWN_CHANCE = 0.01F;

    public static final float USER_PULL = 3F;

    private static final double USER_RANGE = 64.0;

    public static final float TOUGHER_HEALTH = 50F;

    public static final float TOUGHER_ARMOR = 4F;

    private static final ResourceLocation TOUGHER = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_possessed");

    private static final Set<MobSpawnType> WORLD_SPAWNS = EnumSet.of(MobSpawnType.NATURAL,
            MobSpawnType.CHUNK_GENERATION, MobSpawnType.STRUCTURE, MobSpawnType.PATROL, MobSpawnType.EVENT);

    public static final float ARROW_CHANCE = 0.5F;
    public static final float SHARD_CHANCE = 0.2F;

    public static final float REJECTION_DAMAGE = 4F;

    public static final int CALM_TICKS = 100;
    private static final int REMEMBERS_A_BLOW = 200;

    private static final double FOE_RANGE = 24.0;

    private MobStands() {
    }

    public static MobStandHolder holder(Mob mob) {
        return (MobStandHolder) mob;
    }

    public static boolean canHold(LivingEntity entity) {
        return entity instanceof Mob && entity.getType().is(CAN_HOLD);
    }

    public static boolean has(Mob mob) {
        return holder(mob).jojoha$standId() != null;
    }

    public static ResourceLocation standId(Mob mob) {
        String id = holder(mob).jojoha$standId();
        return id == null ? null : ResourceLocation.tryParse(id);
    }

    public static void give(Mob mob, ResourceLocation standId, int skin) {
        holder(mob).jojoha$setStand(standId.toString(), skin);
        mob.setPersistenceRequired();
        toughen(mob);
        if (mob instanceof TamableAnimal pet && pet.getOwnerUUID() != null && mob.level() instanceof ServerLevel level) {
            StandPets.of(level.getServer()).bind(pet.getOwnerUUID(), pet.getUUID());
        }
        if (Anubis.ID.equals(standId)) {
            Anubis.arm(mob);
        }
    }

    public static List<ResourceLocation> pool() {
        List<ResourceLocation> pool = new ArrayList<>();
        for (StandType type : StandTypes.all().values()) {
            if (!type.form().isBound() && !StandTypes.secret(type.id())) {
                pool.add(type.id());
            }
        }
        pool.sort(Comparator.comparing(ResourceLocation::toString));
        return pool;
    }

    public static ResourceLocation roll(RandomSource random) {
        List<ResourceLocation> pool = pool();
        return pool.isEmpty() ? StandTypes.STAR_PLATINUM_ID : pool.get(random.nextInt(pool.size()));
    }

    public static void onSpawn(Mob mob, MobSpawnType spawnType) {
        if (mob.level().isClientSide() || has(mob) || !WORLD_SPAWNS.contains(spawnType)
                || !mob.getType().is(SPAWNS_WITH)) {
            return;
        }
        if (Bearer.rise(mob, spawnType)) {
            return;
        }
        float chance = JojohaServerConfig.MOB_STAND_SPAWN.chance()
                * Continents.at(mob.level(), mob.blockPosition()).standUsers;
        if (drawnToUser(mob)) {
            chance *= JojohaServerConfig.STAND_USER_PULL.value();
        }
        if (mob.getRandom().nextFloat() >= chance) {
            return;
        }
        List<ResourceLocation> born = pool();
        born.remove(Anubis.ID);
        give(mob, born.isEmpty() ? StandTypes.STAR_PLATINUM_ID : born.get(mob.getRandom().nextInt(born.size())), 0);
        Idealogue.marked(mob);
    }

    private static boolean drawnToUser(Mob mob) {
        return mob.level() instanceof ServerLevel level
                && level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), USER_RANGE, MobStands::pulls) != null;
    }

    private static boolean pulls(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.isSpectator()
                || Charms.warded(player)) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.stand.isPresent() || data.anubis;
    }

    private static void toughen(Mob mob) {
        boolean whole = mob.getHealth() >= mob.getMaxHealth();
        raise(mob, Attributes.MAX_HEALTH, JojohaServerConfig.STAND_MOB_HEALTH.chance(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        raise(mob, Attributes.ARMOR, JojohaServerConfig.STAND_MOB_ARMOR.value(), AttributeModifier.Operation.ADD_VALUE);
        if (whole) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static void raise(Mob mob, Holder<Attribute> attribute, double by,
                              AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(TOUGHER);
        if (by > 0.0) {
            instance.addPermanentModifier(new AttributeModifier(TOUGHER, by, operation));
        }
    }

    public static ArrowStrike struck(Mob mob, StandArrowItem arrow, ServerLevel level) {
        if (!canHold(mob) || has(mob) || !mob.isAlive()) {
            return ArrowStrike.NOTHING;
        }
        if (mob instanceof TamableAnimal pet && pet.getOwnerUUID() != null
                && StandPets.taken(level.getServer(), pet.getOwnerUUID(), pet)) {
            if (level.getServer().getPlayerList().getPlayer(pet.getOwnerUUID()) instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.jojoha.stand_pet.one_only",
                        pet.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            }
            return ArrowStrike.NOTHING;
        }
        float chance = JojohaServerConfig.awakening(arrow.isShard() ? SHARD_CHANCE : ARROW_CHANCE, true);
        if (level.getRandom().nextFloat() >= chance) {
            mob.hurt(level.damageSources().magic(), REJECTION_DAMAGE);
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                    12, 0.25, 0.3, 0.25, 0.02);
            level.playSound(null, mob.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE,
                    1.0F, 0.7F);
            return ArrowStrike.REFUSED;
        }
        give(mob, roll(level.getRandom()), 0);
        awakening(level, mob);
        summon(level, mob);
        return ArrowStrike.AWAKENED;
    }

    private static void awakening(ServerLevel level, Mob mob) {
        Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 40, 0.3, 0.5, 0.3, 0.12);
        level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 60, 0.5, 0.8, 0.5, 0.6);
        level.playSound(null, mob.blockPosition(), ModSounds.STAND_AWAKEN.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static StandEntity standOf(Mob mob) {
        UUID id = holder(mob).jojoha$standEntity();
        if (id == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        if (level.getEntity(id) instanceof StandEntity stand && stand.isAlive() && !stand.isRemoved()) {
            return stand;
        }
        holder(mob).jojoha$setStandEntity(null);
        return null;
    }

    public static boolean out(Mob mob) {
        if (!has(mob)) {
            return false;
        }
        if (Anubis.ID.equals(standId(mob))) {
            return Anubis.out(mob);
        }
        StandEntity stand = standOf(mob);
        return stand != null && !stand.isDismissing();
    }

    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
            return;
        }
        if (Anubis.ID.equals(standId(mob))) {
            Anubis.noteMob(mob);
            return;
        }
        MobStandHolder holder = holder(mob);
        LivingEntity foe = foe(mob);
        StandEntity stand = standOf(mob);
        if (foe != null) {
            holder.jojoha$setCalmTicks(0);
            if (stand == null || stand.isDismissing()) {
                summon(level, mob);
            }
        } else if (stand != null && !stand.isDismissing()) {
            int calm = holder.jojoha$calmTicks() + 1;
            holder.jojoha$setCalmTicks(calm);
            if (calm >= CALM_TICKS) {
                stand.beginDismissal();
            }
        }
    }

    public static LivingEntity foe(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (fightable(mob, target)) {
            return target;
        }
        LivingEntity hurtBy = mob.getLastHurtByMob();
        if (fightable(mob, hurtBy) && mob.tickCount - mob.getLastHurtByMobTimestamp() < REMEMBERS_A_BLOW) {
            return hurtBy;
        }
        return null;
    }

    private static boolean fightable(Mob mob, LivingEntity other) {
        if (other == null || other == mob || !other.isAlive() || other.isRemoved() || other instanceof StandEntity
                || other.level() != mob.level() || other.distanceTo(mob) > FOE_RANGE) {
            return false;
        }
        if (other instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return !(mob instanceof TamableAnimal pet && pet.isOwnedBy(other));
    }

    private static void summon(ServerLevel level, Mob mob) {
        ResourceLocation id = standId(mob);
        if (id == null || StandTypes.byId(id) == null || !StandTypes.byId(id).form().hasBody()) {
            return;
        }
        StandEntity stand = new StandEntity(ModRegistries.STAND.get(), level);
        stand.setMobOwner(mob);
        stand.setStandType(id);
        stand.setSkin(holder(mob).jojoha$standSkin());
        Vec3 at = MobStandBrain.shoulder(mob);
        stand.moveTo(at.x, at.y, at.z, mob.getYRot(), 0F);
        level.addFreshEntity(stand);
        holder(mob).jojoha$setStandEntity(stand.getUUID());
        holder(mob).jojoha$setCalmTicks(0);
        level.playSound(null, mob.blockPosition(), StandVoices.summon(stand.getStandType(),
                        StandShadowForm.of(stand) != null),
                SoundSource.HOSTILE, 0.8F, 1.0F);
    }
}
