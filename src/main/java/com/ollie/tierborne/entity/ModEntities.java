package com.ollie.tierborne.entity;

import com.ollie.tierborne.Tierborne;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tierborne.MOD_ID);
    public static final RegistryObject<EntityType<FireballProjectile>> FIREBALL = ENTITIES.register("fireball",
            () -> EntityType.Builder.<FireballProjectile>of(FireballProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":fireball"));
    public static final RegistryObject<EntityType<MageVfxEntity>> MAGE_VFX = ENTITIES.register("mage_vfx",
            () -> EntityType.Builder.<MageVfxEntity>of(MageVfxEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F).clientTrackingRange(16).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":mage_vfx"));
    public static final RegistryObject<EntityType<FlameSlashProjectile>> FLAME_SLASH = ENTITIES.register("flame_slash",
            () -> EntityType.Builder.<FlameSlashProjectile>of(FlameSlashProjectile::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F).clientTrackingRange(12).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":flame_slash"));

    public static final RegistryObject<EntityType<DuneRevenant>> DUNE_REVENANT = ENTITIES.register("dune_revenant",
            () -> EntityType.Builder.of(DuneRevenant::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(Tierborne.MOD_ID + ":dune_revenant"));
    public static final RegistryObject<EntityType<FrostboundArcher>> FROSTBOUND_ARCHER = ENTITIES.register("frostbound_archer",
            () -> EntityType.Builder.of(FrostboundArcher::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).clientTrackingRange(10)
                    .build(Tierborne.MOD_ID + ":frostbound_archer"));
    public static final RegistryObject<EntityType<RuneboundColossus>> RUNEBOUND_COLOSSUS = ENTITIES.register("runebound_colossus",
            () -> EntityType.Builder.of(RuneboundColossus::new, MobCategory.MONSTER)
                    .sized(1.4F, 2.7F).clientTrackingRange(12)
                    .build(Tierborne.MOD_ID + ":runebound_colossus"));
    public static final RegistryObject<EntityType<AbyssalWatcher>> ABYSSAL_WATCHER = ENTITIES.register("abyssal_watcher",
            () -> EntityType.Builder.of(AbyssalWatcher::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.0F).clientTrackingRange(12)
                    .build(Tierborne.MOD_ID + ":abyssal_watcher"));
    public static final RegistryObject<EntityType<GoofyGoblin>> GOOFY_GOBLIN = ENTITIES.register("goofy_goblin",
            () -> EntityType.Builder.of(GoofyGoblin::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.45F).clientTrackingRange(10)
                    .build(Tierborne.MOD_ID + ":goofy_goblin"));
    public static final RegistryObject<EntityType<IceMob>> FROSTMITE = registerIce("frostmite", 3.2F, 1.5F);
    public static final RegistryObject<EntityType<IceMob>> FROZEN_BLAZE = registerIce("frozen_blaze", 1.2F, 1.9F);
    public static final RegistryObject<EntityType<IceMob>> GNUT = registerIce("gnut", 1.1F, 1.9F);
    public static final RegistryObject<EntityType<IceMob>> ICE_WITCH = registerIce("ice_witch", 1.4F, 2.2F);
    public static final RegistryObject<EntityType<IceMob>> ICEOLOGER = registerIce("iceologer", 0.9F, 2.2F);
    public static final RegistryObject<EntityType<IceMob>> SNOWBALL_SPIRIT = registerIce("snowball_spirit", 0.8F, 0.8F);
    public static final RegistryObject<EntityType<IceMob>> UNDEAD_ICE_WARRIOR =
            registerIce("undead_ice_warrior", 1.5F, 3.3F);
    public static final RegistryObject<EntityType<IceMob>> TARTARUS_YETI = registerIce("tartarus_yeti", 2.5F, 3.4F);
    public static final RegistryObject<EntityType<IceMob>> ICE_KNIGHT_MINION_SHIELD =
            registerIce("ice_knight_minion_shield", 1.5F, 2.8F);
    public static final RegistryObject<EntityType<IceMob>> ICE_KNIGHT_MINION_SPEAR =
            registerIce("ice_knight_minion_spear", 1.4F, 2.8F);
    public static final RegistryObject<EntityType<IceMob>> ICE_KNIGHT_MINION_SWORD =
            registerIce("ice_knight_minion_sword", 1.4F, 2.8F);
    public static final RegistryObject<EntityType<IceMob>> ICE_KNIGHT = registerIce("ice_knight", 3.5F, 4.5F);
    public static final RegistryObject<EntityType<IceProjectile>> ICE_PROJECTILE = ENTITIES.register("ice_projectile",
            () -> EntityType.Builder.<IceProjectile>of(IceProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(12).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":ice_projectile"));
    public static final RegistryObject<EntityType<OrcMob>> ORC_WARRIOR = registerOrc("orc_warrior", 0.95F, 2.5F);
    public static final RegistryObject<EntityType<OrcMob>> ORC_SPEARTHROWER = registerOrc("orc_spearthrower", 0.95F, 2.5F);
    public static final RegistryObject<EntityType<OrcMob>> ORC_SHAMAN = registerOrc("orc_shaman", 0.95F, 2.5F);
    public static final RegistryObject<EntityType<OrcMob>> ORC_ELITE = registerOrc("orc_elite", 1.05F, 2.7F);
    public static final RegistryObject<EntityType<OrcMob>> ORC_BOSS = registerOrc("orc_boss", 2.592F, 5.616F);
    public static final RegistryObject<EntityType<OrcProjectile>> ORC_PROJECTILE = ENTITIES.register("orc_projectile",
            () -> EntityType.Builder.<OrcProjectile>of(OrcProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(12).updateInterval(1)
                    .build(Tierborne.MOD_ID + ":orc_projectile"));

    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(DUNE_REVENANT.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .build());
        event.put(FROSTBOUND_ARCHER.get(), AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .build());
        event.put(RUNEBOUND_COLOSSUS.get(), IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 240.0D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .build());
        event.put(ABYSSAL_WATCHER.get(), Guardian.createAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .build());
        event.put(GOOFY_GOBLIN.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .build());
        event.put(FROSTMITE.get(), iceAttributes(70.0D, 0.30D, 7.0D, 6.0D, 0.90D));
        event.put(FROZEN_BLAZE.get(), iceAttributes(35.0D, 0.25D, 5.0D, 1.0D, 0.0D));
        event.put(GNUT.get(), iceAttributes(28.0D, 0.30D, 4.0D, 1.0D, 0.0D));
        event.put(ICE_WITCH.get(), iceAttributes(50.0D, 0.24D, 6.0D, 2.0D, 0.75D));
        event.put(ICEOLOGER.get(), iceAttributes(45.0D, 0.25D, 7.0D, 2.0D, 0.0D));
        event.put(SNOWBALL_SPIRIT.get(), iceAttributes(12.0D, 0.32D, 6.0D, 0.0D, 0.0D));
        event.put(UNDEAD_ICE_WARRIOR.get(), iceAttributes(85.0D, 0.24D, 10.0D, 7.0D, 0.60D));
        event.put(TARTARUS_YETI.get(), iceAttributes(160.0D, 0.25D, 14.0D, 8.0D, 0.50D));
        event.put(ICE_KNIGHT_MINION_SHIELD.get(), iceAttributes(120.0D, 0.23D, 3.0D, 12.0D, 0.90D));
        event.put(ICE_KNIGHT_MINION_SPEAR.get(), iceAttributes(50.0D, 0.30D, 8.0D, 5.0D, 0.65D));
        event.put(ICE_KNIGHT_MINION_SWORD.get(), iceAttributes(80.0D, 0.28D, 5.0D, 8.0D, 0.75D));
        event.put(ICE_KNIGHT.get(), iceAttributes(500.0D, 0.20D, 16.0D, 12.0D, 1.0D));
        event.put(ORC_WARRIOR.get(), orcAttributes(40.0D, 0.25D, 7.0D, 2.0D));
        event.put(ORC_SPEARTHROWER.get(), orcAttributes(34.0D, 0.24D, 7.0D, 1.0D));
        event.put(ORC_SHAMAN.get(), orcAttributes(38.0D, 0.23D, 8.0D, 1.0D));
        event.put(ORC_ELITE.get(), orcAttributes(80.0D, 0.27D, 11.0D, 5.0D));
        event.put(ORC_BOSS.get(), orcAttributes(600.0D, 0.24D, 18.0D, 10.0D));
    }

    private static RegistryObject<EntityType<OrcMob>> registerOrc(String name, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(OrcMob::new, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(12)
                .build(Tierborne.MOD_ID + ":" + name));
    }

    private static RegistryObject<EntityType<IceMob>> registerIce(String name, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(IceMob::new, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(12)
                .build(Tierborne.MOD_ID + ":" + name));
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeSupplier iceAttributes(
            double health, double speed, double damage, double armor, double knockbackResistance) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .build();
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeSupplier orcAttributes(
            double health, double speed, double damage, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, armor >= 10.0D ? 0.85D : 0.25D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .build();
    }

    private ModEntities() {}
}
