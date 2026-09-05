package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientPartyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncPartyPacket(String leader, List<String> members, String invitedBy,
                              String pendingDungeon, boolean canJoin, boolean canBegin) {
    public static void encode(SyncPartyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.leader, 64);
        buffer.writeCollection(packet.members, (target, name) -> target.writeUtf(name, 64));
        buffer.writeUtf(packet.invitedBy, 64);
        buffer.writeUtf(packet.pendingDungeon, 64);
        buffer.writeBoolean(packet.canJoin);
        buffer.writeBoolean(packet.canBegin);
    }

    public static SyncPartyPacket decode(FriendlyByteBuf buffer) {
        return new SyncPartyPacket(buffer.readUtf(64), buffer.readCollection(ArrayList::new, source -> source.readUtf(64)),
                buffer.readUtf(64), buffer.readUtf(64), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SyncPartyPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPartyState.receive(packet));
        context.setPacketHandled(true);
    }
}
