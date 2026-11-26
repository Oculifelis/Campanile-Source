package com.oculifelis.campanile.compat;

import com.oculifelis.campanile.Campanile;
import com.oculifelis.campanile.block.BelfryChimeBlock;
import com.oculifelis.campanile.block.BelfryChimeBlockEntity;
import com.oculifelis.campanile.block.TuningAnvilBlock;
import com.oculifelis.campanile.block.TuningAnvilBlockEntity;
import com.oculifelis.campanile.item.HandbellItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Shows what sits on the Tuning Anvil or hangs in a Belfry Chime, with its voices, notes and stage. */
@WailaPlugin
public class CampanileJadePlugin implements IWailaPlugin {
    private static final Identifier BELLS = Campanile.id("bells");
    private static final String KEY = "CPLines";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Data.INSTANCE, TuningAnvilBlockEntity.class);
        registration.registerBlockDataProvider(Data.INSTANCE, BelfryChimeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, TuningAnvilBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, BelfryChimeBlock.class);
    }

    private static List<Component> lines(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        if (stack.isEmpty()) {
            return out;
        }
        out.add(stack.getHoverName());
        HandbellItem.tuningLine(stack, out::add);
        return out;
    }

    public enum Data implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            ItemStack stack = ItemStack.EMPTY;
            if (accessor.getBlockEntity() instanceof TuningAnvilBlockEntity anvil) {
                stack = anvil.held();
            } else if (accessor.getBlockEntity() instanceof BelfryChimeBlockEntity chime) {
                stack = chime.bell();
            }
            ListTag list = new ListTag();
            for (Component line : lines(stack)) {
                list.add(accessor.encodeAsNbt(ComponentSerialization.STREAM_CODEC, line));
            }
            data.put(KEY, list);
        }

        @Override
        public Identifier getUid() {
            return BELLS;
        }
    }

    public enum Provider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Tag raw = accessor.getServerData().get(KEY);
            if (raw instanceof ListTag list) {
                for (Tag tag : list) {
                    accessor.decodeFromNbt(ComponentSerialization.STREAM_CODEC, tag).ifPresent(tooltip::add);
                }
            }
        }

        @Override
        public Identifier getUid() {
            return BELLS;
        }
    }
}
