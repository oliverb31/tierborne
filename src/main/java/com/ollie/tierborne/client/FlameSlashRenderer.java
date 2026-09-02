package com.ollie.tierborne.client;

import com.ollie.tierborne.entity.FlameSlashProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class FlameSlashRenderer extends ThrownItemRenderer<FlameSlashProjectile> {
    public FlameSlashRenderer(EntityRendererProvider.Context context) {
        super(context, 0.6F, true);
    }
}
