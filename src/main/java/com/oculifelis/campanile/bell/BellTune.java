package com.oculifelis.campanile.bell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The tuning a bell or accessory carries.
 *
 * @param voices tuning ids; the first is the root, any others are chord voices
 * @param notes  where the three partials (hum, tierce, nominal) stand, each counted up from 0 to its target
 * @param rung   rings that reached a matching mob while all three notes were true
 */
public record BellTune(List<Identifier> voices, List<Integer> notes, int rung) {
    public static final int HUM = 0;
    public static final int TIERCE = 1;
    public static final int NOMINAL = 2;
    public static final String[] PARTIALS = {"hum", "tierce", "nominal"};

    public static final Codec<BellTune> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.listOf().fieldOf("voices").forGetter(BellTune::voices),
            Codec.INT.listOf().fieldOf("notes").forGetter(BellTune::notes),
            Codec.INT.optionalFieldOf("rung", 0).forGetter(BellTune::rung)
    ).apply(i, BellTune::new));

    public static final StreamCodec<ByteBuf, BellTune> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), BellTune::voices,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), BellTune::notes,
            ByteBufCodecs.VAR_INT, BellTune::rung,
            BellTune::new);

    public BellTune {
        voices = List.copyOf(voices);
        List<Integer> n = new ArrayList<>(notes);
        while (n.size() < 3) {
            n.add(0);
        }
        notes = List.copyOf(n.subList(0, 3));
    }

    /** A fresh tuning: one root, every note at 0, not rung in. */
    public static BellTune root(Identifier id) {
        return new BellTune(List.of(id), List.of(0, 0, 0), 0);
    }

    public Identifier rootId() {
        return voices.get(0);
    }

    public int note(int partial) {
        return notes.get(partial);
    }

    public int target(int partial) {
        return TuneRules.targets(rootId())[partial];
    }

    public boolean isTrue(int partial) {
        return note(partial) == target(partial);
    }

    public int trueNotes() {
        int n = 0;
        for (int p = 0; p < 3; p++) {
            if (isTrue(p)) {
                n++;
            }
        }
        return n;
    }

    public boolean inTune() {
        return trueNotes() == 3;
    }

    /** 0 rough, 1 true, 2 perfect. */
    public int stage() {
        TuneRules r = TuneRules.get();
        if (rung >= r.perfectAt()) {
            return 2;
        }
        return rung >= r.trueAt() ? 1 : 0;
    }

    public BellTune withNote(int partial, int value) {
        List<Integer> n = new ArrayList<>(notes);
        n.set(partial, Mth.clamp(value, 0, 99));
        return new BellTune(voices, n, rung);
    }

    public BellTune withVoice(Identifier id) {
        List<Identifier> v = new ArrayList<>(voices);
        v.add(id);
        return new BellTune(v, notes, rung);
    }

    public BellTune rungOnce() {
        return new BellTune(voices, notes, rung + 1);
    }
}
