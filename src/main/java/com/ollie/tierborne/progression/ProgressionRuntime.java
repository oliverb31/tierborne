package com.ollie.tierborne.progression;

import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class ProgressionRuntime {
    private static final int EXPLORATION_CHECK_TICKS = 200;
    private static final int EXPLORATION_EXPERIENCE = 1;

    private ProgressionRuntime() {}

    public static void tick(ServerPlayer player) {
        clearVanillaExperience(player);
        if (player.tickCount % 20 == 0) stripEnchantments(player);
        if (player.tickCount % EXPLORATION_CHECK_TICKS != 0) return;

        PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
        PlayerProgress progress = data.get(player.getUUID());
        String chunkKey = player.level.dimension().location() + "|"
                + player.chunkPosition().x + "|" + player.chunkPosition().z;
        if (progress.discoverChunk(chunkKey)) {
            award(player, data, progress, EXPLORATION_EXPERIENCE,
                    Component.literal("Exploration"));
        }
    }

    public static void rewardHostileKill(ServerPlayer player, LivingEntity defeated) {
        if (defeated.getType().getCategory() != MobCategory.MONSTER) return;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(defeated.getType());
        if (entityId == null) return;
        int baseExperience = Math.max(2, Math.min(25, Math.round(defeated.getMaxHealth() / 8.0F)));
        PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
        PlayerProgress progress = data.get(player.getUUID());
        int adjustedExperience = progress.applyHostileKillDiminishingReturns(
                entityId.toString(), player.level.getGameTime(), baseExperience);
        if (adjustedExperience > 0) {
            award(player, data, progress, adjustedExperience, defeated.getDisplayName());
        }
    }

    public static void clearVanillaExperience(ServerPlayer player) {
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
    }

    public static void stripEnchantments(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            ItemStack sanitized = sanitize(current);
            if (sanitized != current) {
                player.getInventory().setItem(slot, sanitized);
                changed = true;
            } else if (removeEnchantmentTags(current)) {
                changed = true;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        ItemStack sanitizedCarried = sanitize(carried);
        if (sanitizedCarried != carried) {
            player.containerMenu.setCarried(sanitizedCarried);
            changed = true;
        } else if (removeEnchantmentTags(carried)) {
            changed = true;
        }
        if (changed) player.containerMenu.broadcastChanges();
    }

    public static ItemStack sanitize(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!stack.is(Items.ENCHANTED_BOOK)) return stack;
        ItemStack book = new ItemStack(Items.BOOK, stack.getCount());
        if (stack.hasCustomHoverName()) book.setHoverName(stack.getHoverName());
        return book;
    }

    public static boolean removeEnchantmentTags(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null) return false;
        boolean changed = stack.getTag().contains("Enchantments")
                || stack.getTag().contains("StoredEnchantments");
        stack.removeTagKey("Enchantments");
        stack.removeTagKey("StoredEnchantments");
        return changed;
    }

    private static void award(ServerPlayer player, PlayerProgressSavedData data,
                              PlayerProgress progress, int amount, Component source) {
        if (progress.level() >= PlayerProgress.MAX_LEVEL) return;
        int levelsGained = progress.addProgressionExperience(amount);
        data.changed();
        ModNetwork.sync(player);
        if (levelsGained > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.tierborne.level_up", progress.level(), progress.skillPoints()));
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.tierborne.progression_xp", amount, source), true);
        }
    }
}
