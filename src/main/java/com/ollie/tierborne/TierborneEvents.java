package com.ollie.tierborne;

import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.GeneralSkillBalance;
import com.ollie.tierborne.playerclass.GeneralSkillRules;
import com.ollie.tierborne.playerclass.SkillEffect;
import com.ollie.tierborne.playerclass.SwordsmanPlayerClass;
import com.ollie.tierborne.combat.AbilityRuntime;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class TierborneEvents {
    private static final String BOW_ARROW_TAG = "tierborne:shot_from_bow";
    public static final UUID INTRINSIC_MOVEMENT_SPEED_ID =
            UUID.fromString("f1530d15-037c-45f6-9838-4a03f55df03c");
    private static final UUID SUBCLASS_MOVEMENT_SPEED_ID = UUID.fromString("55ccdcaf-63c6-40f2-b739-dc9087b36e23");
    private static final UUID ROGUE_HEALTH_ID = UUID.fromString("613cc152-92bb-44dd-94ef-56bcd4c7ab77");
    private static final UUID HEAVY_REACH_ID = UUID.fromString("399f91e1-951f-4097-b29a-9791d7f32bd7");
    private static final java.util.Set<UUID> SHIELD_BLOCKING_DAMAGE = new java.util.HashSet<>();

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setBlockedDamage((float)(event.getOriginalBlockedDamage()
                * RpgBalanceConfig.SHIELD_BLOCK_PERCENT.get() / 100.0));
        SHIELD_BLOCKING_DAMAGE.add(player.getUUID());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public void onSwordDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(player.getMainHandItem().getItem() instanceof SwordItem)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        int bonus = progress.totalBonus(SkillBonusType.SWORD_DAMAGE);
        double totalBonus = bonus + AbilityRuntime.additionalSwordDamagePercent(player, event.getEntity());
        event.setAmount((float)(event.getAmount() * (1.0 + totalBonus / 100.0)));
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AbilityRuntime.offensiveAction(player);
        if (player.getMainHandItem().getItem() instanceof SwordItem && !AbilityRuntime.beginNormalSwordAttack(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        AbilityRuntime.tick(player);
        if (player.tickCount % 2 == 0) ModNetwork.syncAbilities(player);
        applySkillEffects(player, PlayerProgressSavedData.get(player.getServer()).get(player.getUUID()));
    }

    @SubscribeEvent
    public void onBlockedDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == player) return;
        if (SHIELD_BLOCKING_DAMAGE.remove(player.getUUID())) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && AbilityRuntime.internalDamage(attacker)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        AbilityRuntime.State state = AbilityRuntime.state(player);
        if (!progress.hasSkill(SwordsmanPlayerClass.DUAL) || !state.blocking()) return;
        double blocked = progress.hasSkill(SwordsmanPlayerClass.IMPROVED_BLOCK) ? RpgBalanceConfig.IMPROVED_BLOCK_PERCENT.get() : RpgBalanceConfig.BLOCK_PERCENT.get();
        event.setAmount((float)(event.getAmount() * (1.0 - blocked / 100.0)));
        AbilityRuntime.tryParry(player, event.getSource().getEntity());
    }

    @SubscribeEvent
    public void onProjectileCreated(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || arrow.shotFromCrossbow() || !(arrow.getOwner() instanceof ServerPlayer player)) return;
        boolean holdingBow = player.getMainHandItem().getItem() instanceof BowItem
                || player.getOffhandItem().getItem() instanceof BowItem;
        if (holdingBow) arrow.getPersistentData().putBoolean(BOW_ARROW_TAG, true);
    }

    @SubscribeEvent
    public void onBowDamage(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !arrow.getPersistentData().getBoolean(BOW_ARROW_TAG)
                || !(arrow.getOwner() instanceof ServerPlayer player)) return;
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        int bonus = progress.totalBonus(SkillBonusType.BOW_DAMAGE);
        if (bonus > 0) event.setAmount(event.getAmount() * (1.0F + bonus / 100.0F));
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
        PlayerProgress progress = PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (progress.hasEffect(SkillEffect.WOOD_DROPS)
                && GeneralSkillRules.isWoodBlock(state)
                && !state.hasBlockEntity()
                && chance(player, GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT)) {
            ItemStack extra = new ItemStack(state.getBlock().asItem());
            if (!extra.isEmpty()) Block.popResource(level, pos, extra);
        }

        ItemStack tool = player.getMainHandItem();
        if (progress.hasEffect(SkillEffect.ORE_DROPS)
                && state.is(Tags.Blocks.ORES)
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0
                && chance(player, GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT)) {
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

        if (progress.hasEffect(SkillEffect.MATERIAL_RECOVERY)
                && chance(player, GeneralSkillBalance.MATERIAL_REFUND_CHANCE_PERCENT)) {
            refundOneIngredient(player, event.getInventory());
        }
        if (progress.hasEffect(SkillEffect.ENCHANTED_CRAFTING)
                && chance(player, GeneralSkillBalance.ENCHANTED_CRAFTING_CHANCE_PERCENT)) {
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
        if (movementSpeed == null || maxHealth == null || reach == null) return;
        movementSpeed.removeModifier(INTRINSIC_MOVEMENT_SPEED_ID);
        movementSpeed.removeModifier(SUBCLASS_MOVEMENT_SPEED_ID);
        maxHealth.removeModifier(ROGUE_HEALTH_ID);
        reach.removeModifier(HEAVY_REACH_ID);
        int bonus = progress.totalBonus(SkillBonusType.MOVEMENT_SPEED);
        if (bonus > 0) {
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
        if (subclassSpeed != 0) movementSpeed.addPermanentModifier(new AttributeModifier(SUBCLASS_MOVEMENT_SPEED_ID,"Tierborne subclass movement speed",subclassSpeed/100.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (progress.hasSkill(SwordsmanPlayerClass.ROGUE)) {
            maxHealth.addPermanentModifier(new AttributeModifier(ROGUE_HEALTH_ID,"Tierborne Rogue health penalty",-RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get(),AttributeModifier.Operation.ADDITION));
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
        if (progress.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE)) reach.addPermanentModifier(new AttributeModifier(HEAVY_REACH_ID,"Tierborne Heavy Swordsman reach",RpgBalanceConfig.HEAVY_RANGE.get(),AttributeModifier.Operation.ADDITION));
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
