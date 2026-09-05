package com.ollie.tierborne.dungeon;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.entity.OrcMob;
import com.ollie.tierborne.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OrcDungeonLoot {
    private OrcDungeonLoot() {
    }

    public static void dropFor(OrcMob orc) {
        if (!(orc.level instanceof ServerLevel level)
                || !orc.getPersistentData().getBoolean("tierborne:dungeon_marker_spawn")) return;
        Optional<DungeonSavedData.Instance> found = DungeonManager.instanceAt(
                level.getServer(), orc.getX(), orc.getZ());
        if (found.isEmpty() || !DungeonManager.ORC_LUSH_DUNGEON.equals(found.get().dungeon)) return;

        RandomSource random = orc.getRandom();
        switch (orc.kind()) {
            case BOSS -> {
                orc.spawnAtLocation(new ItemStack(ModItems.ORC_HEART.get()));
                orc.spawnAtLocation(new ItemStack(ModItems.TIER_3_ARMOR_TEMPLATE.get()));
                dropChance(orc, ModItems.ORC_LORD_HAMMER.get(),
                        RpgBalanceConfig.ORC_BOSS_HAMMER_DROP_CHANCE.get(), random);
            }
            case ELITE -> dropChance(orc, ModItems.ORC_ELITE_AXE.get(),
                    RpgBalanceConfig.ORC_ELITE_AXE_DROP_CHANCE.get(), random);
            case SHAMAN -> {
                dropChance(orc, ModItems.SHAMANIC_ROBE.get(),
                        RpgBalanceConfig.SHAMANIC_ROBE_DROP_CHANCE.get(), random);
                dropChance(orc, ModItems.TIER_2_ARMOR_TEMPLATE.get(),
                        RpgBalanceConfig.ORC_SHAMAN_TIER_2_DROP_CHANCE.get(), random);
            }
            case SPEARTHROWER -> {
                dropChance(orc, ModItems.ORC_BOW.get(),
                        RpgBalanceConfig.ORC_BOW_DROP_CHANCE.get(), random);
                dropChance(orc, ModItems.TIER_2_ARMOR_TEMPLATE.get(),
                        RpgBalanceConfig.ORC_ARCHER_TIER_2_DROP_CHANCE.get(), random);
            }
            case WARRIOR -> {
                dropChance(orc, ModItems.ORC_AXE.get(),
                        RpgBalanceConfig.ORC_AXE_DROP_CHANCE.get(), random);
                dropChance(orc, ModItems.TIER_2_ARMOR_TEMPLATE.get(),
                        RpgBalanceConfig.ORC_WARRIOR_TIER_2_DROP_CHANCE.get(), random);
            }
        }
    }

    public static void populateChest(ServerLevel level, BlockPos position, BlockState state,
                                     DungeonSavedData.Instance instance) {
        if (instance.authoring || !DungeonManager.ORC_LUSH_DUNGEON.equals(instance.dungeon)
                || !(state.getBlock() instanceof ChestBlock)) return;
        if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) return;

        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof ChestBlockEntity chest)) return;

        RandomSource random = RandomSource.create(instance.seed ^ position.asLong());
        List<ItemStack> loot = new ArrayList<>();
        addChance(loot, new ItemStack(Items.ARROW, between(random, 8, 24)),
                RpgBalanceConfig.ORC_CHEST_ARROW_CHANCE.get(), random);
        addChance(loot, new ItemStack(Items.BREAD, between(random, 1, 4)),
                RpgBalanceConfig.ORC_CHEST_BREAD_CHANCE.get(), random);
        addChance(loot, new ItemStack(ModItems.TIER_2_ARMOR_TEMPLATE.get()),
                RpgBalanceConfig.ORC_CHEST_TIER_2_CHANCE.get(), random);
        if (chance(RpgBalanceConfig.ORC_CHEST_MOB_DROP_CHANCE.get(), random)) {
            loot.add(randomMobDrop(random));
        }
        addChance(loot, new ItemStack(Items.DIAMOND),
                RpgBalanceConfig.ORC_CHEST_DIAMOND_CHANCE.get(), random);
        addChance(loot, new ItemStack(ModItems.TIER_3_ARMOR_TEMPLATE.get()),
                RpgBalanceConfig.ORC_CHEST_TIER_3_CHANCE.get(), random);
        addChance(loot, new ItemStack(Items.GOLDEN_APPLE, between(random, 1, 2)),
                RpgBalanceConfig.ORC_CHEST_GOLDEN_APPLE_CHANCE.get(), random);

        chest.clearContent();
        List<Integer> freeSlots = new ArrayList<>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) freeSlots.add(slot);
        for (ItemStack stack : loot) {
            int selected = random.nextInt(freeSlots.size());
            chest.setItem(freeSlots.remove(selected), stack);
        }
        chest.setChanged();
    }

    private static void dropChance(OrcMob orc, net.minecraft.world.item.Item item,
                                   double percentage, RandomSource random) {
        if (chance(percentage, random)) orc.spawnAtLocation(new ItemStack(item));
    }

    private static void addChance(List<ItemStack> loot, ItemStack stack,
                                  double percentage, RandomSource random) {
        if (chance(percentage, random)) loot.add(stack);
    }

    private static boolean chance(double percentage, RandomSource random) {
        return random.nextDouble() * 100.0D < percentage;
    }

    private static int between(RandomSource random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static ItemStack randomMobDrop(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> new ItemStack(ModItems.ORC_AXE.get());
            case 1 -> new ItemStack(ModItems.ORC_BOW.get());
            case 2 -> new ItemStack(ModItems.SHAMANIC_ROBE.get());
            default -> new ItemStack(ModItems.ORC_ELITE_AXE.get());
        };
    }
}
