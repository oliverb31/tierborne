package com.ollie.tierborne.network;

import com.ollie.tierborne.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenOrcishAltarScreenPacket(BlockPos altarPosition, boolean structureComplete) {
    public static void encode(OpenOrcishAltarScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.altarPosition);
        buffer.writeBoolean(packet.structureComplete);
    }

    public static OpenOrcishAltarScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenOrcishAltarScreenPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(OpenOrcishAltarScreenPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openOrcishAltarScreen(
                        packet.altarPosition, packet.structureComplete)));
        context.setPacketHandled(true);
    }
}
