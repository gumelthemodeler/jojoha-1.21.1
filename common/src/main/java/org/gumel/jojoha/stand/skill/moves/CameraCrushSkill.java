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
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.gumel.jojoha.block.CameraBlockEntity;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import java.util.List;

/**
 * Hermit Purple's real trick: breaking a camera to ask it where something is.
 *
 * <p>Joseph never used this Stand to fight, he used it to find things - smashing a camera and
 * reading the picture that came out. So the move takes a camera and gives back a map, and the vines
 * are only the means.
 *
 * <h2>Why it destroys the camera</h2>
 *
 * <p>Because the answer has to cost something, and a cooldown is not a cost - it is a wait. A
 * consumable makes each use a decision about whether this is the moment worth spending one on, and
 * it is the only reason the move has any tension in it at all.
 *
 * <h2>On a camera you placed, not one in your pocket</h2>
 *
 * <p>It used to reach into the inventory and take the first camera it found, which asked nothing of
 * the player beyond owning one - the move was a button that turned an item into another item. Now
 * the camera has to be standing somewhere and you have to be looking at it, so a picture is
 * something you set up: where it stands, which way it points, and then the break.
 *
 * <p>It also gives the move somewhere to happen. A camera in a hand has no body to shake and no slot
 * for anything to come out of; a camera on the ground has both, which is what the print animation is
 * for - see CameraBlockEntity.
 *
 * <h2>What it finds</h2>
 *
 * <p>Something different each time, drawn from a spread of structure kinds rather than one - a move
 * that always points at the nearest village is a compass, and a compass is not worth a camera. Which
 * of them answers is genuinely unknown until the picture develops, so a use is a question rather
 * than a request.
 *
 * <p>If the roll finds nothing in range the camera is not taken. A vision of nowhere is not a
 * result, and charging for one would be the move lying about what it did.
 */
public final class CameraCrushSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "camera_crush");

    public static final CameraCrushSkill INSTANCE = new CameraCrushSkill();

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    /**
     * The custom model value that turns a map into a print.
     *
     * <p>Paired with an override in {@code assets/minecraft/models/item/filled_map.json}. If that
     * file is ever lost the photograph quietly goes back to looking like a map and nothing breaks,
     * which is the right failure for a purely cosmetic hook.
     */
    public static final int PHOTOGRAPH_MODEL = 1;

    /**
     * How far away a camera can be broken from.
     *
     * <p>Longer than an arm and shorter than a bow. The Stand is what reaches it, so the distance is
     * the Stand's rather than the player's - and the extra range is most of what makes setting the
     * camera up somewhere worth doing, because you can stand back from it.
     */
    private static final double REACH = 8.0;

    /** How far out the search reaches, in chunks, and whether known structures count. */
    private static final int SEARCH_CHUNKS = 100;
    private static final boolean SKIP_KNOWN = false;

    /**
     * What the picture might be of.
     *
     * <p>Deliberately a mixture of the far and the near. A treasure map is a journey; a village is
     * an afternoon. Not knowing which you are about to get is the whole character of the move.
     */
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
        BlockPos found = level.findNearestMapStructure(subject, player.blockPosition(),
                SEARCH_CHUNKS, SKIP_KNOWN);

        // Nothing out there of that kind. The camera survives - see the class note.
        if (found == null) {
            return false;
        }

        // Decided now, handed over when the model has finished ejecting it. The wait is the
        // animation, and the picture is already chosen so what comes out is what was rolled - see
        // CameraBlockEntity.
        camera.beginPrint(picture(level, found, subject, player.blockPosition()));

        aura(level, camera.getBlockPos().getCenter());
        return true;
    }

    /**
     * The camera being looked at, or null.
     *
     * <p>A block trace rather than a search of everything nearby, because which camera is being
     * broken has to be the player's decision. Two cameras a block apart would otherwise be a coin
     * toss, and the move would take the wrong picture at the wrong moment.
     */
    private static CameraBlockEntity look(ServerPlayer player) {
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (!(hit instanceof BlockHitResult block)) {
            return null;
        }

        return player.level().getBlockEntity(block.getBlockPos())
                instanceof CameraBlockEntity camera ? camera : null;
    }

    /**
     * The developed picture: a map to the place, dressed as a photograph.
     *
     * <p>Still a real filled map underneath, and that is deliberate. Everything worth having about
     * it - the marker that tracks you, the terrain filling in as you travel, working in an item
     * frame - is the map item doing its job, and a bespoke photograph item would have to reimplement
     * all of it to end up somewhere worse.
     *
     * <p>What changes is only what it looks like. A custom model value on this stack matches an
     * override added to the vanilla map model, so this one map draws as a print and every other map
     * in the world is untouched. Retexturing {@code item/filled_map} directly would have made every
     * map a photograph, which is a large thing to do to somebody's world for one Stand ability.
     */
    private static ItemStack picture(ServerLevel level, BlockPos found, TagKey<Structure> subject,
                                     BlockPos from) {
        ItemStack map = MapItem.create(level, found.getX(), found.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);

        map.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(PHOTOGRAPH_MODEL));

        // Monochrome, and marked with an X where the subject is - see PhotographDevelop.
        PhotographDevelop.develop(level, map, found);

        map.set(DataComponents.CUSTOM_NAME, Component.translatable("item.jojoha.spirit_photograph")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        // The name of the tag is the closest thing to a caption the game already has, and it reads
        // better than a coordinate - the picture says what it is of, not where it is.
        String what = subject.location().getPath().replace("on_", "").replace("_maps", "")
                .replace('_', ' ');

        map.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("item.jojoha.spirit_photograph.desc")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.literal(what).withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal(bearing(from, found)).withStyle(ChatFormatting.DARK_GRAY))));

        return map;
    }

    /**
     * Which way it is, and how far, written on the back of the print.
     *
     * <p>The X on the map says where the place is relative to the map; this says where it is
     * relative to the player, which is the question actually being asked and the one a map is worst
     * at answering. Reading a bearing off an explorer map means working out which of two similar
     * grey shapes is you, and a player who cannot tell simply wanders.
     *
     * <p>Fixed at the moment the picture was taken, and honestly so - it is a caption on a
     * photograph, not a compass. Walk a thousand blocks and it describes where you were standing
     * when you broke the camera, which is exactly what a photograph does.
     */
    private static String bearing(BlockPos from, BlockPos found) {
        int dx = found.getX() - from.getX();
        int dz = found.getZ() - from.getZ();

        // Sixteen points would be false precision on a number that goes stale as you walk. Eight is
        // enough to set off in the right direction, which is all this has to do.
        String[] points = {"south", "south-west", "west", "north-west",
                "north", "north-east", "east", "south-east"};

        // Minecraft yaw: zero is south, and it turns toward west as it climbs.
        double degrees = Math.toDegrees(Math.atan2(-dx, dz));
        int index = (int) Math.floor(((degrees + 360) % 360) / 45.0 + 0.5) % 8;

        long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
        return distance + " blocks " + points[index];
    }

    /**
     * The vines taking hold of it, at the camera rather than at the player.
     *
     * <p>Moved off the caster deliberately. The Stand reaches out to the thing being broken, so the
     * light and the noise belong where the break is happening - at the player they read as the
     * player doing something to themselves, and a bystander gets no clue what was touched.
     *
     * <p>The glass itself is not broken here any more. That sound comes at the end, with the camera,
     * so the sequence is a grip, a shake, a print, and only then the thing coming apart.
     */
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
