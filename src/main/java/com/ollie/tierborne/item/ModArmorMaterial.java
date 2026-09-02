package com.ollie.tierborne.item;

import com.ollie.tierborne.Tierborne;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;

/**
 * Temporary shared armour values. These are intentionally easy to replace when
 * Tierborne's final equipment progression and repair materials are designed.
 */
public enum ModArmorMaterial implements ArmorMaterial {
    COPPER("copper"),
    SILVER("silver"),
    RUNIC("runic"),
    STEEL("steel"),
    TUNGSTEN("tungsten"),
    MITHRIL("mithril"),
    URU("uru"),
    ORICHALCUM("orichalcum"),
    ADAMANTITE("adamantite");

    private static final int DURABILITY_MULTIPLIER = 18;
    private static final int ENCHANTABILITY = 12;
    private static final float TOUGHNESS = 1.0F;
    private static final float KNOCKBACK_RESISTANCE = 0.0F;
    private static final Map<EquipmentSlot, Integer> BASE_DURABILITY = new EnumMap<>(EquipmentSlot.class);
    private static final Map<EquipmentSlot, Integer> DEFENCE = new EnumMap<>(EquipmentSlot.class);

    static {
        BASE_DURABILITY.put(EquipmentSlot.HEAD, 11);
        BASE_DURABILITY.put(EquipmentSlot.CHEST, 16);
        BASE_DURABILITY.put(EquipmentSlot.LEGS, 15);
        BASE_DURABILITY.put(EquipmentSlot.FEET, 13);

        DEFENCE.put(EquipmentSlot.HEAD, 2);
        DEFENCE.put(EquipmentSlot.CHEST, 6);
        DEFENCE.put(EquipmentSlot.LEGS, 5);
        DEFENCE.put(EquipmentSlot.FEET, 2);
    }

    private final String textureName;

    ModArmorMaterial(String textureName) {
        this.textureName = Tierborne.MOD_ID + ":" + textureName;
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return BASE_DURABILITY.getOrDefault(slot, 0) * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return DEFENCE.getOrDefault(slot, 0);
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTABILITY;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return textureName;
    }

    @Override
    public float getToughness() {
        return TOUGHNESS;
    }

    @Override
    public float getKnockbackResistance() {
        return KNOCKBACK_RESISTANCE;
    }
}
