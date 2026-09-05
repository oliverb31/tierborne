package com.ollie.tierborne.item;

import com.ollie.tierborne.dungeon.DungeonMarkerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class DungeonMarkerWandItem extends Item {
    public DungeonMarkerWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.CONSUME;
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            player.displayClientMessage(Component.literal(
                    "Right-click the top face of the floor block where the mob should stand."), true);
            return InteractionResult.CONSUME;
        }
        DungeonMarkerManager.openEditor(player, context.getClickedPos());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a floor block in dungeon editing mode.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Choose the mob that will spawn above that block.")
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}
