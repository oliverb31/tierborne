package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.world.inventory.ArmorUpgradeMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ArmorUpgradeScreen extends AbstractContainerScreen<ArmorUpgradeMenu> {
    public ArmorUpgradeScreen(ArmorUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        GuiComponent.fill(poseStack, leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3,
                0xFF8B8B8B);
        GuiComponent.fill(poseStack, leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4,
                0xFFC6C6C6);

        drawSlot(poseStack, 25, 34);
        drawSlot(poseStack, 61, 34);
        drawSlot(poseStack, 97, 34);
        drawSlot(poseStack, 142, 34);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(poseStack, 7 + column * 18, 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(poseStack, 7 + column * 18, 141);
        }
        font.draw(poseStack, "+", leftPos + 49, topPos + 39, 0x404040);
        font.draw(poseStack, "+", leftPos + 85, topPos + 39, 0x404040);
        font.draw(poseStack, "→", leftPos + 121, topPos + 39, 0x404040);

        renderGhostItem(poseStack, 0, new ItemStack(Items.PAPER), 26, 35);
        renderGhostItem(poseStack, 1, new ItemStack(Items.IRON_CHESTPLATE), 62, 35);
        renderGhostItem(poseStack, 2, new ItemStack(Items.IRON_INGOT), 98, 35);
    }

    private void renderGhostItem(PoseStack poseStack, int slotIndex, ItemStack icon, int x, int y) {
        if (menu.getSlot(slotIndex).hasItem()) return;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.35F, 0.35F, 0.35F, 0.45F);
        itemRenderer.renderAndDecorateFakeItem(icon, leftPos + x, topPos + y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.fill(poseStack, leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16,
                0x99505050);
    }

    private void drawSlot(PoseStack poseStack, int x, int y) {
        GuiComponent.fill(poseStack, leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, 0xFF373737);
        GuiComponent.fill(poseStack, leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17,
                0xFF8B8B8B);
    }
}
