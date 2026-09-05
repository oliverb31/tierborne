import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Recolours the existing armour UVs without changing their dimensions or alpha masks.
 * This keeps the resource generation reproducible while preserving Minecraft's exact UV layout.
 */
public final class GenerateAdamantiumTextures {
    private static final int MINT = 0xB6F4DC;
    private static final int VIOLET = 0x8C83B5;
    private static final Map<Integer, Integer> PALETTE = createPalette();

    private GenerateAdamantiumTextures() {
    }

    public static void main(String[] args) throws IOException {
        Path projectRoot = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path textureRoot = projectRoot.resolve("src/main/resources/assets/tierborne/textures");

        Path[] targets = {
                textureRoot.resolve("models/armor/adamantite_layer_1.png"),
                textureRoot.resolve("models/armor/adamantite_layer_2.png"),
                textureRoot.resolve("item/adamantite_helmet.png"),
                textureRoot.resolve("item/adamantite_chestplate.png"),
                textureRoot.resolve("item/adamantite_leggings.png"),
                textureRoot.resolve("item/adamantite_boots.png")
        };

        BufferedImage[] finished = new BufferedImage[targets.length];
        for (int index = 0; index < targets.length; index++) {
            Path target = targets[index];
            BufferedImage image = ImageIO.read(target.toFile());
            recolour(image);
            addRunes(image, target.getFileName().toString());
            ImageIO.write(image, "png", target.toFile());
            finished[index] = image;
        }

        writePreview(projectRoot.resolve("build/adamantium-textures-preview.png"), finished);
    }

    private static Map<Integer, Integer> createPalette() {
        Map<Integer, Integer> palette = new HashMap<>();

        // Worn armour layers.
        palette.put(0x2F7F3F, 0x050B0C);
        palette.put(0x358F47, 0x071514);
        palette.put(0x41AF57, 0x0A2623);
        palette.put(0x47BF5F, 0x103D37);
        palette.put(0x4BCF67, 0x18574F);
        palette.put(0x4FDF6F, 0x28776B);
        palette.put(0x77EF97, MINT);

        // Inventory icons.
        palette.put(0x174723, 0x050B0C);
        palette.put(0x1F5F2F, 0x071514);
        palette.put(0x3B9F4F, 0x103D37);
        palette.put(0x9FFFBF, MINT);

        // Make repeated runs idempotent.
        for (int colour : new int[] {0x050B0C, 0x071514, 0x0A2623, 0x103D37, 0x18574F,
                0x28776B, MINT, VIOLET}) {
            palette.put(colour, colour);
        }
        return palette;
    }

    private static void recolour(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }

                int rgb = argb & 0xFFFFFF;
                int replacement = PALETTE.getOrDefault(rgb, fallbackColour(rgb));
                image.setRGB(x, y, alpha << 24 | replacement);
            }
        }
    }

    private static int fallbackColour(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int brightness = Math.max(red, Math.max(green, blue));
        if (brightness < 64) {
            return 0x050B0C;
        }
        if (brightness < 112) {
            return 0x071514;
        }
        if (brightness < 160) {
            return 0x0A2623;
        }
        if (brightness < 208) {
            return 0x18574F;
        }
        return MINT;
    }

    private static void addRunes(BufferedImage image, String fileName) {
        switch (fileName) {
            case "adamantite_layer_1.png" -> {
                // Crown rune on helmet front.
                rune(image, VIOLET, 11, 10, 12, 10, 11, 11, 12, 12, 11, 13);
                rune(image, MINT, 12, 11, 11, 12, 12, 13);

                // Angular central chest sigil.
                rune(image, VIOLET, 22, 22, 25, 22, 22, 25, 25, 25, 23, 27, 24, 27);
                rune(image, MINT, 23, 21, 24, 21, 23, 23, 24, 23, 23, 24, 24, 24,
                        23, 25, 24, 25, 23, 26, 24, 26);

                // Small matching marks on arms and boots.
                rune(image, VIOLET, 45, 23, 46, 23, 5, 24, 6, 24);
                rune(image, MINT, 45, 24, 46, 25, 5, 25, 6, 26);
            }
            case "adamantite_layer_2.png" -> {
                rune(image, VIOLET, 4, 22, 7, 22, 20, 22, 23, 22);
                rune(image, MINT, 5, 23, 6, 24, 5, 25, 21, 23, 22, 24, 21, 25);
            }
            case "adamantite_helmet.png" -> {
                rune(image, VIOLET, 6, 5, 9, 5, 6, 8, 9, 8);
                rune(image, MINT, 7, 5, 8, 6, 7, 7, 8, 8);
            }
            case "adamantite_chestplate.png" -> {
                rune(image, VIOLET, 5, 5, 10, 5, 6, 10, 9, 10);
                rune(image, MINT, 7, 5, 8, 5, 6, 7, 9, 7, 7, 8, 8, 8, 7, 9, 8, 9);
            }
            case "adamantite_leggings.png" -> {
                rune(image, VIOLET, 6, 5, 9, 5, 5, 10, 10, 10);
                rune(image, MINT, 7, 6, 8, 7, 7, 8, 6, 11, 9, 11);
            }
            case "adamantite_boots.png" -> {
                rune(image, VIOLET, 4, 10, 7, 10, 8, 10, 11, 10);
                rune(image, MINT, 5, 11, 6, 11, 9, 11, 10, 11);
            }
            default -> {
            }
        }
    }

    private static void rune(BufferedImage image, int rgb, int... coordinates) {
        for (int index = 0; index < coordinates.length; index += 2) {
            int x = coordinates[index];
            int y = coordinates[index + 1];
            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
                continue;
            }
            int alpha = image.getRGB(x, y) >>> 24;
            if (alpha != 0) {
                image.setRGB(x, y, alpha << 24 | rgb);
            }
        }
    }

    private static void writePreview(Path destination, BufferedImage[] images) throws IOException {
        Files.createDirectories(destination.getParent());
        BufferedImage preview = new BufferedImage(800, 620, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        graphics.setColor(new Color(18, 22, 25));
        graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setColor(new Color(225, 235, 232));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.drawString("Adamantium - Runic Deepwarden", 24, 34);

        graphics.drawImage(images[0], 24, 54, 512, 256, null);
        graphics.drawImage(images[1], 24, 326, 512, 256, null);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        graphics.drawString("Equipped armour UV layers", 552, 76);
        for (int index = 2; index < images.length; index++) {
            int y = 94 + (index - 2) * 128;
            graphics.drawImage(images[index], 568, y, 96, 96, null);
        }
        graphics.dispose();
        ImageIO.write(preview, "png", destination.toFile());
    }
}
