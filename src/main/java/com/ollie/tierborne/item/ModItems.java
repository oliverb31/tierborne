package com.ollie.tierborne.item;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
        registerArmorSet("runic", ModArmorMaterial.RUNIC);
        registerArmorSet("steel", ModArmorMaterial.STEEL);
        registerArmorSet("tungsten", ModArmorMaterial.TUNGSTEN);

        registerArmorSet("mithril", ModArmorMaterial.MITHRIL);
        registerArmorSet("uru", ModArmorMaterial.URU);
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
        ARMOR_ITEMS.add(ITEMS.register(name, () -> new ArmorItem(material, slot,
                new Item.Properties().tab(TIERBORNE_TAB))));
    }

    private static RegistryObject<Item> registerBasicItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().tab(TIERBORNE_TAB)));
    }
}
