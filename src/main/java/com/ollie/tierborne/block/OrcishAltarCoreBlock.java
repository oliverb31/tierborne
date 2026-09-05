package com.ollie.tierborne.block;

import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.OpenOrcishAltarScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

public final class OrcishAltarCoreBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.ENABLED;
    private static final int VALIDATION_INTERVAL_TICKS = 20;

    public OrcishAltarCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ACTIVE, isStructureValid(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos position, BlockState oldState, boolean moving) {
        super.onPlace(state, level, position, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(position, this, 1);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos position, BlockPos neighborPosition) {
        if (!level.isClientSide()) level.scheduleTick(position, this, 1);
        return super.updateShape(state, direction, neighborState, level, position, neighborPosition);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        boolean active = isStructureValid(level, position);
        if (state.getValue(ACTIVE) != active) {
            level.setBlock(position, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(position, this, VALIDATION_INTERVAL_TICKS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos position, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                position.getX() + 0.2D + random.nextDouble() * 0.6D,
                position.getY() + 1.05D,
                position.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.02D, 0.0D);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos position, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        boolean active = isStructureValid(level, position);
        if (state.getValue(ACTIVE) != active) {
            level.setBlock(position, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        if (!active) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.tierborne.orcish_altar_core.incomplete"), true);
            return InteractionResult.CONSUME;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new OpenOrcishAltarScreenPacket(position));
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    public static boolean isStructureValid(LevelReader level, BlockPos altarPosition) {
        BlockPos center = altarPosition.below();
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                BlockState foundation = level.getBlockState(center.offset(xOffset, 0, zOffset));
                if (xOffset == 0 && zOffset == 0) {
                    if (!foundation.is(Blocks.MANGROVE_ROOTS)) return false;
                } else if (Math.abs(xOffset) == 1 && Math.abs(zOffset) == 1) {
                    if (!foundation.is(Blocks.MANGROVE_LOG)) return false;
                } else if (!foundation.is(Blocks.MANGROVE_SLAB)) {
                    return false;
                }
            }
        }
        return true;
    }
}
