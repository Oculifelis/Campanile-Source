package com.oculifelis.campanile.bell;

import com.fundamentum.damage.Elements;
import com.fundamentum.element.ElementAuras;
import com.google.gson.JsonObject;
import com.oculifelis.campanile.CampanileRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One step a tuning runs on a matched mob. Read from the {@code actions} list of a tuning entry. */
public sealed interface TuneAction permits TuneAction.Push, TuneAction.Call, TuneAction.Pacify, TuneAction.Effect,
        TuneAction.ClearAura, TuneAction.Discord {

    void apply(Mob mob, Ring.Context ctx);

    static TuneAction parse(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "push" -> new Push(GsonHelper.getAsDouble(json, "distance", 3.0D));
            case "call" -> new Call(GsonHelper.getAsDouble(json, "speed", 1.1D));
            case "pacify" -> new Pacify(GsonHelper.getAsDouble(json, "seconds", 30.0D));
            case "effect" -> new Effect(Identifier.parse(GsonHelper.getAsString(json, "effect")),
                    GsonHelper.getAsDouble(json, "seconds", 4.0D), GsonHelper.getAsInt(json, "amplifier", 0));
            case "clear_aura" -> new ClearAura();
            case "discord" -> new Discord(GsonHelper.getAsDouble(json, "seconds", 10.0D));
            default -> throw new IllegalArgumentException("Unknown tuning action type: " + type);
        };
    }

    private static int ticks(double seconds, Ring.Context ctx) {
        return Math.max(1, (int) Math.round(seconds * 20.0D * ctx.strength()));
    }

    /** Knocks the mob away from the bell. */
    record Push(double distance) implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            Vec3 c = ctx.center();
            double dx = c.x - mob.getX();
            double dz = c.z - mob.getZ();
            if (dx * dx + dz * dz < 1.0E-4D) {
                dx = mob.getRandom().nextDouble() - 0.5D;
                dz = mob.getRandom().nextDouble() - 0.5D;
            }
            mob.knockback(distance * 0.3D * ctx.strength(), dx, dz);
            mob.hurtMarked = true;
        }
    }

    /** Walks the mob to the ringer, or to the bell when nobody rang it by hand. */
    record Call(double speed) implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            Vec3 to = ctx.ringer() != null ? ctx.ringer().position() : ctx.center();
            mob.getNavigation().moveTo(to.x, to.y, to.z, speed);
        }
    }

    /** Drops the mob's anger and target and gives it Lulled. */
    record Pacify(double seconds) implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            if (mob instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(CampanileRegistry.LULLED, ticks(seconds, ctx)), ctx.ringer());
        }
    }

    /** Applies any status effect, its length scaled by the bell's strength. */
    record Effect(Identifier effect, double seconds, int amplifier) implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            BuiltInRegistries.MOB_EFFECT.get(effect).ifPresent((Holder<MobEffect> h) ->
                    mob.addEffect(new MobEffectInstance(h, ticks(seconds, ctx), amplifier), ctx.ringer()));
        }
    }

    /** Removes every aura of the tuning's element (and its variants). Only meaningful on element tunings. */
    record ClearAura() implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            if (!(ctx.tuning().target() instanceof Tuning.ElementTarget et)) {
                return;
            }
            Elements.Element e = et.resolve();
            if (e == null) {
                return;
            }
            List<ElementAuras.Aura> drop = new ArrayList<>();
            for (ElementAuras.Aura aura : ElementAuras.get(mob)) {
                Elements.Element ae = aura.element().element();
                if (ae != null && ae.isA(e)) {
                    drop.add(aura);
                }
            }
            for (ElementAuras.Aura aura : drop) {
                ElementAuras.remove(mob, aura);
            }
        }
    }

    /** Gives Discord to mobs that resist the tuning's element. */
    record Discord(double seconds) implements TuneAction {
        @Override
        public void apply(Mob mob, Ring.Context ctx) {
            if (!(ctx.tuning().target() instanceof Tuning.ElementTarget et)) {
                return;
            }
            Elements.Element e = et.resolve();
            if (e != null && Tuning.ElementTarget.resistance(mob, e) > 0.0D) {
                mob.addEffect(new MobEffectInstance(CampanileRegistry.DISCORD, ticks(seconds, ctx)), ctx.ringer());
            }
        }
    }
}
