package com.ollie.tierborne.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

/** Vanilla armor sets that are intentionally absent from Tierborne's armor progression. */
public final class RemovedVanillaArmor {
    private static final Set<Item> ITEMS = Set.of(
            Items.IRON_HELMET,
            Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS,
            Items.IRON_BOOTS,
            Items.GOLDEN_HELMET,
            Items.GOLDEN_CHESTPLATE,
            Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS
    );

    private RemovedVanillaArmor() {
    }

    public static boolean contains(ItemStack stack) {
        return !stack.isEmpty() && ITEMS.contains(stack.getItem());
    }

    public static void hideFromCreativeTabs() {
        ITEMS.forEach(item -> item.category = null);
    }
}
