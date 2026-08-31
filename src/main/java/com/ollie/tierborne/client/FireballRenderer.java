package com.ollie.tierborne.client;

import com.ollie.tierborne.entity.FireballProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class FireballRenderer extends ThrownItemRenderer<FireballProjectile> {
    public FireballRenderer(EntityRendererProvider.Context context) {
        super(context, 0.75F, true);
        shadowRadius = 0.0F;
    }
}
