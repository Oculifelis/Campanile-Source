package com.oculifelis.campanile.bell;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.fundamentum.recipeview.RecipeViews;
import com.oculifelis.campanile.Campanile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/** The bell table, synced so tooltips, JEI and Jade read the same numbers as the server. */
public final class Bells {
    public static final DataTable<Bell> TABLE = DataTables
            .register(Campanile.id("bells"), "campanile/bells", Bell::parse)
            .synced()
            .onLoad(t -> RecipeViews.refresh());

    public static void init() {
    }

    @Nullable
    public static Bell of(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        for (Bell bell : TABLE.entries().values()) {
            if (bell.item().equals(id)) {
                return bell;
            }
        }
        return null;
    }

    private Bells() {
    }
}
