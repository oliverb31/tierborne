package com.ollie.tierborne;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.item.ModItems;
import com.ollie.tierborne.item.RemovedVanillaArmor;
import com.ollie.tierborne.registry.ModMenus;
import com.ollie.tierborne.registry.ModRecipes;
import com.ollie.tierborne.registry.ModAttributes;
import com.ollie.tierborne.registry.ModEffects;
import com.ollie.tierborne.registry.ModBlocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
@Mod(Tierborne.MOD_ID)
public final class Tierborne {
 public static final String MOD_ID = "tierborne";
 public static final Logger LOGGER = LogUtils.getLogger();
 public Tierborne(FMLJavaModLoadingContext context) { context.getModEventBus().addListener(this::setup); context.getModEventBus().addListener(ModAttributes::addEntityAttributes); context.getModEventBus().addListener(com.ollie.tierborne.entity.ModEntities::createAttributes); ModBlocks.BLOCKS.register(context.getModEventBus()); ModItems.ITEMS.register(context.getModEventBus()); ModAttributes.ATTRIBUTES.register(context.getModEventBus()); ModEffects.EFFECTS.register(context.getModEventBus()); ModMenus.MENU_TYPES.register(context.getModEventBus()); ModRecipes.RECIPE_TYPES.register(context.getModEventBus()); ModRecipes.RECIPE_SERIALIZERS.register(context.getModEventBus()); com.ollie.tierborne.entity.ModEntities.ENTITIES.register(context.getModEventBus()); context.registerConfig(ModConfig.Type.SERVER, com.ollie.tierborne.config.RpgBalanceConfig.SPEC, "tierborne-balance.toml"); MinecraftForge.EVENT_BUS.register(new TierborneEvents()); MinecraftForge.EVENT_BUS.register(new com.ollie.tierborne.dungeon.DungeonEvents()); MinecraftForge.EVENT_BUS.register(new com.ollie.tierborne.dungeon.DungeonMapPackInstaller()); MinecraftForge.EVENT_BUS.register(new com.ollie.tierborne.raid.OrcPillagerReplacementEvents()); }
 private void setup(FMLCommonSetupEvent event) { event.enqueueWork(() -> { ModNetwork.register(); RemovedVanillaArmor.hideFromCreativeTabs(); }); }
}
