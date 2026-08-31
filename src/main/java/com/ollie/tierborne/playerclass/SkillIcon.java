package com.ollie.tierborne.playerclass;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;

public record SkillIcon(Item item, MobEffect effect) {
    public static SkillIcon item(Item item) {
        return new SkillIcon(item, null);
    }

    public static SkillIcon effect(MobEffect effect) {
        return new SkillIcon(null, effect);
    }

    public boolean isEffect() {
        return effect != null;
    }
}
