package com.oculifelis.campanile.bell;

import com.fundamentum.data.DataTable;
import com.fundamentum.data.DataTables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oculifelis.campanile.Campanile;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.List;

/**
 * The numbers behind tuning, from the {@code campanile/rules} table entry {@code campanile:default}.
 *
 * @param maxStep       note targets fall between 1 and this
 * @param noteStrength  ring strength by how many notes are true (0 to 3)
 * @param crackDamage   durability a bell loses when a note is struck past its target
 * @param trueAt        rings to reach the "true" stage
 * @param perfectAt     rings to reach the "perfect" stage
 * @param stageStrength strength multiplier for rough, true, perfect
 * @param stageRadius   radius multiplier for rough, true, perfect
 * @param chordStrength strength of the root and of the second and third voices
 */
public record TuneRules(int maxStep, double[] noteStrength, int crackDamage, int trueAt, int perfectAt,
                        double[] stageStrength, double[] stageRadius, double[] chordStrength) {
    public static final TuneRules DEFAULT = new TuneRules(6, new double[]{0.4D, 0.6D, 0.8D, 1.0D}, 20, 20, 80,
            new double[]{1.0D, 1.25D, 1.5D}, new double[]{1.0D, 1.15D, 1.3D}, new double[]{1.0D, 0.6D, 0.4D});
    public static final Identifier ENTRY = Campanile.id("default");

    public static final DataTable<TuneRules> TABLE = DataTables
            .register(Campanile.id("rules"), "campanile/rules", TuneRules::parse)
            .synced();

    public static void init() {
    }

    public static TuneRules get() {
        TuneRules r = TABLE.get(ENTRY);
        return r == null ? DEFAULT : r;
    }

    public int maxVoices() {
        return chordStrength.length;
    }

    /** The three note targets for a root tuning: the entry's own {@code partials}, or picked from its id. */
    public static int[] targets(Identifier rootId) {
        Tuning t = Tunings.get(rootId);
        if (t != null && t.partials() != null) {
            List<Integer> p = t.partials();
            return new int[]{p.get(0), p.get(1), p.get(2)};
        }
        int max = Math.max(1, get().maxStep());
        int h = rootId.toString().hashCode();
        return new int[]{1 + Math.floorMod(h, max), 1 + Math.floorMod(h >> 8, max), 1 + Math.floorMod(h >> 16, max)};
    }

    public static TuneRules parse(JsonObject json) {
        TuneRules d = DEFAULT;
        return new TuneRules(
                GsonHelper.getAsInt(json, "max_step", d.maxStep()),
                doubles(json, "note_strength", d.noteStrength(), 4),
                GsonHelper.getAsInt(json, "crack_damage", d.crackDamage()),
                GsonHelper.getAsInt(json, "true_at", d.trueAt()),
                GsonHelper.getAsInt(json, "perfect_at", d.perfectAt()),
                doubles(json, "stage_strength", d.stageStrength(), 3),
                doubles(json, "stage_radius", d.stageRadius(), 3),
                doubles(json, "chord_strength", d.chordStrength(), -1));
    }

    private static double[] doubles(JsonObject json, String key, double[] fallback, int size) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonArray a = GsonHelper.getAsJsonArray(json, key);
        if (size > 0 && a.size() != size) {
            throw new IllegalArgumentException(key + " needs " + size + " numbers");
        }
        if (a.isEmpty()) {
            throw new IllegalArgumentException(key + " is empty");
        }
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsDouble();
        }
        return out;
    }
}
