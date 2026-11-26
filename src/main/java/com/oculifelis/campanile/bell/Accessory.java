package com.oculifelis.campanile.bell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * One worn bell's numbers, from the {@code campanile/accessories} table.
 *
 * @param item     the accessory item
 * @param radius   blocks around the wearer
 * @param cooldown ticks between rings
 * @param strength scales push distance and effect length
 * @param triggers what makes it ring
 * @param sound    sound event
 * @param pitch    base pitch
 */
public record Accessory(Identifier item, double radius, int cooldown, double strength, Set<Trigger> triggers,
                        Identifier sound, float pitch) {

    public enum Trigger {
        /** A mob hits the wearer. */
        MOB_HIT,
        /** The wearer takes fall damage. */
        FALL,
        /** The wearer is hit by the element the accessory is tuned to. */
        ELEMENT
    }

    public static Accessory parse(JsonObject json) {
        EnumSet<Trigger> triggers = EnumSet.noneOf(Trigger.class);
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "triggers")) {
            triggers.add(Trigger.valueOf(e.getAsString().toUpperCase(Locale.ROOT)));
        }
        return new Accessory(
                Identifier.parse(GsonHelper.getAsString(json, "item")),
                GsonHelper.getAsDouble(json, "radius", 5.0D),
                GsonHelper.getAsInt(json, "cooldown", 160),
                GsonHelper.getAsDouble(json, "strength", 1.0D),
                triggers,
                Identifier.parse(GsonHelper.getAsString(json, "sound", "campanile:bell.small")),
                GsonHelper.getAsFloat(json, "pitch", 1.0F));
    }
}
