package com.oculifelis.campanile.bell;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.fundamentum.recipeview.RecipeViews;
import com.oculifelis.campanile.Campanile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/** The worn-bell table. */
public final class Accessories {
    public static final DataTable<Accessory> TABLE = DataTables
            .register(Campanile.id("accessories"), "campanile/accessories", Accessory::parse)
            .synced()
            .onLoad(t -> RecipeViews.refresh());

    public static void init() {
    }

    @Nullable
    public static Accessory of(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        for (Accessory a : TABLE.entries().values()) {
            if (a.item().equals(id)) {
                return a;
            }
        }
        return null;
    }

    private Accessories() {
    }
}
