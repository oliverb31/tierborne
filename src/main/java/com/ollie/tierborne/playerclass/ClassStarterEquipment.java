package com.ollie.tierborne.playerclass;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ClassStarterEquipment {
    private ClassStarterEquipment() {}

    public static void grant(ServerPlayer player, String playerClassId) {
        switch (playerClassId) {
            case SwordsmanPlayerClass.ID -> {
                giveOrDrop(player, new ItemStack(Items.WOODEN_SWORD));
                player.displayClientMessage(Component.translatable(
                        "message.tierborne.class_starter.swordsman"), true);
            }
            case ArcherPlayerClass.ID -> {
                giveOrDrop(player, new ItemStack(Items.BOW));
                giveOrDrop(player, new ItemStack(Items.ARROW, 64));
                player.displayClientMessage(Component.translatable(
                        "message.tierborne.class_starter.archer"), true);
            }
            case FighterPlayerClass.ID -> player.displayClientMessage(Component.translatable(
                    "message.tierborne.class_starter.fighter"), true);
            case BarbarianPlayerClass.ID -> {
                giveOrDrop(player, new ItemStack(Items.WOODEN_AXE));
                player.displayClientMessage(Component.translatable(
                        "message.tierborne.class_starter.barbarian"), true);
            }
            case MagePlayerClass.ID -> {
                giveOrDrop(player, new ItemStack(com.ollie.tierborne.item.ModItems.MAGE_STAFF.get()));
                player.displayClientMessage(Component.translatable(
                        "message.tierborne.class_starter.mage"), true);
            }
            default -> {
            }
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
