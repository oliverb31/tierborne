package com.ollie.tierborne.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DungeonPartyManager {
    private static final Map<UUID, Set<UUID>> PARTIES = new HashMap<>();
    private static final Map<UUID, UUID> LEADERS = new HashMap<>();
    private static final Map<UUID, UUID> INVITES = new HashMap<>();

    private DungeonPartyManager() {
    }

    public static void invite(ServerPlayer inviter, ServerPlayer invited) {
        UUID leader = LEADERS.getOrDefault(inviter.getUUID(), inviter.getUUID());
        if (!leader.equals(inviter.getUUID())) {
            inviter.sendSystemMessage(Component.literal("Only the party leader can invite players."));
            return;
        }
        if (inviter == invited || LEADERS.containsKey(invited.getUUID())) {
            inviter.sendSystemMessage(Component.literal("That player is already in a party."));
            return;
        }
        PARTIES.computeIfAbsent(leader, ignored -> {
            Set<UUID> members = new LinkedHashSet<>();
            members.add(leader);
            LEADERS.put(leader, leader);
            return members;
        });
        INVITES.put(invited.getUUID(), leader);
        inviter.sendSystemMessage(Component.literal("Invited " + invited.getGameProfile().getName() + " to the dungeon party."));
        invited.sendSystemMessage(Component.literal(inviter.getGameProfile().getName()
                + " invited you to a dungeon party. Use /tierborne party accept "
                + inviter.getGameProfile().getName() + "."));
    }

    public static void accept(ServerPlayer player, ServerPlayer leaderPlayer) {
        UUID invitedBy = INVITES.get(player.getUUID());
        if (invitedBy == null || !invitedBy.equals(leaderPlayer.getUUID())) {
            player.sendSystemMessage(Component.literal("You do not have an invitation from that player."));
            return;
        }
        Set<UUID> party = PARTIES.get(invitedBy);
        if (party == null) {
            player.sendSystemMessage(Component.literal("That party no longer exists."));
            INVITES.remove(player.getUUID());
            return;
        }
        party.add(player.getUUID());
        LEADERS.put(player.getUUID(), invitedBy);
        INVITES.remove(player.getUUID());
        party.forEach(memberId -> {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
            if (member != null) member.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " joined the dungeon party."));
        });
    }

    public static void leave(ServerPlayer player) {
        UUID leader = LEADERS.remove(player.getUUID());
        if (leader == null) {
            player.sendSystemMessage(Component.literal("You are not in a dungeon party."));
            return;
        }
        Set<UUID> party = PARTIES.get(leader);
        if (party == null) return;
        if (leader.equals(player.getUUID())) {
            PARTIES.remove(leader);
            for (UUID memberId : party) {
                LEADERS.remove(memberId);
                ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) member.sendSystemMessage(Component.literal("The dungeon party was disbanded."));
            }
        } else {
            party.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("You left the dungeon party."));
        }
    }

    public static List<ServerPlayer> membersForStart(ServerPlayer leader) {
        UUID partyLeader = LEADERS.get(leader.getUUID());
        if (partyLeader != null && !partyLeader.equals(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("Only the party leader can start a dungeon."));
            return List.of();
        }
        Set<UUID> ids = PARTIES.getOrDefault(leader.getUUID(), Set.of(leader.getUUID()));
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayer member = leader.getServer().getPlayerList().getPlayer(id);
            if (member == null || member.level != leader.level || member.distanceToSqr(leader) > 16.0D * 16.0D) {
                leader.sendSystemMessage(Component.literal("Every party member must be online and within 16 blocks to start."));
                return List.of();
            }
            members.add(member);
        }
        return List.copyOf(members);
    }
}
