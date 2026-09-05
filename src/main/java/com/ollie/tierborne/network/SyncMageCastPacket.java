package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientMageCastState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMageCastPacket(int entityId, int durationTicks, int style) {
    public static void encode(SyncMageCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.style);
    }

    public static SyncMageCastPacket decode(FriendlyByteBuf buffer) {
        return new SyncMageCastPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncMageCastPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientMageCastState.start(
                packet.entityId, packet.durationTicks, packet.style));
        context.setPacketHandled(true);
    }
}
