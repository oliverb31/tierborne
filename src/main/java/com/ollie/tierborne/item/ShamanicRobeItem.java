package com.ollie.tierborne.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class ShamanicRobeItem extends ArmorItem {
    private static final UUID MAGIC_BONUS_ID =
            UUID.fromString("d8c698bf-70a7-43c3-bd49-5de2b65a833c");

    public ShamanicRobeItem(Properties properties) {
        super(ArmorMaterials.LEATHER, EquipmentSlot.CHEST, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.CHEST) return ImmutableMultimap.of();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
        modifiers.putAll(super.getDefaultAttributeModifiers(slot));
        modifiers.put(ModAttributes.MAGIC_DAMAGE.get(), new AttributeModifier(MAGIC_BONUS_ID,
                "Shamanic robe magic damage", RpgBalanceConfig.SHAMANIC_ROBE_MAGIC_DAMAGE_PERCENT.get() / 100.0D,
                AttributeModifier.Operation.MULTIPLY_BASE));
        return modifiers.build();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tierborne.shamanic_robe")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
