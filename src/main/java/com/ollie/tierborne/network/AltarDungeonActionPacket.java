package com.ollie.tierborne.network;

import com.ollie.tierborne.block.OrcishAltarCoreBlock;
import com.ollie.tierborne.dungeon.DungeonManager;
import com.ollie.tierborne.dungeon.DungeonPartyManager;
import com.ollie.tierborne.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AltarDungeonActionPacket(BlockPos altarPosition, boolean party) {
    private static final double MAX_DISTANCE_SQUARED = 64.0D;

    public static void encode(AltarDungeonActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.altarPosition);
        buffer.writeBoolean(packet.party);
    }

    public static AltarDungeonActionPacket decode(FriendlyByteBuf buffer) {
        return new AltarDungeonActionPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(AltarDungeonActionPacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            BlockPos position = packet.altarPosition;
            if (player.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D,
                    position.getZ() + 0.5D) > MAX_DISTANCE_SQUARED
                    || !player.level.getBlockState(position).is(ModBlocks.ORCISH_ALTAR_CORE.get())
                    || !OrcishAltarCoreBlock.isStructureValid(player.level, position)) {
                player.displayClientMessage(Component.translatable(
                        "message.tierborne.orcish_altar_core.unavailable"), true);
                return;
            }

            if (packet.party) {
                DungeonPartyManager.offerDungeon(player, DungeonManager.ORC_LUSH_DUNGEON);
            } else {
                DungeonManager.startSolo(player, DungeonManager.ORC_LUSH_DUNGEON);
            }
        });
        context.setPacketHandled(true);
    }
}
