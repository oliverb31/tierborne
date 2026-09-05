package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Tierborne.MOD_ID);

    public static final RegistryObject<Attribute> MAGIC_DAMAGE = ATTRIBUTES.register("magic_damage",
            () -> new RangedAttribute("attribute.name.tierborne.magic_damage", 1.0D, 0.0D, 10.0D)
                    .setSyncable(true));
    public static final RegistryObject<Attribute> DAMAGE_REDUCTION = ATTRIBUTES.register("damage_reduction",
            () -> new RangedAttribute("attribute.name.tierborne.damage_reduction", 1.0D, 0.0D, 2.0D)
                    .setSyncable(true));

    private ModAttributes() {}

    public static void addEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAGIC_DAMAGE.get());
        event.add(EntityType.PLAYER, DAMAGE_REDUCTION.get());
    }
}
