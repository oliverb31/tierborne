package com.ollie.tierborne.client;

import java.util.HashSet;
import java.util.Set;

public final class ClientBlockState {
    private static final Set<Integer> BLOCKING = new HashSet<>();

    private ClientBlockState() {}

    public static void set(int entityId, boolean blocking) {
        if (blocking) BLOCKING.add(entityId);
        else BLOCKING.remove(entityId);
    }

    public static boolean isBlocking(int entityId) {
        return BLOCKING.contains(entityId);
    }

    public static void clear() {
        BLOCKING.clear();
    }
}
