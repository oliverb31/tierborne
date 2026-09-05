package com.ollie.tierborne.dungeon;

import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.SyncPartyPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DungeonPartyManager {
    private DungeonPartyManager() {}

    public static void onLogin(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        data.remember(player.getUUID(), player.getGameProfile().getName());
        PartySavedData.PendingDungeon pending = data.pendingFor(player.getUUID());
        if (pending != null && !pending.leader.equals(player.getUUID())
                && !pending.accepted.contains(player.getUUID())) {
            DungeonManager.captureReturnPoint(player);
        }
        sync(player);
    }

    public static void invite(ServerPlayer inviter, ServerPlayer invited) {
        PartySavedData data = PartySavedData.get(inviter.getServer());
        UUID leader = data.leaderOf(inviter.getUUID());
        if (leader != null && !leader.equals(inviter.getUUID())) {
            message(inviter, "Only the party leader can invite players."); return;
        }
        if (inviter.getUUID().equals(invited.getUUID()) || data.leaderOf(invited.getUUID()) != null) {
            message(inviter, "That player is already in a party."); return;
        }
        LinkedHashSet<UUID> party = data.parties.computeIfAbsent(inviter.getUUID(), ignored -> new LinkedHashSet<>());
        party.add(inviter.getUUID());
        data.memberLeaders.put(inviter.getUUID(), inviter.getUUID());
        data.invites.put(invited.getUUID(), inviter.getUUID());
        data.remember(inviter.getUUID(), inviter.getGameProfile().getName());
        data.remember(invited.getUUID(), invited.getGameProfile().getName());
        data.setDirty();
        message(inviter, "Invited " + invited.getGameProfile().getName() + " to your permanent party.");
        message(invited, inviter.getGameProfile().getName() + " invited you to a party. Accept in the Party tab or use /tierborne party accept " + inviter.getGameProfile().getName() + ".");
        sync(inviter); sync(invited);
    }

    public static void accept(ServerPlayer player, ServerPlayer leaderPlayer) { accept(player, leaderPlayer.getUUID()); }

    public static void acceptLatest(ServerPlayer player) {
        UUID leader = PartySavedData.get(player.getServer()).invites.get(player.getUUID());
        if (leader == null) { message(player, "You do not have a party invitation."); return; }
        accept(player, leader);
    }

    private static void accept(ServerPlayer player, UUID leader) {
        PartySavedData data = PartySavedData.get(player.getServer());
        if (!leader.equals(data.invites.get(player.getUUID()))) {
            message(player, "You do not have an invitation from that player."); return;
        }
        LinkedHashSet<UUID> party = data.parties.get(leader);
        if (party == null) {
            data.invites.remove(player.getUUID()); data.setDirty();
            message(player, "That party no longer exists."); sync(player); return;
        }
        party.add(player.getUUID());
        data.memberLeaders.put(player.getUUID(), leader);
        data.invites.remove(player.getUUID());
        data.remember(player.getUUID(), player.getGameProfile().getName());
        data.setDirty();
        PartySavedData.PendingDungeon pending = data.pendingDungeons.get(leader);
        if (pending != null) DungeonManager.captureReturnPoint(player);
        notifyParty(player.getServer(), party, player.getGameProfile().getName() + " joined the party.");
        syncParty(player.getServer(), party);
    }

    public static void leave(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        UUID leader = data.leaderOf(player.getUUID());
        if (leader == null) { message(player, "You are not in a party."); return; }
        LinkedHashSet<UUID> party = data.parties.get(leader);
        if (party == null) return;
        if (leader.equals(player.getUUID())) {
            data.parties.remove(leader); data.pendingDungeons.remove(leader);
            party.forEach(id -> DungeonManager.discardReturnPoint(player.getServer(), id));
            party.forEach(data.memberLeaders::remove);
            notifyParty(player.getServer(), party, "The party was disbanded.");
        } else {
            party.remove(player.getUUID()); data.memberLeaders.remove(player.getUUID());
            PartySavedData.PendingDungeon pending = data.pendingDungeons.get(leader);
            if (pending != null) pending.accepted.remove(player.getUUID());
            DungeonManager.discardReturnPoint(player.getServer(), player.getUUID());
            message(player, "You left the party.");
            notifyParty(player.getServer(), party, player.getGameProfile().getName() + " left the party.");
        }
        data.setDirty(); syncParty(player.getServer(), party); sync(player);
    }

    public static Set<UUID> members(ServerPlayer player) {
        return PartySavedData.get(player.getServer()).membersOf(player.getUUID());
    }

    public static boolean offerDungeon(ServerPlayer leader, String dungeon) {
        PartySavedData data = PartySavedData.get(leader.getServer());
        UUID partyLeader = data.leaderOf(leader.getUUID());
        if (partyLeader != null && !partyLeader.equals(leader.getUUID())) {
            message(leader, "Only the party leader can initialise a dungeon."); return false;
        }
        if (data.pendingDungeons.containsKey(leader.getUUID())) {
            message(leader, "Your party already has a dungeon invitation open."); return false;
        }
        PartySavedData.PendingDungeon pending = new PartySavedData.PendingDungeon(leader.getUUID(), dungeon);
        data.pendingDungeons.put(leader.getUUID(), pending);
        data.setDirty();
        Set<UUID> party = data.membersOf(leader.getUUID());
        for (UUID id : party) {
            ServerPlayer member = leader.getServer().getPlayerList().getPlayer(id);
            if (member == null) continue;
            DungeonManager.captureReturnPoint(member);
            message(member, id.equals(leader.getUUID())
                    ? "Dungeon " + dungeon + " initialised. Start whenever ready in the Party tab or with /tierborne dungeon begin."
                    : "Party dungeon invitation: " + dungeon + ". Join now in the Party tab or with /tierborne dungeon join. You cannot join after it starts.");
        }
        syncParty(leader.getServer(), party); return true;
    }

    public static void joinDungeon(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PendingDungeon pending = data.pendingFor(player.getUUID());
        if (pending == null || pending.leader.equals(player.getUUID())) {
            message(player, "You do not have a dungeon invitation to join."); return;
        }
        if (!pending.accepted.add(player.getUUID())) {
            message(player, "You have already accepted this dungeon invitation."); return;
        }
        DungeonManager.captureReturnPoint(player); data.setDirty();
        notifyParty(player.getServer(), data.membersOf(player.getUUID()), player.getGameProfile().getName() + " accepted the dungeon invitation.");
        syncParty(player.getServer(), data.membersOf(player.getUUID()));
    }

    public static void beginDungeon(ServerPlayer leader) {
        PartySavedData data = PartySavedData.get(leader.getServer());
        PartySavedData.PendingDungeon pending = data.pendingDungeons.get(leader.getUUID());
        if (pending == null) { message(leader, "You have not initialised a party dungeon."); return; }
        List<ServerPlayer> accepted = new ArrayList<>();
        for (UUID id : pending.accepted) {
            ServerPlayer player = leader.getServer().getPlayerList().getPlayer(id);
            if (player != null) accepted.add(player);
        }
        if (DungeonManager.startAccepted(leader, pending.dungeon, accepted)) {
            Set<UUID> entering = accepted.stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
            data.membersOf(leader.getUUID()).stream()
                    .filter(id -> !entering.contains(id))
                    .forEach(id -> DungeonManager.discardReturnPoint(leader.getServer(), id));
            data.pendingDungeons.remove(leader.getUUID()); data.setDirty();
            syncParty(leader.getServer(), data.membersOf(leader.getUUID()));
        }
    }

    public static void cancelDungeon(ServerPlayer leader) {
        PartySavedData data = PartySavedData.get(leader.getServer());
        PartySavedData.PendingDungeon pending = data.pendingDungeons.remove(leader.getUUID());
        if (pending == null) { message(leader, "You have no pending dungeon to cancel."); return; }
        data.membersOf(leader.getUUID()).forEach(id ->
                DungeonManager.discardReturnPoint(leader.getServer(), id));
        data.setDirty();
        notifyParty(leader.getServer(), data.membersOf(leader.getUUID()), "The pending dungeon invitation was cancelled.");
        syncParty(leader.getServer(), data.membersOf(leader.getUUID()));
    }

    public static SyncPartyPacket snapshot(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        UUID leader = data.leaderOf(player.getUUID());
        List<String> names = data.membersOf(player.getUUID()).stream().map(id -> name(data, player.getServer(), id)).toList();
        UUID invitedBy = data.invites.get(player.getUUID());
        PartySavedData.PendingDungeon pending = data.pendingFor(player.getUUID());
        return new SyncPartyPacket(leader == null ? "" : name(data, player.getServer(), leader), names,
                invitedBy == null ? "" : name(data, player.getServer(), invitedBy), pending == null ? "" : pending.dungeon,
                pending != null && !pending.leader.equals(player.getUUID()) && !pending.accepted.contains(player.getUUID()),
                pending != null && pending.leader.equals(player.getUUID()));
    }

    public static void sync(ServerPlayer player) { ModNetwork.syncParty(player); }

    private static String name(PartySavedData data, MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        return online == null ? data.names.getOrDefault(id, id.toString()) : online.getGameProfile().getName();
    }

    private static void syncParty(MinecraftServer server, Set<UUID> party) {
        party.forEach(id -> { ServerPlayer player = server.getPlayerList().getPlayer(id); if (player != null) sync(player); });
    }

    private static void notifyParty(MinecraftServer server, Set<UUID> party, String text) {
        party.forEach(id -> { ServerPlayer player = server.getPlayerList().getPlayer(id); if (player != null) message(player, text); });
    }

    private static void message(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
