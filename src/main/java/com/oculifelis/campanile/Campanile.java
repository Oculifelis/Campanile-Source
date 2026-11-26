package com.oculifelis.campanile;

import com.oculifelis.campanile.bell.Accessories;
import com.oculifelis.campanile.bell.Bells;
import com.oculifelis.campanile.bell.TuneRules;
import com.oculifelis.campanile.bell.Tunings;
import com.oculifelis.campanile.compat.CampanileViews;
import com.oculifelis.campanile.event.CampanileEvents;
import com.oculifelis.campanile.gametest.CampanileGameTests;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Handbells and hung chimes. A Tuning Anvil binds a bell to Fundamentum creature types or elements, a Tuning Hammer
 * brings its three notes true, and ringing it in makes it stronger. Each ring is a single check of the creatures in
 * range. Bells, tunings and accessories are datapack tables.
 */
@Mod(Campanile.MOD_ID)
public class Campanile {
    public static final String MOD_ID = "campanile";

    public Campanile(IEventBus modBus) {
        CampanileRegistry.register(modBus);
        Bells.init();
        Tunings.init();
        TuneRules.init();
        Accessories.init();
        CampanileEvents.init();
        CampanileViews.init();
        CampanileGameTests.init(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
