package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.AltarDungeonActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public final class OrcishAltarScreen extends Screen {
    private final BlockPos altarPosition;

    public OrcishAltarScreen(BlockPos altarPosition) {
        super(Component.literal("Orcish Altar Core"));
        this.altarPosition = altarPosition;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = height / 2 + 26;
        addRenderableWidget(new Button(centerX - 104, buttonY, 100, 20,
                Component.literal("Solo"), button -> choose(false)));
        addRenderableWidget(new Button(centerX + 4, buttonY, 100, 20,
                Component.literal("Party"), button -> choose(true)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);
        int centerX = width / 2;
        int panelLeft = Math.max(12, centerX - 150);
        int panelRight = Math.min(width - 12, centerX + 150);
        int panelTop = Math.max(16, height / 2 - 74);
        int panelBottom = Math.min(height - 16, height / 2 + 76);
        RpgUi.panel(poseStack, panelLeft, panelTop, panelRight, panelBottom);
        drawCenteredString(poseStack, font, title, centerX, panelTop + 18, RpgUi.GOLD);
        drawCenteredString(poseStack, font, Component.literal("Orc Lush Dungeon"),
                centerX, panelTop + 42, RpgUi.TEXT);
        drawCenteredString(poseStack, font,
                Component.literal("Enter alone or invite your permanent party."),
                centerX, panelTop + 58, RpgUi.MUTED);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void choose(boolean party) {
        ModNetwork.CHANNEL.sendToServer(new AltarDungeonActionPacket(altarPosition, party));
        Minecraft.getInstance().setScreen(party ? new PartyScreen() : null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
