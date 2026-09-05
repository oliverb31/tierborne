package com.ollie.tierborne.item;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;

public enum ModArmorMaterial implements ArmorMaterial {
    COPPER("copper", 2, 15, defenses(1, 4, 3, 2), 0.0F, 0.0F, ArmorPath.NONE, 0.0D),

    SILVER("silver", 4, 24, defenses(2, 6, 5, 3), 1.0F, 0.0F, ArmorPath.SPEED, 0.015D),
    URU("uru", 4, 24, defenses(2, 6, 5, 3), 1.0F, 0.0F, ArmorPath.MAGIC, 0.025D),
    STEEL("steel", 4, 24, defenses(2, 6, 5, 3), 1.0F, 0.0F, ArmorPath.STRENGTH, 0.015D),
    TUNGSTEN("tungsten", 4, 24, defenses(2, 6, 5, 3), 1.0F, 0.0F,
            ArmorPath.DAMAGE_REDUCTION, 0.015D),

    MITHRIL("mithril", 5, 35, defenses(3, 8, 6, 3), 3.0F, 0.05F, ArmorPath.SPEED, 0.03D),
    RUNIC("runic", 5, 35, defenses(3, 8, 6, 3), 3.0F, 0.05F, ArmorPath.MAGIC, 0.05D),
    ORICHALCUM("orichalcum", 5, 35, defenses(3, 8, 6, 3), 3.0F, 0.05F,
            ArmorPath.STRENGTH, 0.03D),
    ADAMANTITE("adamantite", 5, 35, defenses(3, 8, 6, 3), 3.0F, 0.05F,
            ArmorPath.DAMAGE_REDUCTION, 0.03D);

    private static final Map<EquipmentSlot, Integer> BASE_DURABILITY = new EnumMap<>(EquipmentSlot.class);

    static {
        BASE_DURABILITY.put(EquipmentSlot.HEAD, 11);
        BASE_DURABILITY.put(EquipmentSlot.CHEST, 16);
        BASE_DURABILITY.put(EquipmentSlot.LEGS, 15);
        BASE_DURABILITY.put(EquipmentSlot.FEET, 13);
    }

    private final String textureName;
    private final int tier;
    private final int durabilityMultiplier;
    private final Map<EquipmentSlot, Integer> defenses;
    private final float toughness;
    private final float knockbackResistance;
    private final ArmorPath path;
    private final double bonusPerPiece;

    ModArmorMaterial(String textureName, int tier, int durabilityMultiplier,
                     Map<EquipmentSlot, Integer> defenses, float toughness,
                     float knockbackResistance, ArmorPath path, double bonusPerPiece) {
        this.textureName = Tierborne.MOD_ID + ":" + textureName;
        this.tier = tier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defenses = defenses;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.path = path;
        this.bonusPerPiece = bonusPerPiece;
    }

    private static Map<EquipmentSlot, Integer> defenses(int feet, int chest, int legs, int head) {
        Map<EquipmentSlot, Integer> values = new EnumMap<>(EquipmentSlot.class);
        values.put(EquipmentSlot.FEET, feet);
        values.put(EquipmentSlot.CHEST, chest);
        values.put(EquipmentSlot.LEGS, legs);
        values.put(EquipmentSlot.HEAD, head);
        return values;
    }

    public int tier() {
        return tier;
    }

    public ArmorPath path() {
        return path;
    }

    public double bonusPerPiece() {
        return bonusPerPiece * configuredMultiplier(RpgBalanceConfig.ARMOR_BUFF_MULTIPLIER);
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return BASE_DURABILITY.getOrDefault(slot, 0) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return Math.max(0, (int) Math.round(defenses.getOrDefault(slot, 0)
                * configuredMultiplier(RpgBalanceConfig.ARMOR_DEFENSE_MULTIPLIER)));
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
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
        return toughness * (float) configuredMultiplier(RpgBalanceConfig.ARMOR_DEFENSE_MULTIPLIER);
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance * (float) configuredMultiplier(RpgBalanceConfig.ARMOR_BUFF_MULTIPLIER);
    }

    private static double configuredMultiplier(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value) {
        return RpgBalanceConfig.SPEC.isLoaded() ? value.get() : 1.0D;
    }

    public enum ArmorPath {
        NONE,
        SPEED,
        MAGIC,
        STRENGTH,
        DAMAGE_REDUCTION
    }
}
