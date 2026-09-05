package com.ollie.tierborne.playerclass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerClassRegistry {
    private static final Map<String, PlayerClass> PLAYER_CLASSES = new LinkedHashMap<>();

    static {
        register(new SwordsmanPlayerClass());
        register(new ArcherPlayerClass());
        register(new FighterPlayerClass());
        register(new BarbarianPlayerClass());
        register(new MagePlayerClass());
    }

    private PlayerClassRegistry() {}

    private static void register(PlayerClass playerClass) {
        PLAYER_CLASSES.put(playerClass.id(), playerClass);
    }

    public static PlayerClass get(String id) {
        return PLAYER_CLASSES.get(id);
    }

    public static List<PlayerClass> all() {
        return List.copyOf(PLAYER_CLASSES.values());
    }
}
