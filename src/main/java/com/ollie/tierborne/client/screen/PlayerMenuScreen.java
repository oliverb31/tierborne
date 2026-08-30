package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.client.ClientProgress;
import com.ollie.tierborne.playerclass.PlayerClass;
import com.ollie.tierborne.playerclass.PlayerClassRegistry;
import com.ollie.tierborne.playerclass.GeneralSkillTree;
import com.ollie.tierborne.playerclass.Skill;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.SkillEffect;
import com.ollie.tierborne.playerclass.SwordsmanPlayerClass;
import com.ollie.tierborne.playerclass.AlternateAttackDefinition;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.SelectAlternateAttackPacket;
import com.ollie.tierborne.playerclass.SwordsmanStats;
import com.ollie.tierborne.playerclass.SubclassMetadata;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class PlayerMenuScreen extends Screen {
    private int alternateLeft, alternateTop, alternateRight, alternateBottom;
    public PlayerMenuScreen() {
        super(Component.literal("Player"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        GuiComponent.fill(poseStack, 0, 0, width, height, RpgUi.BACKDROP);

        int outerLeft = 10;
        int outerTop = 10;
        int outerRight = width - 10;
        int outerBottom = height - 10;
        RpgUi.panel(poseStack, outerLeft, outerTop, outerRight, outerBottom);
        RpgTabBar.render(poseStack, font, width, outerTop + 5, RpgTab.PLAYER, mouseX, mouseY);
        GuiComponent.fill(poseStack, outerLeft + 10, outerTop + 34,
                outerRight - 10, outerTop + 35, RpgUi.GOLD_DARK);

        int contentTop = outerTop + 43;
        int contentBottom = outerBottom - 10;
        int modelWidth = Math.min(130, Math.max(100, (outerRight - outerLeft) / 3));
        int modelLeft = outerLeft + 10;
        int modelRight = modelLeft + modelWidth;
        renderPlayerPanel(poseStack, modelLeft, contentTop, modelRight, contentBottom, mouseX, mouseY);

        int detailsLeft = modelRight + 10;
        int detailsRight = outerRight - 10;
        int classBottom = Math.min(contentTop + 72, contentBottom - 105);
        renderClassPanel(poseStack, detailsLeft, contentTop, detailsRight, classBottom);

        int subclassTop = classBottom + 10;
        int subclassBottom = contentBottom - 48;
        int subclassGap = 8;
        int subclassWidth = (detailsRight - detailsLeft - subclassGap) / 2;
        renderClassSubclassSlot(poseStack, detailsLeft, subclassTop,
                detailsLeft + subclassWidth, subclassBottom);
        renderGeneralSubclassSlot(poseStack, detailsLeft + subclassWidth + subclassGap, subclassTop,
                detailsRight, subclassBottom);
        alternateLeft=detailsLeft; alternateTop=subclassBottom+8; alternateRight=detailsRight; alternateBottom=contentBottom;
        renderAlternateAttackPanel(poseStack,alternateLeft,alternateTop,alternateRight,alternateBottom);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderPlayerPanel(PoseStack poseStack, int left, int top, int right, int bottom,
                                   int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, left, top, right, bottom, RpgUi.PANEL_LIGHT);
        RpgUi.border(poseStack, left, top, right, bottom, RpgUi.GOLD_DARK);
        if (minecraft == null || minecraft.player == null) return;

        drawCenteredString(poseStack, font, minecraft.player.getDisplayName(),
                (left + right) / 2, top + 10, RpgUi.TEXT);
        GuiComponent.fill(poseStack, left + 8, top + 26, right - 8, bottom - 8, 0x80101318);
        InventoryScreen.renderEntityInInventory(
                (left + right) / 2,
                bottom - 14,
                Math.min(48, Math.max(30, (bottom - top) / 3)),
                (left + right) / 2.0F - mouseX,
                bottom / 2.0F - mouseY,
                minecraft.player);
    }

    private void renderClassPanel(PoseStack poseStack, int left, int top, int right, int bottom) {
        GuiComponent.fill(poseStack, left, top, right, bottom, RpgUi.PANEL_LIGHT);
        RpgUi.border(poseStack, left, top, right, bottom, RpgUi.GOLD_DARK);
        PlayerClass playerClass = PlayerClassRegistry.get(ClientProgress.playerClassId());
        if (playerClass == null) {
            drawCenteredString(poseStack, font, Component.literal("No playerClass selected"),
                    (left + right) / 2, (top + bottom) / 2, RpgUi.MUTED);
            return;
        }

        int iconCenterX = left + 22;
        int iconCenterY = top + 23;
        GuiComponent.fill(poseStack, left + 8, top + 8, left + 37, top + 38, 0xFF11141A);
        RpgUi.border(poseStack, left + 8, top + 8, left + 37, top + 38, RpgUi.GOLD_DARK);
        RpgUi.classIcon(minecraft, playerClass.iconStack(), iconCenterX, iconCenterY);
        drawString(poseStack, font, Component.literal(playerClass.displayName().toUpperCase()),
                left + 45, top + 10, RpgUi.GOLD);
        RpgUi.drawWrapped(poseStack,font,playerClass.description(),left+45,top+23,right-left-52,RpgUi.MUTED,2);

        int buffY = top + 45;
        for (SkillBonusType type : SkillBonusType.values()) {
            if (!playerClass.displayedBonusTypes().contains(type)) continue;
            double bonus = playerClass.totalBonus(type, ClientProgress.unlockedSkills());
            if(playerClass instanceof SwordsmanPlayerClass){if(type==SkillBonusType.SWORD_DAMAGE)bonus+=SwordsmanStats.subclassSwordDamage(ClientProgress.unlockedSkills());if(type==SkillBonusType.MOVEMENT_SPEED)bonus+=SwordsmanStats.subclassMovementSpeed(ClientProgress.unlockedSkills());}
            drawString(poseStack, font,
                    Component.literal(type.displayName() + ": " + signed(bonus) + "%"),
                    left + 10, buffY, RpgUi.TEXT);
            buffY += 13;
        }
    }

    private void renderEmptySubclassSlot(PoseStack poseStack, int number,
                                         int left, int top, int right, int bottom) {
        GuiComponent.fill(poseStack, left, top, right, bottom, 0xFF1C2028);
        RpgUi.border(poseStack, left, top, right, bottom, RpgUi.LOCKED);
        drawCenteredString(poseStack, font, Component.literal("SUBCLASS " + number),
                (left + right) / 2, top + 10, RpgUi.MUTED);
        int iconCenterX = (left + right) / 2;
        GuiComponent.fill(poseStack, iconCenterX - 10, top + 27, iconCenterX + 10, top + 47, 0xFF11141A);
        RpgUi.border(poseStack, iconCenterX - 10, top + 27, iconCenterX + 10, top + 47, RpgUi.LOCKED);
        drawCenteredString(poseStack, font, Component.literal("?"), iconCenterX, top + 33, RpgUi.MUTED);
        drawCenteredString(poseStack, font, Component.literal("No Subclass"),
                iconCenterX, bottom - 16, RpgUi.MUTED);
    }

    private void renderClassSubclassSlot(PoseStack poseStack,int left,int top,int right,int bottom) {
        PlayerClass current=PlayerClassRegistry.get(ClientProgress.playerClassId());
        if (!(current instanceof SwordsmanPlayerClass swordsman)) { renderEmptySubclassSlot(poseStack,1,left,top,right,bottom); return; }
        Skill subclass=swordsman.selectedSubclass(ClientProgress.unlockedSkills());
        if(subclass==null){renderEmptySubclassSlot(poseStack,1,left,top,right,bottom);return;}
        GuiComponent.fill(poseStack,left,top,right,bottom,0xFF1C2028);RpgUi.border(poseStack,left,top,right,bottom,RpgUi.GOLD_DARK);
        drawCenteredString(poseStack,font,Component.literal("SUBCLASS 1"),(left+right)/2,top+8,RpgUi.MUTED);
        drawCenteredString(poseStack,font,Component.literal(subclass.displayName().toUpperCase()),(left+right)/2,top+22,RpgUi.GOLD);
        SubclassMetadata metadata=SubclassMetadata.get(subclass.id());int y=top+35;if(metadata!=null)y=RpgUi.drawWrapped(poseStack,font,metadata.description(),left+5,y,right-left-10,RpgUi.MUTED,2);
        java.util.List<String> details=new java.util.ArrayList<>();
        switch(subclass.id()){
            case SwordsmanPlayerClass.SWORDMASTER->{details.add("Dash: Available");details.add("Cooldown: "+one(RpgBalanceConfig.DASH_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.DUAL->{details.add("Dual Wield: Active");details.add("Block: "+one(ClientProgress.hasSkill(SwordsmanPlayerClass.IMPROVED_BLOCK)?RpgBalanceConfig.IMPROVED_BLOCK_PERCENT.get():RpgBalanceConfig.BLOCK_PERCENT.get())+"%");details.add("Damage/Sword: "+signed(ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL_DAMAGE)?RpgBalanceConfig.DUAL_DAMAGE_UPGRADE.get():RpgBalanceConfig.DUAL_DAMAGE.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.PARRY))details.add("Parry: Unlocked");}
            case SwordsmanPlayerClass.HEAVY->{details.add("Attack Speed: "+signed(RpgBalanceConfig.HEAVY_ATTACK_SPEED.get())+"%");details.add("Draw Delay: "+one(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS.get())+"s");details.add("Sword Movement: "+signed(RpgBalanceConfig.HEAVY_MOVE_PENALTY.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE))details.add("Range: +"+one(RpgBalanceConfig.HEAVY_RANGE.get()));if(ClientProgress.hasSkill(SwordsmanPlayerClass.LEAP_STRIKE))details.add("Leap Strike: Unlocked");}
            case SwordsmanPlayerClass.ROGUE->{details.add("Maximum Health: -"+one(RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get()));details.add("Lower Mob Target Priority");if(ClientProgress.hasSkill(SwordsmanPlayerClass.BACKSTAB))details.add("Backstab: +"+one(RpgBalanceConfig.BACKSTAB_DAMAGE.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIRST_HIT))details.add("First Hit: +"+one(RpgBalanceConfig.FIRST_HIT_DAMAGE.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.NON_AGGRO))details.add("Non-Aggro: +"+one(RpgBalanceConfig.NON_AGGRO_DAMAGE.get())+"%");}
        }
        for(String detail:details){if(y>bottom-10)break;drawString(poseStack,font,Component.literal(detail),left+5,y,RpgUi.TEXT);y+=11;}
    }

    private void renderAlternateAttackPanel(PoseStack poseStack,int left,int top,int right,int bottom){
        GuiComponent.fill(poseStack,left,top,right,bottom,0xFF1C2028);RpgUi.border(poseStack,left,top,right,bottom,RpgUi.GOLD_DARK);
        java.util.List<AlternateAttackDefinition> unlocked=AlternateAttackDefinition.ALL.stream().filter(a->ClientProgress.hasSkill(a.skillId())).toList();
        if(unlocked.isEmpty()){drawCenteredString(poseStack,font,Component.literal("Unlock an alternate attack to get started!"),(left+right)/2,top+14,RpgUi.MUTED);return;}
        AlternateAttackDefinition active=AlternateAttackDefinition.find(ClientProgress.selectedAlternateAttack());
        drawString(poseStack,font,Component.literal("Active Alternate Attack"),left+8,top+7,RpgUi.MUTED);
        drawString(poseStack,font,Component.literal(active==null?"Click to select":active.displayName()),left+8,top+20,RpgUi.GOLD);
        drawString(poseStack,font,Component.literal("Click to cycle unlocked attacks"),right-150,top+20,RpgUi.MUTED);
    }

    private void renderGeneralSubclassSlot(PoseStack poseStack, int left, int top, int right, int bottom) {
        GuiComponent.fill(poseStack, left, top, right, bottom, 0xFF1C2028);
        RpgUi.border(poseStack, left, top, right, bottom, RpgUi.LOCKED);
        drawCenteredString(poseStack, font, Component.literal("SUBCLASS 2"),
                (left + right) / 2, top + 8, RpgUi.MUTED);
        Skill subclass = GeneralSkillTree.INSTANCE.selectedSubclass(ClientProgress.unlockedSkills());
        if (subclass == null) {
            drawCenteredString(poseStack, font, Component.literal("No Subclass"),
                    (left + right) / 2, top + 34, RpgUi.MUTED);
            return;
        }

        drawCenteredString(poseStack, font, Component.literal(subclass.displayName().toUpperCase()),
                (left + right) / 2, top + 23, RpgUi.GOLD);
        int y = top + 36;SubclassMetadata metadata=SubclassMetadata.get(subclass.id());if(metadata!=null)y=RpgUi.drawWrapped(poseStack,font,metadata.description(),left+5,y,right-left-10,RpgUi.MUTED,2);
        for (Skill skill : GeneralSkillTree.INSTANCE.skills()) {
            if (!skill.prerequisites().contains(subclass.id()) || !ClientProgress.hasSkill(skill.id())) continue;
            String text;
            if (skill.upgrade() != null) {
                int value = GeneralSkillTree.INSTANCE.totalBonus(
                        skill.upgrade().type(), ClientProgress.unlockedSkills());
                text = skill.upgrade().type().displayName() + ": +" + value + "%";
            } else if (skill.effect() == SkillEffect.MATERIAL_RECOVERY
                    || skill.effect() == SkillEffect.ENCHANTED_CRAFTING) {
                text = skill.displayName() + ": Unlocked";
            } else {
                continue;
            }
            RpgUi.drawWrapped(poseStack, font, text, left + 5, y, right - left - 10, RpgUi.TEXT);
            y += 13;
        }
    }

    private static String one(double value){return String.format(java.util.Locale.ROOT,"%.1f",value);}
    private static String signed(double value){return (value>=0?"+":"")+one(value);}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && RpgTabBar.mouseClicked(mouseX, mouseY, width, 15, RpgTab.PLAYER)) return true;
        if(button==0&&RpgUi.inside(mouseX,mouseY,alternateLeft,alternateTop,alternateRight,alternateBottom)){
            java.util.List<AlternateAttackDefinition> unlocked=AlternateAttackDefinition.ALL.stream().filter(a->ClientProgress.hasSkill(a.skillId())).toList();
            if(!unlocked.isEmpty()){int current=-1;for(int i=0;i<unlocked.size();i++)if(unlocked.get(i).id().equals(ClientProgress.selectedAlternateAttack()))current=i;ModNetwork.CHANNEL.sendToServer(new SelectAlternateAttackPacket(unlocked.get((current+1)%unlocked.size()).id()));}
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
