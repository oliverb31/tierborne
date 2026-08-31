package com.ollie.tierborne;
import com.ollie.tierborne.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod(Tierborne.MOD_ID)
public final class Tierborne {
 public static final String MOD_ID = "tierborne";
 public Tierborne(FMLJavaModLoadingContext context) { context.getModEventBus().addListener(this::setup); com.ollie.tierborne.entity.ModEntities.ENTITIES.register(context.getModEventBus()); context.registerConfig(ModConfig.Type.SERVER, com.ollie.tierborne.config.RpgBalanceConfig.SPEC, "tierborne-balance.toml"); MinecraftForge.EVENT_BUS.register(new TierborneEvents()); }
 private void setup(FMLCommonSetupEvent event) { event.enqueueWork(ModNetwork::register); }
}
