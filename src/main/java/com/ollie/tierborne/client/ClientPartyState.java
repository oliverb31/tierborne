package com.ollie.tierborne.client;

import com.ollie.tierborne.client.screen.DungeonInvitationScreen;
import com.ollie.tierborne.network.SyncPartyPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientPartyState {
    private static String leader = "";
    private static List<String> members = List.of();
    private static String invitedBy = "";
    private static String pendingDungeon = "";
    private static boolean canJoin;
    private static boolean canBegin;
    private static boolean invitationPromptShown;

    private ClientPartyState() {}

    public static void receive(SyncPartyPacket packet) {
        boolean newInvitation = packet.canJoin()
                && (!canJoin || !packet.pendingDungeon().equals(pendingDungeon));
        leader = packet.leader();
        members = List.copyOf(packet.members());
        invitedBy = packet.invitedBy();
        pendingDungeon = packet.pendingDungeon();
        canJoin = packet.canJoin();
        canBegin = packet.canBegin();
        if (newInvitation || !canJoin) invitationPromptShown = false;
    }

    public static String leader() { return leader; }
    public static List<String> members() { return members; }
    public static String invitedBy() { return invitedBy; }
    public static String pendingDungeon() { return pendingDungeon; }
    public static boolean canJoin() { return canJoin; }
    public static boolean canBegin() { return canBegin; }

    public static void tryOpenDungeonInvitation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canJoin || invitationPromptShown || minecraft.player == null || minecraft.screen != null) return;
        invitationPromptShown = true;
        minecraft.setScreen(new DungeonInvitationScreen());
    }

    public static void clear() {
        leader = "";
        members = List.of();
        invitedBy = "";
        pendingDungeon = "";
        canJoin = false;
        canBegin = false;
        invitationPromptShown = false;
    }
}
