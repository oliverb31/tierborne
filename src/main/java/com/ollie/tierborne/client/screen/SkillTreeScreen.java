package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class SkillTreeScreen extends Screen {
    private static final int NODE_RADIUS = 25;
    private static final int ROOT_NODE_RADIUS = 35;
    private static final int OWNED_CLASS_BORDER = 0xFF35D6D0;
    private static final double MODAL_Z = 800.0D;
    private static final int MODAL_HEIGHT = 205;
    private static final double KEYBOARD_PAN_SPEED = 6.0D;
    private static final int NODE_HORIZONTAL_SPACING = 90;
    private static final int NODE_VERTICAL_SPACING = 70;
    private static final double MIN_ZOOM = 0.6D;
    private static final double MAX_ZOOM = 1.8D;
    private static final double ZOOM_STEP = 0.1D;
    private static final double DRAG_THRESHOLD = 4.0D;
    private Set<String> knownUnlocked = Set.of();
    private String purchaseFlashSkillId;
    private int purchaseFlashTicks;
    private double scrollOffset;
    private double horizontalOffset;
    private double zoom = 1.0D;
    private boolean emptySpaceDragCandidate;
    private boolean draggingTree;
    private double dragStartX;
    private double dragStartY;
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
        resetCameraToRoot();
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
        handleKeyboardPanning();
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
        RpgUi.drawCenteredFitted(poseStack, font, Component.literal(skillTree.displayName() + " Skill Tree"),
                width / 2, top + 32, Math.max(80,width-190), RpgUi.GOLD);
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
                        pathUnlocked ? RpgUi.UNLOCKED : RpgUi.LOCKED,
                        Math.max(1, (int)Math.round(zoom)));
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
            RpgUi.node(poseStack, position.x, position.y, position.radius + pulse, RpgUi.GOLD, true);
        }
        boolean ownedClassNode=state==NodeState.UNLOCKED&&(skill.automaticallyUnlocked()
                ||skill.effect()==com.ollie.tierborne.playerclass.SkillEffect.CLASS_SUBCLASS
                ||skill.effect()==com.ollie.tierborne.playerclass.SkillEffect.GENERAL_SUBCLASS);
        if(ownedClassNode)RpgUi.node(poseStack,position.x,position.y,position.radius+3,OWNED_CLASS_BORDER,false);
        RpgUi.node(poseStack, position.x, position.y, position.radius, stateColor, hovered);
        if (renderIcon) {
            RpgUi.skillIcon(poseStack, minecraft, skill.nodeIcon(), position.x, position.y,
                    (float)((skill.automaticallyUnlocked() ? 1.4D : 1.0D) * zoom));
        }
        if (previewLocked || state == NodeState.UNAVAILABLE) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, 500.0D);
            int overlayRadius=Math.max(17,position.radius-8);
            GuiComponent.fill(poseStack, position.x - overlayRadius, position.y - overlayRadius,
                    position.x + overlayRadius, position.y + overlayRadius, 0x78000000);
            drawCenteredString(poseStack, font, Component.literal("X"),
                    position.x, position.y - 4, 0xFFFF5555);
            poseStack.popPose();
        }

        if (!skill.automaticallyUnlocked()) {
            int badgeSize=Math.max(12,(int)Math.round(15*zoom));
            int badgeLeft=position.x+Math.max(8,position.radius/2),badgeTop=position.y+Math.max(7,position.radius/2);
            GuiComponent.fill(poseStack, badgeLeft, badgeTop, badgeLeft+badgeSize, badgeTop+badgeSize, 0xFF101218);
            RpgUi.border(poseStack, badgeLeft, badgeTop, badgeLeft+badgeSize, badgeTop+badgeSize, stateColor);
            drawCenteredString(poseStack, font, Component.literal(Integer.toString(skill.cost())),
                    badgeLeft+badgeSize/2, badgeTop+Math.max(2,(badgeSize-font.lineHeight)/2), RpgUi.TEXT);
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
        int panelWidth = Math.min(330, screenRight - screenLeft - 34);
        int panelRight = screenRight - 14;
        int panelLeft = panelRight - panelWidth;
        int textLeft = panelLeft + 10;
        int textWidth = panelWidth - 24;
        java.util.List<net.minecraft.util.FormattedCharSequence> descriptionLines =
                font.split(Component.literal(skill.description()), textWidth);
        java.util.List<String> numericalLines = SkillDetailLines.forSkill(
                skill, skillTree, ClientProgress.unlockedSkills());
        java.util.List<String> prerequisiteLines = prerequisiteLines(skillTree, skill);
        java.util.List<String> detailLines = new java.util.ArrayList<>(prerequisiteLines);
        detailLines.addAll(numericalLines);
        int numericalLineCount = detailLines.stream()
                .mapToInt(line -> font.split(Component.literal(line), textWidth).size()).sum();
        int requiredHeight = 48 + descriptionLines.size() * (font.lineHeight + 2)
                + numericalLineCount * (font.lineHeight + 2);
        int panelTop = Math.max(68, screenBottom - Math.max(112, requiredHeight));
        GuiComponent.fill(poseStack, panelLeft, panelTop, panelRight, screenBottom - 8, 0xF0222630);
        RpgUi.border(poseStack, panelLeft, panelTop, panelRight, screenBottom - 8, RpgUi.GOLD_DARK);
        RpgUi.drawFitted(poseStack, font, Component.literal(skill.displayName()), textLeft, panelTop + 7, textWidth, RpgUi.GOLD);
        int availableLines=Math.max(1,descriptionLines.size());
        int detailY = RpgUi.drawWrapped(poseStack, font, skill.description(), textLeft, panelTop + 20,
                textWidth, RpgUi.TEXT,availableLines);
        for (String line : detailLines) {
            if(detailY+font.lineHeight>=screenBottom-25)break;
            int color=prerequisiteLines.contains(line)?RpgUi.MUTED:RpgUi.GOLD;
            detailY=RpgUi.drawWrapped(poseStack,font,line,textLeft,detailY+1,textWidth,color);
        }
        String status = switch (stateOf(skill)) {
            case UNLOCKED -> "Unlocked";
            case AVAILABLE -> "Click node to unlock - Cost: " + skill.cost() + " SP";
            case LOCKED -> missingRequirementText(skillTree, skill);
            case UNAVAILABLE -> "Permanently unavailable";
        };
        RpgUi.drawFitted(poseStack, font, Component.literal(status), textLeft, screenBottom - 19,textWidth,
                stateOf(skill) == NodeState.LOCKED ? RpgUi.MUTED : RpgUi.UNLOCKED);
    }

    private String missingRequirementText(SkillTreeDefinition playerClass, Skill skill) {
        if (ClientProgress.skillPoints() < skill.cost()) return "Not enough skill points";
        String missing = skill.prerequisites().stream().filter(id -> !ClientProgress.hasSkill(id))
                .map(id -> playerClass.findSkill(id)).filter(java.util.Objects::nonNull)
                .map(Skill::displayName).findFirst().orElse("another skill");
        return "Requires: " + missing;
    }

    private java.util.List<String> prerequisiteLines(SkillTreeDefinition tree, Skill skill) {
        if (skill.prerequisites().isEmpty()) return java.util.List.of();
        String prerequisites = skill.prerequisites().stream().map(prerequisiteId -> {
            Skill prerequisite = tree.findSkill(prerequisiteId);
            return prerequisite == null ? prerequisiteId : prerequisite.displayName();
        }).collect(java.util.stream.Collectors.joining(", "));
        return java.util.List.of("Prerequisites: " + prerequisites);
    }

    private void renderConfirmationModal(PoseStack poseStack, SkillTreeDefinition playerClass, int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, 0, 0, width, height, 0xB0000000);
        int modalWidth = Math.min(310, width - 30);
        int modalHeight = confirmationModalHeight(playerClass, modalWidth);
        int left = width / 2 - modalWidth / 2;
        int top = height / 2 - modalHeight / 2;
        int right = left + modalWidth;
        int bottom = top + modalHeight;
        RpgUi.panel(poseStack, left, top, right, bottom);
        drawCenteredString(poseStack, font, Component.literal("Unlock Skill?"), width / 2, top + 13, RpgUi.GOLD);
        RpgUi.drawFitted(poseStack, font, Component.literal(pendingSkill.displayName()), left + 14, top + 34,modalWidth-28,RpgUi.TEXT);
        RpgUi.drawWrapped(poseStack, font, pendingSkill.description(), left + 14, top + 49,
                modalWidth - 28, RpgUi.MUTED, 2);
        int numericY=top+75;
        java.util.List<String> prerequisites=prerequisiteLines(playerClass,pendingSkill);
        java.util.List<String> details=new java.util.ArrayList<>(prerequisites);
        details.addAll(SkillDetailLines.forSkill(pendingSkill,playerClass,ClientProgress.unlockedSkills()));
        for(String line:details){int color=prerequisites.contains(line)?RpgUi.MUTED:RpgUi.GOLD;numericY=RpgUi.drawWrapped(poseStack,font,line,left+14,numericY,modalWidth-28,color);}
        drawString(poseStack, font, Component.literal("Cost: " + pendingSkill.cost() + " Skill Point"),
                left + 14, bottom - 58, RpgUi.GOLD);
        if (!pendingSkill.mutuallyExclusiveWith().isEmpty()) {
            String choices = pendingSkill.mutuallyExclusiveWith().stream()
                    .map(playerClass::findSkill).filter(java.util.Objects::nonNull)
                    .map(Skill::displayName).collect(java.util.stream.Collectors.joining(", "));
            RpgUi.drawFitted(poseStack, font, Component.literal("Exclusive choice - locks: " + choices),
                    left + 14, bottom - 45,modalWidth-28, 0xFFE08A8A);
        }

        int buttonTop = bottom - 30;
        renderModalButton(poseStack, "CANCEL", left + 14, buttonTop, left + 114, buttonTop + 18,
                mouseX, mouseY, false);
        renderModalButton(poseStack, "CONFIRM", right - 114, buttonTop, right - 14, buttonTop + 18,
                mouseX, mouseY, true);
    }

    private int confirmationModalHeight(SkillTreeDefinition tree, int modalWidth) {
        if (pendingSkill == null) return Math.min(MODAL_HEIGHT, height - 20);
        int textWidth = modalWidth - 28;
        int detailLineCount = prerequisiteLines(tree, pendingSkill).stream()
                .mapToInt(line -> font.split(Component.literal(line), textWidth).size()).sum();
        detailLineCount += SkillDetailLines.forSkill(pendingSkill, tree, ClientProgress.unlockedSkills()).stream()
                .mapToInt(line -> font.split(Component.literal(line), textWidth).size()).sum();
        int desiredHeight = Math.max(MODAL_HEIGHT, 145 + detailLineCount * (font.lineHeight + 2));
        return Math.min(desiredHeight, height - 20);
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
            SkillTreeDefinition tree=currentTree();
            int modalHeight = tree==null?Math.min(MODAL_HEIGHT,height-20):confirmationModalHeight(tree,modalWidth);
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
            emptySpaceDragCandidate=true;
            draggingTree=false;
            dragStartX=mouseX;
            dragStartY=mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pendingSkill != null || !RpgUi.inside(mouseX,mouseY,13,69,width-13,height-18)) return true;
        double oldZoom=zoom;
        zoom=Math.max(MIN_ZOOM,Math.min(MAX_ZOOM,zoom+Math.signum(delta)*ZOOM_STEP));
        if(zoom==oldZoom)return true;
        int centerX=viewportCenterX(),centerY=viewportCenterY();
        double treeX=(mouseX-centerX)/oldZoom-horizontalOffset;
        double treeY=(mouseY-centerY)/oldZoom-scrollOffset;
        horizontalOffset=(mouseX-centerX)/zoom-treeX;
        scrollOffset=(mouseY-centerY)/zoom-treeY;
        clampPan();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){
        if(pendingSkill==null&&(button==1||button==0&&emptySpaceDragCandidate)&&RpgUi.inside(mouseX,mouseY,13,69,width-13,height-18)){
            if(button==0&&!draggingTree){double dx=mouseX-dragStartX,dy=mouseY-dragStartY;if(dx*dx+dy*dy<DRAG_THRESHOLD*DRAG_THRESHOLD)return true;draggingTree=true;}
            horizontalOffset+=dragX/zoom;scrollOffset+=dragY/zoom;clampPan();return true;
        }
        return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX,double mouseY,int button){
        if(button==0&&emptySpaceDragCandidate){emptySpaceDragCandidate=false;draggingTree=false;return true;}
        return super.mouseReleased(mouseX,mouseY,button);
    }

    private void clampPan(){
        SkillTreeDefinition tree=currentTree();if(tree==null||tree.skills().isEmpty())return;
        int minColumn=tree.skills().stream().mapToInt(Skill::treeColumn).min().orElse(0),maxColumn=tree.skills().stream().mapToInt(Skill::treeColumn).max().orElse(0);
        int minRow=tree.skills().stream().mapToInt(Skill::treeRow).min().orElse(0),maxRow=tree.skills().stream().mapToInt(Skill::treeRow).max().orElse(0);
        int centerX=viewportCenterX(),centerY=viewportCenterY(),padding=32;
        double minimumX=(13+padding-centerX)/zoom-maxColumn*NODE_HORIZONTAL_SPACING;
        double maximumX=(width-13-padding-centerX)/zoom-minColumn*NODE_HORIZONTAL_SPACING;
        double minimumY=(69+padding-centerY)/zoom-maxRow*NODE_VERTICAL_SPACING;
        double maximumY=(height-18-padding-centerY)/zoom-minRow*NODE_VERTICAL_SPACING;
        horizontalOffset=Math.max(minimumX,Math.min(maximumX,horizontalOffset));
        scrollOffset=Math.max(minimumY,Math.min(maximumY,scrollOffset));
    }

    private void resetCameraToRoot() {
        SkillTreeDefinition tree = currentTree();
        if (tree == null) return;
        Skill root = tree.skills().stream().filter(Skill::automaticallyUnlocked).findFirst().orElse(null);
        if (root == null) return;
        zoom=1.0D;
        horizontalOffset = -root.treeColumn() * NODE_HORIZONTAL_SPACING;
        scrollOffset = -root.treeRow() * NODE_VERTICAL_SPACING;
        clampPan();
    }

    private void handleKeyboardPanning() {
        if (pendingSkill != null || getFocused() instanceof EditBox || minecraft == null) return;
        long window = minecraft.getWindow().getWindow();
        double cameraX = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D) ? 1.0D : 0.0D)
                - (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A) ? 1.0D : 0.0D);
        double cameraY = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S) ? 1.0D : 0.0D)
                - (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W) ? 1.0D : 0.0D);
        if (cameraX == 0.0D && cameraY == 0.0D) return;
        double length = Math.sqrt(cameraX * cameraX + cameraY * cameraY);
        horizontalOffset -= cameraX / length * KEYBOARD_PAN_SPEED / zoom;
        scrollOffset -= cameraY / length * KEYBOARD_PAN_SPEED / zoom;
        clampPan();
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
        int x = (int)Math.round(viewportCenterX()+(skill.treeColumn()*NODE_HORIZONTAL_SPACING+horizontalOffset)*zoom);
        int y = (int)Math.round(viewportCenterY()+(skill.treeRow()*NODE_VERTICAL_SPACING+scrollOffset)*zoom);
        int baseRadius=skill.automaticallyUnlocked()?ROOT_NODE_RADIUS:NODE_RADIUS;
        return new NodePosition(x,y,Math.max(10,(int)Math.round(baseRadius*zoom)));
    }

    private int viewportCenterX(){return width/2;}
    private int viewportCenterY(){return (69+height-18)/2;}

    private enum NodeState { LOCKED, AVAILABLE, UNLOCKED, UNAVAILABLE }

    private record NodePosition(int x, int y, int radius) {
        boolean contains(double mouseX, double mouseY) {
            double dx = mouseX - x;
            double dy = mouseY - y;
            return dx * dx + dy * dy <= radius * radius;
        }
    }
}
