package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;

final class RpgTabBar {
    private static final int GAP = 4;

    private RpgTabBar() {}

    static void render(PoseStack poseStack, Font font, int screenWidth, int top,
                       RpgTab active, int mouseX, int mouseY) {
        int x = left(screenWidth);
        for (RpgTab tab : RpgTab.values()) {
            boolean hovered = RpgUi.inside(mouseX, mouseY, x, top, x + tab.width, top + 20);
            int background = tab == active ? 0xFF554326 : hovered ? 0xFF353A44 : 0xFF232730;
            GuiComponent.fill(poseStack, x, top, x + tab.width, top + 20, background);
            RpgUi.border(poseStack, x, top, x + tab.width, top + 20,
                    tab == active ? RpgUi.GOLD : RpgUi.LOCKED);
            RpgUi.drawCenteredFitted(poseStack, font, Component.literal(tab.label),
                    x + tab.width / 2, top + 6, tab.width - 8, tab == active ? RpgUi.GOLD : RpgUi.TEXT);
            x += tab.width + GAP;
        }
    }

    static boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int top, RpgTab active) {
        int x = left(screenWidth);
        for (RpgTab tab : RpgTab.values()) {
            if (RpgUi.inside(mouseX, mouseY, x, top, x + tab.width, top + 20)) {
                if (tab != active) open(tab);
                return true;
            }
            x += tab.width + GAP;
        }
        return false;
    }

    private static int left(int screenWidth) {
        int total = java.util.Arrays.stream(RpgTab.values()).mapToInt(tab -> tab.width).sum()
                + GAP * (RpgTab.values().length - 1);
        return screenWidth / 2 - total / 2;
    }

    private static void open(RpgTab tab) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (tab) {
            case PLAYER -> minecraft.setScreen(new PlayerMenuScreen());
            case CLASS_SKILLTREE -> minecraft.setScreen(new SkillTreeScreen(false));
            case GENERAL_SKILLTREE -> minecraft.setScreen(new SkillTreeScreen(true));
        }
    }
}
