package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.client.screen.ArmorUpgradeScreen;
import com.ollie.tierborne.entity.ModEntities;
import com.ollie.tierborne.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Tierborne.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIREBALL.get(), FireballRenderer::new);
        event.registerEntityRenderer(ModEntities.FLAME_SLASH.get(), FlameSlashRenderer::new);
        event.registerEntityRenderer(ModEntities.DUNE_REVENANT.get(), DuneRevenantRenderer::new);
        event.registerEntityRenderer(ModEntities.FROSTBOUND_ARCHER.get(), FrostboundArcherRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNEBOUND_COLOSSUS.get(), RuneboundColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_WATCHER.get(), AbyssalWatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.GOOFY_GOBLIN.get(), GoofyGoblinRenderer::new);
        event.registerEntityRenderer(ModEntities.ORC_WARRIOR.get(),
                context -> new OrcRenderer(context, "orc_warrior_am", 0.55F));
        event.registerEntityRenderer(ModEntities.ORC_SPEARTHROWER.get(),
                context -> new OrcRenderer(context, "orc_spearthrower_am", 0.55F));
        event.registerEntityRenderer(ModEntities.ORC_SHAMAN.get(),
                context -> new OrcRenderer(context, "orc_shaman_am", 0.55F));
        event.registerEntityRenderer(ModEntities.ORC_ELITE.get(),
                context -> new OrcRenderer(context, "orc_elite_am", 0.65F));
        event.registerEntityRenderer(ModEntities.ORC_BOSS.get(),
                context -> new OrcRenderer(context, "orc_boss_am", 1.44F));
        event.registerEntityRenderer(ModEntities.ORC_PROJECTILE.get(), OrcProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ARMOR_UPGRADE.get(), ArmorUpgradeScreen::new));
    }
}
