package com.ollie.tierborne.raid;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.entity.ModEntities;
import com.ollie.tierborne.entity.OrcMob;
import com.ollie.tierborne.progression.ProgressionRuntime;
import com.ollie.tierborne.TierborneEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class OrcPillagerReplacementEvents {
    private static final Map<Raid, WaveBarState> RAID_BAR_STATES = new WeakHashMap<>();

    @SubscribeEvent
    public void onVanillaRaiderSpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !isReplacedVanillaMob(event.getEntity())) return;

        Entity original = event.getEntity();
        Raider originalRaider = original instanceof Raider raider ? raider : null;
        Raid raid = originalRaider == null ? null : originalRaider.getCurrentRaid();
        if (raid != null) OrcRaidSavedData.get(level).trackRaid(raid.getId());

        OrcMob replacement = createReplacement(level, original, raid,
                originalRaider == null ? 0 : originalRaider.getWave());
        copyPositionAndIdentity((Mob) original, replacement);

        if (raid == null || originalRaider == null) {
            replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(original.blockPosition()),
                    MobSpawnType.STRUCTURE, null, null);
            copyPatrolState(originalRaider, replacement);
            level.addFreshEntity(replacement);
        } else {
            replaceRaidMember(raid, originalRaider, replacement);
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            OrcRaidSavedData data = OrcRaidSavedData.get(level);
            for (int raidId : data.knownRaids()) {
                Raid raid = level.getRaids().get(raidId);
                if (raid == null || raid.isStopped() || raid.isOver()) {
                    if (raid != null) RAID_BAR_STATES.remove(raid);
                    continue;
                }
                if (!raid.hasFirstWaveSpawned()) continue;
                updateRaidBar(raid);
                int wave = raid.getGroupsSpawned();
                if (wave <= 0 || raid.getTotalRaidersAlive() > 0
                        || !data.claimWaveReward(raidId, wave)) continue;
                rewardNearbyPlayers(level, raid, wave);
            }
        }
    }

    private static void updateRaidBar(Raid raid) {
        int remaining = raid.getTotalRaidersAlive();
        if (remaining <= 0) return;

        int wave = raid.getGroupsSpawned();
        WaveBarState state = RAID_BAR_STATES.get(raid);
        if (state == null || state.wave() != wave || remaining > state.waveSize()) {
            state = new WaveBarState(wave, remaining);
            RAID_BAR_STATES.put(raid, state);
        }

        raid.raidEvent.setProgress(Math.min(1.0F, remaining / (float) state.waveSize()));
        raid.raidEvent.setName(Component.translatable(
                "event.tierborne.orc_raid_remaining", remaining));
    }

    public static boolean isReplacedVanillaMob(Entity entity) {
        return entity instanceof Pillager
                || entity instanceof Witch
                || entity instanceof Ravager
                || entity instanceof Vindicator
                || entity instanceof Evoker
                || entity instanceof Vex;
    }

    private static OrcMob createReplacement(ServerLevel level, Entity original, Raid raid, int wave) {
        EntityType<OrcMob> type;
        if (raid != null && isLastWave(level, raid, wave)
                && OrcRaidSavedData.get(level).assignElite(raid.getId())) {
            type = ModEntities.ORC_ELITE.get();
        } else if (original instanceof Witch || original instanceof Evoker || original instanceof Vex) {
            type = ModEntities.ORC_SHAMAN.get();
        } else if (original instanceof Pillager) {
            type = level.random.nextBoolean()
                    ? ModEntities.ORC_WARRIOR.get()
                    : ModEntities.ORC_SPEARTHROWER.get();
        } else {
            type = ModEntities.ORC_WARRIOR.get();
        }

        OrcMob orc = type.create(level);
        if (orc == null) throw new IllegalStateException("Could not create vanilla raider replacement " + type);
        if (type == ModEntities.ORC_ELITE.get()) orc.markAsRaidElite();
        TierborneEvents.applyConfiguredEnemyHealth(orc);
        return orc;
    }

    private static boolean isLastWave(ServerLevel level, Raid raid, int wave) {
        int lastWave = raid.getNumGroups(level.getDifficulty());
        if (raid.getBadOmenLevel() > 1) lastWave++;
        return wave >= lastWave;
    }

    private static void replaceRaidMember(Raid raid, Raider original, OrcMob replacement) {
        int wave = original.getWave();
        boolean leader = original.isPatrolLeader();
        if (leader) raid.removeLeader(wave);
        raid.removeFromRaid(original, true);
        raid.joinRaid(wave, replacement, original.blockPosition(), false);
        if (leader) {
            replacement.setPatrolLeader(true);
            raid.setLeader(wave, replacement);
        }
        raid.updateBossbar();
    }

    private static void copyPositionAndIdentity(Mob original, OrcMob replacement) {
        replacement.moveTo(original.getX(), original.getY(), original.getZ(),
                original.getYRot(), original.getXRot());
        replacement.setYHeadRot(original.getYHeadRot());
        replacement.setYBodyRot(original.yBodyRot);
        replacement.setCustomName(original.getCustomName());
        replacement.setCustomNameVisible(original.isCustomNameVisible());
        replacement.setNoAi(original.isNoAi());
        replacement.setSilent(original.isSilent());
        replacement.setInvulnerable(original.isInvulnerable());
        replacement.setGlowingTag(original.isCurrentlyGlowing());
    }

    private static void copyPatrolState(Raider original, OrcMob replacement) {
        if (original == null) return;
        if (original.hasPatrolTarget()) replacement.setPatrolTarget(original.getPatrolTarget());
        if (original.isPatrolLeader()) {
            replacement.setPatrolLeader(true);
            replacement.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
        }
    }

    private static void rewardNearbyPlayers(ServerLevel level, Raid raid, int wave) {
        double radius = RpgBalanceConfig.RAID_REWARD_RADIUS.get();
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(
                    raid.getCenter().getX() + 0.5D,
                    raid.getCenter().getY() + 0.5D,
                    raid.getCenter().getZ() + 0.5D) <= radiusSquared) {
                ProgressionRuntime.rewardRaidWave(player, wave);
            }
        }
    }

    private record WaveBarState(int wave, int waveSize) {
    }
}
