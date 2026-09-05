package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientUppercutState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncUppercutPacket(int entityId, int durationTicks) {
    public static void encode(SyncUppercutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static SyncUppercutPacket decode(FriendlyByteBuf buffer) {
        return new SyncUppercutPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncUppercutPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientUppercutState.start(packet.entityId, packet.durationTicks));
        context.setPacketHandled(true);
    }
}
