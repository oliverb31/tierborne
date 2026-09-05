package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.crafting.ArmorUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Tierborne.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Tierborne.MOD_ID);

    public static final RegistryObject<RecipeType<ArmorUpgradeRecipe>> ARMOR_UPGRADE_TYPE =
            RECIPE_TYPES.register("armor_upgrade", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Tierborne.MOD_ID + ":armor_upgrade";
                }
            });
    public static final RegistryObject<RecipeSerializer<ArmorUpgradeRecipe>> ARMOR_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("armor_upgrade", ArmorUpgradeRecipe.Serializer::new);

    private ModRecipes() {}
}
