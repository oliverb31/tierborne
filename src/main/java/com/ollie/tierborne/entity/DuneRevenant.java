package com.ollie.tierborne.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class DuneRevenant extends Zombie {
    public DuneRevenant(EntityType<? extends DuneRevenant> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
        }
        return hurt;
    }
}
