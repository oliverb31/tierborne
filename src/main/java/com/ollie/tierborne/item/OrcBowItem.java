package com.ollie.tierborne.item;

import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;

public final class OrcBowItem extends BowItem {
    public OrcBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        double multiplier = 1.0D + RpgBalanceConfig.ORC_BOW_DAMAGE_BONUS_PERCENT.get() / 100.0D;
        arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
        return arrow;
    }
}
