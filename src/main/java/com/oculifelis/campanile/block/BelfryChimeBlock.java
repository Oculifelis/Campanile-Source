package com.oculifelis.campanile.block;

import com.mojang.serialization.MapCodec;
import com.oculifelis.campanile.item.HandbellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Hangs under a block and holds one handbell. A redstone pulse or a projectile rings it; the comparator reads 15 for
 * one second after each ring. The only scheduled tick is the one that ends that second.
 */
public class BelfryChimeBlock extends BaseEntityBlock {
    public static final MapCodec<BelfryChimeBlock> CODEC = simpleCodec(BelfryChimeBlock::new);
    public static final BooleanProperty HAS_BELL = BooleanProperty.create("has_bell");
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(3.0D, 2.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public BelfryChimeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_BELL, false).setValue(RINGING, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_BELL, RINGING, POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BelfryChimeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (dir == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof HandbellItem) || state.getValue(HAS_BELL)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BelfryChimeBlockEntity be) {
            be.setBell(stack.consumeAndReturn(1, player));
            level.setBlock(pos, state.setValue(HAS_BELL, true), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(HAS_BELL)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BelfryChimeBlockEntity be) {
            ItemStack bell = be.takeBell();
            if (!player.addItem(bell)) {
                player.drop(bell, false);
            }
            level.setBlock(pos, state.setValue(HAS_BELL, false), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        boolean signal = level.hasNeighborSignal(pos);
        if (signal == state.getValue(POWERED)) {
            return;
        }
        BlockState next = state.setValue(POWERED, signal);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        if (signal && level instanceof ServerLevel server) {
            ring(server, next, pos, null);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server) {
            LivingEntity owner = projectile.getOwner() instanceof LivingEntity l ? l : null;
            ring(server, state, hit.getBlockPos(), owner);
        }
    }

    /** Rings the hung bell, if any, and lights the comparator for one second. */
    public static boolean ring(ServerLevel level, BlockState state, BlockPos pos, @Nullable LivingEntity ringer) {
        if (!(level.getBlockEntity(pos) instanceof BelfryChimeBlockEntity be) || !be.ring(level, ringer)) {
            return false;
        }
        level.setBlock(pos, level.getBlockState(pos).setValue(RINGING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), 20);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING)) {
            level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(RINGING) ? 15 : 0;
    }
}
