package com.ollie.tierborne.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.ollie.tierborne.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TierborneArmorItem extends ArmorItem {
    /** Armor is folded into the base value; skill-tree percentages are applied later. */
    private static final AttributeModifier.Operation ARMOR_BONUS_OPERATION =
            AttributeModifier.Operation.MULTIPLY_BASE;
    private static final Map<EquipmentSlot, UUID> BONUS_UUIDS = new EnumMap<>(EquipmentSlot.class);

    static {
        BONUS_UUIDS.put(EquipmentSlot.HEAD, UUID.fromString("2c94897f-d8fb-4fab-a235-e72086b6d251"));
        BONUS_UUIDS.put(EquipmentSlot.CHEST, UUID.fromString("9de05dc8-ca43-44cd-b53b-95eada88dfe8"));
        BONUS_UUIDS.put(EquipmentSlot.LEGS, UUID.fromString("21580ae4-8ac1-4205-b116-22bbd2816c5e"));
        BONUS_UUIDS.put(EquipmentSlot.FEET, UUID.fromString("38f1400d-14e7-4f03-8e47-d50fdcbcd587"));
    }

    private final ModArmorMaterial tierborneMaterial;

    public TierborneArmorItem(ModArmorMaterial material, EquipmentSlot slot, Properties properties) {
        super(material, slot, properties);
        this.tierborneMaterial = material;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> baseModifiers = super.getDefaultAttributeModifiers(slot);
        if (slot != this.slot || tierborneMaterial.path() == ModArmorMaterial.ArmorPath.NONE) {
            return baseModifiers;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
        modifiers.putAll(baseModifiers);
        modifiers.put(pathAttribute(), new AttributeModifier(BONUS_UUIDS.get(slot),
                "Tierborne armour path bonus", tierborneMaterial.bonusPerPiece(),
                ARMOR_BONUS_OPERATION));
        return modifiers.build();
    }

    private Attribute pathAttribute() {
        return switch (tierborneMaterial.path()) {
            case SPEED -> Attributes.MOVEMENT_SPEED;
            case MAGIC -> ModAttributes.MAGIC_DAMAGE.get();
            case STRENGTH -> Attributes.ATTACK_DAMAGE;
            case DAMAGE_REDUCTION -> ModAttributes.DAMAGE_REDUCTION.get();
            case NONE -> throw new IllegalStateException("Armour without a path has no bonus attribute");
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tierborneMaterial.tier() == 5 || super.isFoil(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tierborne.armor_tier", tierborneMaterial.tier())
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
