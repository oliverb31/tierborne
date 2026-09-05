package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDungeonMarkerScreenPacket(BlockPos floorPosition, int existingMarkers) {
    public static void encode(OpenDungeonMarkerScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.floorPosition);
        buffer.writeVarInt(packet.existingMarkers);
    }

    public static OpenDungeonMarkerScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenDungeonMarkerScreenPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(OpenDungeonMarkerScreenPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openDungeonMarkerScreen(
                        packet.floorPosition, packet.existingMarkers)));
        context.setPacketHandled(true);
    }
}
