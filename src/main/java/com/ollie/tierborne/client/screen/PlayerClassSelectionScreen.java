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
    private static final int CARD_WIDTH = 138;
    private static final int CARD_HEIGHT = 135;
    private static final int CARD_GAP = 10;
    private String selectedPlayerClassId;

    public PlayerClassSelectionScreen() {
        super(Component.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        List<PlayerClass> playerClasses = PlayerClassRegistry.all();
        if (selectedPlayerClassId == null && playerClasses.size() == 1) {
            selectedPlayerClassId = playerClasses.get(0).id();
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);

        int panelLeft = Math.max(12, width / 2 - Math.min(430, width - 24) / 2);
        int panelRight = width - panelLeft;
        int panelTop = Math.max(12, height / 2 - Math.min(330, height - 24) / 2);
        int panelBottom = height - panelTop;
        RpgUi.panel(poseStack, panelLeft, panelTop, panelRight, panelBottom);

        drawCenteredString(poseStack, font, title, width / 2, panelTop + 18, RpgUi.GOLD);
        drawCenteredString(poseStack, font, Component.literal("Your path will shape every battle ahead"),
                width / 2, panelTop + 34, RpgUi.MUTED);

        for (int i = 0; i < PlayerClassRegistry.all().size(); i++) {
            renderCard(poseStack, PlayerClassRegistry.all().get(i), i, mouseX, mouseY, panelTop);
        }

        int confirmWidth = 130;
        int confirmLeft = width / 2 - confirmWidth / 2;
        int confirmTop = panelBottom - 34;
        boolean enabled = selectedPlayerClassId != null;
        boolean hovered = RpgUi.inside(mouseX, mouseY, confirmLeft, confirmTop, confirmLeft + confirmWidth, confirmTop + 20);
        GuiComponent.fill(poseStack, confirmLeft, confirmTop, confirmLeft + confirmWidth, confirmTop + 20,
                enabled ? (hovered ? 0xFF8D6C34 : 0xFF594424) : 0xFF292B30);
        RpgUi.border(poseStack, confirmLeft, confirmTop, confirmLeft + confirmWidth, confirmTop + 20,
                enabled ? RpgUi.GOLD : RpgUi.LOCKED);
        drawCenteredString(poseStack, font, Component.literal("CONFIRM CLASS"), width / 2, confirmTop + 6,
                enabled ? RpgUi.TEXT : RpgUi.MUTED);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderCard(PoseStack poseStack, PlayerClass playerClass, int index,
                            int mouseX, int mouseY, int panelTop) {
        CardBounds card = cardBounds(index, panelTop);
        boolean selected = playerClass.id().equals(selectedPlayerClassId);
        boolean hovered = card.contains(mouseX, mouseY);
        int background = selected ? 0xFF343024 : hovered ? 0xFF292E38 : RpgUi.PANEL_LIGHT;
        GuiComponent.fill(poseStack, card.left, card.top, card.right, card.bottom, background);
        RpgUi.border(poseStack, card.left, card.top, card.right, card.bottom,
                selected ? RpgUi.GOLD : hovered ? 0xFFB59250 : 0xFF50545C);

        int portraitLeft = card.centerX() - 27;
        int portraitTop = card.top + 9;
        GuiComponent.fill(poseStack, portraitLeft, portraitTop, portraitLeft + 54, portraitTop + 42, 0xFF11141A);
        RpgUi.border(poseStack, portraitLeft, portraitTop, portraitLeft + 54, portraitTop + 42,
                selected ? RpgUi.GOLD : RpgUi.GOLD_DARK);
        RpgUi.classIcon(minecraft, playerClass.iconStack(), card.centerX(), portraitTop + 21);

        drawCenteredString(poseStack, font, Component.literal(playerClass.displayName()),
                card.centerX(), card.top + 59, selected ? RpgUi.GOLD : RpgUi.TEXT);
        RpgUi.drawWrapped(poseStack, font, playerClass.description(), card.left + 10, card.top + 75,
                CARD_WIDTH - 20, RpgUi.MUTED, 4);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int panelTop = Math.max(12, height / 2 - Math.min(330, height - 24) / 2);
        List<PlayerClass> playerClasses = PlayerClassRegistry.all();
        for (int i = 0; i < playerClasses.size(); i++) {
            if (cardBounds(i, panelTop).contains(mouseX, mouseY)) {
                selectedPlayerClassId = playerClasses.get(i).id();
                return true;
            }
        }

        int panelBottom = height - panelTop;
        if (selectedPlayerClassId != null && RpgUi.inside(mouseX, mouseY,
                width / 2 - 65, panelBottom - 34, width / 2 + 65, panelBottom - 14)) {
            ModNetwork.CHANNEL.sendToServer(new ChoosePlayerClassPacket(selectedPlayerClassId));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private CardBounds cardBounds(int index, int panelTop) {
        int columns = Math.max(1, Math.min(PlayerClassRegistry.all().size(),
                Math.max(1, (width - 24 + CARD_GAP) / (CARD_WIDTH + CARD_GAP))));
        int column = index % columns;
        int row = index / columns;
        int rowCount = (PlayerClassRegistry.all().size() + columns - 1) / columns;
        int totalWidth = columns * CARD_WIDTH + (columns - 1) * CARD_GAP;
        int totalHeight = rowCount * CARD_HEIGHT + (rowCount - 1) * CARD_GAP;
        int left = width / 2 - totalWidth / 2 + column * (CARD_WIDTH + CARD_GAP);
        int top = panelTop + 44 + Math.max(0, (height - panelTop * 2 - 94 - totalHeight) / 2)
                + row * (CARD_HEIGHT + CARD_GAP);
        return new CardBounds(left, top, left + CARD_WIDTH, top + CARD_HEIGHT);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private record CardBounds(int left, int top, int right, int bottom) {
        boolean contains(double x, double y) { return RpgUi.inside(x, y, left, top, right, bottom); }
        int centerX() { return (left + right) / 2; }
    }
}
