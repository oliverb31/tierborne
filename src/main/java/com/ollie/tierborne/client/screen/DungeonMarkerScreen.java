package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.dungeon.DungeonMarkerManager;
import com.ollie.tierborne.network.DungeonMarkerActionPacket;
import com.ollie.tierborne.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DungeonMarkerScreen extends Screen {
    private final BlockPos floorPosition;
    private final int existingMarkers;

    public DungeonMarkerScreen(BlockPos floorPosition, int existingMarkers) {
        super(Component.literal("Dungeon Mob Marker"));
        this.floorPosition = floorPosition;
        this.existingMarkers = existingMarkers;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 2 - 54;
        addMobButton(centerX - 104, top, "Orc Warrior", DungeonMarkerManager.ORC_WARRIOR);
        addMobButton(centerX + 4, top, "Orc Spearthrower", DungeonMarkerManager.ORC_SPEARTHROWER);
        addMobButton(centerX - 104, top + 24, "Orc Shaman", DungeonMarkerManager.ORC_SHAMAN);
        addMobButton(centerX + 4, top + 24, "Orc Elite", DungeonMarkerManager.ORC_ELITE);
        addMobButton(centerX - 50, top + 48, "Orc Boss", DungeonMarkerManager.ORC_BOSS);
        addRenderableWidget(new Button(centerX - 104, top + 78, 100, 20,
                Component.literal("Remove Last"), button -> choose("remove_last")));
        addRenderableWidget(new Button(centerX + 4, top + 78, 100, 20,
                Component.literal("Clear Block"), button -> choose("clear")));
    }

    private void addMobButton(int x, int y, String name, String id) {
        addRenderableWidget(new Button(x, y, 100, 20, Component.literal(name), button -> choose(id)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);
        int centerX = width / 2;
        int panelLeft = Math.max(12, centerX - 150);
        int panelRight = Math.min(width - 12, centerX + 150);
        int panelTop = Math.max(10, height / 2 - 94);
        int panelBottom = Math.min(height - 10, height / 2 + 76);
        RpgUi.panel(poseStack, panelLeft, panelTop, panelRight, panelBottom);
        drawCenteredString(poseStack, font, title, centerX, panelTop + 15, RpgUi.GOLD);
        drawCenteredString(poseStack, font,
                Component.literal("Mob will stand above block " + floorPosition.toShortString()),
                centerX, panelTop + 31, RpgUi.TEXT);
        drawCenteredString(poseStack, font,
                Component.literal(existingMarkers + " marker" + (existingMarkers == 1 ? "" : "s") + " already on this block"),
                centerX, panelTop + 45, RpgUi.MUTED);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void choose(String action) {
        ModNetwork.CHANNEL.sendToServer(new DungeonMarkerActionPacket(floorPosition, action));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
