package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.network.ChoosePlayerClassPacket;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.playerclass.PlayerClass;
import com.ollie.tierborne.playerclass.PlayerClassRegistry;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PlayerClassSelectionScreen extends Screen {
    private int page;
    private int panelLeft, panelRight, panelTop, panelBottom;

    public PlayerClassSelectionScreen() { super(Component.literal("Choose Your Class")); }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);
        List<PlayerClass> classes = PlayerClassRegistry.all();
        if (classes.isEmpty()) return;
        page = Math.floorMod(page, classes.size());
        PlayerClass playerClass = classes.get(page);
        int panelWidth = Math.min(390, width - 24), panelHeight = Math.min(330, height - 24);
        panelLeft = width / 2 - panelWidth / 2; panelRight = panelLeft + panelWidth;
        panelTop = height / 2 - panelHeight / 2; panelBottom = panelTop + panelHeight;
        RpgUi.panel(poseStack, panelLeft, panelTop, panelRight, panelBottom);

        int arrowY = panelTop + 15;
        renderArrow(poseStack, panelLeft + 16, arrowY, false, mouseX, mouseY);
        renderArrow(poseStack, panelRight - 34, arrowY, true, mouseX, mouseY);
        RpgUi.drawCenteredFitted(poseStack, font, Component.literal((page + 1) + " / " + classes.size()), width / 2, arrowY + 5, 80, RpgUi.MUTED);
        RpgUi.drawCenteredFitted(poseStack, font, Component.literal(playerClass.displayName().toUpperCase()), width / 2, panelTop + 34, panelWidth - 40, RpgUi.GOLD);
        GuiComponent.fill(poseStack, width / 2 - 25, panelTop + 47, width / 2 + 25, panelTop + 83, 0xFF11141A);
        RpgUi.border(poseStack, width / 2 - 25, panelTop + 47, width / 2 + 25, panelTop + 83, RpgUi.GOLD_DARK);
        RpgUi.classIcon(minecraft, playerClass.iconStack(), width / 2, panelTop + 65);
        int descriptionBottom = RpgUi.drawWrapped(poseStack, font, playerClass.description(), panelLeft + 25, panelTop + 90, panelWidth - 50, RpgUi.TEXT);
        int subclassesY = Math.max(panelTop + 122, descriptionBottom + 6);
        RpgUi.drawCenteredFitted(poseStack, font, Component.literal("SUBCLASSES"), width / 2, subclassesY, panelWidth - 40, RpgUi.GOLD);
        int nameY = subclassesY + 11;
        for (String name : playerClass.subclassPreviewNames()) {
            if (nameY >= panelBottom - 35) break;
            RpgUi.drawCenteredFitted(poseStack, font, Component.literal(name), width / 2, nameY, panelWidth - 40, RpgUi.TEXT);
            nameY += 10;
        }
        int selectLeft = width / 2 - 55, selectTop = panelBottom - 32;
        boolean hovered = RpgUi.inside(mouseX, mouseY, selectLeft, selectTop, selectLeft + 110, selectTop + 20);
        GuiComponent.fill(poseStack, selectLeft, selectTop, selectLeft + 110, selectTop + 20, hovered ? 0xFF8D6C34 : 0xFF594424);
        RpgUi.border(poseStack, selectLeft, selectTop, selectLeft + 110, selectTop + 20, RpgUi.GOLD);
        RpgUi.drawCenteredFitted(poseStack, font, Component.literal("SELECT"), width / 2, selectTop + 6, 96, RpgUi.TEXT);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderArrow(PoseStack poseStack, int left, int top, boolean right, int mouseX, int mouseY) {
        boolean hovered = RpgUi.inside(mouseX, mouseY, left, top, left + 18, top + 18);
        GuiComponent.fill(poseStack, left, top, left + 18, top + 18, hovered ? 0xFF554326 : 0xFF232730);
        RpgUi.border(poseStack, left, top, left + 18, top + 18, hovered ? RpgUi.GOLD : RpgUi.GOLD_DARK);
        GuiComponent.drawCenteredString(poseStack, font, Component.literal(right ? ">" : "<"), left + 9, top + 5, RpgUi.GOLD);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        List<PlayerClass> classes = PlayerClassRegistry.all();
        if (classes.isEmpty()) return true;
        int arrowY = panelTop + 15;
        if (RpgUi.inside(mouseX, mouseY, panelLeft + 16, arrowY, panelLeft + 34, arrowY + 18)) { page = Math.floorMod(page - 1, classes.size()); return true; }
        if (RpgUi.inside(mouseX, mouseY, panelRight - 34, arrowY, panelRight - 16, arrowY + 18)) { page = (page + 1) % classes.size(); return true; }
        int selectTop = panelBottom - 32;
        if (RpgUi.inside(mouseX, mouseY, width / 2 - 55, selectTop, width / 2 + 55, selectTop + 20)) {
            ModNetwork.CHANNEL.sendToServer(new ChoosePlayerClassPacket(classes.get(page).id())); onClose();
        }
        return true;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}
