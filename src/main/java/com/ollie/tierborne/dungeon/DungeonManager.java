package com.ollie.tierborne.dungeon;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DungeonManager {
    public static final int CELL_SIZE = 2_048;
    public static final int MAP_PADDING = 16;
    public static final ResourceKey<Level> DUNGEON_LEVEL = ResourceKey.create(
            Registry.DIMENSION_REGISTRY, new ResourceLocation(Tierborne.MOD_ID, "dungeons"));
    private static final int BLOCK_BUDGET_PER_TICK = 20_000;
    private static final long ABANDONED_TICKS = 20L * 60L * 5L;
    private static final UUID HEALTH_SCALE_ID = UUID.fromString("0cb4b838-c731-413e-97b9-7406c429ebd2");
    private static final UUID DAMAGE_SCALE_ID = UUID.fromString("93cba4f1-771a-41ca-a910-b679fefb7c52");
    private static final Map<Integer, LoadedTile> TILE_CACHE = new HashMap<>();
    private static final Map<Integer, Map<Long, BlockState>> FIRE_GUARDS = new HashMap<>();
    private static final java.util.Set<UUID> AUTHORIZED_TRAVEL = new java.util.HashSet<>();
    private static Map<String, DungeonManifest> manifests = Map.of();

    private record PlacedBlock(BlockPos position, BlockState state) {
    }

    private record LoadedTile(int tileIndex, List<PlacedBlock> blocks) {
    }

    private DungeonManager() {
    }

    public static void reload(MinecraftServer server) {
        manifests = DungeonManifest.loadAll(server);
        TILE_CACHE.clear();
    }

    public static List<String> dungeonNames(MinecraftServer server) {
        if (manifests.isEmpty()) reload(server);
        return manifests.keySet().stream().sorted().toList();
    }

    public static boolean start(ServerPlayer leader, String dungeonName) {
        MinecraftServer server = leader.getServer();
        if (server == null) return false;
        if (manifests.isEmpty()) reload(server);
        DungeonManifest manifest = manifests.get(dungeonName);
        if (manifest == null) {
            leader.sendSystemMessage(Component.literal("Dungeon data '" + dungeonName
                    + "' is not installed. Add its locally converted datapack, then run /reload."));
            return false;
        }
        ServerLevel dungeonLevel = server.getLevel(DUNGEON_LEVEL);
        if (dungeonLevel == null) {
            leader.sendSystemMessage(Component.literal("The Tierborne dungeon dimension is unavailable."));
            return false;
        }
        if (manifest.width() + MAP_PADDING * 2 > CELL_SIZE || manifest.length() + MAP_PADDING * 2 > CELL_SIZE
                || manifest.height() > dungeonLevel.getHeight() - 16) {
            leader.sendSystemMessage(Component.literal("Dungeon dimensions exceed the safe instance bounds."));
            return false;
        }

        DungeonSavedData data = DungeonSavedData.get(server);
        List<ServerPlayer> party = DungeonPartyManager.membersForStart(leader);
        if (party.isEmpty()) return false;
        for (ServerPlayer member : party) {
            if (findByPlayer(data, member.getUUID()).isPresent()) {
                leader.sendSystemMessage(Component.literal(member.getGameProfile().getName()
                        + " is already assigned to a dungeon."));
                return false;
            }
        }

        int id = firstFreeId(data);
        int gridX = id % 1_024;
        int gridZ = id / 1_024;
        DungeonSavedData.Instance instance = new DungeonSavedData.Instance();
        instance.id = id;
        instance.dungeon = dungeonName;
        instance.sourceHash = manifest.sourceHash();
        instance.cellX = gridX * CELL_SIZE;
        instance.cellZ = gridZ * CELL_SIZE;
        instance.originX = instance.cellX + (CELL_SIZE - manifest.width()) / 2;
        instance.originY = dungeonLevel.getMinBuildHeight() + 8;
        instance.originZ = instance.cellZ + (CELL_SIZE - manifest.length()) / 2;
        instance.width = manifest.width();
        instance.height = manifest.height();
        instance.length = manifest.length();
        instance.seed = dungeonLevel.random.nextLong();
        instance.leader = leader.getUUID();
        instance.party.addAll(party.stream().map(ServerPlayer::getUUID).toList());
        instance.partySizeSnapshot = party.size();
        instance.lastOccupiedTick = dungeonLevel.getGameTime();
        for (ServerPlayer member : party) {
            data.returnPoints().put(member.getUUID(), new DungeonSavedData.ReturnPoint(
                    member.level.dimension().location().toString(), member.getX(), member.getY(), member.getZ(),
                    member.getYRot(), member.getXRot(), member.gameMode.getGameModeForPlayer().getId()));
        }
        data.instances().put(id, instance);
        data.changed();
        party.forEach(member -> member.sendSystemMessage(Component.literal("Preparing the full " + dungeonName
                + " dungeon for a party of " + party.size() + ". Entry begins only after every tile is verified.")));
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (manifests.isEmpty()) {
            try {
                reload(server);
            } catch (RuntimeException exception) {
                Tierborne.LOGGER.error("Could not load dungeon manifests", exception);
                return;
            }
        }
        ServerLevel level = server.getLevel(DUNGEON_LEVEL);
        if (level == null) return;
        DungeonSavedData data = DungeonSavedData.get(server);
        for (DungeonSavedData.Instance instance : new ArrayList<>(data.instances().values())) {
            if (instance.state.equals("PREPARING")) prepare(level, data, instance);
            else if (instance.state.equals("CLEANING")) clean(level, data, instance);
            if (!instance.state.equals("CLEANING")) {
                enforceParty(level, data, instance);
                if (instance.state.equals("ACTIVE")) protectFromFire(level, instance);
            }
        }
    }

    private static void prepare(ServerLevel level, DungeonSavedData data, DungeonSavedData.Instance instance) {
        DungeonManifest manifest = manifests.get(instance.dungeon);
        if (manifest == null || !manifest.sourceHash().equals(instance.sourceHash)) {
            fail(level.getServer(), data, instance, "Dungeon source manifest disappeared or changed; preparation stopped safely.");
            return;
        }
        int budget = BLOCK_BUDGET_PER_TICK;
        long shellTotal = shellBlockCount(instance);
        while (budget > 0 && instance.floorIndex < shellTotal) {
            BlockPos position = shellPosition(instance, instance.floorIndex++);
            level.setBlock(position, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            budget--;
        }
        try {
            while (budget > 0 && instance.tileIndex < manifest.tiles().size()) {
                DungeonManifest.Tile tile = manifest.tiles().get(instance.tileIndex);
                LoadedTile loaded = loadTile(level.getServer(), instance, manifest, tile, instance.tileIndex);
                while (budget > 0 && instance.blockIndex < loaded.blocks().size()) {
                    PlacedBlock block = loaded.blocks().get(instance.blockIndex++);
                    level.setBlock(block.position(), block.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    if ((block.state().is(Blocks.FIRE) || block.state().is(Blocks.SOUL_FIRE))
                            && !instance.firePositions.contains(block.position().asLong())) {
                        instance.firePositions.add(block.position().asLong());
                    }
                    instance.placedBlocks++;
                    budget--;
                }
                if (instance.blockIndex == loaded.blocks().size()) {
                    instance.tileIndex++;
                    instance.blockIndex = 0;
                    TILE_CACHE.remove(instance.id);
                }
            }
            while (budget > 0 && instance.tileIndex == manifest.tiles().size()
                    && instance.updateTileIndex < manifest.tiles().size()) {
                DungeonManifest.Tile tile = manifest.tiles().get(instance.updateTileIndex);
                LoadedTile loaded = loadTile(level.getServer(), instance, manifest, tile, instance.updateTileIndex);
                while (budget > 0 && instance.updateBlockIndex < loaded.blocks().size()) {
                    PlacedBlock block = loaded.blocks().get(instance.updateBlockIndex++);
                    level.blockUpdated(block.position(), block.state().getBlock());
                    budget--;
                }
                if (instance.updateBlockIndex == loaded.blocks().size()) {
                    instance.updateTileIndex++;
                    instance.updateBlockIndex = 0;
                    TILE_CACHE.remove(instance.id);
                }
            }
        } catch (RuntimeException exception) {
            Tierborne.LOGGER.error("Dungeon {} instance {} failed integrity validation", instance.dungeon, instance.id, exception);
            fail(level.getServer(), data, instance, "A dungeon tile was missing or unreadable; nobody was admitted.");
            return;
        }
        if (instance.updateTileIndex == manifest.tiles().size()) {
            if (instance.placedBlocks != manifest.nonAirBlocks()) {
                fail(level.getServer(), data, instance, "Dungeon block total did not match its manifest; nobody was admitted.");
                return;
            }
            instance.state = "ACTIVE";
            instance.lastOccupiedTick = level.getGameTime();
            createFireGuard(level, instance);
            enterParty(level, data, instance);
        }
        data.changed();
    }

    private static LoadedTile loadTile(MinecraftServer server, DungeonSavedData.Instance instance,
                                       DungeonManifest manifest, DungeonManifest.Tile tile, int tileIndex) {
        LoadedTile cached = TILE_CACHE.get(instance.id);
        if (cached != null && cached.tileIndex() == tileIndex) return cached;
        StructureTemplate template = server.getStructureManager().get(tile.structure())
                .orElseThrow(() -> new IllegalStateException("Missing structure " + tile.structure()));
        if (template.getSize().getX() != tile.width() || template.getSize().getY() != tile.height()
                || template.getSize().getZ() != tile.length()) {
            throw new IllegalStateException("Structure size mismatch for " + tile.structure());
        }
        CompoundTag encoded = template.save(new CompoundTag());
        ListTag palette = encoded.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocks = encoded.getList("blocks", Tag.TAG_COMPOUND);
        if (blocks.size() != tile.blocks()) {
            throw new IllegalStateException("Structure block count mismatch for " + tile.structure());
        }
        List<PlacedBlock> loadedBlocks = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag encodedBlock = blocks.getCompound(i);
            ListTag position = encodedBlock.getList("pos", Tag.TAG_INT);
            BlockPos worldPosition = new BlockPos(
                    instance.originX + tile.x() * manifest.tileSize() + position.getInt(0),
                    instance.originY + tile.y() * manifest.tileSize() + position.getInt(1),
                    instance.originZ + tile.z() * manifest.tileSize() + position.getInt(2));
            int stateId = encodedBlock.getInt("state");
            if (stateId < 0 || stateId >= palette.size()) {
                throw new IllegalStateException("Invalid palette entry in " + tile.structure());
            }
            loadedBlocks.add(new PlacedBlock(worldPosition, NbtUtils.readBlockState(palette.getCompound(stateId))));
        }
        LoadedTile loaded = new LoadedTile(tileIndex, List.copyOf(loadedBlocks));
        TILE_CACHE.put(instance.id, loaded);
        return loaded;
    }

    private static void enterParty(ServerLevel level, DungeonSavedData data, DungeonSavedData.Instance instance) {
        DungeonManifest manifest = manifests.get(instance.dungeon);
        DungeonManifest.Entrance configuredEntrance = manifest.entrance();
        BlockPos entrance = configuredEntrance == null
                ? findSafeEntrance(level, instance)
                : new BlockPos(Mth.floor(instance.originX + configuredEntrance.x()),
                Mth.floor(instance.originY + configuredEntrance.y()),
                Mth.floor(instance.originZ + configuredEntrance.z()));
        for (UUID playerId : instance.party) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) continue;
            data.returnPoints().putIfAbsent(playerId, new DungeonSavedData.ReturnPoint(
                    player.level.dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer().getId()));
            DungeonSavedData.Checkpoint checkpoint = new DungeonSavedData.Checkpoint(
                    entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
                    configuredEntrance == null ? player.getYRot() : configuredEntrance.yaw(),
                    configuredEntrance == null ? player.getXRot() : configuredEntrance.pitch());
            instance.checkpoints.put(playerId, checkpoint);
            player.stopRiding();
            player.setGameMode(GameType.ADVENTURE);
            player.teleportTo(level, checkpoint.x(), checkpoint.y(), checkpoint.z(), checkpoint.yaw(), checkpoint.pitch());
            player.sendSystemMessage(Component.literal("Dungeon ready: all " + manifests.get(instance.dungeon).tiles().size()
                    + " tiles and " + instance.placedBlocks + " blocks were verified and placed."));
        }
    }

    private static BlockPos findSafeEntrance(ServerLevel level, DungeonSavedData.Instance instance) {
        int maxZ = Math.min(instance.length - 1, 63);
        for (int z = 0; z <= maxZ; z++) {
            for (int x = 0; x < instance.width; x++) {
                for (int y = instance.height - 1; y >= 0; y--) {
                    BlockPos floor = new BlockPos(instance.originX + x, instance.originY + y, instance.originZ + z);
                    if (!level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                            && level.getBlockState(floor.above()).isAir()
                            && level.getBlockState(floor.above(2)).isAir()) return floor.above();
                }
            }
        }
        return new BlockPos(instance.originX + MAP_PADDING, instance.originY + instance.height + 2,
                instance.originZ + MAP_PADDING);
    }

    private static void enforceParty(ServerLevel level, DungeonSavedData data, DungeonSavedData.Instance instance) {
        boolean occupied = false;
        for (UUID playerId : instance.party) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) continue;
            occupied = true;
            if (instance.state.equals("ACTIVE") && player.level.dimension().equals(DUNGEON_LEVEL)) {
                if (!instance.contains(player.getX(), player.getZ()) || player.getY() < instance.originY - 1) {
                    teleportToCheckpoint(player, instance, "You crossed the dungeon boundary and were returned to safety.");
                }
            }
        }
        if (occupied) instance.lastOccupiedTick = level.getGameTime();
        else if (instance.state.equals("ACTIVE") && level.getGameTime() - instance.lastOccupiedTick >= ABANDONED_TICKS) {
            beginCleanup(level.getServer(), instance, "The abandoned dungeon instance is being cleaned up.");
        }
        data.changed();
    }

    public static void checkpoint(ServerPlayer player) {
        DungeonSavedData data = DungeonSavedData.get(player.getServer());
        Optional<DungeonSavedData.Instance> found = findByPlayer(data, player.getUUID());
        if (found.isEmpty() || !player.level.dimension().equals(DUNGEON_LEVEL)
                || !found.get().contains(player.getX(), player.getZ())) {
            player.sendSystemMessage(Component.literal("You are not inside your active dungeon instance."));
            return;
        }
        found.get().checkpoints.put(player.getUUID(), new DungeonSavedData.Checkpoint(
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        data.changed();
        player.sendSystemMessage(Component.literal("Dungeon checkpoint saved."));
    }

    public static void teleportToCheckpoint(ServerPlayer player, DungeonSavedData.Instance instance, String message) {
        DungeonSavedData.Checkpoint checkpoint = instance.checkpoints.get(player.getUUID());
        ServerLevel level = player.getServer().getLevel(DUNGEON_LEVEL);
        if (checkpoint == null || level == null) return;
        player.stopRiding();
        player.teleportTo(level, checkpoint.x(), checkpoint.y(), checkpoint.z(), checkpoint.yaw(), checkpoint.pitch());
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.sendSystemMessage(Component.literal(message));
    }

    public static void leave(ServerPlayer player) {
        DungeonSavedData data = DungeonSavedData.get(player.getServer());
        Optional<DungeonSavedData.Instance> instance = findByPlayer(data, player.getUUID());
        if (instance.isEmpty()) {
            player.sendSystemMessage(Component.literal("You are not assigned to an active dungeon."));
            return;
        }
        restorePlayer(player, data);
        DungeonSavedData.Instance value = instance.get();
        value.party.remove(player.getUUID());
        value.checkpoints.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("You left the dungeon."));
        if (value.party.isEmpty()) beginCleanup(player.getServer(), value, "Dungeon instance is being cleaned up.");
        data.changed();
    }

    public static void finish(ServerPlayer player) {
        DungeonSavedData data = DungeonSavedData.get(player.getServer());
        Optional<DungeonSavedData.Instance> found = findByPlayer(data, player.getUUID());
        if (found.isEmpty() || (!player.getUUID().equals(found.get().leader) && !player.hasPermissions(2))) {
            player.sendSystemMessage(Component.literal("Only the party leader can finish this dungeon."));
            return;
        }
        beginCleanup(player.getServer(), found.get(), "The party left the dungeon; cleanup has started.");
    }

    private static void beginCleanup(MinecraftServer server, DungeonSavedData.Instance instance, String message) {
        DungeonSavedData data = DungeonSavedData.get(server);
        ServerLevel dungeonLevel = server.getLevel(DUNGEON_LEVEL);
        Map<Long, BlockState> guardedBlocks = FIRE_GUARDS.remove(instance.id);
        if (dungeonLevel != null && guardedBlocks != null) {
            guardedBlocks.forEach((encoded, expected) -> dungeonLevel.setBlock(
                    BlockPos.of(encoded), expected, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        }
        instance.state = "CLEANING";
        instance.tileIndex = 0;
        instance.blockIndex = 0;
        instance.floorIndex = 0;
        TILE_CACHE.remove(instance.id);
        for (UUID playerId : new ArrayList<>(instance.party)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                restorePlayer(player, data);
                player.sendSystemMessage(Component.literal(message));
            }
        }
        instance.party.clear();
        instance.checkpoints.clear();
        data.changed();
    }

    private static void clean(ServerLevel level, DungeonSavedData data, DungeonSavedData.Instance instance) {
        DungeonManifest manifest = manifests.get(instance.dungeon);
        if (manifest == null) {
            data.instances().remove(instance.id);
            data.changed();
            return;
        }
        int budget = BLOCK_BUDGET_PER_TICK;
        try {
            while (budget > 0 && instance.tileIndex < manifest.tiles().size()) {
                DungeonManifest.Tile tile = manifest.tiles().get(instance.tileIndex);
                LoadedTile loaded = loadTile(level.getServer(), instance, manifest, tile, instance.tileIndex);
                while (budget > 0 && instance.blockIndex < loaded.blocks().size()) {
                    level.setBlock(loaded.blocks().get(instance.blockIndex++).position(), Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    budget--;
                }
                if (instance.blockIndex == loaded.blocks().size()) {
                    instance.tileIndex++;
                    instance.blockIndex = 0;
                    TILE_CACHE.remove(instance.id);
                }
            }
        } catch (RuntimeException exception) {
            Tierborne.LOGGER.error("Cleanup could not read dungeon tiles for instance {}", instance.id, exception);
            return;
        }
        long shellTotal = shellBlockCount(instance);
        while (budget > 0 && instance.tileIndex == manifest.tiles().size() && instance.floorIndex < shellTotal) {
            level.setBlock(shellPosition(instance, instance.floorIndex++), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            budget--;
        }
        if (instance.tileIndex == manifest.tiles().size() && instance.floorIndex == shellTotal) {
            AABB bounds = new AABB(instance.cellX, level.getMinBuildHeight(), instance.cellZ,
                    instance.cellX + CELL_SIZE, level.getMaxBuildHeight(), instance.cellZ + CELL_SIZE);
            level.getEntities((Entity) null, bounds, entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
            data.instances().remove(instance.id);
            TILE_CACHE.remove(instance.id);
            FIRE_GUARDS.remove(instance.id);
        }
        data.changed();
    }

    private static long shellBlockCount(DungeonSavedData.Instance instance) {
        long paddedWidth = instance.width + MAP_PADDING * 2L;
        long paddedLength = instance.length + MAP_PADDING * 2L;
        long wallHeight = instance.height + 8L;
        return paddedWidth * paddedLength + 2L * wallHeight * (paddedWidth + paddedLength);
    }

    private static BlockPos shellPosition(DungeonSavedData.Instance instance, long index) {
        int minX = instance.originX - MAP_PADDING;
        int minZ = instance.originZ - MAP_PADDING;
        int width = instance.width + MAP_PADDING * 2;
        int length = instance.length + MAP_PADDING * 2;
        int floorY = instance.originY - 2;
        long floor = (long) width * length;
        if (index < floor) return new BlockPos(minX + (int) (index % width), floorY, minZ + (int) (index / width));
        long wallIndex = index - floor;
        int perimeter = 2 * (width + length);
        int y = floorY + 1 + (int) (wallIndex / perimeter);
        int around = (int) (wallIndex % perimeter);
        if (around < width) return new BlockPos(minX + around, y, minZ);
        around -= width;
        if (around < length) return new BlockPos(minX + width - 1, y, minZ + around);
        around -= length;
        if (around < width) return new BlockPos(minX + width - 1 - around, y, minZ + length - 1);
        around -= width;
        return new BlockPos(minX, y, minZ + length - 1 - around);
    }

    public static Optional<DungeonSavedData.Instance> instanceAt(MinecraftServer server, double x, double z) {
        return DungeonSavedData.get(server).instances().values().stream()
                .filter(instance -> instance.contains(x, z)).findFirst();
    }

    public static Optional<DungeonSavedData.Instance> findByPlayer(DungeonSavedData data, UUID player) {
        return data.instances().values().stream()
                .filter(instance -> !instance.state.equals("CLEANING") && instance.party.contains(player))
                .findFirst();
    }

    public static boolean isActiveDungeonPosition(ServerLevel level, BlockPos position) {
        return level.dimension().equals(DUNGEON_LEVEL)
                && instanceAt(level.getServer(), position.getX() + 0.5D, position.getZ() + 0.5D).isPresent();
    }

    public static boolean isParticipant(ServerPlayer player) {
        return findByPlayer(DungeonSavedData.get(player.getServer()), player.getUUID()).isPresent();
    }

    public static void initializeFireGuards(MinecraftServer server) {
        ServerLevel level = server.getLevel(DUNGEON_LEVEL);
        if (level == null) return;
        DungeonSavedData.get(server).instances().values().stream()
                .filter(instance -> instance.state.equals("ACTIVE") && !FIRE_GUARDS.containsKey(instance.id))
                .forEach(instance -> createFireGuard(level, instance));
    }

    private static void createFireGuard(ServerLevel level, DungeonSavedData.Instance instance) {
        Map<Long, BlockState> expected = new HashMap<>();
        for (long encoded : instance.firePositions) {
            BlockPos fire = BlockPos.of(encoded);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 4; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos position = fire.offset(x, y, z);
                        expected.put(position.asLong(), level.getBlockState(position));
                    }
                }
            }
        }
        FIRE_GUARDS.put(instance.id, expected);
    }

    private static void protectFromFire(ServerLevel level, DungeonSavedData.Instance instance) {
        Map<Long, BlockState> expected = FIRE_GUARDS.get(instance.id);
        if (expected == null) {
            createFireGuard(level, instance);
            expected = FIRE_GUARDS.get(instance.id);
        }
        expected.forEach((encoded, state) -> {
            BlockPos position = BlockPos.of(encoded);
            if (level.getBlockState(position) != state) {
                level.setBlock(position, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        });
    }

    public static boolean isAuthorizedTravel(Entity entity) {
        return AUTHORIZED_TRAVEL.contains(entity.getUUID());
    }

    public static void recoverOnLogin(ServerPlayer player) {
        DungeonSavedData data = DungeonSavedData.get(player.getServer());
        Optional<DungeonSavedData.Instance> instance = findByPlayer(data, player.getUUID());
        if (instance.isPresent() && instance.get().state.equals("ACTIVE")) {
            player.setGameMode(GameType.ADVENTURE);
            teleportToCheckpoint(player, instance.get(), "Returned to your dungeon checkpoint.");
        } else if (instance.isEmpty() || instance.get().state.equals("CLEANING")) {
            restorePlayer(player, data);
        }
    }

    public static void recoverAfterRespawn(ServerPlayer player) {
        findByPlayer(DungeonSavedData.get(player.getServer()), player.getUUID())
                .filter(instance -> instance.state.equals("ACTIVE"))
                .ifPresent(instance -> teleportToCheckpoint(player, instance, "You respawned at your dungeon checkpoint."));
    }

    private static void restorePlayer(ServerPlayer player, DungeonSavedData data) {
        DungeonSavedData.ReturnPoint point = data.returnPoints().remove(player.getUUID());
        if (point == null) return;
        ResourceLocation dimensionId = ResourceLocation.tryParse(point.dimension());
        ServerLevel destination = dimensionId == null ? null : player.getServer().getLevel(
                ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionId));
        if (destination == null) destination = player.getServer().overworld();
        player.stopRiding();
        AUTHORIZED_TRAVEL.add(player.getUUID());
        try {
            player.teleportTo(destination, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        } finally {
            AUTHORIZED_TRAVEL.remove(player.getUUID());
        }
        player.setGameMode(GameType.byId(point.gameMode()));
        data.changed();
    }

    public static void scaleEncounterMob(Mob mob) {
        if (!(mob.level instanceof ServerLevel level) || !level.dimension().equals(DUNGEON_LEVEL)
                || mob.getPersistentData().getBoolean("tierborne:dungeon_scaled")) return;
        Optional<DungeonSavedData.Instance> found = instanceAt(level.getServer(), mob.getX(), mob.getZ());
        if (found.isEmpty() || !found.get().state.equals("ACTIVE")) return;
        int partySize = Math.max(1, found.get().partySizeSnapshot);
        double healthBonus = RpgBalanceConfig.DUNGEON_HEALTH_PER_EXTRA_PLAYER.get() / 100.0D * (partySize - 1);
        double damageBonus = RpgBalanceConfig.DUNGEON_DAMAGE_PER_EXTRA_PLAYER.get() / 100.0D * (partySize - 1);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (health != null && healthBonus > 0.0D) health.addPermanentModifier(new AttributeModifier(
                HEALTH_SCALE_ID, "Tierborne dungeon party health scaling", healthBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (damage != null && damageBonus > 0.0D) damage.addPermanentModifier(new AttributeModifier(
                DAMAGE_SCALE_ID, "Tierborne dungeon party damage scaling", damageBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putBoolean("tierborne:dungeon_scaled", true);
        mob.getPersistentData().putLong("tierborne:dungeon_seed", found.get().seed);
    }

    private static void fail(MinecraftServer server, DungeonSavedData data,
                             DungeonSavedData.Instance instance, String message) {
        for (UUID playerId : instance.party) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) player.sendSystemMessage(Component.literal(message));
        }
        beginCleanup(server, instance, message);
        data.changed();
    }

    private static int firstFreeId(DungeonSavedData data) {
        for (int id = 0; id < 14_000 * 1_024; id++) if (!data.instances().containsKey(id)) return id;
        throw new IllegalStateException("No dungeon instance cells remain");
    }
}
