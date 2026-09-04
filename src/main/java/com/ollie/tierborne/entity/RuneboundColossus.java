package com.ollie.tierborne.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class RuneboundColossus extends IronGolem {
    private static final double STOMP_RADIUS = 5.0D;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public RuneboundColossus(EntityType<? extends RuneboundColossus> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 100;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        LivingEntity target = this.getTarget();
        if (target != null && this.tickCount % 80 == 0 && this.distanceToSqr(target) <= STOMP_RADIUS * STOMP_RADIUS) {
            stompNearbyEnemies();
        }
    }

    private void stompNearbyEnemies() {
        for (Player player : this.level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(STOMP_RADIUS), Player::isAlive)) {
            if (player.isCreative() || player.isSpectator()) continue;
            player.hurt(DamageSource.mobAttack(this), 8.0F);
            Vec3 direction = player.position().subtract(this.position());
            double horizontalLength = Math.max(0.1D, Math.sqrt(direction.x * direction.x + direction.z * direction.z));
            player.push(direction.x / horizontalLength * 1.1D, 0.55D, direction.z / horizontalLength * 1.1D);
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
}
