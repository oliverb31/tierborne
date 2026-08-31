package com.ollie.tierborne.combat;
public record AbilityStatus(String name, int remainingTicks, int totalTicks, boolean active, String stateLabel) {}
