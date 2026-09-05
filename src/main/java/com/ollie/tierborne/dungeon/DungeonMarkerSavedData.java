package com.ollie.tierborne.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DungeonMarkerSavedData extends SavedData {
    private static final String NAME = "tierborne_dungeon_markers";
    private final List<MobMarker> markers = new ArrayList<>();
    private final Set<String> initializedDungeons = new HashSet<>();

    public static DungeonMarkerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DungeonMarkerSavedData::load, DungeonMarkerSavedData::new, NAME);
    }

    public List<MobMarker> markers(String dungeon) {
        return markers.stream().filter(marker -> marker.dungeon().equals(dungeon)).toList();
    }

    public void ensureDefaults(String dungeon, List<MobMarker> defaults) {
        if (!initializedDungeons.add(dungeon)) return;
        if (markers(dungeon).isEmpty()) markers.addAll(defaults);
        setDirty();
    }

    public void add(MobMarker marker) {
        markers.add(marker);
        setDirty();
    }

    public MobMarker removeLast(String dungeon, int x, int y, int z) {
        for (int index = markers.size() - 1; index >= 0; index--) {
            MobMarker marker = markers.get(index);
            if (marker.matches(dungeon, x, y, z)) {
                markers.remove(index);
                setDirty();
                return marker;
            }
        }
        return null;
    }

    public int clear(String dungeon, int x, int y, int z) {
        int before = markers.size();
        markers.removeIf(marker -> marker.matches(dungeon, x, y, z));
        int removed = before - markers.size();
        if (removed > 0) setDirty();
        return removed;
    }

    public int count(String dungeon, int x, int y, int z) {
        return (int) markers.stream().filter(marker -> marker.matches(dungeon, x, y, z)).count();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedMarkers = new ListTag();
        markers.forEach(marker -> savedMarkers.add(marker.save()));
        tag.put("Markers", savedMarkers);
        ListTag initialized = new ListTag();
        initializedDungeons.forEach(dungeon ->
                initialized.add(net.minecraft.nbt.StringTag.valueOf(dungeon)));
        tag.put("InitializedDungeons", initialized);
        return tag;
    }

    private static DungeonMarkerSavedData load(CompoundTag tag) {
        DungeonMarkerSavedData data = new DungeonMarkerSavedData();
        ListTag savedMarkers = tag.getList("Markers", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedMarkers.size(); index++) {
            data.markers.add(MobMarker.load(savedMarkers.getCompound(index)));
        }
        ListTag initialized = tag.getList("InitializedDungeons", Tag.TAG_STRING);
        for (int index = 0; index < initialized.size(); index++) {
            data.initializedDungeons.add(initialized.getString(index));
        }
        return data;
    }

    public record MobMarker(String dungeon, int x, int y, int z, String mob, float yaw) {
        private boolean matches(String dungeon, int x, int y, int z) {
            return this.dungeon.equals(dungeon) && this.x == x && this.y == y && this.z == z;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dungeon", dungeon);
            tag.putInt("X", x);
            tag.putInt("Y", y);
            tag.putInt("Z", z);
            tag.putString("Mob", mob);
            tag.putFloat("Yaw", yaw);
            return tag;
        }

        private static MobMarker load(CompoundTag tag) {
            return new MobMarker(tag.getString("Dungeon"), tag.getInt("X"), tag.getInt("Y"),
                    tag.getInt("Z"), tag.getString("Mob"), tag.getFloat("Yaw"));
        }
    }
}
