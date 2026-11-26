package com.oculifelis.campanile.compat;

import com.fundamentum.recipeview.RecipeViews;
import com.fundamentum.recipeview.ViewCategory;
import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.bell.TuneAction;
import com.oculifelis.campanile.bell.Tuning;
import com.oculifelis.campanile.bell.Tunings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recipe viewer pages through Fundamentum: one "Tuning" page per tuning entry (item, reagent, the tuned item, and
 * what it acts on), plus information pages for the anvil, the chime and the accessories. Pages read the synced
 * table, so datapack tunings and elements other mods add show up.
 */
public final class CampanileViews {
    public static final String TUNING = "campanile:tuning";

    public static void init() {
        ViewCategory<Map.Entry<Identifier, Tuning>> c = RecipeViews.<Map.Entry<Identifier, Tuning>>category(TUNING)
                .title(Component.translatable("jei.campanile.tuning"))
                .icon(() -> new ItemStack(CampanileRegistry.TUNING_ANVIL.get()))
                .size(150, 64)
                .arrow(48, 4, 0)
                .catalysts(() -> List.of(new ItemStack(CampanileRegistry.TUNING_ANVIL.get())));
        int bell = c.input(4, 4);
        int reagent = c.input(24, 4);
        int out = c.output(80, 4);
        c.recipes(CampanileViews::sorted)
                .key(e -> e.getKey().getNamespace() + "/" + e.getKey().getPath())
                .fill((e, page) -> {
                    Tuning t = e.getValue();
                    List<ItemStack> plain = new ArrayList<>();
                    List<ItemStack> tuned = new ArrayList<>();
                    for (Item item : CampanileRegistry.tunables()) {
                        plain.add(new ItemStack(item));
                        ItemStack s = new ItemStack(item);
                        s.set(CampanileRegistry.TUNING.get(), BellTune.root(e.getKey()));
                        tuned.add(s);
                    }
                    page.items(bell, plain).items(reagent, t.reagent().stacks()).items(out, tuned);
                    page.text(Component.translatable("jei.campanile.target", t.target().name()), 2, 28);
                    page.text(actions(t), 2, 40);
                });

        RecipeViews.info(() -> List.of(new ItemStack(CampanileRegistry.TUNING_ANVIL.get())),
                () -> List.of(Component.translatable("info.campanile.tuning_anvil")));
        RecipeViews.info(() -> List.of(new ItemStack(CampanileRegistry.BELFRY_CHIME.get())),
                () -> List.of(Component.translatable("info.campanile.belfry_chime")));
        RecipeViews.info(() -> List.of(new ItemStack(CampanileRegistry.CHIME_PIN.get()),
                        new ItemStack(CampanileRegistry.BELL_ANKLET.get())),
                () -> List.of(Component.translatable("info.campanile.accessories")));
        RecipeViews.info(() -> CampanileRegistry.handbells().stream().map(ItemStack::new).toList(),
                () -> List.of(Component.translatable("info.campanile.handbells")));
    }

    private static List<Map.Entry<Identifier, Tuning>> sorted() {
        List<Map.Entry<Identifier, Tuning>> out = new ArrayList<>(Tunings.all().entrySet());
        out.sort(Comparator.comparing(e -> e.getKey().toString()));
        return out;
    }

    private static Component actions(Tuning t) {
        var line = Component.empty();
        boolean first = true;
        for (TuneAction a : t.actions()) {
            if (!first) {
                line.append(", ");
            }
            first = false;
            line.append(Component.translatable("jei.campanile.action."
                    + a.getClass().getSimpleName().toLowerCase(Locale.ROOT)));
        }
        return line;
    }

    private CampanileViews() {
    }
}
