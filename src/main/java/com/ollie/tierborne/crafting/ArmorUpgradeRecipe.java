package com.ollie.tierborne.crafting;

import com.google.gson.JsonObject;
import com.ollie.tierborne.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public final class ArmorUpgradeRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient material;
    private final ItemStack result;

    public ArmorUpgradeRecipe(ResourceLocation id, Ingredient template, Ingredient base,
                              Ingredient material, ItemStack result) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.material = material;
        this.result = result;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return template.test(container.getItem(0))
                && base.test(container.getItem(1))
                && material.test(container.getItem(2));
    }

    @Override
    public ItemStack assemble(Container container) {
        ItemStack baseStack = container.getItem(1);
        ItemStack upgradedStack = result.copy();
        upgradedStack.setCount(1);

        if (baseStack.hasTag()) {
            upgradedStack.setTag(baseStack.getTag().copy());
        }

        if (baseStack.isDamageableItem() && upgradedStack.isDamageableItem()) {
            double wear = (double) baseStack.getDamageValue() / baseStack.getMaxDamage();
            int upgradedDamage = (int) Math.round(wear * upgradedStack.getMaxDamage());
            upgradedStack.setDamageValue(Math.min(upgradedDamage, upgradedStack.getMaxDamage() - 1));
        }

        return upgradedStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(template);
        ingredients.add(base);
        ingredients.add(material);
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ARMOR_UPGRADE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ARMOR_UPGRADE_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<ArmorUpgradeRecipe> {
        @Override
        public ArmorUpgradeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient template = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "template"));
            Ingredient base = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "base"));
            Ingredient material = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "material"));
            JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultJson, "item"));
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Unknown armour upgrade result item " + itemId);
            }
            int count = GsonHelper.getAsInt(resultJson, "count", 1);
            return new ArmorUpgradeRecipe(recipeId, template, base, material, new ItemStack(item, count));
        }

        @Nullable
        @Override
        public ArmorUpgradeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new ArmorUpgradeRecipe(recipeId, Ingredient.fromNetwork(buffer), Ingredient.fromNetwork(buffer),
                    Ingredient.fromNetwork(buffer), buffer.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ArmorUpgradeRecipe recipe) {
            recipe.template.toNetwork(buffer);
            recipe.base.toNetwork(buffer);
            recipe.material.toNetwork(buffer);
            buffer.writeItem(recipe.result);
        }
    }
}
