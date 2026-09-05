package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OrcishAltarSchematicScreen extends Screen {
    private static final int CELL_SIZE = 24;

    public OrcishAltarSchematicScreen() {
        super(Component.literal("Orcish Altar Core Schematic"));
    }

    @Override
    protected void init() {
        addRenderableWidget(new Button(width / 2 - 50, height / 2 + 83, 100, 20,
                Component.literal("Close"), button -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);

        int centerX = width / 2;
        int panelLeft = Math.max(12, centerX - 150);
        int panelRight = Math.min(width - 12, centerX + 150);
        int panelTop = Math.max(8, height / 2 - 108);
        int panelBottom = Math.min(height - 8, height / 2 + 108);
        RpgUi.panel(poseStack, panelLeft, panelTop, panelRight, panelBottom);
        drawCenteredString(poseStack, font, title, centerX, panelTop + 14, RpgUi.GOLD);
        drawCenteredString(poseStack, font,
                Component.literal("Build this one-layer foundation beneath the core"),
                centerX, panelTop + 30, RpgUi.TEXT);

        int gridLeft = centerX - CELL_SIZE * 3 / 2;
        int gridTop = panelTop + 47;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int left = gridLeft + column * CELL_SIZE;
                int top = gridTop + row * CELL_SIZE;
                GuiComponent.fill(poseStack, left, top, left + CELL_SIZE - 2, top + CELL_SIZE - 2,
                        RpgUi.PANEL_LIGHT);
                RpgUi.border(poseStack, left, top, left + CELL_SIZE - 2, top + CELL_SIZE - 2,
                        RpgUi.GOLD_DARK);
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(
                        foundationItem(row, column), left + 3, top + 3);
            }
        }

        int textY = gridTop + CELL_SIZE * 3 + 8;
        drawCenteredString(poseStack, font,
                Component.literal("Corners: Mangrove Logs  •  Edges: Mangrove Slabs"),
                centerX, textY, RpgUi.TEXT);
        drawCenteredString(poseStack, font,
                Component.literal("Centre: Mangrove Roots"), centerX, textY + 13, RpgUi.TEXT);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(
                new ItemStack(com.ollie.tierborne.item.ModItems.ORCISH_ALTAR_CORE.get()),
                centerX - 8, textY + 25);
        drawCenteredString(poseStack, font,
                Component.literal("Place the core directly above the centre roots"),
                centerX, textY + 44, RpgUi.MUTED);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private static ItemStack foundationItem(int row, int column) {
        if (row == 1 && column == 1) return new ItemStack(Items.MANGROVE_ROOTS);
        if (row != 1 && column != 1) return new ItemStack(Items.MANGROVE_LOG);
        return new ItemStack(Items.MANGROVE_SLAB);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
