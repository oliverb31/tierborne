package com.ollie.tierborne.world.inventory;

import com.ollie.tierborne.crafting.ArmorUpgradeRecipe;
import com.ollie.tierborne.registry.ModMenus;
import com.ollie.tierborne.registry.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.List;

public final class ArmorUpgradeMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT_COUNT = 3;
    private static final int RESULT_SLOT = 3;
    private static final int PLAYER_INVENTORY_START = 4;
    private static final int PLAYER_INVENTORY_END = 40;

    private final Container inputSlots = new SimpleContainer(INPUT_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArmorUpgradeMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Level level;
    @Nullable
    private ArmorUpgradeRecipe selectedArmorRecipe;
    @Nullable
    private UpgradeRecipe selectedVanillaRecipe;
    private boolean consumingInputs;

    public ArmorUpgradeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level, buffer.readBlockPos()));
    }

    public ArmorUpgradeMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ARMOR_UPGRADE.get(), containerId);
        this.access = access;
        this.level = inventory.player.level;

        addSlot(new Slot(inputSlots, 0, 26, 35));
        addSlot(new Slot(inputSlots, 1, 62, 35));
        addSlot(new Slot(inputSlots, 2, 98, 35));
        addSlot(new Slot(resultSlots, 0, 143, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return selectedArmorRecipe != null && selectedArmorRecipe.matches(inputSlots, level)
                        || selectedVanillaRecipe != null && selectedVanillaRecipe.matches(vanillaSmithingInputs(), level);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                takeResult(player, stack);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == inputSlots && !consumingInputs) {
            createResult();
        }
    }

    private void createResult() {
        List<ArmorUpgradeRecipe> recipes = level.getRecipeManager()
                .getRecipesFor(ModRecipes.ARMOR_UPGRADE_TYPE.get(), inputSlots, level);
        if (!recipes.isEmpty()) {
            selectedArmorRecipe = recipes.get(0);
            selectedVanillaRecipe = null;
            resultSlots.setRecipeUsed(selectedArmorRecipe);
            resultSlots.setItem(0, selectedArmorRecipe.assemble(inputSlots));
            return;
        }

        selectedArmorRecipe = null;
        if (!inputSlots.getItem(0).isEmpty()) {
            selectedVanillaRecipe = null;
            resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }

        SimpleContainer vanillaInputs = vanillaSmithingInputs();
        List<UpgradeRecipe> vanillaRecipes = level.getRecipeManager()
                .getRecipesFor(RecipeType.SMITHING, vanillaInputs, level);
        if (vanillaRecipes.isEmpty()) {
            selectedVanillaRecipe = null;
            resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }

        selectedVanillaRecipe = vanillaRecipes.get(0);
        resultSlots.setRecipeUsed(selectedVanillaRecipe);
        resultSlots.setItem(0, selectedVanillaRecipe.assemble(vanillaInputs));
    }

    private void takeResult(Player player, ItemStack stack) {
        stack.onCraftedBy(player.level, player, stack.getCount());
        resultSlots.awardUsedRecipes(player);
        consumingInputs = true;
        resultSlots.setItem(0, ItemStack.EMPTY);
        int firstConsumedSlot = selectedArmorRecipe == null ? 1 : 0;
        for (int slot = firstConsumedSlot; slot < INPUT_SLOT_COUNT; slot++) {
            inputSlots.removeItem(slot, 1);
        }
        consumingInputs = false;
        createResult();
        access.execute((level, pos) -> level.levelEvent(1044, pos, 0));
    }

    private SimpleContainer vanillaSmithingInputs() {
        SimpleContainer vanillaInputs = new SimpleContainer(2);
        vanillaInputs.setItem(0, inputSlots.getItem(1));
        vanillaInputs.setItem(1, inputSlots.getItem(2));
        return vanillaInputs;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, INPUT_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.SMITHING_TABLE);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputSlots));
    }
}
