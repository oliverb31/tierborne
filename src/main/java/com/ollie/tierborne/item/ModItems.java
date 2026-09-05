package com.ollie.tierborne.item;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.ollie.tierborne.entity.ModEntities;
import com.ollie.tierborne.registry.ModBlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModItems {
    public static final CreativeModeTab TIERBORNE_TAB = new CreativeModeTab("tierborne") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        }
    };

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Tierborne.MOD_ID);

    private static final List<RegistryObject<Item>> ARMOR_ITEMS = new ArrayList<>();

    public static final RegistryObject<Item> SILVER_INGOT = registerBasicItem("silver_ingot");
    public static final RegistryObject<Item> RUNIC_INGOT = registerBasicItem("runic_ingot");
    public static final RegistryObject<Item> STEEL_INGOT = registerBasicItem("steel_ingot");
    public static final RegistryObject<Item> TUNGSTEN_INGOT = registerBasicItem("tungsten_ingot");
    public static final RegistryObject<Item> MITHRIL_INGOT = registerBasicItem("mithril_ingot");
    public static final RegistryObject<Item> URU_INGOT = registerBasicItem("uru_ingot");
    public static final RegistryObject<Item> ORICHALCUM_INGOT = registerBasicItem("orichalcum_ingot");
    public static final RegistryObject<Item> ADAMANTITE_INGOT = registerBasicItem("adamantite_ingot");
    public static final RegistryObject<Item> ORC_ELITE_AXE = ITEMS.register("orc_elite_axe",
            () -> new AxeItem(Tiers.IRON, 6.0F, -3.1F,
                    new Item.Properties().tab(TIERBORNE_TAB)));
    public static final RegistryObject<Item> ORCISH_ALTAR_CORE = ITEMS.register("orcish_altar_core",
            () -> new BlockItem(ModBlocks.ORCISH_ALTAR_CORE.get(),
                    new Item.Properties().tab(TIERBORNE_TAB)));
    public static final RegistryObject<Item> DUNGEON_MARKER_WAND = ITEMS.register("dungeon_marker_wand",
            () -> new DungeonMarkerWandItem(new Item.Properties().tab(TIERBORNE_TAB).stacksTo(1)));

    public static final RegistryObject<Item> DUNE_REVENANT_SPAWN_EGG = registerSpawnEgg(
            "dune_revenant_spawn_egg", () -> ModEntities.DUNE_REVENANT.get(), 0x75634A, 0x8CC6A3);
    public static final RegistryObject<Item> FROSTBOUND_ARCHER_SPAWN_EGG = registerSpawnEgg(
            "frostbound_archer_spawn_egg", () -> ModEntities.FROSTBOUND_ARCHER.get(), 0xA6BEC8, 0x354A56);
    public static final RegistryObject<Item> RUNEBOUND_COLOSSUS_SPAWN_EGG = registerSpawnEgg(
            "runebound_colossus_spawn_egg", () -> ModEntities.RUNEBOUND_COLOSSUS.get(), 0x292B2F, 0xB33C2E);
    public static final RegistryObject<Item> ABYSSAL_WATCHER_SPAWN_EGG = registerSpawnEgg(
            "abyssal_watcher_spawn_egg", () -> ModEntities.ABYSSAL_WATCHER.get(), 0x87957F, 0x266B70);
    public static final RegistryObject<Item> GOOFY_GOBLIN_SPAWN_EGG = registerSpawnEgg(
            "goofy_goblin_spawn_egg", () -> ModEntities.GOOFY_GOBLIN.get(), 0x4D9991, 0x302936);

    public static final RegistryObject<Item> COPPER_UPGRADE_TEMPLATE = registerBasicItem("copper_upgrade_template");
    public static final RegistryObject<Item> CHAINMAIL_UPGRADE_TEMPLATE = registerBasicItem("chainmail_upgrade_template");
    public static final RegistryObject<Item> SILVER_UPGRADE_TEMPLATE = registerBasicItem("silver_upgrade_template");
    public static final RegistryObject<Item> RUNIC_UPGRADE_TEMPLATE = registerBasicItem("runic_upgrade_template");
    public static final RegistryObject<Item> STEEL_UPGRADE_TEMPLATE = registerBasicItem("steel_upgrade_template");
    public static final RegistryObject<Item> TUNGSTEN_UPGRADE_TEMPLATE = registerBasicItem("tungsten_upgrade_template");
    public static final RegistryObject<Item> MITHRIL_UPGRADE_TEMPLATE = registerBasicItem("mithril_upgrade_template");
    public static final RegistryObject<Item> URU_UPGRADE_TEMPLATE = registerBasicItem("uru_upgrade_template");
    public static final RegistryObject<Item> ORICHALCUM_UPGRADE_TEMPLATE = registerBasicItem("orichalcum_upgrade_template");
    public static final RegistryObject<Item> ADAMANTITE_UPGRADE_TEMPLATE = registerBasicItem("adamantite_upgrade_template");

    static {
        registerArmorSet("copper", ModArmorMaterial.COPPER);

        registerArmorSet("silver", ModArmorMaterial.SILVER);
        registerArmorSet("uru", ModArmorMaterial.URU);
        registerArmorSet("steel", ModArmorMaterial.STEEL);
        registerArmorSet("tungsten", ModArmorMaterial.TUNGSTEN);

        registerArmorSet("mithril", ModArmorMaterial.MITHRIL);
        registerArmorSet("runic", ModArmorMaterial.RUNIC);
        registerArmorSet("orichalcum", ModArmorMaterial.ORICHALCUM);
        registerArmorSet("adamantite", ModArmorMaterial.ADAMANTITE);
    }

    private ModItems() {}

    public static List<RegistryObject<Item>> armorItems() {
        return Collections.unmodifiableList(ARMOR_ITEMS);
    }

    private static void registerArmorSet(String materialName, ModArmorMaterial material) {
        registerArmorPiece(materialName + "_helmet", material, EquipmentSlot.HEAD);
        registerArmorPiece(materialName + "_chestplate", material, EquipmentSlot.CHEST);
        registerArmorPiece(materialName + "_leggings", material, EquipmentSlot.LEGS);
        registerArmorPiece(materialName + "_boots", material, EquipmentSlot.FEET);
    }

    private static void registerArmorPiece(String name, ModArmorMaterial material, EquipmentSlot slot) {
        ARMOR_ITEMS.add(ITEMS.register(name, () -> new TierborneArmorItem(material, slot,
                new Item.Properties().tab(TIERBORNE_TAB))));
    }

    private static RegistryObject<Item> registerBasicItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().tab(TIERBORNE_TAB)));
    }

    private static RegistryObject<Item> registerSpawnEgg(String name,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> entityType,
            int primaryColor, int secondaryColor) {
        return ITEMS.register(name, () -> new ForgeSpawnEggItem(entityType, primaryColor, secondaryColor,
                new Item.Properties().tab(TIERBORNE_TAB)));
    }
}
