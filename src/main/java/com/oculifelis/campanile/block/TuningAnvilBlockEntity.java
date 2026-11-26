package com.oculifelis.campanile.block;

import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.bell.TuneRules;
import com.oculifelis.campanile.bell.Tunings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Holds the one bell or accessory being tuned. */
public class TuningAnvilBlockEntity extends BlockEntity {
    private ItemStack held = ItemStack.EMPTY;

    public TuningAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(CampanileRegistry.TUNING_ANVIL_BE.get(), pos, state);
    }

    public ItemStack held() {
        return held;
    }

    public void setHeld(ItemStack stack) {
        held = stack;
        changed();
    }

    public ItemStack take() {
        ItemStack out = held;
        held = ItemStack.EMPTY;
        changed();
        return out;
    }

    /** What a reagent did: set a new root, or added a chord voice. */
    public record Tuned(Identifier id, boolean chord) {
    }

    /** What a hammer strike did. */
    public enum Strike { NONE, MOVED, TRUE, CRACKED, BROKE }

    /**
     * Tunes the held item with this reagent. An untuned item (or {@code reset}, from sneaking) gets this tuning as a
     * fresh root. A tuned item gets it as a chord voice while it has room for one.
     *
     * @return what changed, or null when the reagent does nothing here
     */
    @Nullable
    public Tuned tune(ItemStack reagent, boolean reset) {
        if (held.isEmpty()) {
            return null;
        }
        Identifier id = Tunings.byReagent(reagent);
        if (id == null) {
            return null;
        }
        BellTune now = held.get(CampanileRegistry.TUNING.get());
        if (now == null || now.voices().isEmpty() || reset) {
            if (BellTune.root(id).equals(now)) {
                return null;
            }
            held.set(CampanileRegistry.TUNING.get(), BellTune.root(id));
            changed();
            return new Tuned(id, false);
        }
        if (now.voices().contains(id) || now.voices().size() >= TuneRules.get().maxVoices()) {
            return null;
        }
        held.set(CampanileRegistry.TUNING.get(), now.withVoice(id));
        changed();
        return new Tuned(id, true);
    }

    /**
     * Moves one partial of the held item's tuning by {@code delta}. Striking past the target cracks the bell: the
     * note drops back to 0 and the bell loses durability (an item with none only loses the note).
     */
    public Strike strike(int partial, int delta) {
        BellTune now = held.isEmpty() ? null : held.get(CampanileRegistry.TUNING.get());
        if (now == null || now.voices().isEmpty()) {
            return Strike.NONE;
        }
        int next = now.note(partial) + delta;
        if (next < 0) {
            return Strike.NONE;
        }
        if (next > now.target(partial)) {
            held.set(CampanileRegistry.TUNING.get(), now.withNote(partial, 0));
            Strike out = Strike.CRACKED;
            if (held.isDamageableItem()) {
                int damage = held.getDamageValue() + TuneRules.get().crackDamage();
                if (damage >= held.getMaxDamage()) {
                    held = ItemStack.EMPTY;
                    out = Strike.BROKE;
                } else {
                    held.setDamageValue(damage);
                }
            }
            changed();
            return out;
        }
        held.set(CampanileRegistry.TUNING.get(), now.withNote(partial, next));
        changed();
        return next == now.target(partial) ? Strike.TRUE : Strike.MOVED;
    }

    /** 5 per true note, so a fully true bell reads 15. */
    public int signal() {
        BellTune t = held.isEmpty() ? null : held.get(CampanileRegistry.TUNING.get());
        return t == null || t.voices().isEmpty() ? 0 : 5 * t.trueNotes();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        held = input.read("Held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!held.isEmpty()) {
            output.store("Held", ItemStack.CODEC, held);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !held.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, held);
            held = ItemStack.EMPTY;
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
