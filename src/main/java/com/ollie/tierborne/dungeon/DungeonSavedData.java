package com.ollie.tierborne.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DungeonSavedData extends SavedData {
    private static final String NAME = "tierborne_dungeons";
    private final Map<Integer, Instance> instances = new LinkedHashMap<>();
    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();

    public static DungeonSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DungeonSavedData::load, DungeonSavedData::new, NAME);
    }

    public Map<Integer, Instance> instances() {
        return instances;
    }

    public Map<UUID, ReturnPoint> returnPoints() {
        return returnPoints;
    }

    public void changed() {
        setDirty();
    }

    public static final class Instance {
        public int id;
        public String dungeon = "";
        public String sourceHash = "";
        public String state = "PREPARING";
        public int cellX;
        public int cellZ;
        public int originX;
        public int originY;
        public int originZ;
        public int width;
        public int height;
        public int length;
        public int floorIndex;
        public int tileIndex;
        public int blockIndex;
        public int updateTileIndex;
        public int updateBlockIndex;
        public long placedBlocks;
        public long seed;
        public long lastOccupiedTick;
        public int partySizeSnapshot;
        public boolean authoring;
        public UUID leader;
        public final List<UUID> party = new ArrayList<>();
        public final Map<UUID, Checkpoint> checkpoints = new HashMap<>();
        public final List<Long> firePositions = new ArrayList<>();
        public final Set<Integer> spawnedMarkers = new LinkedHashSet<>();

        public boolean contains(double x, double z) {
            return x >= cellX && x < cellX + DungeonManager.CELL_SIZE
                    && z >= cellZ && z < cellZ + DungeonManager.CELL_SIZE;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", id);
            tag.putString("Dungeon", dungeon);
            tag.putString("SourceHash", sourceHash);
            tag.putString("State", state);
            tag.putInt("CellX", cellX);
            tag.putInt("CellZ", cellZ);
            tag.putInt("OriginX", originX);
            tag.putInt("OriginY", originY);
            tag.putInt("OriginZ", originZ);
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            tag.putInt("Length", length);
            tag.putInt("FloorIndex", floorIndex);
            tag.putInt("TileIndex", tileIndex);
            tag.putInt("BlockIndex", blockIndex);
            tag.putInt("UpdateTileIndex", updateTileIndex);
            tag.putInt("UpdateBlockIndex", updateBlockIndex);
            tag.putLong("PlacedBlocks", placedBlocks);
            tag.putLong("Seed", seed);
            tag.putLong("LastOccupiedTick", lastOccupiedTick);
            tag.putInt("PartySizeSnapshot", partySizeSnapshot);
            tag.putBoolean("Authoring", authoring);
            if (leader != null) tag.putUUID("Leader", leader);
            ListTag members = new ListTag();
            for (UUID member : party) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("Id", member);
                members.add(memberTag);
            }
            tag.put("Party", members);
            ListTag savedCheckpoints = new ListTag();
            checkpoints.forEach((player, checkpoint) -> {
                CompoundTag checkpointTag = checkpoint.save();
                checkpointTag.putUUID("Player", player);
                savedCheckpoints.add(checkpointTag);
            });
            tag.put("Checkpoints", savedCheckpoints);
            ListTag fires = new ListTag();
            firePositions.forEach(position -> fires.add(LongTag.valueOf(position)));
            tag.put("FirePositions", fires);
            tag.putIntArray("SpawnedMarkers", spawnedMarkers.stream().mapToInt(Integer::intValue).toArray());
            return tag;
        }

        private static Instance load(CompoundTag tag) {
            Instance instance = new Instance();
            instance.id = tag.getInt("Id");
            instance.dungeon = tag.getString("Dungeon");
            instance.sourceHash = tag.getString("SourceHash");
            instance.state = tag.getString("State");
            instance.cellX = tag.getInt("CellX");
            instance.cellZ = tag.getInt("CellZ");
            instance.originX = tag.getInt("OriginX");
            instance.originY = tag.getInt("OriginY");
            instance.originZ = tag.getInt("OriginZ");
            instance.width = tag.getInt("Width");
            instance.height = tag.getInt("Height");
            instance.length = tag.getInt("Length");
            instance.floorIndex = tag.getInt("FloorIndex");
            instance.tileIndex = tag.getInt("TileIndex");
            instance.blockIndex = tag.getInt("BlockIndex");
            instance.updateTileIndex = tag.getInt("UpdateTileIndex");
            instance.updateBlockIndex = tag.getInt("UpdateBlockIndex");
            instance.placedBlocks = tag.getLong("PlacedBlocks");
            instance.seed = tag.getLong("Seed");
            instance.lastOccupiedTick = tag.getLong("LastOccupiedTick");
            instance.partySizeSnapshot = Math.max(1, tag.getInt("PartySizeSnapshot"));
            instance.authoring = tag.getBoolean("Authoring");
            if (tag.hasUUID("Leader")) instance.leader = tag.getUUID("Leader");
            ListTag members = tag.getList("Party", Tag.TAG_COMPOUND);
            for (int i = 0; i < members.size(); i++) instance.party.add(members.getCompound(i).getUUID("Id"));
            ListTag checkpoints = tag.getList("Checkpoints", Tag.TAG_COMPOUND);
            for (int i = 0; i < checkpoints.size(); i++) {
                CompoundTag checkpointTag = checkpoints.getCompound(i);
                instance.checkpoints.put(checkpointTag.getUUID("Player"), Checkpoint.load(checkpointTag));
            }
            ListTag fires = tag.getList("FirePositions", Tag.TAG_LONG);
            for (int i = 0; i < fires.size(); i++) instance.firePositions.add(((LongTag) fires.get(i)).getAsLong());
            for (int marker : tag.getIntArray("SpawnedMarkers")) instance.spawnedMarkers.add(marker);
            return instance;
        }
    }

    public record Checkpoint(double x, double y, double z, float yaw, float pitch) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);
            return tag;
        }

        private static Checkpoint load(CompoundTag tag) {
            return new Checkpoint(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                    tag.getFloat("Yaw"), tag.getFloat("Pitch"));
        }
    }

    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch, int gameMode) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);
            tag.putInt("GameMode", gameMode);
            return tag;
        }

        private static ReturnPoint load(CompoundTag tag) {
            return new ReturnPoint(tag.getString("Dimension"), tag.getDouble("X"), tag.getDouble("Y"),
                    tag.getDouble("Z"), tag.getFloat("Yaw"), tag.getFloat("Pitch"), tag.getInt("GameMode"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedInstances = new ListTag();
        instances.values().forEach(instance -> savedInstances.add(instance.save()));
        tag.put("Instances", savedInstances);
        ListTag savedReturns = new ListTag();
        returnPoints.forEach((player, point) -> {
            CompoundTag pointTag = point.save();
            pointTag.putUUID("Player", player);
            savedReturns.add(pointTag);
        });
        tag.put("ReturnPoints", savedReturns);
        return tag;
    }

    private static DungeonSavedData load(CompoundTag tag) {
        DungeonSavedData data = new DungeonSavedData();
        ListTag instances = tag.getList("Instances", Tag.TAG_COMPOUND);
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = Instance.load(instances.getCompound(i));
            data.instances.put(instance.id, instance);
        }
        ListTag returnPoints = tag.getList("ReturnPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < returnPoints.size(); i++) {
            CompoundTag pointTag = returnPoints.getCompound(i);
            data.returnPoints.put(pointTag.getUUID("Player"), ReturnPoint.load(pointTag));
        }
        return data;
    }
}
