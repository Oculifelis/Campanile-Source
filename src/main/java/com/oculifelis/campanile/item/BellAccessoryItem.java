package com.oculifelis.campanile.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A worn bell (Curios). It rings by itself on the triggers its {@code campanile/accessories} entry lists. */
public class BellAccessoryItem extends Item {
    public BellAccessoryItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return HandbellItem.tuning(stack) != null || super.isFoil(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        HandbellItem.tuningLine(stack, tooltip);
    }
}
