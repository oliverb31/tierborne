package com.ollie.tierborne.playerclass;

/** A numerical bonus contribution used by gameplay effects and generic confirmation UI. */
public record SkillUpgrade(SkillBonusType type, int percentagePoints) {
}
