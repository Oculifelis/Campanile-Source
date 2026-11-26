package com.oculifelis.campanile.gametest;

import com.fundamentum.damage.Elements;
import com.oculifelis.campanile.Campanile;
import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.bell.Ring;
import com.oculifelis.campanile.bell.TuneRules;
import com.oculifelis.campanile.bell.Tuning;
import com.oculifelis.campanile.bell.TuneAction;
import com.oculifelis.campanile.bell.Tunings;
import com.oculifelis.campanile.block.BelfryChimeBlock;
import com.oculifelis.campanile.block.BelfryChimeBlockEntity;
import com.oculifelis.campanile.block.TuningAnvilBlock;
import com.oculifelis.campanile.block.TuningAnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The core rules: who a ring reaches, what the actions do, the chime's redstone, and tuning on the anvil. */
public final class CampanileGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, Campanile.MOD_ID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    private static final Identifier SOUND = Campanile.id("bell.iron");

    static {
        TESTS.put("untuned_ring_acts_on_nothing", CampanileGameTests::untunedRing);
        TESTS.put("creature_tuning_matches_only_its_type", CampanileGameTests::creatureMatch);
        TESTS.put("pacify_lulls_and_blocks_targeting", CampanileGameTests::pacify);
        TESTS.put("push_moves_mobs_away", CampanileGameTests::push);
        TESTS.put("bosses_and_pets_are_exempt", CampanileGameTests::exempt);
        TESTS.put("discord_lowers_element_resistance", CampanileGameTests::discord);
        TESTS.put("every_element_has_a_tuning", CampanileGameTests::elementDefaults);
        TESTS.put("anvil_tunes_with_reagent", CampanileGameTests::anvil);
        TESTS.put("anvil_adds_chord_voices", CampanileGameTests::chord);
        TESTS.put("hammer_strikes_notes_and_cracks", CampanileGameTests::hammer);
        TESTS.put("hit_height_picks_the_partial", CampanileGameTests::partials);
        TESTS.put("true_notes_ring_stronger_and_ring_in", CampanileGameTests::ringIn);
        TESTS.put("chime_rings_on_redstone_pulse", CampanileGameTests::chimeRedstone);
        TESTS.put("chime_needs_a_block_above", CampanileGameTests::chimeSupport);
    }

    public static void init(IEventBus bus) {
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        FUNCTIONS.register(bus);
        bus.addListener(CampanileGameTests::onRegister);
    }

    private static void onRegister(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env =
                event.registerEnvironment(Campanile.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS.keySet()) {
            event.registerTest(Campanile.id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Campanile.id(name)),
                    new TestData<>(env, Campanile.id("platform"), 120, 0, true)));
        }
    }

    private static Tuning undead(TuneAction... actions) {
        return new Tuning(new Tuning.CreatureTarget("undead"), 1.0F, 0xFFFFFF, List.of(actions),
                Tuning.Reagent.parse("minecraft:rotten_flesh"), null);
    }

    private static Vec3 centre(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(3.5D, 1.0D, 3.5D));
    }

    private static void untunedRing(GameTestHelper helper) {
        helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        int hit = Ring.ring(helper.getLevel(), centre(helper), null, 8.0D, 1.0D, null, null, SOUND, 1.0F);
        helper.assertTrue(hit == 0, "an untuned ring touched " + hit + " mobs");
        helper.succeed();
    }

    private static void creatureMatch(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        int hit = Ring.ring(helper.getLevel(), centre(helper), null, 8.0D, 1.0D, null,
                undead(new TuneAction.Pacify(30.0D)), SOUND, 1.0F);
        helper.assertTrue(hit == 1, "undead tuning matched " + hit + " mobs, wanted 1");
        helper.assertTrue(zombie.hasEffect(CampanileRegistry.LULLED), "zombie was not lulled");
        helper.assertTrue(!cow.hasEffect(CampanileRegistry.LULLED), "cow was lulled by an undead tuning");
        helper.succeed();
    }

    private static void pacify(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        zombie.setTarget(player);
        Ring.ring(helper.getLevel(), centre(helper), null, 8.0D, 1.0D, null, undead(new TuneAction.Pacify(30.0D)),
                SOUND, 1.0F);
        helper.assertTrue(zombie.getTarget() == null, "pacify left the zombie's target set");
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == null, "a lulled zombie picked a target");
        helper.succeed();
    }

    private static void push(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 3));
        Vec3 c = centre(helper);
        double before = zombie.position().distanceTo(c);
        Ring.ring(helper.getLevel(), c, null, 8.0D, 1.0D, null, undead(new TuneAction.Push(3.0D)), SOUND, 1.0F);
        helper.runAfterDelay(10, () -> {
            double after = zombie.position().distanceTo(c);
            helper.assertTrue(after > before + 0.5D, "push moved the zombie from " + before + " to " + after);
            helper.succeed();
        });
    }

    private static void exempt(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Wolf pet = helper.spawn(EntityType.WOLF, new BlockPos(2, 1, 2));
        Wolf wild = helper.spawn(EntityType.WOLF, new BlockPos(4, 1, 4));
        pet.tame(player);
        helper.assertTrue(Ring.exempt(pet), "a tamed wolf is not exempt");
        helper.assertTrue(!Ring.exempt(wild), "a wild wolf is exempt");
        helper.assertTrue(Ring.exempt(helper.spawn(EntityType.WITHER, new BlockPos(3, 1, 3))), "the wither is not exempt");
        helper.succeed();
    }

    private static void discord(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        AttributeInstance fire = zombie.getAttribute(Elements.FIRE.resistance());
        helper.assertTrue(fire != null, "zombies have no fire resistance attribute");
        double before = fire.getValue();
        zombie.addEffect(new MobEffectInstance(CampanileRegistry.DISCORD, 200, 0));
        double after = fire.getValue();
        helper.assertTrue(after < before - 0.2D, "discord changed fire resistance from " + before + " to " + after);
        helper.succeed();
    }

    private static void elementDefaults(GameTestHelper helper) {
        for (Elements.Element e : Elements.all()) {
            Tuning t = Tunings.get(e.id());
            helper.assertTrue(t != null, "no tuning for element " + e.id());
            helper.assertTrue(t.target() instanceof Tuning.ElementTarget et && et.element().equals(e.id()),
                    "tuning " + e.id() + " targets the wrong thing");
        }
        helper.assertTrue(Tunings.all().size() >= Elements.all().size(), "fewer tunings than elements");
        helper.succeed();
    }

    private static ItemStack firstReagent(Identifier skip) {
        for (Map.Entry<Identifier, Tuning> e : Tunings.all().entrySet()) {
            List<ItemStack> stacks = e.getValue().reagent().stacks();
            Identifier sets = stacks.isEmpty() ? null : Tunings.byReagent(stacks.get(0));
            if (sets != null && !sets.equals(skip)) {
                return stacks.get(0);
            }
        }
        return ItemStack.EMPTY;
    }

    private static TuningAnvilBlockEntity anvilWith(GameTestHelper helper, ItemStack held) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, CampanileRegistry.TUNING_ANVIL.get());
        TuningAnvilBlockEntity be = helper.getBlockEntity(pos, TuningAnvilBlockEntity.class);
        if (!held.isEmpty()) {
            be.setHeld(held);
        }
        return be;
    }

    private static void anvil(GameTestHelper helper) {
        TuningAnvilBlockEntity be = anvilWith(helper, ItemStack.EMPTY);
        helper.assertTrue(be.tune(new ItemStack(Items.DIRT), false) == null, "an empty anvil tuned something");
        be.setHeld(new ItemStack(CampanileRegistry.IRON_HANDBELL.get()));
        helper.assertTrue(be.tune(new ItemStack(Items.DIRT), false) == null, "dirt set a tuning");
        ItemStack reagent = firstReagent(null);
        helper.assertTrue(!reagent.isEmpty(), "no tuning has a reagent item");
        TuningAnvilBlockEntity.Tuned set = be.tune(reagent, false);
        helper.assertTrue(set != null && !set.chord(), "reagent " + reagent + " set no root tuning");
        BellTune tune = be.held().get(CampanileRegistry.TUNING.get());
        helper.assertTrue(tune != null && set.id().equals(tune.rootId()), "the held bell kept no tuning");
        helper.assertTrue(tune.trueNotes() == 0 && tune.rung() == 0, "a fresh tuning starts with notes set");
        helper.assertTrue(be.signal() == 0, "an untrue bell gives a comparator signal");
        helper.succeed();
    }

    private static void chord(GameTestHelper helper) {
        TuningAnvilBlockEntity be = anvilWith(helper, new ItemStack(CampanileRegistry.IRON_HANDBELL.get()));
        ItemStack first = firstReagent(null);
        Identifier root = be.tune(first, false).id();
        helper.assertTrue(be.tune(first, false) == null, "the same reagent added its own tuning as a chord");
        ItemStack second = firstReagent(root);
        helper.assertTrue(!second.isEmpty(), "only one tuning has a reagent");
        TuningAnvilBlockEntity.Tuned voice = be.tune(second, false);
        helper.assertTrue(voice != null && voice.chord(), "a second reagent added no chord voice");
        BellTune tune = be.held().get(CampanileRegistry.TUNING.get());
        helper.assertTrue(tune.voices().size() == 2 && tune.rootId().equals(root), "chord voices are wrong: " + tune);
        TuningAnvilBlockEntity.Tuned reset = be.tune(second, true);
        helper.assertTrue(reset != null && !reset.chord(), "sneaking with a reagent did not start over");
        helper.assertTrue(be.held().get(CampanileRegistry.TUNING.get()).voices().size() == 1, "reset kept the chord");
        helper.assertTrue(TuneRules.get().maxVoices() == 3, "default rules allow other than 3 voices");
        helper.succeed();
    }

    private static void hammer(GameTestHelper helper) {
        ItemStack bell = new ItemStack(CampanileRegistry.IRON_HANDBELL.get());
        TuningAnvilBlockEntity be = anvilWith(helper, bell);
        helper.assertTrue(be.strike(BellTune.HUM, 1) == TuningAnvilBlockEntity.Strike.NONE, "an untuned bell took a note");
        be.tune(firstReagent(null), false);
        BellTune tune = be.held().get(CampanileRegistry.TUNING.get());
        for (int p = 0; p < 3; p++) {
            int target = tune.target(p);
            helper.assertTrue(target >= 1 && target <= TuneRules.get().maxStep(), "target out of range: " + target);
            for (int i = 1; i <= target; i++) {
                TuningAnvilBlockEntity.Strike r = be.strike(p, 1);
                helper.assertTrue(r == (i == target ? TuningAnvilBlockEntity.Strike.TRUE
                        : TuningAnvilBlockEntity.Strike.MOVED), "strike " + i + " of " + target + " gave " + r);
            }
            helper.assertTrue(be.signal() == 5 * (p + 1), "comparator reads " + be.signal() + " after " + (p + 1));
        }
        helper.assertTrue(be.held().get(CampanileRegistry.TUNING.get()).inTune(), "all notes struck but not in tune");
        helper.assertTrue(be.strike(BellTune.NOMINAL, -1) == TuningAnvilBlockEntity.Strike.MOVED, "striking down failed");
        be.strike(BellTune.NOMINAL, 1);
        int before = be.held().getDamageValue();
        helper.assertTrue(be.strike(BellTune.NOMINAL, 1) == TuningAnvilBlockEntity.Strike.CRACKED, "overshoot did not crack");
        helper.assertTrue(be.held().get(CampanileRegistry.TUNING.get()).note(BellTune.NOMINAL) == 0, "crack kept the note");
        helper.assertTrue(be.held().getDamageValue() == before + TuneRules.get().crackDamage(), "crack cost no durability");
        helper.assertTrue(be.strike(BellTune.HUM, -10) == TuningAnvilBlockEntity.Strike.NONE, "a note went below 0");
        helper.succeed();
    }

    private static void partials(GameTestHelper helper) {
        helper.assertTrue(TuningAnvilBlock.partialAt(0.1D) == BellTune.HUM, "base is not the hum");
        helper.assertTrue(TuningAnvilBlock.partialAt(0.4D) == BellTune.TIERCE, "waist is not the tierce");
        helper.assertTrue(TuningAnvilBlock.partialAt(0.8D) == BellTune.NOMINAL, "top is not the nominal");
        helper.succeed();
    }

    private static void ringIn(GameTestHelper helper) {
        Identifier undead = null;
        for (Map.Entry<Identifier, Tuning> e : Tunings.all().entrySet()) {
            if (e.getValue().target() instanceof Tuning.CreatureTarget ct && ct.type().equals("undead")) {
                undead = e.getKey();
            }
        }
        helper.assertTrue(undead != null, "no undead tuning in the table");
        helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        BellTune rough = BellTune.root(undead);
        BellTune tuned = rough;
        for (int p = 0; p < 3; p++) {
            tuned = tuned.withNote(p, tuned.target(p));
        }
        ItemStack bell = new ItemStack(CampanileRegistry.IRON_HANDBELL.get());
        bell.set(CampanileRegistry.TUNING.get(), rough);
        Ring.ringStack(helper.getLevel(), centre(helper), null, 8.0D, 1.0D, bell, SOUND, 1.0F);
        helper.assertTrue(bell.get(CampanileRegistry.TUNING.get()).rung() == 0, "an untrue bell rang in");
        bell.set(CampanileRegistry.TUNING.get(), tuned);
        int hit = Ring.ringStack(helper.getLevel(), centre(helper), null, 8.0D, 1.0D, bell, SOUND, 1.0F);
        helper.assertTrue(hit == 1, "the tuned bell reached " + hit + " mobs");
        helper.assertTrue(bell.get(CampanileRegistry.TUNING.get()).rung() == 1, "a true ring did not count");
        TuneRules r = TuneRules.get();
        helper.assertTrue(r.noteStrength()[3] > r.noteStrength()[0], "true notes are not stronger");
        BellTune perfect = new BellTune(tuned.voices(), tuned.notes(), r.perfectAt());
        helper.assertTrue(perfect.stage() == 2 && tuned.stage() == 0, "stages are wrong");
        helper.succeed();
    }

    private static void chimeRedstone(GameTestHelper helper) {
        BlockPos chime = new BlockPos(3, 2, 3);
        helper.setBlock(chime.above(), Blocks.STONE);
        helper.setBlock(chime, CampanileRegistry.BELFRY_CHIME.get());
        BelfryChimeBlockEntity be = helper.getBlockEntity(chime, BelfryChimeBlockEntity.class);
        be.setBell(new ItemStack(CampanileRegistry.IRON_HANDBELL.get()));
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(chime);
        level.setBlock(abs, level.getBlockState(abs).setValue(BelfryChimeBlock.HAS_BELL, true), 3);
        helper.setBlock(chime.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            BlockState s = level.getBlockState(abs);
            helper.assertTrue(s.getValue(BelfryChimeBlock.RINGING), "the chime did not ring on a pulse");
            helper.assertTrue(s.getAnalogOutputSignal(level, abs, Direction.NORTH) == 15, "comparator is not 15");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!level.getBlockState(abs).getValue(BelfryChimeBlock.RINGING), "the chime kept ringing");
            helper.succeed();
        });
    }

    private static void chimeSupport(GameTestHelper helper) {
        BlockPos chime = new BlockPos(3, 2, 3);
        BlockState state = CampanileRegistry.BELFRY_CHIME.get().defaultBlockState();
        ServerLevel level = helper.getLevel();
        helper.assertTrue(!state.canSurvive(level, helper.absolutePos(chime)), "a chime survives with air above");
        helper.setBlock(chime.above(), Blocks.STONE);
        helper.assertTrue(state.canSurvive(level, helper.absolutePos(chime)), "a chime fails under stone");
        helper.succeed();
    }

    private CampanileGameTests() {
    }
}
