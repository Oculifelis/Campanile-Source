package com.oculifelis.campanile.bell;

import com.fundamentum.creature.CreatureTypes;
import com.oculifelis.campanile.CampanileRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One ring: a single area query around the bell, then the tuning's actions on every mob it matches. Players, bosses
 * and owned pets are never touched. Nothing here ticks; the only follow-up is the effects the actions apply.
 */
public final class Ring {
    public static final TagKey<EntityType<?>> C_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));

    /** Everything a single ring needs. */
    public record Context(ServerLevel level, Vec3 center, @Nullable LivingEntity ringer, double radius, double strength,
                          @Nullable Identifier tuningId, @Nullable Tuning tuning) {
    }

    /**
     * Rings at {@code center}. Untuned bells (no tuning, or a tuning id missing from the table) only make the sound.
     *
     * @return how many mobs the tuning acted on
     */
    public static int ring(ServerLevel level, Vec3 center, @Nullable LivingEntity ringer, double radius, double strength,
                           @Nullable Identifier tuningId, Identifier sound, float pitch) {
        return ring(level, center, ringer, radius, strength, tuningId,
                tuningId == null ? null : Tunings.get(tuningId), sound, pitch);
    }

    /** Rings with a tuning already looked up (or built by a test). */
    public static int ring(ServerLevel level, Vec3 center, @Nullable LivingEntity ringer, double radius, double strength,
                           @Nullable Identifier tuningId, @Nullable Tuning tuning, Identifier sound, float pitch) {
        show(level, center, radius, tuning, sound, pitch);
        return tuning == null ? 0 : act(level, center, ringer, radius, strength, tuningId, tuning);
    }

    /**
     * Rings a bell or accessory stack with its own tuning. Strength follows how many notes are true and how far the
     * bell is rung in, and each chord voice acts in turn at its own share. A ring that reaches a matching mob while
     * every note is true counts toward ringing in.
     *
     * @return how many mobs the voices acted on, counted once per voice
     */
    public static int ringStack(ServerLevel level, Vec3 center, @Nullable LivingEntity ringer, double radius,
                                double strength, ItemStack stack, Identifier sound, float pitch) {
        BellTune tune = stack.get(CampanileRegistry.TUNING.get());
        if (tune == null || tune.voices().isEmpty()) {
            return ring(level, center, ringer, radius, strength, null, null, sound, pitch);
        }
        TuneRules r = TuneRules.get();
        int stage = tune.stage();
        double s = strength * r.noteStrength()[tune.trueNotes()] * r.stageStrength()[stage];
        double rad = radius * r.stageRadius()[stage];
        show(level, center, rad, Tunings.get(tune.rootId()), sound, pitch);
        int matched = 0;
        for (int i = 0; i < tune.voices().size() && i < r.maxVoices(); i++) {
            Identifier id = tune.voices().get(i);
            Tuning t = Tunings.get(id);
            if (t != null) {
                matched += act(level, center, ringer, rad, s * r.chordStrength()[i], id, t);
            }
        }
        if (matched > 0 && tune.inTune() && tune.rung() < r.perfectAt()) {
            stack.set(CampanileRegistry.TUNING.get(), tune.rungOnce());
        }
        return matched;
    }

    private static void show(ServerLevel level, Vec3 center, double radius, @Nullable Tuning tuning, Identifier sound,
                             float pitch) {
        float finalPitch = pitch * (tuning == null ? 1.0F : tuning.pitch());
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(sound);
        if (event != null) {
            level.playSound(null, center.x, center.y, center.z, event, SoundSource.BLOCKS, 1.5F, finalPitch);
        }
        particles(level, center, radius, tuning == null ? 0xD8C690 : tuning.color());
    }

    private static int act(ServerLevel level, Vec3 center, @Nullable LivingEntity ringer, double radius,
                           double strength, @Nullable Identifier tuningId, Tuning tuning) {
        Context ctx = new Context(level, center, ringer, radius, strength, tuningId, tuning);
        AABB box = new AABB(center, center).inflate(radius);
        double r2 = radius * radius;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box,
                m -> m.isAlive() && m.distanceToSqr(center) <= r2 && !exempt(m) && tuning.target().matches(m));
        for (Mob mob : mobs) {
            for (TuneAction action : tuning.actions()) {
                action.apply(mob, ctx);
            }
        }
        return mobs.size();
    }

    /** Bosses and owned pets ignore every tuning. */
    public static boolean exempt(Mob mob) {
        if (mob.getType().builtInRegistryHolder().is(C_BOSSES) || CreatureTypes.is(mob, "boss")) {
            return true;
        }
        return mob instanceof OwnableEntity owned && owned.getOwnerReference() != null;
    }

    private static void particles(ServerLevel level, Vec3 c, double radius, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.2F);
        int points = (int) Math.min(64, Math.max(16, radius * 4));
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(dust, c.x + Math.cos(a) * radius, c.y + 0.3D, c.z + Math.sin(a) * radius,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    private Ring() {
    }
}
