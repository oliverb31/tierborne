package com.ollie.tierborne.dungeon;

import com.ollie.tierborne.Tierborne;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DungeonMapPackInstaller {
    private static final String BUNDLED_PACK_DIRECTORY = "dungeon_maps";
    private static final String INSTALLED_PACK_DIRECTORY = "tierborne-dungeon-maps";
    private static final String INSTALLED_PACK_ID = "file/" + INSTALLED_PACK_DIRECTORY;

    @SubscribeEvent
    public void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Level.OVERWORLD)) return;

        MinecraftServer server = level.getServer();
        try {
            Path packSource = ModList.get().getModFileById(Tierborne.MOD_ID).getFile()
                    .findResource(BUNDLED_PACK_DIRECTORY);
            Path packDestination = server.getWorldPath(LevelResource.DATAPACK_DIR)
                    .resolve(INSTALLED_PACK_DIRECTORY);
            copyPack(packSource, packDestination);
            enableInstalledPack(server);
            Tierborne.LOGGER.info("Installed Tierborne dungeon maps in new world {}",
                    server.getWorldPath(LevelResource.ROOT));
        } catch (IOException | RuntimeException exception) {
            Tierborne.LOGGER.error("Could not install Tierborne dungeon maps in the new world", exception);
        }
    }

    private static void copyPack(Path source, Path destination) throws IOException {
        if (Files.exists(destination)) return;

        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
    }

    private static void enableInstalledPack(MinecraftServer server) {
        server.getPackRepository().reload();
        List<String> selectedPacks = new ArrayList<>(server.getPackRepository().getSelectedIds());
        if (!selectedPacks.contains(INSTALLED_PACK_ID)) selectedPacks.add(INSTALLED_PACK_ID);
        server.reloadResources(selectedPacks).join();
        DungeonManager.reload(server);
    }
}
