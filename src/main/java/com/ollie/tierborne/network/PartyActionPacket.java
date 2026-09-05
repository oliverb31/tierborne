package com.ollie.tierborne.network;

import com.ollie.tierborne.dungeon.DungeonPartyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PartyActionPacket(String action, String value) {
    public static void encode(PartyActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action, 32); buffer.writeUtf(packet.value, 64);
    }

    public static PartyActionPacket decode(FriendlyByteBuf buffer) {
        return new PartyActionPacket(buffer.readUtf(32), buffer.readUtf(64));
    }

    public static void handle(PartyActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            switch (packet.action) {
                case "invite" -> {
                    ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(packet.value);
                    if (target == null) player.displayClientMessage(Component.literal("That player is not online."), true);
                    else DungeonPartyManager.invite(player, target);
                }
                case "accept" -> DungeonPartyManager.acceptLatest(player);
                case "leave" -> DungeonPartyManager.leave(player);
                case "join" -> DungeonPartyManager.joinDungeon(player);
                case "begin" -> DungeonPartyManager.beginDungeon(player);
                case "cancel" -> DungeonPartyManager.cancelDungeon(player);
                default -> player.displayClientMessage(Component.literal("Unknown party action."), true);
            }
        });
        context.setPacketHandled(true);
    }
}
