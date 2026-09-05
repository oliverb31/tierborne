package com.ollie.tierborne.raid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class OrcRaidSavedData extends SavedData {
    private static final String FILE_NAME = "tierborne_orc_raids";
    private final Set<Integer> knownRaids = new HashSet<>();
    private final Set<Integer> raidsWithElite = new HashSet<>();
    private final Set<Long> rewardedWaves = new HashSet<>();

    public static OrcRaidSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                OrcRaidSavedData::load, OrcRaidSavedData::new, FILE_NAME);
    }

    public boolean assignElite(int raidId) {
        if (!raidsWithElite.add(raidId)) return false;
        setDirty();
        return true;
    }

    public void trackRaid(int raidId) {
        if (knownRaids.add(raidId)) setDirty();
    }

    public Set<Integer> knownRaids() {
        return Set.copyOf(knownRaids);
    }

    public boolean claimWaveReward(int raidId, int wave) {
        long key = ((long) raidId << 32) | (wave & 0xffffffffL);
        if (!rewardedWaves.add(key)) return false;
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putIntArray("KnownRaids", knownRaids.stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("RaidsWithElite", raidsWithElite.stream().mapToInt(Integer::intValue).toArray());
        tag.putLongArray("RewardedWaves", rewardedWaves.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    private static OrcRaidSavedData load(CompoundTag tag) {
        OrcRaidSavedData data = new OrcRaidSavedData();
        for (int raidId : tag.getIntArray("KnownRaids")) {
            data.knownRaids.add(raidId);
        }
        for (int raidId : tag.getIntArray("RaidsWithElite")) {
            data.raidsWithElite.add(raidId);
        }
        for (long wave : tag.getLongArray("RewardedWaves")) {
            data.rewardedWaves.add(wave);
        }
        return data;
    }
}
