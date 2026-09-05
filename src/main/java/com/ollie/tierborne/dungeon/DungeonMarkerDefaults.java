package com.ollie.tierborne.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ollie.tierborne.Tierborne;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DungeonMarkerDefaults {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve("tierborne-dungeon-markers");
    private static Map<String, List<DungeonMarkerSavedData.MobMarker>> markers = Map.of();

    private DungeonMarkerDefaults() {
    }

    public static void reload(MinecraftServer server) {
        Map<String, List<DungeonMarkerSavedData.MobMarker>> loaded = new HashMap<>();
        server.getResourceManager().listResources("dungeon_markers",
                        location -> location.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        loadJson(loaded, GSON.fromJson(reader, JsonObject.class));
                    } catch (IOException | RuntimeException exception) {
                        throw new IllegalStateException("Invalid dungeon marker resource " + location, exception);
                    }
                });
        if (Files.isDirectory(CONFIG_DIRECTORY)) {
            try (var files = Files.list(CONFIG_DIRECTORY)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> {
                            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                                loadJson(loaded, GSON.fromJson(reader, JsonObject.class));
                            } catch (IOException | RuntimeException exception) {
                                throw new IllegalStateException("Invalid shared dungeon marker file " + path,
                                        exception);
                            }
                        });
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read shared dungeon marker directory", exception);
            }
        }
        markers = Map.copyOf(loaded);
        Tierborne.LOGGER.info("Loaded {} bundled dungeon mob markers",
                markers.values().stream().mapToInt(List::size).sum());
    }

    public static List<DungeonMarkerSavedData.MobMarker> markers(String dungeon) {
        return markers.getOrDefault(dungeon, List.of());
    }

    public static void saveGlobal(String dungeon, List<DungeonMarkerSavedData.MobMarker> dungeonMarkers)
            throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("dungeon", dungeon);
        JsonArray entries = new JsonArray();
        for (DungeonMarkerSavedData.MobMarker marker : dungeonMarkers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", marker.x());
            entry.addProperty("y", marker.y());
            entry.addProperty("z", marker.z());
            entry.addProperty("mob", marker.mob());
            entry.addProperty("yaw", marker.yaw());
            entries.add(entry);
        }
        root.add("markers", entries);
        Files.createDirectories(CONFIG_DIRECTORY);
        Files.writeString(CONFIG_DIRECTORY.resolve(safeFileName(dungeon) + ".json"),
                GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        Map<String, List<DungeonMarkerSavedData.MobMarker>> updated = new HashMap<>(markers);
        updated.put(dungeon, List.copyOf(dungeonMarkers));
        markers = Map.copyOf(updated);
    }

    private static void loadJson(Map<String, List<DungeonMarkerSavedData.MobMarker>> loaded, JsonObject json) {
        if (json == null || !json.has("format") || json.get("format").getAsInt() != 1) {
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
    }

    private static String safeFileName(String dungeon) {
        return dungeon.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
