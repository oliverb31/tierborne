package com.ollie.tierborne.dungeon;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.item.ModItems;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.OpenDungeonMarkerScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;

public final class DungeonMarkerManager {
    public static final String ORC_WARRIOR = "tierborne:orc_warrior";
    public static final String ORC_SPEARTHROWER = "tierborne:orc_spearthrower";
    public static final String ORC_SHAMAN = "tierborne:orc_shaman";
    public static final String ORC_ELITE = "tierborne:orc_elite";
    public static final String ORC_BOSS = "tierborne:orc_boss";
    public static final String FROSTMITE = "tierborne:frostmite";
    public static final String FROZEN_BLAZE = "tierborne:frozen_blaze";
    public static final String GNUT = "tierborne:gnut";
    public static final String ICE_WITCH = "tierborne:ice_witch";
    public static final String ICEOLOGER = "tierborne:iceologer";
    public static final String SNOWBALL_SPIRIT = "tierborne:snowball_spirit";
    public static final String UNDEAD_ICE_WARRIOR = "tierborne:undead_ice_warrior";
    public static final String TARTARUS_YETI = "tierborne:tartarus_yeti";
    public static final String ICE_KNIGHT_MINION_SHIELD = "tierborne:ice_knight_minion_shield";
    public static final String ICE_KNIGHT_MINION_SPEAR = "tierborne:ice_knight_minion_spear";
    public static final String ICE_KNIGHT_MINION_SWORD = "tierborne:ice_knight_minion_sword";
    public static final String ICE_KNIGHT = "tierborne:ice_knight";
    private static final Map<String, String> MOB_NAMES = Map.ofEntries(
            Map.entry(ORC_WARRIOR, "Orc Warrior"),
            Map.entry(ORC_SPEARTHROWER, "Orc Spearthrower"),
            Map.entry(ORC_SHAMAN, "Orc Shaman"),
            Map.entry(ORC_ELITE, "Orc Elite"),
            Map.entry(ORC_BOSS, "Orc Boss"),
            Map.entry(FROSTMITE, "Frostmite"),
            Map.entry(FROZEN_BLAZE, "Frozen Blaze"),
            Map.entry(GNUT, "Gnut"),
            Map.entry(ICE_WITCH, "Ice Witch"),
            Map.entry(ICEOLOGER, "Iceologer"),
            Map.entry(SNOWBALL_SPIRIT, "Snowball Spirit"),
            Map.entry(UNDEAD_ICE_WARRIOR, "Undead Ice Warrior"),
            Map.entry(TARTARUS_YETI, "Yeti"),
            Map.entry(ICE_KNIGHT_MINION_SHIELD, "Ice Knight Shield"),
            Map.entry(ICE_KNIGHT_MINION_SPEAR, "Ice Knight Spear"),
            Map.entry(ICE_KNIGHT_MINION_SWORD, "Ice Knight Sword"),
            Map.entry(ICE_KNIGHT, "Ice Knight Boss"));

    private DungeonMarkerManager() {
    }

    public static void openEditor(ServerPlayer player, BlockPos floorPosition) {
        AuthoringTarget target = authoringTarget(player, floorPosition);
        if (target == null) return;
        int count = data(player.getServer(), target.instance.dungeon).count(target.instance.dungeon,
                target.localX, target.localY, target.localZ);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenDungeonMarkerScreenPacket(floorPosition, count));
    }

    public static void applyAction(ServerPlayer player, BlockPos floorPosition, String action) {
        AuthoringTarget target = authoringTarget(player, floorPosition);
        if (target == null) return;
        DungeonMarkerSavedData data = data(player.getServer(), target.instance.dungeon);
        if ("remove_last".equals(action)) {
            DungeonMarkerSavedData.MobMarker removed = data.removeLast(target.instance.dungeon,
                    target.localX, target.localY, target.localZ);
            if (removed == null) {
                message(player, "There is no mob marker on that block.");
            } else {
                message(player, "Removed one " + displayName(removed.mob()) + " marker.");
                saveGlobal(player, data, target.instance.dungeon);
            }
            return;
        }
        if ("clear".equals(action)) {
            int removed = data.clear(target.instance.dungeon, target.localX, target.localY, target.localZ);
            message(player, removed == 0 ? "There are no mob markers on that block."
                    : "Removed " + removed + " mob marker" + (removed == 1 ? "." : "s."));
            if (removed > 0) saveGlobal(player, data, target.instance.dungeon);
            return;
        }
        if (!MOB_NAMES.containsKey(action)) {
            message(player, "That mob cannot be used by the dungeon marker wand.");
            return;
        }

        BlockPos spawnPosition = floorPosition.above();
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(action));
        Entity preview = entityType == null ? null : entityType.create(player.level);
        if (!(preview instanceof Mob mob)) {
            if (preview != null) preview.discard();
            message(player, "That mob type is unavailable.");
            return;
        }
        mob.moveTo(spawnPosition.getX() + 0.5D, spawnPosition.getY(), spawnPosition.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        boolean clear = player.level.noCollision(mob);
        mob.discard();
        if (!clear) {
            message(player, "That mob does not fit above the selected block. Clear more space first.");
            return;
        }

        data.add(new DungeonMarkerSavedData.MobMarker(target.instance.dungeon,
                target.localX, target.localY, target.localZ, action, player.getYRot()));
        int count = data.count(target.instance.dungeon, target.localX, target.localY, target.localZ);
        message(player, "Added " + displayName(action) + " at this block (" + count + " total).");
        saveGlobal(player, data, target.instance.dungeon);
    }

    public static void tickConfiguredSpawns(ServerLevel level, DungeonSavedData.Instance instance) {
        if (instance.authoring) return;
        java.util.List<DungeonMarkerSavedData.MobMarker> markers =
                data(level.getServer(), instance.dungeon).markers(instance.dungeon);
        for (int markerIndex = 0; markerIndex < markers.size(); markerIndex++) {
            if (instance.spawnedMarkers.contains(markerIndex)) continue;
            DungeonMarkerSavedData.MobMarker marker = markers.get(markerIndex);
            BlockPos position = new BlockPos(instance.originX + marker.x(),
                    instance.originY + marker.y(), instance.originZ + marker.z());
            if (!level.hasChunkAt(position) || !shouldActivate(level, instance, position)) continue;
            spawnMarker(level, marker, position);
            instance.spawnedMarkers.add(markerIndex);
            DungeonSavedData.get(level.getServer()).changed();
        }
    }

    private static void spawnMarker(ServerLevel level, DungeonMarkerSavedData.MobMarker marker,
                                    BlockPos position) {
        ResourceLocation id = ResourceLocation.tryParse(marker.mob());
        EntityType<?> type = id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
        Entity entity = type == null ? null : type.create(level);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            Tierborne.LOGGER.warn("Skipped unavailable dungeon marker mob {}", marker.mob());
            return;
        }
        Vec3 safePosition = findSafeSpawnPosition(level, mob, position, marker.yaw());
        if (safePosition == null) {
            Tierborne.LOGGER.warn("Skipped dungeon marker mob {} at blocked position {}, {}, {}",
                    marker.mob(), marker.x(), marker.y(), marker.z());
            mob.discard();
            return;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.EVENT, null, null);
        mob.moveTo(safePosition.x, safePosition.y, safePosition.z, marker.yaw(), 0.0F);
        if (!level.noCollision(mob)) {
            Tierborne.LOGGER.warn("Skipped dungeon marker mob {} because its final dimensions do not fit at {}, {}, {}",
                    marker.mob(), marker.x(), marker.y(), marker.z());
            mob.discard();
            return;
        }
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean("tierborne:dungeon_marker_spawn", true);
        level.addFreshEntity(mob);
    }

    private static Vec3 findSafeSpawnPosition(ServerLevel level, Mob mob, BlockPos markerPosition,
                                              float yaw) {
        int[] supportOffsets = {-1, 0, -2, 1, 2};
        for (int supportOffset : supportOffsets) {
            BlockPos supportPosition = new BlockPos(markerPosition.getX(),
                    markerPosition.getY() + supportOffset, markerPosition.getZ());
            VoxelShape shape = level.getBlockState(supportPosition)
                    .getCollisionShape(level, supportPosition);
            if (shape.isEmpty()) continue;

            double surfaceHeight = -1.0D;
            for (AABB box : shape.toAabbs()) {
                if (box.minX <= 0.5D && box.maxX >= 0.5D
                        && box.minZ <= 0.5D && box.maxZ >= 0.5D) {
                    surfaceHeight = Math.max(surfaceHeight, box.maxY);
                }
            }
            if (surfaceHeight < 0.0D) continue;

            Vec3 candidate = new Vec3(markerPosition.getX() + 0.5D,
                    supportPosition.getY() + surfaceHeight + 1.0D, markerPosition.getZ() + 0.5D);
            mob.moveTo(candidate.x, candidate.y, candidate.z, yaw, 0.0F);
            if (level.noCollision(mob)) return candidate;
            return null;
        }
        return null;
    }

    private static boolean shouldActivate(ServerLevel level, DungeonSavedData.Instance instance,
                                          BlockPos spawnPosition) {
        double activationRange = RpgBalanceConfig.DUNGEON_MARKER_ACTIVATION_RANGE.get();
        double activationRangeSquared = activationRange * activationRange;
        Vec3 target = Vec3.atBottomCenterOf(spawnPosition).add(0.0D, 1.0D, 0.0D);
        for (java.util.UUID playerId : instance.party) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.level != level || !player.isAlive() || player.isSpectator()) continue;
            if (player.distanceToSqr(target) <= activationRangeSquared || hasClearLine(level, player, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClearLine(ServerLevel level, ServerPlayer player, Vec3 target) {
        HitResult hit = level.clip(new ClipContext(player.getEyePosition(), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(target) < 0.25D;
    }

    public static void showMarkers(ServerPlayer player) {
        if (!player.hasPermissions(2) || !player.getMainHandItem().is(ModItems.DUNGEON_MARKER_WAND.get())
                || !(player.level instanceof ServerLevel level)) return;
        Optional<DungeonSavedData.Instance> found = DungeonManager.findByPlayer(
                DungeonSavedData.get(player.getServer()), player.getUUID());
        if (found.isEmpty() || !found.get().authoring) return;
        DungeonSavedData.Instance instance = found.get();
        for (DungeonMarkerSavedData.MobMarker marker
                : data(player.getServer(), instance.dungeon).markers(instance.dungeon)) {
            double x = instance.originX + marker.x() + 0.5D;
            double y = instance.originY + marker.y() + 0.15D;
            double z = instance.originZ + marker.z() + 0.5D;
            if (player.distanceToSqr(x, y, z) <= 48.0D * 48.0D) {
                level.sendParticles(player, ParticleTypes.ENCHANT, true,
                        x, y, z, 3, 0.25D, 0.5D, 0.25D, 0.0D);
            }
        }
    }

    public static void giveWand(ServerPlayer player) {
        ItemStack wand = new ItemStack(ModItems.DUNGEON_MARKER_WAND.get());
        if (player.getInventory().contains(wand)) return;
        if (!player.getInventory().add(wand)) player.drop(wand, false);
        message(player, "Dungeon Marker Wand added to your inventory.");
    }

    private static AuthoringTarget authoringTarget(ServerPlayer player, BlockPos floorPosition) {
        if (!player.hasPermissions(2)) {
            message(player, "Only an operator can edit dungeon mob markers.");
            return null;
        }
        if (!player.getMainHandItem().is(ModItems.DUNGEON_MARKER_WAND.get())) {
            message(player, "Hold the Dungeon Marker Wand in your main hand.");
            return null;
        }
        if (player.distanceToSqr(floorPosition.getX() + 0.5D, floorPosition.getY() + 0.5D,
                floorPosition.getZ() + 0.5D) > 64.0D) {
            message(player, "That block is too far away.");
            return null;
        }
        Optional<DungeonSavedData.Instance> found = DungeonManager.findByPlayer(
                DungeonSavedData.get(player.getServer()), player.getUUID());
        if (found.isEmpty() || !found.get().authoring || !player.level.dimension().equals(DungeonManager.DUNGEON_LEVEL)) {
            message(player, "Use /tierborne dungeon edit <name> before placing mob markers.");
            return null;
        }
        DungeonSavedData.Instance instance = found.get();
        int localX = floorPosition.getX() - instance.originX;
        int localY = floorPosition.getY() + 1 - instance.originY;
        int localZ = floorPosition.getZ() - instance.originZ;
        if (localX < 0 || localX >= instance.width || localY < 0 || localY > instance.height
                || localZ < 0 || localZ >= instance.length) {
            message(player, "Choose a floor block inside the imported dungeon map.");
            return null;
        }
        return new AuthoringTarget(instance, localX, localY, localZ);
    }

    private static String displayName(String mob) {
        return MOB_NAMES.getOrDefault(mob, mob);
    }

    private static DungeonMarkerSavedData data(net.minecraft.server.MinecraftServer server, String dungeon) {
        DungeonMarkerSavedData data = DungeonMarkerSavedData.get(server);
        data.ensureDefaults(dungeon, DungeonMarkerDefaults.markers(dungeon));
        return data;
    }

    private static void message(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    private static void saveGlobal(ServerPlayer player, DungeonMarkerSavedData data, String dungeon) {
        try {
            DungeonMarkerDefaults.saveGlobal(dungeon, data.markers(dungeon));
            message(player, "Dungeon markers saved for this and future worlds.");
        } catch (java.io.IOException exception) {
            Tierborne.LOGGER.error("Could not save shared {} dungeon markers", dungeon, exception);
            message(player, "Marker saved in this world, but the shared marker file could not be updated.");
        }
    }

    private record AuthoringTarget(DungeonSavedData.Instance instance, int localX, int localY, int localZ) {
    }
}
