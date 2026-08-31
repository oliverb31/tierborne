package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientBlockState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncBlockStatePacket(int entityId, boolean blocking) {
    public static void encode(SyncBlockStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.blocking);
    }

    public static SyncBlockStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncBlockStatePacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SyncBlockStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientBlockState.set(packet.entityId, packet.blocking));
        context.setPacketHandled(true);
    }
}
