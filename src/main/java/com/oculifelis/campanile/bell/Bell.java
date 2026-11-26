package com.oculifelis.campanile.bell;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

/**
 * One handbell's numbers, from the {@code campanile/bells} table.
 *
 * @param item             the handbell item this entry belongs to
 * @param radius           blocks around the ringer that the ring reaches
 * @param cooldown         ticks before the bell rings again
 * @param strength         scales push distance and effect length
 * @param waterMultiplier  radius and strength multiplier while the ringer is under water
 * @param chimeMultiplier  radius multiplier when the bell hangs in a Belfry Chime
 * @param sound            the sound event played on a ring
 * @param pitch            base pitch, multiplied by the tuning's pitch
 */
public record Bell(Identifier item, double radius, int cooldown, double strength, double waterMultiplier,
                   double chimeMultiplier, Identifier sound, float pitch) {

    public static Bell parse(JsonObject json) {
        return new Bell(
                Identifier.parse(GsonHelper.getAsString(json, "item")),
                GsonHelper.getAsDouble(json, "radius", 8.0D),
                GsonHelper.getAsInt(json, "cooldown", 50),
                GsonHelper.getAsDouble(json, "strength", 1.0D),
                GsonHelper.getAsDouble(json, "water_multiplier", 1.0D),
                GsonHelper.getAsDouble(json, "chime_multiplier", 1.5D),
                Identifier.parse(GsonHelper.getAsString(json, "sound", "campanile:bell.iron")),
                GsonHelper.getAsFloat(json, "pitch", 1.0F));
    }
}
