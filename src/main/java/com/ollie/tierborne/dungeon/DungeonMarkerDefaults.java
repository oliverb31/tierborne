package com.ollie.tierborne.dungeon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ollie.tierborne.Tierborne;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DungeonMarkerDefaults {
    private static final Gson GSON = new Gson();
    private static Map<String, List<DungeonMarkerSavedData.MobMarker>> markers = Map.of();

    private DungeonMarkerDefaults() {
    }

    public static void reload(MinecraftServer server) {
        Map<String, List<DungeonMarkerSavedData.MobMarker>> loaded = new HashMap<>();
        server.getResourceManager().listResources("dungeon_markers",
                        location -> location.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        if (json.get("format").getAsInt() != 1) {
                            throw new IllegalArgumentException("Unsupported marker format");
                        }
                        String dungeon = json.get("dungeon").getAsString();
                        JsonArray entries = json.getAsJsonArray("markers");
                        List<DungeonMarkerSavedData.MobMarker> dungeonMarkers = new ArrayList<>();
                        for (JsonElement element : entries) {
                            JsonObject marker = element.getAsJsonObject();
                            dungeonMarkers.add(new DungeonMarkerSavedData.MobMarker(dungeon,
                                    marker.get("x").getAsInt(), marker.get("y").getAsInt(),
                                    marker.get("z").getAsInt(), marker.get("mob").getAsString(),
                                    marker.get("yaw").getAsFloat()));
                        }
                        loaded.put(dungeon, List.copyOf(dungeonMarkers));
                    } catch (IOException | RuntimeException exception) {
                        throw new IllegalStateException("Invalid dungeon marker resource " + location, exception);
                    }
                });
        markers = Map.copyOf(loaded);
        Tierborne.LOGGER.info("Loaded {} bundled dungeon mob markers",
                markers.values().stream().mapToInt(List::size).sum());
    }

    public static List<DungeonMarkerSavedData.MobMarker> markers(String dungeon) {
        return markers.getOrDefault(dungeon, List.of());
    }
}
