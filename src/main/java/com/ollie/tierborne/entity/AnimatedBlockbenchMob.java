package com.ollie.tierborne.entity;

public interface AnimatedBlockbenchMob {
    String getAnimationName();

    int getAnimationStartTick();

    boolean hasActiveAttackAnimation();
}
