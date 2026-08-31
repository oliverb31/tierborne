package com.ollie.tierborne.entity;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tierborne.MOD_ID);
    public static final RegistryObject<EntityType<FireballProjectile>> FIREBALL = ENTITIES.register("fireball",
            () -> EntityType.Builder.<FireballProjectile>of(FireballProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":fireball"));
    private ModEntities() {}
}
