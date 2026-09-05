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
import com.ollie.tierborne.playerclass.ArcherPlayerClass;
import com.ollie.tierborne.playerclass.FighterPlayerClass;
import com.ollie.tierborne.playerclass.BarbarianPlayerClass;
import com.ollie.tierborne.playerclass.FighterStats;
import com.ollie.tierborne.playerclass.AlternateAttackDefinition;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.SelectAlternateAttackPacket;
import com.ollie.tierborne.network.SetMovementSpeedLimitPacket;
import com.ollie.tierborne.playerclass.SwordsmanStats;
import com.ollie.tierborne.playerclass.SubclassMetadata;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class PlayerMenuScreen extends Screen {
    private int alternateLeft, alternateTop, alternateRight, alternateBottom;
    private int speedLimitLeft, speedLimitRight, speedLimitTop;
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
        int contentLeft=outerLeft+10,contentRight=outerRight-10;
        int classBottom=Math.min(contentTop+92,contentBottom-115);
        renderClassPanel(poseStack,contentLeft,contentTop,contentRight,classBottom);
        int subclassTop=classBottom+8;
        int subclassBottom = contentBottom - 48;
        int modelWidth=Math.min(118,Math.max(82,(contentRight-contentLeft)/4));
        int modelLeft=contentLeft,modelRight=modelLeft+modelWidth;
        renderPlayerPanel(poseStack,modelLeft,subclassTop,modelRight,contentBottom,mouseX,mouseY);
        int detailsLeft=modelRight+8,detailsRight=contentRight;
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
        drawCenteredString(poseStack, font, Component.literal("Level " + ClientProgress.level()),
                (left + right) / 2, top + 21, RpgUi.GOLD);
        int experienceLeft = left + 10;
        int experienceRight = right - 10;
        int experienceTop = top + 33;
        int experienceRequired = ClientProgress.experienceToNextLevel();
        float experienceProgress = experienceRequired <= 0 ? 1.0F
                : Math.min(1.0F, ClientProgress.progressionExperience() / (float) experienceRequired);
        GuiComponent.fill(poseStack, experienceLeft, experienceTop,
                experienceRight, experienceTop + 7, RpgUi.LOCKED);
        int experienceFilled = Math.round((experienceRight - experienceLeft) * experienceProgress);
        GuiComponent.fill(poseStack, experienceLeft, experienceTop,
                experienceLeft + experienceFilled, experienceTop + 7, RpgUi.GOLD);
        RpgUi.border(poseStack, experienceLeft, experienceTop,
                experienceRight, experienceTop + 7, RpgUi.GOLD_DARK);
        int speedPanelTop = bottom - 52;
        GuiComponent.fill(poseStack, left + 8, top + 44, right - 8, speedPanelTop - 4, 0x80101318);
        int modelAreaHeight = Math.max(50, speedPanelTop - top - 48);
        InventoryScreen.renderEntityInInventory(
                (left + right) / 2,
                speedPanelTop - 7,
                Math.min(48, Math.max(30, modelAreaHeight / 2)),
                (left + right) / 2.0F - mouseX,
                (top + modelAreaHeight / 2.0F) - mouseY,
                minecraft.player);
        speedLimitLeft=left+10;speedLimitRight=right-10;speedLimitTop=bottom-22;
        GuiComponent.fill(poseStack,left+6,speedPanelTop,right-6,bottom-6,0xE0101319);
        String movementMode = ClientProgress.moddedMovementSpeedEnabled() ? "ON" : "VANILLA";
        RpgUi.drawCenteredFitted(poseStack,font,Component.literal("Modded Speed: "+movementMode+" (V)"),(left+right)/2,bottom-48,speedLimitRight-speedLimitLeft,RpgUi.TEXT);
        RpgUi.drawCenteredFitted(poseStack,font,Component.literal(ClientProgress.movementSpeedLimitPercent()+"%"),(left+right)/2,bottom-37,speedLimitRight-speedLimitLeft,RpgUi.GOLD);
        GuiComponent.fill(poseStack,speedLimitLeft,speedLimitTop,speedLimitRight,speedLimitTop+7,RpgUi.LOCKED);
        int filled=(speedLimitRight-speedLimitLeft)*ClientProgress.movementSpeedLimitPercent()/100;
        GuiComponent.fill(poseStack,speedLimitLeft,speedLimitTop,speedLimitLeft+filled,speedLimitTop+7,RpgUi.GOLD);
        RpgUi.border(poseStack,speedLimitLeft,speedLimitTop,speedLimitRight,speedLimitTop+7,RpgUi.GOLD_DARK);
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
        RpgUi.drawFitted(poseStack, font, Component.literal(playerClass.displayName().toUpperCase()),
                left + 45, top + 10, right-left-52, RpgUi.GOLD);
        RpgUi.drawWrapped(poseStack,font,playerClass.description(),left+45,top+23,right-left-52,RpgUi.MUTED,2);

        java.util.List<String> stats=new java.util.ArrayList<>();
        if(playerClass instanceof ArcherPlayerClass){stats.add("Bow Damage: "+signed(archerBowDamage())+"%");stats.add("Crossbow Damage: "+signed(archerCrossbowDamage())+"%");double movement=playerClass.totalBonus(SkillBonusType.MOVEMENT_SPEED,ClientProgress.unlockedSkills())+(ClientProgress.hasSkill(ArcherPlayerClass.RANGER)?RpgBalanceConfig.RANGER_MOVEMENT.get():0);stats.add("Movement Speed: "+signed(movement)+"%");stats.add("Dash Distance: "+one(RpgBalanceConfig.ARCHER_DASH_DISTANCE.get()));stats.add("Dash Cooldown: "+one(RpgBalanceConfig.ARCHER_DASH_COOLDOWN_SECONDS.get())+"s");}
        else if(playerClass instanceof FighterPlayerClass){if(fighterGeneral()>0)stats.add("General Damage: "+signed(fighterGeneral())+"%");if(fighterFist()>0)stats.add("Fist Damage: "+signed(fighterFist())+"%");if(fighterMovement()>0)stats.add("Movement Speed: "+signed(fighterMovement())+"%");if(fighterCharge()>0)stats.add("Melee Charge Speed: "+signed(fighterCharge())+"%");}
        else if(playerClass instanceof BarbarianPlayerClass){double damage=RpgBalanceConfig.BARBARIAN_AXE_DAMAGE.get()+(ClientProgress.hasSkill(BarbarianPlayerClass.EXECUTIONER)?RpgBalanceConfig.EXECUTIONER_AXE_DAMAGE.get():0);stats.add("Axe Damage: "+signed(damage)+"%");stats.add("Axe Charge Speed: "+signed(RpgBalanceConfig.BARBARIAN_AXE_CHARGE_SPEED.get()+(ClientProgress.hasSkill(BarbarianPlayerClass.EXECUTIONER)?RpgBalanceConfig.EXECUTIONER_ATTACK_SPEED.get():0))+"%");if(ClientProgress.hasSkill(BarbarianPlayerClass.BERSERKER))stats.add("Maximum Health: +"+one(RpgBalanceConfig.BERSERKER_MAX_HEALTH.get()));}
        else for(SkillBonusType type:SkillBonusType.values()){if(!playerClass.displayedBonusTypes().contains(type))continue;double bonus=playerClass.totalBonus(type,ClientProgress.unlockedSkills());if(playerClass instanceof SwordsmanPlayerClass){if(type==SkillBonusType.SWORD_DAMAGE)bonus+=SwordsmanStats.subclassSwordDamage(ClientProgress.unlockedSkills());if(type==SkillBonusType.MOVEMENT_SPEED)bonus+=SwordsmanStats.subclassMovementSpeed(ClientProgress.unlockedSkills());}stats.add(type.displayName()+": "+signed(bonus)+"%");}
        renderStatGrid(poseStack,stats,left+10,top+47,right-10,bottom-6);
    }

    private void renderStatGrid(PoseStack poseStack,java.util.List<String> stats,int left,int top,int right,int bottom){int available=Math.max(1,right-left);int columns=Math.max(1,Math.min(3,available/125));int columnWidth=available/columns;int rows=(stats.size()+columns-1)/columns;for(int i=0;i<stats.size();i++){int column=i/Math.max(1,rows),row=i%Math.max(1,rows);int y=top+row*12;if(y+9>bottom)break;RpgUi.drawFitted(poseStack,font,Component.literal(stats.get(i)),left+column*columnWidth,y,columnWidth-8,RpgUi.TEXT);}}

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
        Skill subclass=current instanceof SwordsmanPlayerClass swordsman?swordsman.selectedSubclass(ClientProgress.unlockedSkills()):current instanceof ArcherPlayerClass archer?archer.selectedSubclass(ClientProgress.unlockedSkills()):current instanceof FighterPlayerClass fighter?fighter.selectedSubclass(ClientProgress.unlockedSkills()):current instanceof BarbarianPlayerClass barbarian?barbarian.selectedSubclass(ClientProgress.unlockedSkills()):null;
        if(subclass==null){renderEmptySubclassSlot(poseStack,1,left,top,right,bottom);return;}
        GuiComponent.fill(poseStack,left,top,right,bottom,0xFF1C2028);RpgUi.border(poseStack,left,top,right,bottom,RpgUi.GOLD_DARK);
        drawCenteredString(poseStack,font,Component.literal("SUBCLASS 1"),(left+right)/2,top+8,RpgUi.MUTED);
        drawCenteredString(poseStack,font,Component.literal(subclass.displayName().toUpperCase()),(left+right)/2,top+22,RpgUi.GOLD);
        SubclassMetadata metadata=SubclassMetadata.get(subclass.id());int y=top+35;if(metadata!=null)y=RpgUi.drawWrapped(poseStack,font,metadata.description(),left+5,y,right-left-10,RpgUi.MUTED,2);
        java.util.List<String> details=new java.util.ArrayList<>();
        switch(subclass.id()){
            case SwordsmanPlayerClass.SWORDMASTER->{details.add("Dash: Available");details.add("Cooldown: "+one(RpgBalanceConfig.DASH_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.DUAL->{details.add("Dual Wield: Active");details.add("Block: "+one(RpgBalanceConfig.BLOCK_PERCENT.get())+"% / "+one(RpgBalanceConfig.BLOCK_COOLDOWN_SECONDS.get())+"s cooldown");details.add("Damage/Sword: "+signed(ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL_DAMAGE)?RpgBalanceConfig.DUAL_DAMAGE_UPGRADE.get():RpgBalanceConfig.DUAL_DAMAGE.get())+"%");details.add("Sword Charge Speed: "+signed(ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL_SPEED)?RpgBalanceConfig.DUAL_SPEED_UPGRADE.get():RpgBalanceConfig.DUAL_ATTACK_SPEED.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.IMPROVED_BLOCK))details.add("Steadfast Guard: Active");if(ClientProgress.hasSkill(SwordsmanPlayerClass.PARRY))details.add("Parry: Unlocked");}
            case SwordsmanPlayerClass.HEAVY->{details.add("Sword Charge Speed: "+signed(RpgBalanceConfig.HEAVY_ATTACK_SPEED.get())+"%");details.add("Draw Delay: "+one(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS.get())+"s");details.add("Sword Movement: "+signed(RpgBalanceConfig.HEAVY_MOVE_PENALTY.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE))details.add("Range: +"+one(RpgBalanceConfig.HEAVY_RANGE.get()));if(ClientProgress.hasSkill(SwordsmanPlayerClass.LEAP_STRIKE))details.add("Leap Strike: Unlocked");}
            case SwordsmanPlayerClass.ROGUE->{details.add("Maximum Health: -"+one(RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get()));details.add("Lower Mob Target Priority");if(ClientProgress.hasSkill(SwordsmanPlayerClass.BACKSTAB))details.add("Backstab: +"+one(RpgBalanceConfig.BACKSTAB_DAMAGE.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIRST_HIT))details.add("First Hit: +"+one(RpgBalanceConfig.FIRST_HIT_DAMAGE.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.NON_AGGRO))details.add("Non-Aggro: +"+one(RpgBalanceConfig.NON_AGGRO_DAMAGE.get())+"%");}
            case SwordsmanPlayerClass.MAGIC->{details.add("Intrinsic Fire: "+one(RpgBalanceConfig.MAGIC_SWORD_FIRE_SECONDS.get())+"s");if(ClientProgress.hasSkill(SwordsmanPlayerClass.ELEMENTAL_VULNERABILITY))details.add("Elemental Vulnerability: +"+one(RpgBalanceConfig.ELEMENTAL_VULNERABILITY_BONUS_PERCENT.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_COOLDOWN))details.add("Fireball Cooldown: "+one(RpgBalanceConfig.FIREBALL_UPGRADED_COOLDOWN_SECONDS.get())+"s");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_CHARGE))details.add("Fireball Charge: Unlocked");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_SPEED_I))details.add("Fireball Speed: "+one(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_SPEED_II)?RpgBalanceConfig.FIREBALL_SPEED_II.get():RpgBalanceConfig.FIREBALL_SPEED_I.get())+" blocks/s");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_DAMAGE_I))details.add("Fireball Damage: "+one(ClientProgress.hasSkill(SwordsmanPlayerClass.FIREBALL_DAMAGE_II)?RpgBalanceConfig.FIREBALL_DAMAGE_II.get():RpgBalanceConfig.FIREBALL_DAMAGE_I.get())+"%");if(ClientProgress.hasSkill(SwordsmanPlayerClass.TRIPLE_FIREBALL))details.add("Triple Fireball: 3 shots / "+one(RpgBalanceConfig.TRIPLE_FIREBALL_RECAST_SECONDS.get())+"s recast");if(ClientProgress.hasSkill(SwordsmanPlayerClass.FLAME_SLASH))details.add("Flame Slash: Unlocked");}
            case ArcherPlayerClass.CROSSBOWMAN->{details.add("Charge Speed: "+signed((ClientProgress.hasSkill(ArcherPlayerClass.CROSSBOW_SPEED_1)?RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_I.get():0)+(ClientProgress.hasSkill(ArcherPlayerClass.CROSSBOW_SPEED_2)?RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_II.get():0))+"%");details.add("Multishot: "+one(RpgBalanceConfig.MULTISHOT_DELAY_SECONDS.get())+"s / "+one(RpgBalanceConfig.MULTISHOT_COOLDOWN_SECONDS.get())+"s cooldown");}
            case ArcherPlayerClass.LONGBOWMAN->{details.add("Bow Draw Time: +"+one(-RpgBalanceConfig.LONGBOWMAN_CHARGE_SPEED.get())+"%");details.add("Movement While Drawing: "+signed(ClientProgress.hasSkill(ArcherPlayerClass.FULLY_CHARGED_MOBILITY)?RpgBalanceConfig.FULLY_CHARGED_IMPROVED_MOVEMENT_PENALTY.get():RpgBalanceConfig.LONGBOWMAN_DRAW_MOVEMENT.get())+"%");if(ClientProgress.hasSkill(ArcherPlayerClass.FULLY_CHARGED_MOBILITY))details.add("Draw Mobility Upgrade: Active");}
            case ArcherPlayerClass.ELEMENTAL_ARCHER->{if(ClientProgress.hasSkill(ArcherPlayerClass.FIRE))details.add("Fire Arrows: "+one(ClientProgress.hasSkill(ArcherPlayerClass.FIRE_DURATION)?RpgBalanceConfig.FIRE_UPGRADED_SECONDS.get():RpgBalanceConfig.FIRE_PASSIVE_SECONDS.get())+"s");if(ClientProgress.hasSkill(ArcherPlayerClass.ICE))details.add("Ice Arrows: Slowness "+(ClientProgress.hasSkill(ArcherPlayerClass.ICE_POTENCY)?RpgBalanceConfig.ICE_UPGRADED_LEVEL.get():RpgBalanceConfig.ICE_PASSIVE_LEVEL.get()));}
            case ArcherPlayerClass.RANGER->{details.add("Movement Speed: +"+one(RpgBalanceConfig.RANGER_MOVEMENT.get())+"%");details.add("Backstep: "+one(ClientProgress.hasSkill(ArcherPlayerClass.BACKSTEP_RANGE)?RpgBalanceConfig.BACKSTEP_UPGRADED_DISTANCE.get():RpgBalanceConfig.BACKSTEP_DISTANCE.get())+" blocks");if(ClientProgress.hasSkill(ArcherPlayerClass.NATURES_ROOTS))details.add("Nature's Roots: "+one(RpgBalanceConfig.ROOTS_CHANNEL_SECONDS.get())+"s / "+one(RpgBalanceConfig.ROOTS_COOLDOWN_SECONDS.get())+"s cooldown");}
            case FighterPlayerClass.MONK->{details.add("Martial Arts: Both hands empty");details.add("Pull Range: "+one(ClientProgress.hasSkill(FighterPlayerClass.EXTENDED_PULL)?RpgBalanceConfig.PULL_UPGRADED_RANGE.get():RpgBalanceConfig.PULL_RANGE.get()));details.add("Pull Cooldown: "+one(ClientProgress.hasSkill(FighterPlayerClass.PULL_RECOVERY)?RpgBalanceConfig.PULL_UPGRADED_COOLDOWN_SECONDS.get():RpgBalanceConfig.PULL_COOLDOWN_SECONDS.get())+"s");if(ClientProgress.hasSkill(FighterPlayerClass.UPPERCUT))details.add("Uppercut: "+one(RpgBalanceConfig.UPPERCUT_DAMAGE_PERCENT.get())+"% bonus / "+one(RpgBalanceConfig.UPPERCUT_COOLDOWN_SECONDS.get())+"s cooldown");}
            case FighterPlayerClass.CHAMPION->{details.add("Combo Compound: +"+one(RpgBalanceConfig.COMBO_COMPOUND_PERCENT.get())+"%");details.add("Window: "+one(ClientProgress.hasSkill(FighterPlayerClass.COMBO_WINDOW_II)?RpgBalanceConfig.COMBO_WINDOW_II_SECONDS.get():ClientProgress.hasSkill(FighterPlayerClass.COMBO_WINDOW_I)?RpgBalanceConfig.COMBO_WINDOW_I_SECONDS.get():RpgBalanceConfig.COMBO_WINDOW_SECONDS.get())+"s");details.add("Same-target hits required");}
            case FighterPlayerClass.DUELIST->{details.add("Counter Chance: "+one(ClientProgress.hasSkill(FighterPlayerClass.COUNTER_CHANCE)?RpgBalanceConfig.DUELIST_UPGRADED_COUNTER_CHANCE.get():RpgBalanceConfig.DUELIST_COUNTER_CHANCE.get())+"%");if(ClientProgress.hasSkill(FighterPlayerClass.PERFECT_COUNTER))details.add("Perfect Counter: Active");if(ClientProgress.hasSkill(FighterPlayerClass.REFLECTIVE_COUNTER))details.add("Reflection: Active");details.add("Disarm: "+one(ClientProgress.hasSkill(FighterPlayerClass.DISARM_DURATION)?RpgBalanceConfig.DISARM_UPGRADED_DURATION_SECONDS.get():RpgBalanceConfig.DISARM_DURATION_SECONDS.get())+"s");}
            case BarbarianPlayerClass.BERSERKER->{details.add("Maximum Health: +"+one(RpgBalanceConfig.BERSERKER_MAX_HEALTH.get()));details.add("Shields: Disabled");details.add("Berserk Duration: "+one(ClientProgress.hasSkill(BarbarianPlayerClass.LONGER_BERSERK)?RpgBalanceConfig.BERSERK_UPGRADED_DURATION_SECONDS.get():RpgBalanceConfig.BERSERK_MAX_DURATION_SECONDS.get())+"s");details.add("Lifesteal: "+one(ClientProgress.hasSkill(BarbarianPlayerClass.BLOOD_FEAST)?RpgBalanceConfig.BERSERK_UPGRADED_LIFESTEAL_PERCENT.get():RpgBalanceConfig.BERSERK_LIFESTEAL_PERCENT.get())+"%");}
            case BarbarianPlayerClass.EXECUTIONER->{details.add("Low-health Damage: +"+one(ClientProgress.hasSkill(BarbarianPlayerClass.LOW_HEALTH_DAMAGE)?RpgBalanceConfig.EXECUTIONER_UPGRADED_LOW_HEALTH_DAMAGE.get():RpgBalanceConfig.EXECUTIONER_LOW_HEALTH_DAMAGE.get())+"%");details.add("Execute Charge: "+one(ClientProgress.hasSkill(BarbarianPlayerClass.EXECUTE_CHARGE)?RpgBalanceConfig.EXECUTE_UPGRADED_CHARGE_SECONDS.get():RpgBalanceConfig.EXECUTE_CHARGE_SECONDS.get())+"s");details.add("Execute Instant Kill: <"+one(RpgBalanceConfig.EXECUTE_KILL_THRESHOLD_PERCENT.get())+"% HP");}
        }
        for(String detail:details){if(y>bottom-10)break;drawString(poseStack,font,Component.literal(detail),left+5,y,RpgUi.TEXT);y+=11;}
    }

    private void renderAlternateAttackPanel(PoseStack poseStack,int left,int top,int right,int bottom){
        GuiComponent.fill(poseStack,left,top,right,bottom,0xFF1C2028);RpgUi.border(poseStack,left,top,right,bottom,RpgUi.GOLD_DARK);
        java.util.List<AlternateAttackDefinition> unlocked=AlternateAttackDefinition.ALL.stream().filter(a->ClientProgress.hasSkill(a.skillId())).toList();
        if(unlocked.isEmpty()){drawCenteredString(poseStack,font,Component.literal("Unlock an alternate attack to get started!"),(left+right)/2,top+14,RpgUi.MUTED);return;}
        AlternateAttackDefinition active=AlternateAttackDefinition.find(ClientProgress.selectedAlternateAttack());
        RpgUi.drawFitted(poseStack,font,Component.literal("Active Alternate Attack"),left+8,top+7,right-left-16,RpgUi.MUTED);
        RpgUi.drawFitted(poseStack,font,Component.literal(active==null?"Click to select":active.displayName()),left+8,top+20,right-left-16,RpgUi.GOLD);
        RpgUi.drawFitted(poseStack,font,Component.literal(active==null?"Click to cycle unlocked attacks":active.description()),left+8,top+31,right-left-16,RpgUi.MUTED);
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
    private static double archerBowDamage(){double v=RpgBalanceConfig.ARCHER_BOW_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.BOW_DAMAGE_1))v+=RpgBalanceConfig.BOW_DAMAGE_I.get();if(ClientProgress.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_1))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_I.get();if(ClientProgress.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_2))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_II.get();if(ClientProgress.hasSkill(ArcherPlayerClass.RANGER_DAMAGE))v+=RpgBalanceConfig.RANGER_RANGED_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.LONGBOWMAN))v+=RpgBalanceConfig.LONGBOWMAN_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.LONGBOW_DAMAGE))v+=RpgBalanceConfig.LONGBOW_DAMAGE_UPGRADE.get();return v;}
    private static double archerCrossbowDamage(){double v=RpgBalanceConfig.ARCHER_CROSSBOW_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.CROSSBOW_DAMAGE_1))v+=RpgBalanceConfig.CROSSBOW_DAMAGE_I.get();if(ClientProgress.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_1))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_I.get();if(ClientProgress.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_2))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_II.get();if(ClientProgress.hasSkill(ArcherPlayerClass.RANGER_DAMAGE))v+=RpgBalanceConfig.RANGER_RANGED_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.CROSSBOWMAN))v+=RpgBalanceConfig.CROSSBOWMAN_DAMAGE.get();if(ClientProgress.hasSkill(ArcherPlayerClass.CROSSBOW_DAMAGE_2))v+=RpgBalanceConfig.CROSSBOW_DAMAGE_II.get();return v;}
    private static double fighterGeneral(){double v=0;if(ClientProgress.hasSkill(FighterPlayerClass.CHAMPION_DAMAGE))v+=RpgBalanceConfig.FIGHTER_CHAMPION_PATH_DAMAGE.get();if(ClientProgress.hasSkill(FighterPlayerClass.DUELIST_DAMAGE))v+=RpgBalanceConfig.FIGHTER_DUELIST_PATH_DAMAGE.get();if(ClientProgress.hasSkill(FighterPlayerClass.DUELIST))v+=RpgBalanceConfig.DUELIST_DAMAGE.get();if(ClientProgress.hasSkill(FighterPlayerClass.DUELIST_DAMAGE_UPGRADE))v+=RpgBalanceConfig.DUELIST_DAMAGE_UPGRADE.get();return v;}
    private static double fighterFist(){double v=ClientProgress.hasSkill(FighterPlayerClass.MONK_FIST)?RpgBalanceConfig.FIGHTER_MONK_PATH_FIST.get():0;if(ClientProgress.hasSkill(FighterPlayerClass.MONK))v+=RpgBalanceConfig.MONK_FIST.get();if(ClientProgress.hasSkill(FighterPlayerClass.MONK_FIST_UPGRADE))v+=RpgBalanceConfig.MONK_FIST_UPGRADE.get();return v;}
    private static double fighterMovement(){double v=ClientProgress.hasSkill(FighterPlayerClass.MONK_MOVE)?RpgBalanceConfig.FIGHTER_MONK_PATH_MOVE.get():0;if(ClientProgress.hasSkill(FighterPlayerClass.MONK))v+=RpgBalanceConfig.MONK_MOVE.get();return v;}
    private static double fighterCharge(){double v=ClientProgress.hasSkill(FighterPlayerClass.CHAMPION_SPEED)?RpgBalanceConfig.FIGHTER_CHAMPION_PATH_CHARGE.get():0;if(ClientProgress.hasSkill(FighterPlayerClass.DUELIST_SPEED))v+=RpgBalanceConfig.FIGHTER_DUELIST_PATH_CHARGE.get();return v;}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && RpgTabBar.mouseClicked(mouseX, mouseY, width, 15, RpgTab.PLAYER)) return true;
        if(button==0&&RpgUi.inside(mouseX,mouseY,speedLimitLeft,speedLimitTop-4,speedLimitRight,speedLimitTop+11)){setSpeedLimit(mouseX);return true;}
        if(button==0&&RpgUi.inside(mouseX,mouseY,alternateLeft,alternateTop,alternateRight,alternateBottom)){
            java.util.List<AlternateAttackDefinition> unlocked=AlternateAttackDefinition.ALL.stream().filter(a->ClientProgress.hasSkill(a.skillId())).toList();
            if(!unlocked.isEmpty()){int current=-1;for(int i=0;i<unlocked.size();i++)if(unlocked.get(i).id().equals(ClientProgress.selectedAlternateAttack()))current=i;ModNetwork.CHANNEL.sendToServer(new SelectAlternateAttackPacket(unlocked.get((current+1)%unlocked.size()).id()));}
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){if(button==0&&mouseY>=speedLimitTop-8&&mouseY<=speedLimitTop+14){setSpeedLimit(mouseX);return true;}return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);}
    private void setSpeedLimit(double mouseX){int percent=(int)Math.round((mouseX-speedLimitLeft)*100.0/Math.max(1,speedLimitRight-speedLimitLeft));percent=Math.max(10,Math.min(100,percent));ModNetwork.CHANNEL.sendToServer(new SetMovementSpeedLimitPacket(percent));}
}
