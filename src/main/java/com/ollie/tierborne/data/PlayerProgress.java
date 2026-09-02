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
import java.util.Set;

public final class PlayerProgress {
    private String playerClassId = "";
    private int skillPoints = 20;
    private final Set<String> unlockedSkills = new HashSet<>();
    private String selectedAlternateAttack = "";
    private int movementSpeedLimitPercent = 100;
    private boolean receivedStartingArmor;

    public String playerClassId() { return playerClassId; }
    public int skillPoints() { return skillPoints; }
    public String selectedAlternateAttack() { return selectedAlternateAttack; }
    public int movementSpeedLimitPercent() { return movementSpeedLimitPercent; }
    public boolean receivedStartingArmor() { return receivedStartingArmor; }
    public void markReceivedStartingArmor() { receivedStartingArmor = true; }
    public boolean setMovementSpeedLimitPercent(int value) { int clamped=Math.max(10,Math.min(100,value));if(clamped==movementSpeedLimitPercent)return false;movementSpeedLimitPercent=clamped;return true; }

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
        return refunded;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("PlayerClass", playerClassId);
        tag.putInt("SkillPoints", skillPoints);
        tag.putString("SelectedAlternateAttack", selectedAlternateAttack);
        tag.putInt("MovementSpeedLimitPercent", movementSpeedLimitPercent);
        tag.putBoolean("ReceivedStartingArmor", receivedStartingArmor);
        ListTag skills = new ListTag();
        unlockedSkills.forEach(id -> skills.add(StringTag.valueOf(id)));
        tag.put("UnlockedSkills", skills);
        return tag;
    }

    public static PlayerProgress load(CompoundTag tag) {
        PlayerProgress progress = new PlayerProgress();
        progress.playerClassId = tag.getString("PlayerClass");
        progress.skillPoints = tag.getInt("SkillPoints");
        progress.selectedAlternateAttack = tag.getString("SelectedAlternateAttack");
        progress.receivedStartingArmor = tag.getBoolean("ReceivedStartingArmor");
        if(tag.contains("MovementSpeedLimitPercent"))progress.movementSpeedLimitPercent=Math.max(10,Math.min(100,tag.getInt("MovementSpeedLimitPercent")));
        ListTag skills = tag.getList("UnlockedSkills", Tag.TAG_STRING);
        for (int i = 0; i < skills.size(); i++) progress.unlockedSkills.add(skills.getString(i));
        return progress;
    }
}
