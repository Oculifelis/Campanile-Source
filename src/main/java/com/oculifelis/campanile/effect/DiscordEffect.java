package com.oculifelis.campanile.effect;

import com.fundamentum.damage.Elements;
import com.oculifelis.campanile.Campanile;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Lowers every element resistance the mob has by 0.25 per level. The elements are read from Fundamentum when the
 * effect starts, so elements other mods add are covered too.
 */
public final class DiscordEffect extends MobEffect {
    public static final Identifier MODIFIER = Campanile.id("discord");
    public static final double PER_LEVEL = -0.25D;

    public DiscordEffect() {
        super(MobEffectCategory.HARMFUL, 0x8E5BB5);
    }

    private static Set<Holder<Attribute>> resistances() {
        Set<Holder<Attribute>> out = new LinkedHashSet<>();
        for (Elements.Element e : Elements.all()) {
            if (e.resistance() != null) {
                out.add(e.resistance());
            }
        }
        return out;
    }

    private static AttributeModifier modifier(int amplifier) {
        return new AttributeModifier(MODIFIER, PER_LEVEL * (amplifier + 1), AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        AttributeModifier m = modifier(amplifier);
        for (Holder<Attribute> attr : resistances()) {
            consumer.accept(attr, m);
        }
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        AttributeModifier m = modifier(amplifier);
        for (Holder<Attribute> attr : resistances()) {
            AttributeInstance inst = attributes.getInstance(attr);
            if (inst != null) {
                inst.removeModifier(MODIFIER);
                inst.addPermanentModifier(m);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        for (Holder<Attribute> attr : resistances()) {
            AttributeInstance inst = attributes.getInstance(attr);
            if (inst != null) {
                inst.removeModifier(MODIFIER);
            }
        }
    }
}
