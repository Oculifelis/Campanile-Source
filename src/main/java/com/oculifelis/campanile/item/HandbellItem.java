package com.oculifelis.campanile.item;

import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.Bell;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.bell.Bells;
import com.oculifelis.campanile.bell.Ring;
import com.oculifelis.campanile.bell.Tuning;
import com.oculifelis.campanile.bell.Tunings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** A handbell. Right click rings it; its numbers come from the {@code campanile/bells} table. */
public class HandbellItem extends Item {
    /** Used when a pack removes this bell's table entry. */
    private static final Bell FALLBACK = new Bell(Identifier.withDefaultNamespace("air"), 8.0D, 50, 1.0D, 1.0D, 1.5D,
            Identifier.fromNamespaceAndPath("campanile", "bell.iron"), 1.0F);

    public HandbellItem(Properties properties) {
        super(properties);
    }

    public Bell bell() {
        Bell b = Bells.of(this);
        return b == null ? FALLBACK : b;
    }

    /** The root tuning id, or null when untuned. */
    @Nullable
    public static Identifier tuning(ItemStack stack) {
        BellTune t = tune(stack);
        return t == null || t.voices().isEmpty() ? null : t.rootId();
    }

    @Nullable
    public static BellTune tune(ItemStack stack) {
        return stack.get(CampanileRegistry.TUNING.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Bell b = bell();
        if (level instanceof ServerLevel server) {
            double mult = player.isUnderWater() ? b.waterMultiplier() : 1.0D;
            Ring.ringStack(server, player.position().add(0.0D, 1.0D, 0.0D), player, b.radius() * mult,
                    b.strength() * mult, stack, b.sound(), b.pitch());
            if (tuning(stack) != null) {
                stack.hurtAndBreak(1, player, hand);
            }
        }
        player.getCooldowns().addCooldown(stack, b.cooldown());
        player.swing(hand, true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tuning(stack) != null || super.isFoil(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tuningLine(stack, tooltip);
    }

    /** Shared by bells and accessories: the voices, then the notes, then the ringing-in stage. */
    public static void tuningLine(ItemStack stack, Consumer<Component> tooltip) {
        BellTune tune = tune(stack);
        if (tune == null || tune.voices().isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.campanile.untuned").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < tune.voices().size(); i++) {
            tooltip.accept(Component.translatable(i == 0 ? "tooltip.campanile.tuned" : "tooltip.campanile.voice",
                    targetName(tune.voices().get(i))).withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        }
        tooltip.accept(notesLine(tune));
        tooltip.accept(Component.translatable("tooltip.campanile.stage." + tune.stage(), tune.rung())
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    public static Component targetName(Identifier id) {
        Tuning t = Tunings.get(id);
        return t == null ? Component.literal(id.toString()) : t.target().name();
    }

    /** One line with each partial as note/target, green when true. */
    public static Component notesLine(BellTune tune) {
        MutableComponent line = Component.empty();
        for (int p = 0; p < 3; p++) {
            if (p > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.translatable("tooltip.campanile.note." + BellTune.PARTIALS[p], tune.note(p),
                    tune.target(p)).withStyle(tune.isTrue(p) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        return line;
    }
}
