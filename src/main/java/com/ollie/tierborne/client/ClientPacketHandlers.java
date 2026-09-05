package com.ollie.tierborne.client;

import com.ollie.tierborne.client.screen.OrcishAltarScreen;
import com.ollie.tierborne.client.screen.OrcishAltarSchematicScreen;
import com.ollie.tierborne.client.screen.DungeonMarkerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openOrcishAltarScreen(BlockPos altarPosition, boolean structureComplete) {
        Minecraft.getInstance().setScreen(structureComplete
                ? new OrcishAltarScreen(altarPosition)
                : new OrcishAltarSchematicScreen());
    }

    public static void openDungeonMarkerScreen(BlockPos floorPosition, int existingMarkers) {
        Minecraft.getInstance().setScreen(new DungeonMarkerScreen(floorPosition, existingMarkers));
    }
}
