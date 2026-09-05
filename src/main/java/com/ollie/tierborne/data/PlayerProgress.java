package com.ollie.tierborne.data;

import com.ollie.tierborne.playerclass.PlayerClass;
import com.ollie.tierborne.playerclass.PlayerClassRegistry;
import com.ollie.tierborne.playerclass.GeneralSkillTree;
import com.ollie.tierborne.playerclass.Skill;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.SkillEffect;
import com.ollie.tierborne.playerclass.SkillTreeDefinition;
import com.ollie.tierborne.playerclass.AlternateAttackDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PlayerProgress {
    private String playerClassId = "";
    public static final int MAX_LEVEL = 30;
    public static final int STARTING_SKILL_POINTS = 3;
    private int skillPoints = STARTING_SKILL_POINTS;
    private int level = 1;
    private int progressionExperience;
    private final Set<String> unlockedSkills = new HashSet<>();
    private String selectedAlternateAttack = "";
    private int movementSpeedLimitPercent = 100;
    private boolean moddedMovementSpeedEnabled = true;
    private boolean receivedStartingArmor;
    private final Set<String> discoveredChunks = new HashSet<>();
    private final Map<String, Integer> recentHostileKills = new HashMap<>();
    private long hostileKillWindowStart;
    private int hostileKillsInWindow;

    public String playerClassId() { return playerClassId; }
    public int skillPoints() { return skillPoints; }
    public int level() { return level; }
    public int progressionExperience() { return progressionExperience; }
    public int experienceToNextLevel() { return level >= MAX_LEVEL ? 0 : experienceRequired(level); }
    public String selectedAlternateAttack() { return selectedAlternateAttack; }
    public int movementSpeedLimitPercent() { return movementSpeedLimitPercent; }
    public boolean moddedMovementSpeedEnabled() { return moddedMovementSpeedEnabled; }
    public boolean receivedStartingArmor() { return receivedStartingArmor; }
    public void markReceivedStartingArmor() { receivedStartingArmor = true; }
    public boolean setMovementSpeedLimitPercent(int value) { int clamped=Math.max(10,Math.min(100,value));if(clamped==movementSpeedLimitPercent)return false;movementSpeedLimitPercent=clamped;return true; }
    public boolean toggleModdedMovementSpeed() { moddedMovementSpeedEnabled = !moddedMovementSpeedEnabled; return moddedMovementSpeedEnabled; }

    public int addProgressionExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        progressionExperience += amount;
        int levelsGained = 0;
        while (level < MAX_LEVEL && progressionExperience >= experienceRequired(level)) {
            progressionExperience -= experienceRequired(level);
            level++;
            skillPoints++;
            levelsGained++;
        }
        if (level >= MAX_LEVEL) progressionExperience = 0;
        return levelsGained;
    }

    public boolean discoverChunk(String dimensionAndChunk) {
        if (level >= MAX_LEVEL || discoveredChunks.size() >= 8192) return false;
        return discoveredChunks.add(dimensionAndChunk);
    }

    public int applyHostileKillDiminishingReturns(String entityType, long gameTime, int baseExperience) {
        if (gameTime < hostileKillWindowStart || gameTime - hostileKillWindowStart >= 6000L) {
            hostileKillWindowStart = gameTime;
            hostileKillsInWindow = 0;
            recentHostileKills.clear();
        }
        hostileKillsInWindow++;
        int sameTypeKills = recentHostileKills.merge(entityType, 1, Integer::sum);
        if (hostileKillsInWindow > 40 || sameTypeKills > 16) return 0;
        if (hostileKillsInWindow > 24 || sameTypeKills > 8) return Math.max(1, baseExperience / 4);
        return baseExperience;
    }

    public static int experienceRequired(int currentLevel) {
        int offset = Math.max(0, currentLevel - 1);
        return 100 + 30 * offset + 5 * offset * offset;
    }

    public Set<String> unlockedSkills() {
        Set<String> effectiveSkills = new HashSet<>(unlockedSkills);
        PlayerClass playerClass = PlayerClassRegistry.get(playerClassId);
        if (playerClass != null) effectiveSkills.addAll(playerClass.automaticSkillIds());
        effectiveSkills.addAll(GeneralSkillTree.INSTANCE.automaticSkillIds());
        return Set.copyOf(effectiveSkills);
    }

    public boolean hasSkill(String id) {
        return unlockedSkills().contains(id);
    }

    public int totalBonus(SkillBonusType type) {
        PlayerClass playerClass = PlayerClassRegistry.get(playerClassId);
        int classBonus = playerClass == null ? 0 : playerClass.totalBonus(type, unlockedSkills());
        return classBonus + GeneralSkillTree.INSTANCE.totalBonus(type, unlockedSkills());
    }

    public boolean hasEffect(SkillEffect effect) {
        return GeneralSkillTree.INSTANCE.skills().stream()
                .anyMatch(skill -> skill.effect() == effect && hasSkill(skill.id()));
    }

    public boolean choosePlayerClass(String id) {
        if (!playerClassId.isEmpty() || PlayerClassRegistry.get(id) == null) return false;
        playerClassId = id;
        return true;
    }

    public boolean unlock(String id) {
        PlayerClass playerClass = PlayerClassRegistry.get(playerClassId);
        SkillTreeDefinition tree = playerClass != null && playerClass.findSkill(id) != null
                ? playerClass : GeneralSkillTree.INSTANCE.findSkill(id) != null ? GeneralSkillTree.INSTANCE : null;
        if (tree == null || hasSkill(id)) return false;
        Skill skill = tree.findSkill(id);
        if (skill == null || skill.automaticallyUnlocked() || skillPoints < skill.cost()
                || !skill.prerequisitesMet(unlockedSkills()) || skill.conflictsWith(unlockedSkills())) return false;
        skillPoints -= skill.cost();
        boolean added = unlockedSkills.add(id);
        AlternateAttackDefinition attack = AlternateAttackDefinition.forSkill(id);
        if (added && attack != null && selectedAlternateAttack.isEmpty()) selectedAlternateAttack = attack.id();
        return added;
    }

    public boolean selectAlternateAttack(String id) {
        AlternateAttackDefinition attack = AlternateAttackDefinition.find(id);
        if (attack == null || !hasSkill(attack.skillId())) return false;
        selectedAlternateAttack = id;
        return true;
    }

    public int resetProgression() {
        int refunded = 0;
        for (String skillId : unlockedSkills) {
            Skill skill = null;
            for (PlayerClass playerClass : PlayerClassRegistry.all()) {
                if (playerClass.findSkill(skillId) != null) { skill = playerClass.findSkill(skillId); break; }
            }
            if (skill == null) skill = GeneralSkillTree.INSTANCE.findSkill(skillId);
            if (skill != null) refunded += skill.cost();
        }
        skillPoints += refunded;
        unlockedSkills.clear();
        playerClassId = "";
        selectedAlternateAttack = "";
        movementSpeedLimitPercent = 100;
        moddedMovementSpeedEnabled = true;
        return refunded;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerClass", playerClassId);
        tag.putInt("SkillPoints", skillPoints);
        tag.putInt("Level", level);
        tag.putInt("ProgressionExperience", progressionExperience);
        tag.putString("SelectedAlternateAttack", selectedAlternateAttack);
        tag.putInt("MovementSpeedLimitPercent", movementSpeedLimitPercent);
        tag.putBoolean("ModdedMovementSpeedEnabled", moddedMovementSpeedEnabled);
        tag.putBoolean("ReceivedStartingArmor", receivedStartingArmor);
        ListTag skills = new ListTag();
        unlockedSkills.forEach(id -> skills.add(StringTag.valueOf(id)));
        tag.put("UnlockedSkills", skills);
        ListTag chunks = new ListTag();
        discoveredChunks.forEach(chunk -> chunks.add(StringTag.valueOf(chunk)));
        tag.put("DiscoveredChunks", chunks);
        CompoundTag kills = new CompoundTag();
        recentHostileKills.forEach(kills::putInt);
        tag.put("RecentHostileKills", kills);
        tag.putLong("HostileKillWindowStart", hostileKillWindowStart);
        tag.putInt("HostileKillsInWindow", hostileKillsInWindow);
        return tag;
    }

    public static PlayerProgress load(CompoundTag tag) {
        PlayerProgress progress = new PlayerProgress();
        progress.playerClassId = tag.getString("PlayerClass");
        progress.skillPoints = tag.contains("SkillPoints") ? tag.getInt("SkillPoints") : STARTING_SKILL_POINTS;
        progress.level = tag.contains("Level") ? Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level"))) : 1;
        progress.progressionExperience = tag.contains("ProgressionExperience")
                ? Math.max(0, tag.getInt("ProgressionExperience")) : 0;
        progress.selectedAlternateAttack = tag.getString("SelectedAlternateAttack");
        progress.receivedStartingArmor = tag.getBoolean("ReceivedStartingArmor");
        if(tag.contains("MovementSpeedLimitPercent"))progress.movementSpeedLimitPercent=Math.max(10,Math.min(100,tag.getInt("MovementSpeedLimitPercent")));
        if(tag.contains("ModdedMovementSpeedEnabled"))progress.moddedMovementSpeedEnabled=tag.getBoolean("ModdedMovementSpeedEnabled");
        ListTag skills = tag.getList("UnlockedSkills", Tag.TAG_STRING);
        for (int i = 0; i < skills.size(); i++) progress.unlockedSkills.add(skills.getString(i));
        ListTag chunks = tag.getList("DiscoveredChunks", Tag.TAG_STRING);
        for (int i = 0; i < chunks.size() && i < 8192; i++) progress.discoveredChunks.add(chunks.getString(i));
        CompoundTag kills = tag.getCompound("RecentHostileKills");
        for (String key : kills.getAllKeys()) progress.recentHostileKills.put(key, kills.getInt(key));
        progress.hostileKillWindowStart = tag.getLong("HostileKillWindowStart");
        progress.hostileKillsInWindow = Math.max(0, tag.getInt("HostileKillsInWindow"));
        return progress;
    }
}
