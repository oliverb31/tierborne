package com.ollie.tierborne.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class CombatHitboxes {
    private static final double PLAYER_HALF_WIDTH = 0.3D;
    private static final double PLAYER_HEIGHT = 1.8D;

    private CombatHitboxes() {
    }

    static AABB standardPlayer(Player player) {
        return new AABB(player.getX() - PLAYER_HALF_WIDTH, player.getY(),
                player.getZ() - PLAYER_HALF_WIDTH, player.getX() + PLAYER_HALF_WIDTH,
                player.getY() + PLAYER_HEIGHT, player.getZ() + PLAYER_HALF_WIDTH);
    }

    static boolean intersectsHorizontalRadius(Vec3 center, Player player, double radius) {
        AABB box = standardPlayer(player);
        double closestX = clamp(center.x, box.minX, box.maxX);
        double closestZ = clamp(center.z, box.minZ, box.maxZ);
        double deltaX = closestX - center.x;
        double deltaZ = closestZ - center.z;
        return deltaX * deltaX + deltaZ * deltaZ <= radius * radius;
    }

    static boolean intersectsCone(Vec3 center, Vec3 forward, Player player,
                                  double radius, double minimumDot) {
        AABB box = standardPlayer(player);
        double[] xValues = {box.minX, player.getX(), box.maxX};
        double[] zValues = {box.minZ, player.getZ(), box.maxZ};
        for (double x : xValues) {
            for (double z : zValues) {
                Vec3 offset = new Vec3(x - center.x, 0.0D, z - center.z);
                double lengthSquared = offset.lengthSqr();
                if (lengthSquared <= radius * radius && lengthSquared > 0.0001D
                        && forward.dot(offset.normalize()) >= minimumDot) return true;
            }
        }
        return standardPlayer(player).contains(center);
    }

    static boolean hasLineOfSightToPlayer(Entity attacker, Player player) {
        Level level = attacker.level;
        Vec3 start = attacker.getEyePosition();
        AABB box = standardPlayer(player);
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double[] heights = {0.25D, 0.9D, 1.6D};
        for (double height : heights) {
            Vec3 end = new Vec3(centerX, player.getY() + height, centerZ);
            HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, attacker));
            if (hit.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    static EntityHitResult firstStandardPlayerHit(Projectile projectile, Vec3 start, Vec3 end) {
        AABB search = new AABB(start, end).inflate(0.75D);
        EntityHitResult closest = null;
        double closestDistance = Double.MAX_VALUE;
        double projectilePadding = projectile.getBbWidth() * 0.5D + 0.05D;
        for (Player player : projectile.level.getEntitiesOfClass(Player.class, search,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            if (player == projectile.getOwner()) continue;
            java.util.Optional<Vec3> intersection = standardPlayer(player).inflate(projectilePadding).clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceToSqr(intersection.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = new EntityHitResult(player, intersection.get());
            }
        }
        return closest;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
