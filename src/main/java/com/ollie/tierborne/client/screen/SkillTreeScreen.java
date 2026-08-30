package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.client.ClientProgress;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.UnlockSkillPacket;
import com.ollie.tierborne.playerclass.PlayerClass;
import com.ollie.tierborne.playerclass.PlayerClassRegistry;
import com.ollie.tierborne.playerclass.GeneralSkillTree;
import com.ollie.tierborne.playerclass.Skill;
import com.ollie.tierborne.playerclass.SkillTreeDefinition;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Set;

public final class SkillTreeScreen extends Screen {
    private static final int NODE_RADIUS = 25;
    private static final double MODAL_Z = 800.0D;
    private static final int MODAL_HEIGHT = 205;
    private Set<String> knownUnlocked = Set.of();
    private String purchaseFlashSkillId;
    private int purchaseFlashTicks;
    private int scrollOffset;
    private Skill pendingSkill;
    private final boolean generalTree;

    public SkillTreeScreen() {
        this(false);
    }

    public SkillTreeScreen(boolean generalTree) {
        super(Component.literal("Skill Tree"));
        this.generalTree = generalTree;
    }

    public boolean isGeneralTree() {
        return generalTree;
    }

    @Override
    protected void init() {
        knownUnlocked = ClientProgress.unlockedSkills();
    }

    @Override
    public void tick() {
        super.tick();
        Set<String> current = ClientProgress.unlockedSkills();
        for (String skillId : current) {
            if (!knownUnlocked.contains(skillId)) {
                purchaseFlashSkillId = skillId;
                purchaseFlashTicks = 18;
            }
        }
        knownUnlocked = current;
        if (purchaseFlashTicks > 0) purchaseFlashTicks--;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);

        int left = 10;
        int top = 10;
        int right = width - 10;
        int bottom = height - 10;
        RpgUi.panel(poseStack, left, top, right, bottom);
        SkillTreeDefinition skillTree = currentTree();
        if (skillTree == null) {
            drawCenteredString(poseStack, font, Component.literal("No playerClass selected"),
                    width / 2, height / 2, RpgUi.MUTED);
            return;
        }

        RpgTabBar.render(poseStack, font, width, top + 5,
                generalTree ? RpgTab.GENERAL_SKILLTREE : RpgTab.CLASS_SKILLTREE, mouseX, mouseY);
        drawCenteredString(poseStack, font, Component.literal(skillTree.displayName() + " Skill Tree"),
                width / 2, top + 32, RpgUi.GOLD);
        drawString(poseStack, font, Component.literal("Skill Points"), left + 13, top + 32, RpgUi.MUTED);
        drawString(poseStack, font, Component.literal(Integer.toString(ClientProgress.skillPoints())),
                left + 13, top + 44, RpgUi.GOLD);
        GuiComponent.fill(poseStack, left + 10, top + 57, right - 10, top + 58, RpgUi.GOLD_DARK);

        int viewportTop = top + 59;
        int viewportBottom = bottom - 8;
        applyTreeScissor(left + 3, viewportTop, right - 3, viewportBottom);

        Skill hoveredSkill = null;
        try {
        for (Skill child : skillTree.skills()) {
            NodePosition childPosition = nodePosition(skillTree, child);
            for (String prerequisiteId : child.prerequisites()) {
                Skill prerequisite = skillTree.findSkill(prerequisiteId);
                if (prerequisite == null) continue;
                NodePosition parentPosition = nodePosition(skillTree, prerequisite);
                boolean pathUnlocked = ClientProgress.hasSkill(prerequisite.id());
                RpgUi.line(poseStack, parentPosition.x, parentPosition.y,
                        childPosition.x, childPosition.y,
                        pathUnlocked ? RpgUi.UNLOCKED : RpgUi.LOCKED);
            }
        }

        if (pendingSkill == null && RpgUi.inside(mouseX, mouseY, left + 3, viewportTop, right - 3, viewportBottom)) {
            for (Skill skill : skillTree.skills()) {
                NodePosition position = nodePosition(skillTree, skill);
                if (position.contains(mouseX, mouseY)) hoveredSkill = skill;
            }
        }
        Set<String> previewLockouts = previewLockouts(skillTree, hoveredSkill);
        for (Skill skill : skillTree.skills()) {
            NodePosition position = nodePosition(skillTree, skill);
            renderNode(poseStack, skill, position, skill == hoveredSkill,
                    previewLockouts.contains(skill.id()), pendingSkill == null);
        }
        } finally {
            RenderSystem.disableScissor();
        }

        if (hoveredSkill != null && pendingSkill == null) {
            renderInformationPanelForeground(poseStack, skillTree, hoveredSkill, left, right, bottom);
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
        if (pendingSkill != null) renderConfirmationModalForeground(poseStack, skillTree, mouseX, mouseY);
    }

    private void applyTreeScissor(int left, int top, int right, int bottom) {
        double scale = minecraft.getWindow().getGuiScale();
        int x = (int)Math.floor(left * scale);
        int y = (int)Math.floor(minecraft.getWindow().getHeight() - bottom * scale);
        int scissorWidth = (int)Math.ceil((right - left) * scale);
        int scissorHeight = (int)Math.ceil((bottom - top) * scale);
        RenderSystem.enableScissor(x, y, Math.max(0, scissorWidth), Math.max(0, scissorHeight));
    }

    private void renderInformationPanelForeground(PoseStack poseStack, SkillTreeDefinition tree, Skill skill,
                                                  int left, int right, int bottom) {
        minecraft.renderBuffers().bufferSource().endBatch();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        poseStack.pushPose();
        try {
            poseStack.translate(0.0D, 0.0D, 600.0D);
            renderInformationPanel(poseStack, tree, skill, left, right, bottom);
            minecraft.renderBuffers().bufferSource().endBatch();
        } finally {
            poseStack.popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
        }
    }

    /**
     * Item models can submit buffered geometry with GUI depth values above normal
     * blits. Flush that work first, then draw the complete modal without depth
     * testing so every modal element is guaranteed to cover the skill tree.
     */
    private void renderConfirmationModalForeground(PoseStack poseStack, SkillTreeDefinition skillTree,
                                                   int mouseX, int mouseY) {
        minecraft.renderBuffers().bufferSource().endBatch();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.pushPose();
        try {
            poseStack.translate(0.0D, 0.0D, MODAL_Z);
            renderConfirmationModal(poseStack, skillTree, mouseX, mouseY);
            minecraft.renderBuffers().bufferSource().endBatch();
        } finally {
            poseStack.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
        }
    }

    private void renderNode(PoseStack poseStack, Skill skill, NodePosition position,
                            boolean hovered, boolean previewLocked, boolean renderIcon) {
        NodeState state = stateOf(skill);
        int stateColor = switch (state) {
            case LOCKED -> RpgUi.LOCKED;
            case AVAILABLE -> RpgUi.AVAILABLE;
            case UNLOCKED -> RpgUi.UNLOCKED;
            case UNAVAILABLE -> 0xFF713D43;
        };
        if (previewLocked) stateColor = 0xFF555860;
        if (skill.id().equals(purchaseFlashSkillId) && purchaseFlashTicks > 0) {
            int pulse = purchaseFlashTicks % 6 < 3 ? 4 : 2;
            RpgUi.node(poseStack, position.x, position.y, NODE_RADIUS + pulse, RpgUi.GOLD, true);
        }
        RpgUi.node(poseStack, position.x, position.y, NODE_RADIUS, stateColor, hovered);
        if (renderIcon) {
            RpgUi.skillIcon(poseStack, minecraft, skill.nodeIcon(), position.x, position.y);
        }
        if (previewLocked || state == NodeState.UNAVAILABLE) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, 500.0D);
            GuiComponent.fill(poseStack, position.x - 17, position.y - 17,
                    position.x + 17, position.y + 17, 0x78000000);
            drawCenteredString(poseStack, font, Component.literal("X"),
                    position.x, position.y - 4, 0xFFFF5555);
            poseStack.popPose();
        }

        if (!skill.automaticallyUnlocked()) {
            GuiComponent.fill(poseStack, position.x + 13, position.y + 12, position.x + 28, position.y + 27, 0xFF101218);
            RpgUi.border(poseStack, position.x + 13, position.y + 12, position.x + 28, position.y + 27, stateColor);
            drawCenteredString(poseStack, font, Component.literal(Integer.toString(skill.cost())),
                    position.x + 21, position.y + 16, RpgUi.TEXT);
        }
    }

    private Set<String> previewLockouts(SkillTreeDefinition tree, Skill hoveredSkill) {
        if (hoveredSkill == null || hoveredSkill.mutuallyExclusiveWith().isEmpty()) return Set.of();
        Set<String> affected = new java.util.HashSet<>(hoveredSkill.mutuallyExclusiveWith());
        boolean changed;
        do {
            changed = false;
            for (Skill skill : tree.skills()) {
                if (!affected.contains(skill.id())
                        && skill.prerequisites().stream().anyMatch(affected::contains)) {
                    changed |= affected.add(skill.id());
                }
            }
        } while (changed);
        return affected;
    }

    private void renderInformationPanel(PoseStack poseStack, SkillTreeDefinition skillTree, Skill skill,
                                        int screenLeft, int screenRight, int screenBottom) {
        int panelWidth = Math.min(300, screenRight - screenLeft - 24);
        int panelLeft = screenRight - panelWidth - 8;
        int panelTop = Math.max(68, screenBottom - 112);
        GuiComponent.fill(poseStack, panelLeft, panelTop, screenRight - 8, screenBottom - 8, 0xF0222630);
        RpgUi.border(poseStack, panelLeft, panelTop, screenRight - 8, screenBottom - 8, RpgUi.GOLD_DARK);
        drawString(poseStack, font, Component.literal(skill.displayName()), panelLeft + 8, panelTop + 7, RpgUi.GOLD);
        int detailY = RpgUi.drawWrapped(poseStack, font, skill.description(), panelLeft + 8, panelTop + 20,
                panelWidth - 16, RpgUi.TEXT, 2);
        for(String line:SkillDetailLines.forSkill(skill,skillTree,ClientProgress.unlockedSkills())){drawString(poseStack,font,Component.literal(line),panelLeft+8,detailY+1,RpgUi.GOLD);detailY+=11;}
        String status = switch (stateOf(skill)) {
            case UNLOCKED -> "Unlocked";
            case AVAILABLE -> "Click node to unlock - Cost: " + skill.cost() + " SP";
            case LOCKED -> missingRequirementText(skillTree, skill);
            case UNAVAILABLE -> "Permanently unavailable";
        };
        drawString(poseStack, font, Component.literal(status), panelLeft + 8, screenBottom - 19,
                stateOf(skill) == NodeState.LOCKED ? RpgUi.MUTED : RpgUi.UNLOCKED);
    }

    private String missingRequirementText(SkillTreeDefinition playerClass, Skill skill) {
        if (ClientProgress.skillPoints() < skill.cost()) return "Not enough skill points";
        String missing = skill.prerequisites().stream().filter(id -> !ClientProgress.hasSkill(id))
                .map(id -> playerClass.findSkill(id)).filter(java.util.Objects::nonNull)
                .map(Skill::displayName).findFirst().orElse("another skill");
        return "Requires: " + missing;
    }

    private void renderConfirmationModal(PoseStack poseStack, SkillTreeDefinition playerClass, int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, 0, 0, width, height, 0xB0000000);
        int modalWidth = Math.min(310, width - 30);
        int modalHeight = Math.min(MODAL_HEIGHT, height - 20);
        int left = width / 2 - modalWidth / 2;
        int top = height / 2 - modalHeight / 2;
        int right = left + modalWidth;
        int bottom = top + modalHeight;
        RpgUi.panel(poseStack, left, top, right, bottom);
        drawCenteredString(poseStack, font, Component.literal("Unlock Skill?"), width / 2, top + 13, RpgUi.GOLD);
        drawString(poseStack, font, Component.literal(pendingSkill.displayName()), left + 14, top + 34, RpgUi.TEXT);
        RpgUi.drawWrapped(poseStack, font, pendingSkill.description(), left + 14, top + 49,
                modalWidth - 28, RpgUi.MUTED, 2);
        int numericY=top+75;
        for(String line:SkillDetailLines.forSkill(pendingSkill,playerClass,ClientProgress.unlockedSkills())){drawString(poseStack,font,Component.literal(line),left+14,numericY,RpgUi.GOLD);numericY+=11;}
        drawString(poseStack, font, Component.literal("Cost: " + pendingSkill.cost() + " Skill Point"),
                left + 14, bottom - 58, RpgUi.GOLD);
        if (!pendingSkill.mutuallyExclusiveWith().isEmpty()) {
            String choices = pendingSkill.mutuallyExclusiveWith().stream()
                    .map(playerClass::findSkill).filter(java.util.Objects::nonNull)
                    .map(Skill::displayName).collect(java.util.stream.Collectors.joining(", "));
            drawString(poseStack, font, Component.literal("Exclusive choice - locks: " + choices),
                    left + 14, bottom - 45, 0xFFE08A8A);
        }

        int buttonTop = bottom - 30;
        renderModalButton(poseStack, "CANCEL", left + 14, buttonTop, left + 114, buttonTop + 18,
                mouseX, mouseY, false);
        renderModalButton(poseStack, "CONFIRM", right - 114, buttonTop, right - 14, buttonTop + 18,
                mouseX, mouseY, true);
    }

    private void renderModalButton(PoseStack poseStack, String label, int left, int top, int right, int bottom,
                                   int mouseX, int mouseY, boolean confirm) {
        boolean hovered = RpgUi.inside(mouseX, mouseY, left, top, right, bottom);
        int background = confirm ? (hovered ? 0xFF6E5630 : 0xFF493A24) : (hovered ? 0xFF454951 : 0xFF30333A);
        GuiComponent.fill(poseStack, left, top, right, bottom, background);
        RpgUi.border(poseStack, left, top, right, bottom, confirm ? RpgUi.GOLD : RpgUi.LOCKED);
        drawCenteredString(poseStack, font, Component.literal(label), (left + right) / 2, top + 5, RpgUi.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingSkill != null) {
            if (button != 0) return true;
            int modalWidth = Math.min(310, width - 30);
            int modalHeight = Math.min(MODAL_HEIGHT, height - 20);
            int left = width / 2 - modalWidth / 2;
            int top = height / 2 - modalHeight / 2;
            int right = left + modalWidth;
            int bottom = top + modalHeight;
            int buttonTop = bottom - 30;
            if (RpgUi.inside(mouseX, mouseY, left + 14, buttonTop, left + 114, buttonTop + 18)) {
                pendingSkill = null;
                return true;
            }
            if (RpgUi.inside(mouseX, mouseY, right - 114, buttonTop, right - 14, buttonTop + 18)) {
                if (stateOf(pendingSkill) == NodeState.AVAILABLE) {
                    ModNetwork.CHANNEL.sendToServer(new UnlockSkillPacket(pendingSkill.id()));
                }
                pendingSkill = null;
                return true;
            }
            return true;
        }
        if (RpgTabBar.mouseClicked(mouseX, mouseY, width, 15,
                generalTree ? RpgTab.GENERAL_SKILLTREE : RpgTab.CLASS_SKILLTREE)) return true;
        if (button == 0) {
            if (!RpgUi.inside(mouseX, mouseY, 13, 69, width - 13, height - 18)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            SkillTreeDefinition skillTree = currentTree();
            if (skillTree != null) {
                for (Skill skill : skillTree.skills()) {
                    if (!nodePosition(skillTree, skill).contains(mouseX, mouseY)) continue;
                    if (stateOf(skill) == NodeState.AVAILABLE) {
                        pendingSkill = skill;
                    }
                        return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pendingSkill != null) return true;
        scrollOffset += (int) Math.signum(delta) * 18;
        scrollOffset = Math.max(-240, Math.min(240, scrollOffset));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pendingSkill != null && keyCode == 256) {
            pendingSkill = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private NodeState stateOf(Skill skill) {
        if (ClientProgress.hasSkill(skill.id())) return NodeState.UNLOCKED;
        SkillTreeDefinition tree = currentTree();
        if (tree != null && isPermanentlyUnavailable(tree, skill, new java.util.HashSet<>())) {
            return NodeState.UNAVAILABLE;
        }
        if (ClientProgress.skillPoints() >= skill.cost()
                && skill.prerequisitesMet(ClientProgress.unlockedSkills())) return NodeState.AVAILABLE;
        return NodeState.LOCKED;
    }

    private boolean isPermanentlyUnavailable(SkillTreeDefinition tree, Skill skill, Set<String> visited) {
        if (!visited.add(skill.id())) return false;
        if (skill.conflictsWith(ClientProgress.unlockedSkills())) return true;
        for (String prerequisiteId : skill.prerequisites()) {
            Skill prerequisite = tree.findSkill(prerequisiteId);
            if (prerequisite != null && isPermanentlyUnavailable(tree, prerequisite, visited)) return true;
        }
        return false;
    }

    private SkillTreeDefinition currentTree() {
        return generalTree ? GeneralSkillTree.INSTANCE : PlayerClassRegistry.get(ClientProgress.playerClassId());
    }

    private NodePosition nodePosition(SkillTreeDefinition playerClass, Skill skill) {
        int minColumn = playerClass.skills().stream().mapToInt(Skill::treeColumn).min().orElse(0);
        int maxColumn = playerClass.skills().stream().mapToInt(Skill::treeColumn).max().orElse(0);
        int columnCount = maxColumn - minColumn + 1;
        int horizontalSpacing = Math.min(110, Math.max(50, (width - 80) / Math.max(1, columnCount - 1)));
        int treeWidth = (columnCount - 1) * horizontalSpacing;
        int x = width / 2 - treeWidth / 2 + (skill.treeColumn() - minColumn) * horizontalSpacing;
        int y = 88 + skill.treeRow() * 53 + scrollOffset;
        return new NodePosition(x, y);
    }

    private enum NodeState { LOCKED, AVAILABLE, UNLOCKED, UNAVAILABLE }

    private record NodePosition(int x, int y) {
        boolean contains(double mouseX, double mouseY) {
            double dx = mouseX - x;
            double dy = mouseY - y;
            return dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS;
        }
    }
}
