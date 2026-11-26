package com.oculifelis.campanile.bell;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.fundamentum.damage.Elements;
import com.fundamentum.recipeview.RecipeViews;
import com.oculifelis.campanile.Campanile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The tuning table. Every element Fundamentum knows has a tuning even without an entry here (its id is the element
 * id), so elements added by other mods or packs work at once; an entry with that id replaces the default.
 */
public final class Tunings {
    public static final DataTable<Tuning> TABLE = DataTables
            .register(Campanile.id("tunings"), "campanile/tunings", Tuning::parse)
            .synced()
            .onLoad(t -> RecipeViews.refresh());

    public static void init() {
    }

    @Nullable
    public static Tuning get(Identifier id) {
        Tuning t = TABLE.get(id);
        if (t != null) {
            return t;
        }
        Elements.Element e = Elements.get(id);
        return e == null ? null : Tuning.forElement(e);
    }

    /** Table entries plus a default for each element without one, in a stable order. */
    public static Map<Identifier, Tuning> all() {
        Map<Identifier, Tuning> out = new LinkedHashMap<>(TABLE.entries());
        for (Elements.Element e : Elements.all()) {
            out.putIfAbsent(e.id(), Tuning.forElement(e));
        }
        return out;
    }

    /** The tuning a reagent sets, or null. Table entries are checked before element defaults. */
    @Nullable
    public static Identifier byReagent(ItemStack stack) {
        for (Map.Entry<Identifier, Tuning> e : all().entrySet()) {
            if (e.getValue().reagent().test(stack)) {
                return e.getKey();
            }
        }
        return null;
    }

    private Tunings() {
    }
}
