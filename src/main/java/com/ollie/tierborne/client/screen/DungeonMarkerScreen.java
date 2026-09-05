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
    private final boolean tartarusPage;

    public DungeonMarkerScreen(BlockPos floorPosition, int existingMarkers) {
        this(floorPosition, existingMarkers, false);
    }

    private DungeonMarkerScreen(BlockPos floorPosition, int existingMarkers, boolean tartarusPage) {
        super(Component.literal("Dungeon Mob Marker"));
        this.floorPosition = floorPosition;
        this.existingMarkers = existingMarkers;
        this.tartarusPage = tartarusPage;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 2 - 66;
        if (tartarusPage) {
            String[][] mobs = {
                    {"Frostmite", DungeonMarkerManager.FROSTMITE},
                    {"Frozen Blaze", DungeonMarkerManager.FROZEN_BLAZE},
                    {"Gnut", DungeonMarkerManager.GNUT},
                    {"Ice Witch", DungeonMarkerManager.ICE_WITCH},
                    {"Iceologer", DungeonMarkerManager.ICEOLOGER},
                    {"Snow Spirit", DungeonMarkerManager.SNOWBALL_SPIRIT},
                    {"Undead Warrior", DungeonMarkerManager.UNDEAD_ICE_WARRIOR},
                    {"Yeti", DungeonMarkerManager.TARTARUS_YETI},
                    {"Knight Shield", DungeonMarkerManager.ICE_KNIGHT_MINION_SHIELD},
                    {"Knight Spear", DungeonMarkerManager.ICE_KNIGHT_MINION_SPEAR},
                    {"Knight Sword", DungeonMarkerManager.ICE_KNIGHT_MINION_SWORD},
                    {"Ice Knight Boss", DungeonMarkerManager.ICE_KNIGHT}
            };
            for (int index = 0; index < mobs.length; index++) {
                addMobButton(centerX - 148 + index % 3 * 100, top + index / 3 * 24,
                        mobs[index][0], mobs[index][1], 96);
            }
        } else {
            addMobButton(centerX - 104, top, "Orc Warrior", DungeonMarkerManager.ORC_WARRIOR);
            addMobButton(centerX + 4, top, "Orc Spearthrower", DungeonMarkerManager.ORC_SPEARTHROWER);
            addMobButton(centerX - 104, top + 24, "Orc Shaman", DungeonMarkerManager.ORC_SHAMAN);
            addMobButton(centerX + 4, top + 24, "Orc Elite", DungeonMarkerManager.ORC_ELITE);
            addMobButton(centerX - 50, top + 48, "Orc Boss", DungeonMarkerManager.ORC_BOSS);
        }
        int controlsY = top + 102;
        addRenderableWidget(new Button(centerX - 148, controlsY, 96, 20,
                Component.literal(tartarusPage ? "Orc Mobs" : "Tartarus Mobs"),
                button -> Minecraft.getInstance().setScreen(
                        new DungeonMarkerScreen(floorPosition, existingMarkers, !tartarusPage))));
        addRenderableWidget(new Button(centerX - 48, controlsY, 96, 20,
                Component.literal("Remove Last"), button -> choose("remove_last")));
        addRenderableWidget(new Button(centerX + 52, controlsY, 96, 20,
                Component.literal("Clear Block"), button -> choose("clear")));
    }

    private void addMobButton(int x, int y, String name, String id) {
        addMobButton(x, y, name, id, 100);
    }

    private void addMobButton(int x, int y, String name, String id, int width) {
        addRenderableWidget(new Button(x, y, width, 20, Component.literal(name), button -> choose(id)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);
        int centerX = width / 2;
        int panelLeft = Math.max(8, centerX - 158);
        int panelRight = Math.min(width - 8, centerX + 158);
        int panelTop = Math.max(8, height / 2 - 106);
        int panelBottom = Math.min(height - 8, height / 2 + 72);
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
