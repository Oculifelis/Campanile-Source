package com.oculifelis.campanile.bell;

import com.fundamentum.creature.CreatureTypes;
import com.fundamentum.damage.Elements;
import com.fundamentum.element.ElementAuras;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a tuned bell does: which mobs it reaches and the actions it runs on each of them.
 *
 * @param target  the creature type or element this tuning answers to
 * @param pitch   multiplies the bell's pitch
 * @param color   ring particle color
 * @param actions run in order on every matched mob
 * @param reagent  what the Tuning Anvil takes to set this tuning
 * @param partials the three note targets (hum, tierce, nominal), or null to pick them from the id
 */
public record Tuning(Target target, float pitch, int color, List<TuneAction> actions, Reagent reagent,
                     @Nullable List<Integer> partials) {

    /** An item, or any item in a tag, written {@code "minecraft:blaze_rod"} or {@code "#c:rods/blaze"}. */
    public record Reagent(@Nullable TagKey<Item> tag, @Nullable Identifier item) {
        public static Reagent parse(String s) {
            return s.startsWith("#")
                    ? new Reagent(TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1))), null)
                    : new Reagent(null, Identifier.parse(s));
        }

        public boolean test(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (tag != null) {
                return stack.is(tag);
            }
            return item != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }

        /** Every item this reagent accepts, for recipe pages. */
        public List<ItemStack> stacks() {
            List<ItemStack> out = new ArrayList<>();
            if (tag != null) {
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> out.add(new ItemStack(h)));
            } else if (item != null) {
                BuiltInRegistries.ITEM.getOptional(item).ifPresent(i -> out.add(new ItemStack(i)));
            }
            return out;
        }
    }

    /** Who a tuning reaches. */
    public sealed interface Target permits CreatureTarget, ElementTarget {
        boolean matches(Mob mob);

        Component name();
    }

    /** Mobs in {@code #fundamentum:creature/<type>}. */
    public record CreatureTarget(String type) implements Target {
        @Override
        public boolean matches(Mob mob) {
            return CreatureTypes.is(mob, type);
        }

        @Override
        public Component name() {
            return Component.translatable("campanile.creature." + type);
        }
    }

    /** Mobs carrying an aura of the element, or resisting it. */
    public record ElementTarget(Identifier element) implements Target {
        @Nullable
        public Elements.Element resolve() {
            return Elements.get(element);
        }

        @Override
        public boolean matches(Mob mob) {
            Elements.Element e = resolve();
            if (e == null) {
                return false;
            }
            return hasAura(mob, e) || resistance(mob, e) > 0.0D;
        }

        @Override
        public Component name() {
            Elements.Element e = resolve();
            return e == null ? Component.literal(element.toString()) : e.displayName();
        }

        public static boolean hasAura(Mob mob, Elements.Element e) {
            for (ElementAuras.Aura aura : ElementAuras.get(mob)) {
                Elements.Element ae = aura.element().element();
                if (ae != null && ae.isA(e)) {
                    return true;
                }
            }
            return false;
        }

        public static double resistance(Mob mob, Elements.Element e) {
            Holder<Attribute> attr = e.resistance();
            if (attr == null) {
                return 0.0D;
            }
            AttributeInstance inst = mob.getAttribute(attr);
            return inst == null ? 0.0D : inst.getValue();
        }
    }

    public static Tuning parse(JsonObject json) {
        Target target;
        if (json.has("creature")) {
            target = new CreatureTarget(GsonHelper.getAsString(json, "creature"));
        } else {
            target = new ElementTarget(Identifier.parse(GsonHelper.getAsString(json, "element")));
        }
        List<TuneAction> actions = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "actions")) {
            actions.add(TuneAction.parse(e.getAsJsonObject()));
        }
        Reagent reagent;
        if (json.has("reagent")) {
            reagent = Reagent.parse(GsonHelper.getAsString(json, "reagent"));
        } else if (target instanceof ElementTarget et && et.resolve() != null) {
            reagent = new Reagent(et.resolve().weapons(), null);
        } else {
            throw new IllegalArgumentException("a creature tuning needs a reagent");
        }
        List<Integer> partials = null;
        if (json.has("partials")) {
            partials = new ArrayList<>();
            for (JsonElement p : GsonHelper.getAsJsonArray(json, "partials")) {
                partials.add(Math.max(1, p.getAsInt()));
            }
            if (partials.size() != 3) {
                throw new IllegalArgumentException("partials needs 3 numbers");
            }
            partials = List.copyOf(partials);
        }
        return new Tuning(target, GsonHelper.getAsFloat(json, "pitch", 1.0F), color(json), List.copyOf(actions),
                reagent, partials);
    }

    /** The default tuning for an element no table entry names: strip its aura and Discord the mobs that resist it. */
    public static Tuning forElement(Elements.Element e) {
        float pitch = 0.75F + (Math.floorMod(e.id().hashCode(), 50) / 100.0F);
        return new Tuning(new ElementTarget(e.id()), pitch, e.color(),
                List.of(new TuneAction.ClearAura(), new TuneAction.Discord(10.0D)), new Reagent(e.weapons(), null),
                null);
    }

    private static int color(JsonObject json) {
        if (!json.has("color")) {
            return 0xD8C690;
        }
        JsonElement c = json.get("color");
        if (c.isJsonPrimitive() && c.getAsJsonPrimitive().isString()) {
            String s = c.getAsString();
            return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16);
        }
        return c.getAsInt();
    }
}
