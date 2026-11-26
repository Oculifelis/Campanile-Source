package com.oculifelis.campanile.block;

import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.Bell;
import com.oculifelis.campanile.bell.Ring;
import com.oculifelis.campanile.item.HandbellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Holds the hung handbell and when it last rang. */
public class BelfryChimeBlockEntity extends BlockEntity {
    private ItemStack bell = ItemStack.EMPTY;
    private long lastRing = Long.MIN_VALUE / 2;

    public BelfryChimeBlockEntity(BlockPos pos, BlockState state) {
        super(CampanileRegistry.BELFRY_CHIME_BE.get(), pos, state);
    }

    public ItemStack bell() {
        return bell;
    }

    public void setBell(ItemStack stack) {
        this.bell = stack;
        setChanged();
    }

    public ItemStack takeBell() {
        ItemStack out = bell;
        bell = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /**
     * Rings the hung bell with its own numbers, the radius scaled by the bell's chime multiplier. Uses no durability.
     *
     * @return false when there is no bell or it is still on cooldown
     */
    public boolean ring(ServerLevel level, @Nullable LivingEntity ringer) {
        if (!(bell.getItem() instanceof HandbellItem item)) {
            return false;
        }
        Bell b = item.bell();
        long now = level.getGameTime();
        if (now - lastRing < b.cooldown()) {
            return false;
        }
        lastRing = now;
        setChanged();
        Ring.ringStack(level, worldPosition.getCenter(), ringer, b.radius() * b.chimeMultiplier(), b.strength(), bell,
                b.sound(), b.pitch());
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bell = input.read("Bell", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        lastRing = input.getLongOr("LastRing", Long.MIN_VALUE / 2);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!bell.isEmpty()) {
            output.store("Bell", ItemStack.CODEC, bell);
        }
        output.putLong("LastRing", lastRing);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !bell.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, bell);
            bell = ItemStack.EMPTY;
        }
    }
}
