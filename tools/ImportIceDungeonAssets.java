import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts licensed Blockbench sources into Tierborne's private runtime asset layout. */
public final class ImportIceDungeonAssets {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ImportIceDungeonAssets() {
    }

    public static void main(String[] args) throws IOException {
        Path project = Path.of(args[0]);
        Path audit = project.resolve("build/import-audit-ice-tartarus");
        Path icePack = audit.resolve("ice_mob_pack/ice_mob_pack/MythicMobs/packs/ice_mob_pack/models");
        Path knightPack = audit.resolve("ice_knight/ModelEngine/blueprints/EliteCreatures/Ice Knight");
        Path modelDestination = project.resolve("src/main/resources/assets/tierborne/models/entity");
        Path textureDestination = project.resolve("src/main/resources/assets/tierborne/textures/entity");
        Files.createDirectories(modelDestination);
        Files.createDirectories(textureDestination);

        Map<Path, String> models = new LinkedHashMap<>();
        models.put(icePack.resolve("frostmite.bbmodel"), "frostmite");
        models.put(icePack.resolve("frozen_blaze.bbmodel"), "frozen_blaze");
        models.put(icePack.resolve("gnut.bbmodel"), "gnut");
        models.put(icePack.resolve("ice_witch.bbmodel"), "ice_witch");
        models.put(icePack.resolve("iceologer.bbmodel"), "iceologer");
        models.put(icePack.resolve("snowball_spirit.bbmodel"), "snowball_spirit");
        models.put(icePack.resolve("undead_ice_warrior.bbmodel"), "undead_ice_warrior");
        models.put(icePack.resolve("yeti.bbmodel"), "tartarus_yeti");
        models.put(knightPack.resolve("ice_knight-minion-shield.bbmodel"), "ice_knight_minion_shield");
        models.put(knightPack.resolve("ice_knight-minion-spear.bbmodel"), "ice_knight_minion_spear");
        models.put(knightPack.resolve("ice_knight-minion-sword.bbmodel"), "ice_knight_minion_sword");
        models.put(knightPack.resolve("ice_knight.bbmodel"), "ice_knight");

        for (Map.Entry<Path, String> entry : models.entrySet()) {
            convert(entry.getKey(), entry.getValue(), modelDestination, textureDestination);
        }

        Path sourceSounds = audit.resolve("ice_knight/ItemsAdder/contents/assets/ec_iceknight/sounds");
        Path soundDestination = project.resolve("src/main/resources/assets/tierborne/sounds/ice_knight");
        Files.createDirectories(soundDestination);
        try (var paths = Files.list(sourceSounds)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".ogg"))
                    .forEach(path -> copy(path, soundDestination.resolve(path.getFileName())));
        }
    }

    private static void convert(Path source, String assetName, Path modelDestination,
                                Path textureDestination) throws IOException {
        JsonObject model = GSON.fromJson(Files.readString(source), JsonObject.class);
        JsonArray textureEntries = model.getAsJsonArray("textures");
        List<BufferedImage> textures = new ArrayList<>();
        for (JsonElement textureEntry : textureEntries) {
            String encoded = textureEntry.getAsJsonObject().get("source").getAsString();
            int comma = encoded.indexOf(',');
            if (comma < 0) throw new IOException("Texture is not embedded in " + source);
            byte[] imageBytes = Base64.getDecoder().decode(encoded.substring(comma + 1));
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (image == null) throw new IOException("Unreadable embedded texture in " + source);
            textures.add(image);
        }
        if (textures.isEmpty()) throw new IOException("No embedded texture in " + source);

        BufferedImage atlas = textures.size() == 1 ? textures.get(0) : stackTextures(model, textures);
        ImageIO.write(atlas, "png", textureDestination.resolve(assetName + ".png").toFile());

        JsonArray retainedTexture = new JsonArray();
        retainedTexture.add(textureEntries.get(0));
        model.add("textures", retainedTexture);
        Files.writeString(modelDestination.resolve(assetName + ".bbmodel"), GSON.toJson(model),
                StandardCharsets.UTF_8);
    }

    private static BufferedImage stackTextures(JsonObject model, List<BufferedImage> textures) {
        int width = textures.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int height = textures.stream().mapToInt(BufferedImage::getHeight).sum();
        BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        int pixelOffset = 0;
        for (BufferedImage texture : textures) {
            graphics.drawImage(texture, 0, pixelOffset, null);
            pixelOffset += texture.getHeight();
        }
        graphics.dispose();

        JsonObject resolution = model.getAsJsonObject("resolution");
        float logicalHeight = resolution.get("height").getAsFloat();
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                JsonObject face = faceEntry.getValue().getAsJsonObject();
                if (!face.has("texture") || face.get("texture").isJsonNull()) continue;
                int textureIndex = face.get("texture").getAsInt();
                if (textureIndex <= 0) continue;
                JsonArray uv = face.getAsJsonArray("uv");
                uv.set(1, GSON.toJsonTree(uv.get(1).getAsFloat() + logicalHeight * textureIndex));
                uv.set(3, GSON.toJsonTree(uv.get(3).getAsFloat() + logicalHeight * textureIndex));
                face.addProperty("texture", 0);
            }
        }
        resolution.addProperty("height", logicalHeight * textures.size());
        return atlas;
    }

    private static void copy(Path source, Path destination) {
        try {
            Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not copy " + source, exception);
        }
    }
}
