package com.ollie.tierborne.playerclass;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface SkillTreeDefinition {
    String id();
    String displayName();
    ItemStack iconStack();
    List<Skill> skills();

    default Skill findSkill(String id) {
        return skills().stream().filter(skill -> skill.id().equals(id)).findFirst().orElse(null);
    }

    default Set<String> automaticSkillIds() {
        return skills().stream().filter(Skill::automaticallyUnlocked).map(Skill::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    default int totalBonus(SkillBonusType type, Set<String> unlockedSkills) {
        return skills().stream().filter(skill -> unlockedSkills.contains(skill.id()))
                .map(Skill::upgrade).filter(Objects::nonNull)
                .filter(upgrade -> upgrade.type() == type)
                .mapToInt(SkillUpgrade::percentagePoints).sum();
    }

    default Set<SkillBonusType> displayedBonusTypes() {
        return skills().stream().map(Skill::upgrade).filter(Objects::nonNull)
                .map(SkillUpgrade::type).collect(Collectors.toUnmodifiableSet());
    }
}
