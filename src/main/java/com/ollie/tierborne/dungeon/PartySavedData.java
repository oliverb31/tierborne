package com.ollie.tierborne.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PartySavedData extends SavedData {
    private static final String NAME = "tierborne_parties";

    final Map<UUID, LinkedHashSet<UUID>> parties = new HashMap<>();
    final Map<UUID, UUID> memberLeaders = new HashMap<>();
    final Map<UUID, UUID> invites = new HashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    final Map<UUID, PendingDungeon> pendingDungeons = new HashMap<>();

    static PartySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PartySavedData::load, PartySavedData::new, NAME);
    }

    UUID leaderOf(UUID playerId) {
        return memberLeaders.get(playerId);
    }

    Set<UUID> membersOf(UUID playerId) {
        UUID leader = memberLeaders.get(playerId);
        return leader == null ? Set.of(playerId) : Set.copyOf(parties.getOrDefault(leader, new LinkedHashSet<>()));
    }

    PendingDungeon pendingFor(UUID playerId) {
        UUID leader = memberLeaders.getOrDefault(playerId, playerId);
        return pendingDungeons.get(leader);
    }

    void remember(UUID playerId, String name) {
        if (!name.equals(names.put(playerId, name))) setDirty();
    }

    void rebuildMemberIndex() {
        memberLeaders.clear();
        parties.forEach((leader, members) -> members.forEach(member -> memberLeaders.put(member, leader)));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedParties = new ListTag();
        parties.forEach((leader, members) -> {
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("Leader", leader);
            ListTag savedMembers = new ListTag();
            members.forEach(member -> {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("Id", member);
                savedMembers.add(memberTag);
            });
            partyTag.put("Members", savedMembers);
            savedParties.add(partyTag);
        });
        tag.put("Parties", savedParties);

        ListTag savedInvites = new ListTag();
        invites.forEach((player, leader) -> {
            CompoundTag inviteTag = new CompoundTag();
            inviteTag.putUUID("Player", player);
            inviteTag.putUUID("Leader", leader);
            savedInvites.add(inviteTag);
        });
        tag.put("Invites", savedInvites);

        CompoundTag savedNames = new CompoundTag();
        names.forEach((id, name) -> savedNames.putString(id.toString(), name));
        tag.put("Names", savedNames);

        ListTag savedPending = new ListTag();
        pendingDungeons.values().forEach(pending -> savedPending.add(pending.save()));
        tag.put("PendingDungeons", savedPending);
        return tag;
    }

    private static PartySavedData load(CompoundTag tag) {
        PartySavedData data = new PartySavedData();
        ListTag savedParties = tag.getList("Parties", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedParties.size(); index++) {
            CompoundTag partyTag = savedParties.getCompound(index);
            UUID leader = partyTag.getUUID("Leader");
            LinkedHashSet<UUID> members = new LinkedHashSet<>();
            ListTag savedMembers = partyTag.getList("Members", Tag.TAG_COMPOUND);
            for (int memberIndex = 0; memberIndex < savedMembers.size(); memberIndex++) {
                members.add(savedMembers.getCompound(memberIndex).getUUID("Id"));
            }
            members.add(leader);
            data.parties.put(leader, members);
        }
        data.rebuildMemberIndex();

        ListTag savedInvites = tag.getList("Invites", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedInvites.size(); index++) {
            CompoundTag inviteTag = savedInvites.getCompound(index);
            data.invites.put(inviteTag.getUUID("Player"), inviteTag.getUUID("Leader"));
        }
        CompoundTag savedNames = tag.getCompound("Names");
        savedNames.getAllKeys().forEach(id -> {
            try {
                data.names.put(UUID.fromString(id), savedNames.getString(id));
            } catch (IllegalArgumentException ignored) {
            }
        });
        ListTag savedPending = tag.getList("PendingDungeons", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedPending.size(); index++) {
            PendingDungeon pending = PendingDungeon.load(savedPending.getCompound(index));
            data.pendingDungeons.put(pending.leader, pending);
        }
        return data;
    }

    static final class PendingDungeon {
        final UUID leader;
        final String dungeon;
        final LinkedHashSet<UUID> accepted = new LinkedHashSet<>();

        PendingDungeon(UUID leader, String dungeon) {
            this.leader = leader;
            this.dungeon = dungeon;
            this.accepted.add(leader);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Leader", leader);
            tag.putString("Dungeon", dungeon);
            ListTag acceptedPlayers = new ListTag();
            accepted.forEach(player -> acceptedPlayers.add(StringTag.valueOf(player.toString())));
            tag.put("Accepted", acceptedPlayers);
            return tag;
        }

        private static PendingDungeon load(CompoundTag tag) {
            PendingDungeon pending = new PendingDungeon(tag.getUUID("Leader"), tag.getString("Dungeon"));
            ListTag acceptedPlayers = tag.getList("Accepted", Tag.TAG_STRING);
            for (int index = 0; index < acceptedPlayers.size(); index++) {
                try {
                    pending.accepted.add(UUID.fromString(acceptedPlayers.getString(index)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return pending;
        }
    }
}
