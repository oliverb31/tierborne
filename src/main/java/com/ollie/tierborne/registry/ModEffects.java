package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tierborne.MOD_ID);
    public static final RegistryObject<MobEffect> BLEED = EFFECTS.register("bleed",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9E1B32) {});

    private ModEffects() {}
}
