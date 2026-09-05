package com.ollie.tierborne.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientUppercutState {
    private static final Map<Integer, Animation> ANIMATIONS = new HashMap<>();

    private ClientUppercutState() {}

    public static void start(int entityId, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ANIMATIONS.put(entityId, new Animation(minecraft.level.getGameTime(), Math.max(1, durationTicks)));
    }

    public static boolean isActive(int entityId) {
        return progress(entityId, 0.0F) < 1.0F;
    }

    public static float progress(int entityId, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Animation animation = ANIMATIONS.get(entityId);
        if (minecraft.level == null || animation == null) return 1.0F;
        float progress = (minecraft.level.getGameTime() + partialTick - animation.startedAt)
                / animation.durationTicks;
        if (progress >= 1.0F) ANIMATIONS.remove(entityId);
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        long now = minecraft.level.getGameTime();
        Iterator<Animation> iterator = ANIMATIONS.values().iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (now >= animation.startedAt + animation.durationTicks) iterator.remove();
        }
    }

    public static void clear() {
        ANIMATIONS.clear();
    }

    private record Animation(long startedAt, float durationTicks) {}
}
