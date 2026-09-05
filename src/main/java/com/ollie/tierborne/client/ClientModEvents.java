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
        event.registerEntityRenderer(ModEntities.MAGE_VFX.get(), MageVfxRenderer::new);
        event.registerEntityRenderer(ModEntities.FLAME_SLASH.get(), FlameSlashRenderer::new);
        event.registerEntityRenderer(ModEntities.DUNE_REVENANT.get(), DuneRevenantRenderer::new);
        event.registerEntityRenderer(ModEntities.FROSTBOUND_ARCHER.get(), FrostboundArcherRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNEBOUND_COLOSSUS.get(), RuneboundColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_WATCHER.get(), AbyssalWatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.GOOFY_GOBLIN.get(), GoofyGoblinRenderer::new);
        event.registerEntityRenderer(ModEntities.FROSTMITE.get(),
                context -> new IceMobRenderer(context, "frostmite", 1.2F, 1.0F));
        event.registerEntityRenderer(ModEntities.FROZEN_BLAZE.get(),
                context -> new IceMobRenderer(context, "frozen_blaze", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.GNUT.get(),
                context -> new IceMobRenderer(context, "gnut", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.ICE_WITCH.get(),
                context -> new IceMobRenderer(context, "ice_witch", 0.7F, 1.0F));
        event.registerEntityRenderer(ModEntities.ICEOLOGER.get(),
                context -> new IceMobRenderer(context, "iceologer", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.SNOWBALL_SPIRIT.get(),
                context -> new IceMobRenderer(context, "snowball_spirit", 0.35F, 1.0F));
        event.registerEntityRenderer(ModEntities.UNDEAD_ICE_WARRIOR.get(),
                context -> new IceMobRenderer(context, "undead_ice_warrior", 0.9F, 1.0F));
        event.registerEntityRenderer(ModEntities.TARTARUS_YETI.get(),
                context -> new IceMobRenderer(context, "tartarus_yeti", 1.2F, 1.5F));
        event.registerEntityRenderer(ModEntities.ICE_KNIGHT_MINION_SHIELD.get(),
                context -> new IceMobRenderer(context, "ice_knight_minion_shield", 0.8F, 1.25F));
        event.registerEntityRenderer(ModEntities.ICE_KNIGHT_MINION_SPEAR.get(),
                context -> new IceMobRenderer(context, "ice_knight_minion_spear", 0.8F, 1.25F));
        event.registerEntityRenderer(ModEntities.ICE_KNIGHT_MINION_SWORD.get(),
                context -> new IceMobRenderer(context, "ice_knight_minion_sword", 0.8F, 1.25F));
        event.registerEntityRenderer(ModEntities.ICE_KNIGHT.get(),
                context -> new IceMobRenderer(context, "ice_knight", 1.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.ICE_PROJECTILE.get(), IceProjectileRenderer::new);
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
