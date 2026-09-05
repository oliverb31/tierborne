package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.client.ClientPartyState;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.PartyActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DungeonInvitationScreen extends Screen {
    public DungeonInvitationScreen() {
        super(Component.literal("Dungeon Invitation"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = height / 2 + 26;
        addRenderableWidget(new Button(centerX - 104, buttonY, 100, 20,
                Component.literal("Join Dungeon"), button -> join()));
        addRenderableWidget(new Button(centerX + 4, buttonY, 100, 20,
                Component.literal("Not Now"), button -> onClose()));
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
        String leader = ClientPartyState.leader();
        drawCenteredString(poseStack, font, Component.literal(leader.isEmpty()
                        ? "Your party is preparing to enter."
                        : leader + " is preparing to enter."),
                centerX, panelTop + 58, RpgUi.MUTED);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private static void join() {
        ModNetwork.CHANNEL.sendToServer(new PartyActionPacket("join", ""));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
