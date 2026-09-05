package com.ollie.tierborne.client;
import com.ollie.tierborne.client.screen.PlayerClassSelectionScreen; import com.ollie.tierborne.network.SyncProgressPacket; import com.ollie.tierborne.playerclass.GeneralSkillTree; import com.ollie.tierborne.playerclass.PlayerClass; import com.ollie.tierborne.playerclass.PlayerClassRegistry; import com.ollie.tierborne.playerclass.SkillBonusType; import net.minecraft.client.Minecraft; import java.util.Set;
public final class ClientProgress {
 private static String playerClassId="",selectedAlternateAttack=""; private static int skillPoints,movementSpeedLimitPercent=100,level=1,progressionExperience,experienceToNextLevel=100; private static Set<String> unlockedSkills=Set.of(); private static boolean selectionPromptPending,moddedMovementSpeedEnabled=true; private ClientProgress(){}
 public static void receive(SyncProgressPacket p){playerClassId=p.playerClassId();skillPoints=p.skillPoints();unlockedSkills=Set.copyOf(p.unlockedSkills());selectedAlternateAttack=p.selectedAlternateAttack();movementSpeedLimitPercent=p.movementSpeedLimitPercent();moddedMovementSpeedEnabled=p.moddedMovementSpeedEnabled();level=p.level();progressionExperience=p.progressionExperience();experienceToNextLevel=p.experienceToNextLevel();selectionPromptPending=playerClassId.isEmpty();tryOpenSelectionScreen();}
 public static void tryOpenSelectionScreen(){Minecraft mc=Minecraft.getInstance();if(selectionPromptPending&&mc.player!=null&&mc.level!=null&&mc.screen==null){mc.setScreen(new PlayerClassSelectionScreen());selectionPromptPending=false;}}
 public static String playerClassId(){return playerClassId;} public static int skillPoints(){return skillPoints;} public static boolean hasSkill(String id){return unlockedSkills.contains(id);} public static Set<String> unlockedSkills(){return Set.copyOf(unlockedSkills);}
 public static String selectedAlternateAttack(){return selectedAlternateAttack;}
 public static int movementSpeedLimitPercent(){return movementSpeedLimitPercent;}
 public static boolean moddedMovementSpeedEnabled(){return moddedMovementSpeedEnabled;}
 public static int level(){return level;}
 public static int progressionExperience(){return progressionExperience;}
 public static int experienceToNextLevel(){return experienceToNextLevel;}
 public static int totalBonus(SkillBonusType type){PlayerClass playerClass=PlayerClassRegistry.get(playerClassId);int classBonus=playerClass==null?0:playerClass.totalBonus(type,unlockedSkills);return classBonus+GeneralSkillTree.INSTANCE.totalBonus(type,unlockedSkills);}
}
