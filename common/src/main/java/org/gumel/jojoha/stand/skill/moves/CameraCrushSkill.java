package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.CameraBlockEntity;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.level.StructureSeek;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.List;

public final class CameraCrushSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "camera_crush");

    public static final CameraCrushSkill INSTANCE = new CameraCrushSkill();

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    public static final int PHOTOGRAPH_MODEL = 1;

    private static final double REACH = 8.0;

    private static final int SEARCH_CHUNKS = 100;

    private static final List<TagKey<Structure>> SUBJECTS = List.of(
            StructureTags.ON_TREASURE_MAPS,
            StructureTags.ON_WOODLAND_EXPLORER_MAPS,
            StructureTags.ON_OCEAN_EXPLORER_MAPS,
            StructureTags.VILLAGE,
            StructureTags.MINESHAFT,
            StructureTags.SHIPWRECK,
            StructureTags.RUINED_PORTAL,
            StructureTags.OCEAN_RUIN);

    private CameraCrushSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.camera_crush";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();

        CameraBlockEntity camera = look(player);
        if (camera == null || camera.isPrinting()) {
            return false;
        }

        TagKey<Structure> subject = SUBJECTS.get(level.random.nextInt(SUBJECTS.size()));
        BlockPos found = StructureSeek.nearest(level, subject, player.blockPosition(), SEARCH_CHUNKS);

        if (found == null) {
            return false;
        }

        camera.beginPrint(picture(level, found, subject, player.blockPosition()));

        aura(level, camera.getBlockPos().getCenter());

        MovementFx.pose(player, MovementFx.THROW);
        return true;
    }

    private static CameraBlockEntity look(ServerPlayer player) {
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (!(hit instanceof BlockHitResult block)) {
            return null;
        }

        return player.level().getBlockEntity(block.getBlockPos())
                instanceof CameraBlockEntity camera ? camera : null;
    }

    private static ItemStack picture(ServerLevel level, BlockPos found, TagKey<Structure> subject,
                                     BlockPos from) {
        ItemStack map = MapItem.create(level, found.getX(), found.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);

        map.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(PHOTOGRAPH_MODEL));

        PhotographDevelop.develop(level, map, found);

        map.set(DataComponents.CUSTOM_NAME, Component.translatable("item.jojoha.spirit_photograph")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        String what = subject.location().getPath().replace("on_", "").replace("_maps", "")
                .replace('_', ' ');

        map.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("item.jojoha.spirit_photograph.desc.1")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("item.jojoha.spirit_photograph.desc.2")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.literal(what).withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal(bearing(from, found)).withStyle(ChatFormatting.DARK_GRAY))));

        return map;
    }

    private static String bearing(BlockPos from, BlockPos found) {
        int dx = found.getX() - from.getX();
        int dz = found.getZ() - from.getZ();

        String[] points = {"south", "south-west", "west", "north-west",
                "north", "north-east", "east", "south-east"};

        double degrees = Math.toDegrees(Math.atan2(-dx, dz));
        int index = (int) Math.floor(((degrees + 360) % 360) / 45.0 + 0.5) % 8;

        long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
        return distance + " blocks " + points[index];
    }

    private static void aura(ServerLevel level, Vec3 at) {
        level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 28,
                0.45, 0.45, 0.45, 0.02);
        level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 0.4, at.z, 40,
                0.6, 0.5, 0.6, 0.6);

        BlockPos where = BlockPos.containing(at);
        level.playSound(null, where, ModSounds.STAND_HIT.get(),
                SoundSource.BLOCKS, 0.7F, 0.9F);
        level.playSound(null, where, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.9F, 1.3F);
    }
}
