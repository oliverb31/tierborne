package com.ollie.tierborne.dungeon;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DungeonEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tierborne")
                .then(Commands.literal("party")
                        .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    DungeonPartyManager.invite(context.getSource().getPlayerOrException(),
                                            EntityArgument.getPlayer(context, "player"));
                                    return 1;
                                })))
                        .then(Commands.literal("accept").then(Commands.argument("leader", EntityArgument.player())
                                .executes(context -> {
                                    DungeonPartyManager.accept(context.getSource().getPlayerOrException(),
                                            EntityArgument.getPlayer(context, "leader"));
                                    return 1;
                                })))
                        .then(Commands.literal("leave").executes(context -> {
                            DungeonPartyManager.leave(context.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("start")
                                .then(Commands.argument("dungeon", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                DungeonManager.dungeonNames(context.getSource().getServer()), builder))
                                        .executes(context -> DungeonManager.start(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "dungeon")) ? 1 : 0)
                                ))
                        .then(Commands.literal("checkpoint").executes(context -> {
                            DungeonManager.checkpoint(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("leave").executes(context -> {
                            DungeonManager.leave(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("finish").executes(context -> {
                            DungeonManager.finish(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(context -> {
                            DungeonManager.reload(context.getSource().getServer());
                            context.getSource().sendSuccess(Component.literal("Tierborne dungeon manifests reloaded."), false);
                            return 1;
                        }))));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) DungeonManager.initializeFireGuards(event.getServer());
        else DungeonManager.tick(event.getServer());
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && DungeonManager.isActiveDungeonPosition(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player) deny(player, "Building and breaking are disabled in dungeons.");
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && DungeonManager.isActiveDungeonPosition(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onFluidSpread(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && DungeonManager.isActiveDungeonPosition(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPortalCreated(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(DungeonManager.DUNGEON_LEVEL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPiston(PistonEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level
                && DungeonManager.isActiveDungeonPosition(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Start event) {
        if (event.getLevel() instanceof ServerLevel level
                && DungeonManager.instanceAt(level.getServer(), event.getExplosion().getPosition().x,
                event.getExplosion().getPosition().z).isPresent()
                && level.dimension().equals(DungeonManager.DUNGEON_LEVEL)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(DungeonManager.DUNGEON_LEVEL)) return;
        event.getAffectedBlocks().removeIf(position -> DungeonManager.isActiveDungeonPosition(level, position));
    }

    @SubscribeEvent
    public void onMobGrief(EntityMobGriefingEvent event) {
        if (event.getEntity().level instanceof ServerLevel level
                && level.dimension().equals(DungeonManager.DUNGEON_LEVEL)
                && DungeonManager.instanceAt(level.getServer(), event.getEntity().getX(), event.getEntity().getZ()).isPresent()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DungeonManager.isParticipant(player)) return;
        if (event.getItemStack().getItem() instanceof EnderpearlItem
                || event.getItemStack().getItem() instanceof ChorusFruitItem
                || event.getItemStack().getItem() instanceof BucketItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            deny(player, "That item cannot be used inside a dungeon instance.");
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DungeonManager.isParticipant(player)) return;
        if (event.getItemStack().getItem() instanceof BlockItem
                || event.getItemStack().getItem() instanceof BucketItem
                || event.getItemStack().getItem() instanceof FlintAndSteelItem
                || event.getItemStack().getItem() instanceof FireChargeItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            deny(player, "That item cannot alter the dungeon.");
        }
    }

    @SubscribeEvent
    public void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (DungeonManager.isParticipant(event.getPlayer())) {
            event.setCanceled(true);
            deny(event.getPlayer(), "Ender pearls are disabled in dungeons.");
        }
    }

    @SubscribeEvent
    public void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntityLiving() instanceof ServerPlayer player && DungeonManager.isParticipant(player)) {
            event.setCanceled(true);
            deny(player, "Chorus fruit is disabled in dungeons.");
        }
    }

    @SubscribeEvent
    public void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level.dimension().equals(DungeonManager.DUNGEON_LEVEL)
                && !DungeonManager.isAuthorizedTravel(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player
                && DungeonManager.isParticipant(player)) {
            event.setCanceled(true);
            deny(player, "Mounts are disabled in dungeon instances.");
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (level.dimension().equals(DungeonManager.DUNGEON_LEVEL)
                && event.getEntity() instanceof FallingBlockEntity fallingBlock
                && (fallingBlock.getBlockState().is(BlockTags.SAND)
                || fallingBlock.getBlockState().is(Blocks.GRAVEL)
                || fallingBlock.getBlockState().getBlock() instanceof ConcretePowderBlock)) {
            event.setCanceled(true);
            level.setBlock(fallingBlock.getStartPos(), fallingBlock.getBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.getBlockTicks().clearArea(new BoundingBox(fallingBlock.getStartPos()));
            return;
        }

        if (event.getEntity() instanceof Mob mob) DungeonManager.scaleEncounterMob(mob);
    }

    private static void deny(ServerPlayer player, String message) {
        long now = player.level.getGameTime();
        long last = player.getPersistentData().getLong("tierborne:last_dungeon_denial");
        if (last == 0L || now - last >= 20L) {
            player.displayClientMessage(Component.literal(message), true);
            player.getPersistentData().putLong("tierborne:last_dungeon_denial", now);
        }
    }
}
