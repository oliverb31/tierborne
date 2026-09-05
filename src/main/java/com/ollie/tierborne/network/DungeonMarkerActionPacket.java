package com.ollie.tierborne.network;

import com.ollie.tierborne.dungeon.DungeonMarkerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DungeonMarkerActionPacket(BlockPos floorPosition, String action) {
    public static void encode(DungeonMarkerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.floorPosition);
        buffer.writeUtf(packet.action, 64);
    }

    public static DungeonMarkerActionPacket decode(FriendlyByteBuf buffer) {
        return new DungeonMarkerActionPacket(buffer.readBlockPos(), buffer.readUtf(64));
    }

    public static void handle(DungeonMarkerActionPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) DungeonMarkerManager.applyAction(player, packet.floorPosition, packet.action);
        });
        context.setPacketHandled(true);
    }
}
