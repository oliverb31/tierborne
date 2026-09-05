package com.ollie.tierborne.dungeon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DungeonManifest(
        String name,
        String sourceHash,
        int width,
        int height,
        int length,
        long decodedBlocks,
        long nonAirBlocks,
        int tileSize,
        List<Tile> tiles,
        Entrance entrance
) {
    private static final Gson GSON = new Gson();

    public record Tile(int x, int y, int z, int width, int height, int length,
                       int blocks, ResourceLocation structure) {
    }

    public record Entrance(double x, double y, double z, float yaw, float pitch) {
    }

    public static Map<String, DungeonManifest> loadAll(MinecraftServer server) {
        Map<String, DungeonManifest> manifests = new LinkedHashMap<>();
        server.getResourceManager().listResources("dungeons", location -> location.getPath().endsWith(".json"))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        DungeonManifest manifest = parse(GSON.fromJson(reader, JsonObject.class));
                        manifests.put(manifest.name(), manifest);
                    } catch (RuntimeException | IOException exception) {
                        throw new IllegalStateException("Invalid dungeon manifest " + entry.getKey(), exception);
                    }
                });
        return Map.copyOf(manifests);
    }

    private static DungeonManifest parse(JsonObject json) {
        if (json.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported dungeon manifest format");
        }
        JsonArray dimensions = json.getAsJsonArray("source_dimensions");
        List<Tile> tiles = new ArrayList<>();
        long tileBlockTotal = 0L;
        for (JsonElement element : json.getAsJsonArray("tiles")) {
            JsonObject tile = element.getAsJsonObject();
            JsonArray size = tile.getAsJsonArray("size");
            Tile parsed = new Tile(
                    tile.get("x").getAsInt(), tile.get("y").getAsInt(), tile.get("z").getAsInt(),
                    size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(),
                    tile.get("blocks").getAsInt(), new ResourceLocation(tile.get("structure").getAsString()));
            if (parsed.width() < 1 || parsed.height() < 1 || parsed.length() < 1
                    || parsed.width() > 48 || parsed.height() > 48 || parsed.length() > 48
                    || parsed.blocks() < 1) {
                throw new IllegalArgumentException("Invalid tile bounds or block count");
            }
            tiles.add(parsed);
            tileBlockTotal += parsed.blocks();
        }
        long nonAirBlocks = json.get("non_air_blocks").getAsLong();
        if (tiles.isEmpty() || tileBlockTotal != nonAirBlocks) {
            throw new IllegalArgumentException("Tile block total does not match manifest total");
        }
        long decodedBlocks = json.get("decoded_blocks").getAsLong();
        long expectedDecodedBlocks = (long) dimensions.get(0).getAsInt()
                * dimensions.get(1).getAsInt() * dimensions.get(2).getAsInt();
        if (decodedBlocks != expectedDecodedBlocks) {
            throw new IllegalArgumentException("Decoded source block total does not cover the full dimensions");
        }
        Entrance entrance = null;
        if (json.has("entrance")) {
            JsonObject entranceJson = json.getAsJsonObject("entrance");
            JsonArray position = entranceJson.getAsJsonArray("position");
            entrance = new Entrance(position.get(0).getAsDouble(), position.get(1).getAsDouble(),
                    position.get(2).getAsDouble(), entranceJson.get("yaw").getAsFloat(),
                    entranceJson.get("pitch").getAsFloat());
            if (entrance.x() < 0.0D || entrance.x() >= dimensions.get(0).getAsInt()
                    || entrance.y() < 0.0D || entrance.y() >= dimensions.get(1).getAsInt()
                    || entrance.z() < 0.0D || entrance.z() >= dimensions.get(2).getAsInt()) {
                throw new IllegalArgumentException("Dungeon entrance is outside the source dimensions");
            }
        }
        return new DungeonManifest(
                json.get("name").getAsString(), json.get("source_sha256").getAsString(),
                dimensions.get(0).getAsInt(), dimensions.get(1).getAsInt(), dimensions.get(2).getAsInt(),
                decodedBlocks, nonAirBlocks, json.get("tile_size").getAsInt(),
                List.copyOf(tiles), entrance);
    }
}
