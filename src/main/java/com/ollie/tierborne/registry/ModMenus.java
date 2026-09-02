package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.world.inventory.ArmorUpgradeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Tierborne.MOD_ID);
    public static final RegistryObject<MenuType<ArmorUpgradeMenu>> ARMOR_UPGRADE =
            MENU_TYPES.register("armor_upgrade", () -> IForgeMenuType.create(ArmorUpgradeMenu::new));

    private ModMenus() {}
}
