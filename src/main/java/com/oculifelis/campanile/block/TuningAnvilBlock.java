package com.oculifelis.campanile.block;

import com.mojang.serialization.MapCodec;
import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.item.HandbellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A bell or accessory goes on top. A reagent sets its tuning (sneak to start over), a second or third reagent adds chord
 * voices, and the tuning hammer moves one note per strike: the base for the hum, the waist for the tierce, the top for
 * the nominal (sneak to strike down). An empty hand takes the item back. Comparators read 5 per true note.
 */
public class TuningAnvilBlock extends BaseEntityBlock {
    public static final MapCodec<TuningAnvilBlock> CODEC = simpleCodec(TuningAnvilBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D),
            Block.box(4.0D, 4.0D, 4.0D, 12.0D, 9.0D, 12.0D), Block.box(1.0D, 9.0D, 3.0D, 15.0D, 14.0D, 13.0D));

    public TuningAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getClockWise());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TuningAnvilBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TuningAnvilBlockEntity be) || stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (be.held().isEmpty()) {
            if (!stack.is(CampanileRegistry.TUNABLE)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                be.setHeld(stack.consumeAndReturn(1, player));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.is(CampanileRegistry.TUNING_HAMMER.get())) {
            return strike(be, stack, level, pos, player, hand, hit);
        }
        TuningAnvilBlockEntity.Tuned set = be.tune(stack, player.isShiftKeyDown());
        if (set == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        stack.consume(1, player);
        level.playSound(null, pos, CampanileRegistry.TUNE.get(), SoundSource.BLOCKS, 1.0F, set.chord() ? 1.2F : 1.0F);
        player.sendOverlayMessage(Component.translatable(set.chord() ? "message.campanile.chord"
                : "message.campanile.tuned", HandbellItem.targetName(set.id())));
        return InteractionResult.SUCCESS;
    }

    /** Which partial a hit height picks: the base is the hum, the waist the tierce, the top the nominal. */
    public static int partialAt(double y) {
        if (y < 0.28D) {
            return BellTune.HUM;
        }
        return y < 0.56D ? BellTune.TIERCE : BellTune.NOMINAL;
    }

    private static InteractionResult strike(TuningAnvilBlockEntity be, ItemStack hammer, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
        int partial = partialAt(hit.getLocation().y - pos.getY());
        TuningAnvilBlockEntity.Strike r = be.strike(partial, player.isShiftKeyDown() ? -1 : 1);
        if (r == TuningAnvilBlockEntity.Strike.NONE) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        hammer.hurtAndBreak(1, player, hand);
        level.playSound(null, pos, CampanileRegistry.TUNE.get(), SoundSource.BLOCKS, 0.8F, 0.8F + 0.2F * partial);
        Component part = Component.translatable("partial.campanile." + BellTune.PARTIALS[partial]);
        String key = switch (r) {
            case CRACKED -> "message.campanile.cracked";
            case BROKE -> "message.campanile.broke";
            case TRUE -> "message.campanile.note_true";
            default -> "message.campanile.note";
        };
        BellTune t = be.held().get(CampanileRegistry.TUNING.get());
        int note = t == null ? 0 : t.note(partial);
        int target = t == null ? 0 : t.target(partial);
        player.sendOverlayMessage(Component.translatable(key, part, note, target));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TuningAnvilBlockEntity be ? be.signal() : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TuningAnvilBlockEntity be) || be.held().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack out = be.take();
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
