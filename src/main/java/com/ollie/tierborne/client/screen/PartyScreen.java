package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.client.ClientPartyState;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.PartyActionPacket;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PartyScreen extends Screen {
    private EditBox inviteName;
    private Button acceptButton;
    private Button joinButton;
    private Button beginButton;
    private Button cancelButton;

    public PartyScreen() { super(Component.literal("Party")); }

    @Override
    protected void init() {
        int center = width / 2;
        inviteName = new EditBox(font, center - 150, 62, 200, 20, Component.literal("Player name"));
        addRenderableWidget(inviteName);
        addRenderableWidget(new Button(center + 56, 62, 94, 20, Component.literal("Invite"),
                button -> send("invite", inviteName.getValue())));
        acceptButton = addRenderableWidget(new Button(center - 150, 92, 145, 20, Component.literal("Accept Party Invite"),
                button -> send("accept", "")));
        addRenderableWidget(new Button(center + 5, 92, 145, 20, Component.literal("Leave / Disband"),
                button -> send("leave", "")));
        joinButton = addRenderableWidget(new Button(center - 150, height - 66, 145, 20, Component.literal("Join Dungeon"),
                button -> send("join", "")));
        beginButton = addRenderableWidget(new Button(center + 5, height - 66, 145, 20, Component.literal("Start Dungeon"),
                button -> send("begin", "")));
        cancelButton = addRenderableWidget(new Button(center - 72, height - 40, 144, 20, Component.literal("Cancel Invitation"),
                button -> send("cancel", "")));
        refreshButtons();
    }

    @Override
    public void tick() {
        inviteName.tick();
        refreshButtons();
    }

    private void refreshButtons() {
        acceptButton.active = !ClientPartyState.invitedBy().isEmpty();
        joinButton.active = ClientPartyState.canJoin();
        beginButton.active = ClientPartyState.canBegin();
        cancelButton.active = ClientPartyState.canBegin();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);
        RpgUi.panel(poseStack, 10, 10, width - 10, height - 10);
        RpgTabBar.render(poseStack, font, width, 15, RpgTab.PARTY, mouseX, mouseY);
        GuiComponent.fill(poseStack, 20, 44, width - 20, 45, RpgUi.GOLD_DARK);
        int center = width / 2;
        String leader = ClientPartyState.leader();
        drawCenteredString(poseStack, font, Component.literal(leader.isEmpty() ? "No permanent party" : "Leader: " + leader),
                center, 122, leader.isEmpty() ? RpgUi.MUTED : RpgUi.GOLD);
        int y = 140;
        for (String member : ClientPartyState.members()) {
            drawCenteredString(poseStack, font, Component.literal(member), center, y, RpgUi.TEXT);
            y += 12;
        }
        if (!ClientPartyState.invitedBy().isEmpty()) {
            drawCenteredString(poseStack, font, Component.literal("Party invite from " + ClientPartyState.invitedBy()),
                    center, height - 94, RpgUi.GOLD);
        }
        if (!ClientPartyState.pendingDungeon().isEmpty()) {
            drawCenteredString(poseStack, font, Component.literal("Pending dungeon: " + ClientPartyState.pendingDungeon()),
                    center, height - 82, RpgUi.GOLD);
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && RpgTabBar.mouseClicked(mouseX, mouseY, width, 15, RpgTab.PARTY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void send(String action, String value) {
        ModNetwork.CHANNEL.sendToServer(new PartyActionPacket(action, value));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
