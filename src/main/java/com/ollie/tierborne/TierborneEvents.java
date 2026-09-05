package com.ollie.tierborne;

import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.item.RemovedVanillaArmor;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.GeneralSkillBalance;
import com.ollie.tierborne.playerclass.GeneralSkillRules;
import com.ollie.tierborne.playerclass.SkillEffect;
import com.ollie.tierborne.playerclass.SwordsmanPlayerClass;
import com.ollie.tierborne.playerclass.ArcherPlayerClass;
import com.ollie.tierborne.playerclass.ArcherStats;
import com.ollie.tierborne.playerclass.FighterPlayerClass;
import com.ollie.tierborne.playerclass.FighterStats;
import com.ollie.tierborne.playerclass.BarbarianPlayerClass;
import com.ollie.tierborne.playerclass.BarbarianStats;
import com.ollie.tierborne.combat.FighterCombat;
import com.ollie.tierborne.combat.BarbarianCombat;
import com.ollie.tierborne.combat.AbilityRuntime;
import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.progression.ProgressionRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import com.ollie.tierborne.world.inventory.ArmorUpgradeMenu;
import com.ollie.tierborne.registry.ModAttributes;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class TierborneEvents {
    private static final String BOW_ARROW_TAG = "tierborne:shot_from_bow";
    private static final String RANGED_MULTIPLIER_TAG = "tierborne:ranged_multiplier";
    public static final UUID INTRINSIC_MOVEMENT_SPEED_ID =
            UUID.fromString("f1530d15-037c-45f6-9838-4a03f55df03c");
    private static final UUID SUBCLASS_MOVEMENT_SPEED_ID = UUID.fromString("55ccdcaf-63c6-40f2-b739-dc9087b36e23");
    private static final UUID ROGUE_HEALTH_ID = UUID.fromString("613cc152-92bb-44dd-94ef-56bcd4c7ab77");
    private static final UUID HEAVY_REACH_ID = UUID.fromString("399f91e1-951f-4097-b29a-9791d7f32bd7");
    private static final UUID SWORD_CHARGE_SPEED_ID = UUID.fromString("5f6de030-2995-4c1c-bd2b-ddf8536aa637");
    private static final UUID MOVEMENT_SPEED_LIMIT_ID = UUID.fromString("82d6df8a-a98c-4c02-8582-85707844e6b4");
    private static final UUID ARCHER_MOVEMENT_ID = UUID.fromString("091c2d97-9428-4449-8948-385b80b6bc3c");
    private static final UUID FIGHTER_ATTACK_SPEED_ID = UUID.fromString("bff841f1-c9c5-4e2c-a666-652a09d6cad8");
    private static final UUID BARBARIAN_MAX_HEALTH_ID = UUID.fromString("70e391ef-f76c-48d6-8c80-fbd180079454");
    private static final UUID BARBARIAN_ATTACK_SPEED_ID = UUID.fromString("18d50fa9-483f-4caf-bade-79082b1575d9");
    private static final UUID BARBARIAN_MOVEMENT_ID = UUID.fromString("16f38554-2c4a-4100-aecf-af73c9da9831");
    private static final UUID ENEMY_HEALTH_SCALE_ID = UUID.fromString("ae5d33c7-46cf-419d-87cc-30adb9d052d1");
    private static final java.util.Set<UUID> SHIELD_BLOCKING_DAMAGE = new java.util.HashSet<>();

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("rpgreset").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
            PlayerProgress progress = data.get(player.getUUID());
            progress.resetProgression();
            data.changed();
            AbilityRuntime.resetTransient(player);
            applySkillEffects(player, progress);
            ModNetwork.sync(player);
            ModNetwork.syncAbilities(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "RPG progression reset. All spent skill points have been refunded."), true);
            return 1;
        }));
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("rpgprogress")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
                    player.displayClientMessage(Component.literal("Tierborne level " + progress.level()
                            + " — " + progress.progressionExperience() + "/"
                            + progress.experienceToNextLevel() + " XP — "
                            + progress.skillPoints() + " skill points"), true);
                    return progress.level();
                }));
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("rpgxp")
                .requires(source -> source.hasPermission(2))
                .then(net.minecraft.commands.Commands.argument("amount",
                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = com.mojang.brigadier.arguments.IntegerArgumentType
                                    .getInteger(context, "amount");
                            PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
                            PlayerProgress progress = data.get(player.getUUID());
                            progress.addProgressionExperience(amount);
                            data.changed();
                            ModNetwork.sync(player);
                            player.displayClientMessage(Component.literal("Granted " + amount
                                    + " Tierborne XP. Current level: " + progress.level() + "."), true);
                            return amount;
                        })));
    }

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        if (progress.hasSkill(BarbarianPlayerClass.BERSERKER)) {
            event.setCanceled(true);
            return;
        }
        event.setBlockedDamage((float)(event.getOriginalBlockedDamage()
                * RpgBalanceConfig.SHIELD_BLOCK_PERCENT.get() / 100.0));
        SHIELD_BLOCKING_DAMAGE.add(player.getUUID());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){AbilityRuntime.resetTransient(player);giveStartingArmor(player);removeObsoleteArmor(player, true);com.ollie.tierborne.dungeon.DungeonManager.recoverOnLogin(player);com.ollie.tierborne.dungeon.DungeonPartyManager.onLogin(player);ProgressionRuntime.clearVanillaExperience(player);ProgressionRuntime.stripEnchantments(player);}
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onVanillaExperiencePickup(PlayerXpEvent.PickupXp event) {
        event.setCanceled(true);
        event.getOrb().discard();
    }

    @SubscribeEvent
    public void onVanillaExperienceChange(PlayerXpEvent.XpChange event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onVanillaLevelChange(PlayerXpEvent.LevelChange event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onAnvilEnchantAttempt(AnvilUpdateEvent event) {
        if (event.getLeft().isEnchanted() || event.getRight().isEnchanted()
                || event.getLeft().is(Items.ENCHANTED_BOOK)
                || event.getRight().is(Items.ENCHANTED_BOOK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onObsoleteArmorPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !RemovedVanillaArmor.contains(event.getItem().getItem())) return;

        event.setCanceled(true);
        event.getItem().discard();
        player.displayClientMessage(Component.translatable("message.tierborne.obsolete_armor_removed"), true);
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        event.getTrades().values().forEach(listings -> listings.replaceAll(original -> (trader, random) -> {
            net.minecraft.world.item.trading.MerchantOffer offer = original.getOffer(trader, random);
            return offer != null && RemovedVanillaArmor.contains(offer.getResult()) ? null : offer;
        }));
    }

    @SubscribeEvent
    public void onObsoleteArmorEquipped(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level.isClientSide || !RemovedVanillaArmor.contains(event.getTo())) return;
        event.getEntity().setItemSlot(event.getSlot(), ItemStack.EMPTY);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.tierborne.obsolete_armor_removed"), true);
        }
    }

    @SubscribeEvent
    public void onSmithingTableUsed(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.SMITHING_TABLE)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new ArmorUpgradeMenu(containerId, inventory,
                            net.minecraft.world.inventory.ContainerLevelAccess.create(event.getLevel(), event.getPos())),
                    Component.translatable("container.tierborne.armor_upgrade")), event.getPos());
        }
    }

    @SubscribeEvent
    public void onEnchantingTableUsed(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.tierborne.enchantments_disabled"), true);
        }
    }

    private static void giveStartingArmor(ServerPlayer player) {
        PlayerProgressSavedData data = PlayerProgressSavedData.get(player.getServer());
        PlayerProgress progress = data.get(player.getUUID());
        if (progress.receivedStartingArmor()) return;

        giveStartingPiece(player, EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        giveStartingPiece(player, EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        giveStartingPiece(player, EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        giveStartingPiece(player, EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        progress.markReceivedStartingArmor();
        data.changed();
        player.displayClientMessage(Component.translatable("message.tierborne.starting_armor_received"), true);
    }

    private static void giveStartingPiece(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){
            AbilityRuntime.resetTransient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && com.ollie.tierborne.dungeon.DungeonManager.shouldKeepInventoryAfterDungeonDeath(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){AbilityRuntime.resetTransient(player);com.ollie.tierborne.dungeon.DungeonManager.recoverAfterRespawn(player);}
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() != player) {
            ProgressionRuntime.rewardHostileKill(player, event.getEntity());
        }
        com.ollie.tierborne.combat.ElementalCombat.clear(event.getEntity());
        com.ollie.tierborne.combat.CombatControl.clear(event.getEntity());
        AbilityRuntime.resetRoot(event.getEntity());
        if(event.getEntity() instanceof ServerPlayer player){
            ProgressionRuntime.resetExperienceOnDeath(player);
            com.ollie.tierborne.dungeon.DungeonManager.handlePlayerDeath(player);
            AbilityRuntime.resetTransient(player);
        }
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if(event.getEntity() instanceof net.minecraft.world.entity.Mob
                &&event.getNewTarget() instanceof ServerPlayer player
                &&AbilityRuntime.isCloaked(player))event.setNewTarget(null);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if(event.getEntity() instanceof ServerPlayer player
                &&event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob
                &&AbilityRuntime.isCloaked(player)){event.setCanceled(true);return;}
        if(event.getSource().getEntity()!=null&&(AbilityRuntime.isRooted(event.getSource().getEntity())||FighterCombat.offenseDisabled(event.getSource().getEntity()))){event.setCanceled(true);return;}
        if(event.getEntity() instanceof ServerPlayer defender&&event.getSource().getEntity() instanceof LivingEntity attacker&&!(attacker instanceof ServerPlayer serverAttacker&&AbilityRuntime.internalDamage(serverAttacker))&&FighterCombat.tryCounter(defender,attacker,event.getAmount())){event.setCanceled(true);return;}
        if(!(event.getSource().getEntity() instanceof ServerPlayer player)
                ||!(player.getMainHandItem().getItem() instanceof SwordItem))return;
        PlayerProgress progress=PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        if(progress.hasSkill(SwordsmanPlayerClass.DUAL))event.getEntity().invulnerableTime=0;
    }

    @SubscribeEvent
    public void onConfiguredDamageScaling(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Mob) {
            event.setAmount(event.getAmount() * RpgBalanceConfig.ENEMY_DAMAGE_MULTIPLIER.get().floatValue());
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        double multiplier = 1.0D;
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow) {
            multiplier = arrow.shotFromCrossbow()
                    ? RpgBalanceConfig.CROSSBOW_DAMAGE_MULTIPLIER.get()
                    : RpgBalanceConfig.BOW_DAMAGE_MULTIPLIER.get();
        } else if (direct == player) {
            if (player.getMainHandItem().getItem() instanceof SwordItem) {
                multiplier = RpgBalanceConfig.SWORD_DAMAGE_MULTIPLIER.get();
            } else if (player.getMainHandItem().getItem() instanceof AxeItem) {
                multiplier = RpgBalanceConfig.AXE_DAMAGE_MULTIPLIER.get();
            } else if (player.getMainHandItem().isEmpty()) {
                multiplier = RpgBalanceConfig.FIST_DAMAGE_MULTIPLIER.get();
            }
        }
        event.setAmount(event.getAmount() * (float) multiplier);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)AbilityRuntime.resetTransient(player);
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            if (AbilityRuntime.isCloaked(target)) ModNetwork.syncCloak(viewer,target,true);
            if (AbilityRuntime.isBlocking(target)) ModNetwork.syncBlock(viewer,target,true);
        }
    }

    @SubscribeEvent
    public void onSwordDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity()!=player
                || !(player.getMainHandItem().getItem() instanceof SwordItem)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        int bonus = progress.totalBonus(SkillBonusType.SWORD_DAMAGE);
        double totalBonus = bonus + AbilityRuntime.additionalSwordDamagePercent(player, event.getEntity());
        float swordDamage=(float)(event.getAmount() * (1.0 + totalBonus / 100.0));
        if(progress.hasSkill(SwordsmanPlayerClass.MAGIC)) {
            swordDamage = com.ollie.tierborne.combat.ElementalCombat.modifyDamage(
                    event.getEntity(), com.ollie.tierborne.combat.Element.FIRE, swordDamage);
            swordDamage *= (float) player.getAttributeValue(ModAttributes.MAGIC_DAMAGE.get());
        }
        event.setAmount(swordDamage);
    }

    @SubscribeEvent
    public void onAxeDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !(player.getMainHandItem().getItem() instanceof AxeItem)) return;
        event.setAmount(BarbarianCombat.modifyAxeDamage(player, event.getEntity(), event.getAmount()));
    }

    @SubscribeEvent
    public void onBarbarianDamageApplied(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer defender) {
            BarbarianCombat.onDamageTaken(defender, event.getAmount());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getDirectEntity() == attacker) {
            BarbarianCombat.onDamageDealt(attacker, event.getEntity(), event.getAmount());
        }
    }

    @SubscribeEvent
    public void onMagicSwordDamageApplied(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !(player.getMainHandItem().getItem() instanceof SwordItem)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        if (!progress.hasSkill(SwordsmanPlayerClass.MAGIC)) return;
        int fireTicks = RpgBalanceConfig.ticks(RpgBalanceConfig.MAGIC_SWORD_FIRE_SECONDS);
        if (event.getEntity().getRemainingFireTicks() < fireTicks) event.getEntity().setRemainingFireTicks(fireTicks);
        if (progress.hasSkill(SwordsmanPlayerClass.ELEMENTAL_VULNERABILITY))
            com.ollie.tierborne.combat.ElementalCombat.applyVulnerability(event.getEntity());
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if(AbilityRuntime.isRooted(player)||FighterCombat.offenseDisabled(player)){event.setCanceled(true);return;}
        AbilityRuntime.offensiveAction(player);
        boolean trackedMelee=player.getMainHandItem().getItem() instanceof SwordItem
                || player.getMainHandItem().getItem() instanceof AxeItem || progressIsMonkFist(player);
        if(trackedMelee&&!AbilityRuntime.beginNormalSwordAttack(player))event.setCanceled(true);
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event){if(FighterCombat.offenseDisabled(event.getEntity()))event.setCanceled(true);}

    @SubscribeEvent
    public void onFighterIntentionalDamage(LivingHurtEvent event){
        if(!(event.getSource().getEntity() instanceof ServerPlayer player))return;
        LivingEntity target=event.getEntity();
        Entity direct=event.getSource().getDirectEntity();
        boolean intentional=direct==player||direct instanceof AbstractArrow||direct instanceof com.ollie.tierborne.entity.FireballProjectile||direct instanceof com.ollie.tierborne.entity.FlameSlashProjectile;
        if(!intentional)return;
        event.setAmount(FighterCombat.modifyIntentionalHit(player,target,event.getAmount()));
    }

    @SubscribeEvent
    public void onArmorDamageReduction(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().isBypassArmor()) return;
        double reduction = Math.max(0.0D,
                player.getAttributeValue(ModAttributes.DAMAGE_REDUCTION.get()) - 1.0D);
        if (reduction > 0.0D) {
            event.setAmount((float) (event.getAmount() * (1.0D - Math.min(reduction, 0.8D))));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        removeObsoleteArmor(player, false);
        ProgressionRuntime.tick(player);
        AbilityRuntime.tick(player);
        if (player.tickCount % 2 == 0) ModNetwork.syncAbilities(player);
        applySkillEffects(player, PlayerProgressSavedData.get(player.getServer()).get(player.getUUID()));
    }

    @SubscribeEvent
    public void onRangedWeaponUseTick(LivingEntityUseItemEvent.Tick event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        PlayerProgress progress=PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        if(event.getItem().getItem() instanceof BowItem)AbilityRuntime.noteFullyChargedDraw(player);
        if(event.getItem().getItem() instanceof BowItem){event.setDuration(AbilityRuntime.adjustBowUseDuration(player,event.getDuration()));return;}
        double speed=event.getItem().getItem() instanceof CrossbowItem?ArcherStats.crossbowChargeSpeed(progress):0;
        if(speed>0&&player.tickCount%Math.max(1,(int)Math.round(100/speed))==0)event.setDuration(Math.max(1,event.getDuration()-1));
        if(speed<0&&player.tickCount%Math.max(1,(int)Math.round(100/-speed))==0)event.setDuration(event.getDuration()+1);
    }

    @SubscribeEvent
    public void onOffensiveItemUse(LivingEntityUseItemEvent.Start event){
        if(FighterCombat.offenseDisabled(event.getEntity())&&(event.getItem().getItem() instanceof BowItem||event.getItem().getItem() instanceof CrossbowItem||event.getItem().getItem() instanceof TridentItem))event.setCanceled(true);
        if(event.getEntity() instanceof ServerPlayer player && event.getItem().getItem() instanceof ShieldItem){
            PlayerProgress progress=PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
            if(progress.hasSkill(BarbarianPlayerClass.BERSERKER)){event.setCanceled(true);player.stopUsingItem();}
        }
    }

    @SubscribeEvent
    public void onDisabledRightClick(PlayerInteractEvent.RightClickItem event){
        if(FighterCombat.offenseDisabled(event.getEntity())&&event.getItemStack().getItem() instanceof net.minecraft.world.item.ThrowablePotionItem)event.setCanceled(true);
        if(event.getEntity() instanceof ServerPlayer player && event.getItemStack().getItem() instanceof ShieldItem){
            PlayerProgress progress=PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
            if(progress.hasSkill(BarbarianPlayerClass.BERSERKER)){
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                player.displayClientMessage(Component.literal("Berserkers cannot block with shields."),true);
            }
        }
    }

    @SubscribeEvent
    public void onBlockedDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == player) return;
        if (SHIELD_BLOCKING_DAMAGE.remove(player.getUUID())) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && AbilityRuntime.internalDamage(attacker)) return;
        if (event.getSource().isBypassArmor()) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        AbilityRuntime.State state = AbilityRuntime.state(player);
        if (!progress.hasSkill(SwordsmanPlayerClass.DUAL) || !state.blocking()) return;
        double blocked = RpgBalanceConfig.BLOCK_PERCENT.get();
        event.setAmount((float)(event.getAmount() * (1.0 - blocked / 100.0)));
        AbilityRuntime.tryParry(player, event.getSource().getEntity());
    }

    @SubscribeEvent
    public void onSwordBlockKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        if (progress.hasSkill(SwordsmanPlayerClass.IMPROVED_BLOCK)
                && AbilityRuntime.isBlocking(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onProjectileCreated(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ExperienceOrb experienceOrb) {
            event.setCanceled(true);
            experienceOrb.discard();
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity
                && RemovedVanillaArmor.contains(itemEntity.getItem())) {
            itemEntity.discard();
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack sanitized = ProgressionRuntime.sanitize(itemEntity.getItem());
            if (sanitized != itemEntity.getItem()) itemEntity.setItem(sanitized);
            ProgressionRuntime.removeEnchantmentTags(itemEntity.getItem());
        }
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            applyConfiguredEnemyHealth(livingEntity);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (RemovedVanillaArmor.contains(livingEntity.getItemBySlot(slot))) {
                    livingEntity.setItemSlot(slot, ItemStack.EMPTY);
                } else {
                    ProgressionRuntime.removeEnchantmentTags(livingEntity.getItemBySlot(slot));
                }
            }
        }
        if (!(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) return;
        if(arrow.getPersistentData().contains(RANGED_MULTIPLIER_TAG))return;
        boolean holdingBow = player.getMainHandItem().getItem() instanceof BowItem
                || player.getOffhandItem().getItem() instanceof BowItem;
        if (holdingBow) arrow.getPersistentData().putBoolean(BOW_ARROW_TAG, true);
        if(arrow.getPersistentData().getString("tierborne:ranged_type").isEmpty())arrow.getPersistentData().putString("tierborne:ranged_type",arrow.shotFromCrossbow()?"crossbow":"bow");
        arrow.getPersistentData().putDouble(RANGED_MULTIPLIER_TAG,AbilityRuntime.consumeArrowMultiplier(player,arrow));
    }

    public static void applyConfiguredEnemyHealth(LivingEntity entity) {
        if (entity.level.isClientSide || entity.getType().getCategory() != MobCategory.MONSTER
                || com.ollie.tierborne.raid.OrcPillagerReplacementEvents.isReplacedVanillaMob(entity)) return;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;

        float healthRatio = entity.getMaxHealth() <= 0.0F ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        health.removeModifier(ENEMY_HEALTH_SCALE_ID);
        double multiplier = RpgBalanceConfig.ENEMY_HEALTH_MULTIPLIER.get();
        if (multiplier != 1.0D) {
            health.addPermanentModifier(new AttributeModifier(ENEMY_HEALTH_SCALE_ID,
                    "Tierborne configured enemy health", multiplier - 1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        entity.setHealth(Math.max(0.01F, entity.getMaxHealth() * healthRatio));
    }

    @SubscribeEvent
    public void onObsoleteArmorCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !RemovedVanillaArmor.contains(event.getCrafting())) return;

        event.getCrafting().setCount(0);
        player.displayClientMessage(Component.translatable("message.tierborne.obsolete_armor_removed"), true);
    }

    private static void removeObsoleteArmor(ServerPlayer player, boolean notifyInChat) {
        boolean removed = false;
        for (int slotIndex = 0; slotIndex < player.getInventory().getContainerSize(); slotIndex++) {
            if (RemovedVanillaArmor.contains(player.getInventory().getItem(slotIndex))) {
                player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                removed = true;
            }
        }

        for (net.minecraft.world.inventory.Slot slot : player.containerMenu.slots) {
            if (RemovedVanillaArmor.contains(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
                removed = true;
            }
        }
        if (RemovedVanillaArmor.contains(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            removed = true;
        }

        if (!removed) return;
        player.containerMenu.broadcastChanges();
        if (notifyInChat) {
            player.displayClientMessage(Component.translatable("message.tierborne.obsolete_armor_removed_existing"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tierborne.obsolete_armor_removed"), true);
        }
    }

    @SubscribeEvent
    public void onBowDamage(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        event.setAmount(event.getAmount()*(float)arrow.getPersistentData().getDouble(RANGED_MULTIPLIER_TAG));
        if(arrow.getPersistentData().getBoolean("tierborne:elemental_shot")){event.getEntity().setSecondsOnFire((int)Math.ceil(RpgBalanceConfig.ELEMENTAL_SHOT_FIRE_SECONDS.get()));event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,RpgBalanceConfig.ticks(RpgBalanceConfig.ELEMENTAL_SHOT_SLOW_SECONDS),RpgBalanceConfig.ELEMENTAL_SHOT_SLOW_LEVEL.get()-1));}
        if(arrow.getPersistentData().getBoolean("tierborne:fire_arrow")){double seconds=progress.hasSkill(ArcherPlayerClass.FIRE_DURATION)?RpgBalanceConfig.FIRE_UPGRADED_SECONDS.get():RpgBalanceConfig.FIRE_PASSIVE_SECONDS.get();event.getEntity().setSecondsOnFire((int)Math.ceil(seconds));if(progress.hasSkill(ArcherPlayerClass.FIRE_DAMAGE))event.setAmount(event.getAmount()*(1+(float)(RpgBalanceConfig.FIRE_BONUS_DAMAGE.get()/100)));}
        if(arrow.getPersistentData().getBoolean("tierborne:ice_arrow")){int level=progress.hasSkill(ArcherPlayerClass.ICE_POTENCY)?RpgBalanceConfig.ICE_UPGRADED_LEVEL.get():RpgBalanceConfig.ICE_PASSIVE_LEVEL.get();double seconds=progress.hasSkill(ArcherPlayerClass.ICE_DURATION)?RpgBalanceConfig.ICE_UPGRADED_SECONDS.get():RpgBalanceConfig.ICE_PASSIVE_SECONDS.get();event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,RpgBalanceConfig.ticksValue(seconds),level-1));}
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        BlockState state = event.getState();
        int bonus = 0;
        if (GeneralSkillRules.isWoodBlock(state)) {
            bonus = progress.totalBonus(SkillBonusType.WOODCUTTING_SPEED);
        } else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            bonus = progress.totalBonus(SkillBonusType.MINING_SPEED);
        }
        if (bonus > 0) event.setNewSpeed(event.getNewSpeed() * (1.0F + bonus / 100.0F));
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.isCanceled()) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        int woodDropChance = progress.totalBonus(SkillBonusType.WOOD_DROP_CHANCE);
        if (woodDropChance > 0
                && GeneralSkillRules.isWoodBlock(state)
                && !state.hasBlockEntity()
                && chance(player, woodDropChance)) {
            ItemStack extra = new ItemStack(state.getBlock().asItem());
            if (!extra.isEmpty()) Block.popResource(level, pos, extra);
        }

        ItemStack tool = player.getMainHandItem();
        int oreDropChance = progress.totalBonus(SkillBonusType.ORE_DROP_CHANCE);
        if (oreDropChance > 0
                && state.is(Tags.Blocks.ORES)
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0
                && chance(player, oreDropChance)) {
            List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
            drops.stream().filter(stack -> !stack.isEmpty()).findFirst().ifPresent(stack -> {
                ItemStack extra = stack.copy();
                extra.setCount(1);
                Block.popResource(level, pos, extra);
            });
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isEquipment(event.getCrafting())) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());

        int materialRecoveryChance = progress.totalBonus(SkillBonusType.MATERIAL_RECOVERY_CHANCE);
        if (materialRecoveryChance > 0 && chance(player, materialRecoveryChance)) {
            refundOneIngredient(player, event.getInventory());
        }
        int enchantedCraftingChance = progress.totalBonus(SkillBonusType.ENCHANTED_CRAFTING_CHANCE);
        if (enchantedCraftingChance > 0 && chance(player, enchantedCraftingChance)) {
            int level = GeneralSkillBalance.ENCHANTMENT_MIN_LEVEL + player.getRandom().nextInt(
                    GeneralSkillBalance.ENCHANTMENT_MAX_LEVEL
                            - GeneralSkillBalance.ENCHANTMENT_MIN_LEVEL + 1);
            ItemStack enchanted = EnchantmentHelper.enchantItem(
                    player.getRandom(), event.getCrafting().copy(), level, false);
            event.getCrafting().setTag(enchanted.getTag());
        }
    }

    public static void applySkillEffects(ServerPlayer player, PlayerProgress progress) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance reach = player.getAttribute(ForgeMod.REACH_DISTANCE.get());
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (movementSpeed == null || maxHealth == null || reach == null || attackSpeed == null) return;
        movementSpeed.removeModifier(INTRINSIC_MOVEMENT_SPEED_ID);
        movementSpeed.removeModifier(SUBCLASS_MOVEMENT_SPEED_ID);
        movementSpeed.removeModifier(MOVEMENT_SPEED_LIMIT_ID);
        movementSpeed.removeModifier(ARCHER_MOVEMENT_ID);
        maxHealth.removeModifier(ROGUE_HEALTH_ID);
        reach.removeModifier(HEAVY_REACH_ID);
        attackSpeed.removeModifier(SWORD_CHARGE_SPEED_ID);
        attackSpeed.removeModifier(FIGHTER_ATTACK_SPEED_ID);
        attackSpeed.removeModifier(BARBARIAN_ATTACK_SPEED_ID);
        movementSpeed.removeModifier(BARBARIAN_MOVEMENT_ID);
        maxHealth.removeModifier(BARBARIAN_MAX_HEALTH_ID);
        int bonus = progress.totalBonus(SkillBonusType.MOVEMENT_SPEED);
        if (progress.moddedMovementSpeedEnabled() && bonus > 0) {
            movementSpeed.addPermanentModifier(new AttributeModifier(
                    INTRINSIC_MOVEMENT_SPEED_ID,
                    "Tierborne intrinsic movement speed",
                    bonus / 100.0,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        double subclassSpeed = 0.0;
        if (progress.hasSkill(SwordsmanPlayerClass.SWORDMASTER)) subclassSpeed += RpgBalanceConfig.SWORDMASTER_SPEED.get();
        if (progress.hasSkill(SwordsmanPlayerClass.SM_SPEED)) subclassSpeed += RpgBalanceConfig.SWORDMASTER_UPGRADE_SPEED.get();
        if (progress.hasSkill(SwordsmanPlayerClass.ROGUE)) subclassSpeed += RpgBalanceConfig.ROGUE_SPEED.get();
        if (progress.hasSkill(SwordsmanPlayerClass.HEAVY) && AbilityRuntime.heavyMovementPenaltyActive(player)) subclassSpeed += RpgBalanceConfig.HEAVY_MOVE_PENALTY.get();
        if(progress.hasSkill(FighterPlayerClass.MONK))subclassSpeed+=RpgBalanceConfig.MONK_MOVE.get();
        if (progress.moddedMovementSpeedEnabled() && subclassSpeed != 0) movementSpeed.addPermanentModifier(new AttributeModifier(SUBCLASS_MOVEMENT_SPEED_ID,"Tierborne subclass movement speed",subclassSpeed/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        double archerSpeed=progress.hasSkill(ArcherPlayerClass.RANGER)?RpgBalanceConfig.RANGER_MOVEMENT.get():0;
        boolean drawingBow=player.isUsingItem()&&player.getUseItem().getItem() instanceof BowItem;
        if(drawingBow&&progress.hasSkill(ArcherPlayerClass.LONGBOWMAN))archerSpeed+=progress.hasSkill(ArcherPlayerClass.FULLY_CHARGED_MOBILITY)?RpgBalanceConfig.FULLY_CHARGED_IMPROVED_MOVEMENT_PENALTY.get():RpgBalanceConfig.LONGBOWMAN_DRAW_MOVEMENT.get();
        else if(AbilityRuntime.fullyChargedActive(player))archerSpeed+=progress.hasSkill(ArcherPlayerClass.FULLY_CHARGED_MOBILITY)?RpgBalanceConfig.FULLY_CHARGED_IMPROVED_MOVEMENT_PENALTY.get():RpgBalanceConfig.FULLY_CHARGED_MOVEMENT_PENALTY.get();
        if(progress.moddedMovementSpeedEnabled()&&archerSpeed!=0)movementSpeed.addPermanentModifier(new AttributeModifier(ARCHER_MOVEMENT_ID,"Tierborne Archer movement",archerSpeed/100,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(progress.moddedMovementSpeedEnabled()&&progress.movementSpeedLimitPercent()<100)movementSpeed.addPermanentModifier(new AttributeModifier(MOVEMENT_SPEED_LIMIT_ID,"Tierborne voluntary movement speed limit",progress.movementSpeedLimitPercent()/100.0-1.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (progress.hasSkill(SwordsmanPlayerClass.ROGUE)) {
            maxHealth.addPermanentModifier(new AttributeModifier(ROGUE_HEALTH_ID,"Tierborne Rogue health penalty",-RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get(),AttributeModifier.Operation.ADDITION));
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
        if (progress.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE)) reach.addPermanentModifier(new AttributeModifier(HEAVY_REACH_ID,"Tierborne Heavy Swordsman reach",RpgBalanceConfig.HEAVY_RANGE.get(),AttributeModifier.Operation.ADDITION));
        double swordChargeSpeed=progress.hasSkill(SwordsmanPlayerClass.HEAVY)?RpgBalanceConfig.HEAVY_ATTACK_SPEED.get():progress.hasSkill(SwordsmanPlayerClass.DUAL_SPEED)?RpgBalanceConfig.DUAL_SPEED_UPGRADE.get():progress.hasSkill(SwordsmanPlayerClass.DUAL)?RpgBalanceConfig.DUAL_ATTACK_SPEED.get():0.0;
        if(swordChargeSpeed!=0.0 && player.getMainHandItem().getItem() instanceof SwordItem)attackSpeed.addPermanentModifier(new AttributeModifier(SWORD_CHARGE_SPEED_ID,"Tierborne sword charge speed",swordChargeSpeed/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        double fighterCharge=FighterStats.meleeChargeSpeed(progress);if(fighterCharge!=0)attackSpeed.addPermanentModifier(new AttributeModifier(FIGHTER_ATTACK_SPEED_ID,"Tierborne Fighter melee charge speed",fighterCharge/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(progress.hasSkill(BarbarianPlayerClass.BERSERKER)){
            maxHealth.addPermanentModifier(new AttributeModifier(BARBARIAN_MAX_HEALTH_ID,"Tierborne Berserker health",RpgBalanceConfig.BERSERKER_MAX_HEALTH.get(),AttributeModifier.Operation.ADDITION));
        }
        double barbarianMovement=BarbarianCombat.isBerserkActive(player)?BarbarianStats.berserkMovement(progress):0.0;
        if(BarbarianCombat.executeMovementPenaltyActive(player))barbarianMovement+=BarbarianStats.executeMovement(progress);
        if(progress.moddedMovementSpeedEnabled()&&barbarianMovement!=0)movementSpeed.addPermanentModifier(new AttributeModifier(BARBARIAN_MOVEMENT_ID,"Tierborne Barbarian movement",barbarianMovement/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(BarbarianPlayerClass.ID.equals(progress.playerClassId())&&player.getMainHandItem().getItem() instanceof AxeItem){
            double axeCharge=RpgBalanceConfig.BARBARIAN_AXE_CHARGE_SPEED.get();
            if(progress.hasSkill(BarbarianPlayerClass.EXECUTIONER))axeCharge+=RpgBalanceConfig.EXECUTIONER_ATTACK_SPEED.get();
            if(axeCharge!=0)attackSpeed.addPermanentModifier(new AttributeModifier(BARBARIAN_ATTACK_SPEED_ID,"Tierborne axe charge speed",axeCharge/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
    }

    private void sync(Player entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        applySkillEffects(player, progress);
        ModNetwork.sync(player);
    }

    private static boolean chance(ServerPlayer player, int percentage) {
        return player.getRandom().nextInt(100) < percentage;
    }

    private static boolean progressIsMonkFist(ServerPlayer player){PlayerProgress progress=PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());return progress.hasSkill(FighterPlayerClass.MONK)&&FighterStats.isFist(player);}

    private static boolean isEquipment(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
    }

    private static void refundOneIngredient(ServerPlayer player, Container craftingInventory) {
        List<ItemStack> candidates = new ArrayList<>();
        for (int slot = 0; slot < craftingInventory.getContainerSize(); slot++) {
            ItemStack ingredient = craftingInventory.getItem(slot);
            if (!ingredient.isEmpty() && !ingredient.getItem().hasCraftingRemainingItem()) {
                candidates.add(ingredient);
            }
        }
        if (candidates.isEmpty()) return;
        ItemStack refund = candidates.get(player.getRandom().nextInt(candidates.size())).copy();
        refund.setCount(1);
        if (!player.getInventory().add(refund)) player.drop(refund, false);
    }
}
