package com.ollie.tierborne.network;

import com.ollie.tierborne.TierborneEvents;
import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ToggleMovementSpeedPacket {
    public ToggleMovementSpeedPacket() {}

    public static void encode(ToggleMovementSpeedPacket packet, FriendlyByteBuf buffer) {}

    public static ToggleMovementSpeedPacket decode(FriendlyByteBuf buffer) {
        return new ToggleMovementSpeedPacket();
    }

    public static void handle(ToggleMovementSpeedPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
            PlayerProgress progress = data.get(player.getUUID());
            boolean enabled = progress.toggleModdedMovementSpeed();
            data.changed();
            TierborneEvents.applySkillEffects(player, progress);
            ModNetwork.sync(player);
            player.displayClientMessage(Component.translatable(enabled
                    ? "message.tierborne.modded_speed_enabled"
                    : "message.tierborne.modded_speed_disabled"), true);
        });
        context.setPacketHandled(true);
    }
}
