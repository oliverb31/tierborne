package com.ollie.tierborne.playerclass;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** Immutable, render-independent definition of one node in a playerClass skill tree. */
public record Skill(String id, String displayName, String nodeLabel, String description, int cost,
                    ResourceLocation icon, SkillIcon nodeIcon, int treeColumn, int treeRow, List<String> prerequisites,
                    List<String> mutuallyExclusiveWith, boolean automaticallyUnlocked,
                    SkillUpgrade upgrade, SkillEffect effect) {
    public Skill {
        prerequisites = List.copyOf(prerequisites);
        mutuallyExclusiveWith = List.copyOf(mutuallyExclusiveWith);
    }

    public boolean prerequisitesMet(Set<String> unlockedSkills) {
        return unlockedSkills.containsAll(prerequisites);
    }

    public boolean conflictsWith(Set<String> unlockedSkills) {
        return mutuallyExclusiveWith.stream().anyMatch(unlockedSkills::contains);
    }

}
