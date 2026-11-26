package com.oculifelis.campanile;

import com.oculifelis.campanile.effect.DiscordEffect;
import com.oculifelis.campanile.bell.BellTune;
import com.oculifelis.campanile.block.BelfryChimeBlock;
import com.oculifelis.campanile.block.BelfryChimeBlockEntity;
import com.oculifelis.campanile.block.TuningAnvilBlock;
import com.oculifelis.campanile.block.TuningAnvilBlockEntity;
import com.oculifelis.campanile.item.BellAccessoryItem;
import com.oculifelis.campanile.item.HandbellItem;
import com.oculifelis.campanile.item.TuningHammerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class CampanileRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Campanile.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Campanile.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Campanile.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Campanile.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Campanile.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Campanile.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Campanile.MOD_ID);

    /** Items that fit on the Tuning Anvil. */
    public static final TagKey<Item> TUNABLE = TagKey.create(Registries.ITEM, Campanile.id("tunable"));

    /** The tuning a bell or accessory carries: its voices, its three notes and how far it is rung in. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BellTune>> TUNING =
            COMPONENTS.registerComponentType("tuning",
                    b -> b.persistent(BellTune.CODEC).networkSynchronized(BellTune.STREAM_CODEC));

    public static final List<String> METALS = List.of("copper", "iron", "gold", "amethyst", "prismarine");

    public static final DeferredHolder<SoundEvent, SoundEvent> RING_COPPER = sound("bell.copper");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_IRON = sound("bell.iron");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_GOLD = sound("bell.gold");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_AMETHYST = sound("bell.amethyst");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_PRISMARINE = sound("bell.prismarine");
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_SMALL = sound("bell.small");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUNE = sound("block.tuning_anvil.tune");

    public static final DeferredBlock<TuningAnvilBlock> TUNING_ANVIL = BLOCKS.registerBlock("tuning_anvil",
            TuningAnvilBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F).sound(SoundType.ANVIL).requiresCorrectToolForDrops().noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<BelfryChimeBlock> BELFRY_CHIME = BLOCKS.registerBlock("belfry_chime",
            BelfryChimeBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(1.5F).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<?> TUNING_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(TUNING_ANVIL);
    public static final DeferredItem<?> BELFRY_CHIME_ITEM = ITEMS.registerSimpleBlockItem(BELFRY_CHIME);

    public static final DeferredItem<HandbellItem> COPPER_HANDBELL = handbell("copper", 128, Rarity.COMMON,
            ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/copper")));
    public static final DeferredItem<HandbellItem> IRON_HANDBELL = handbell("iron", 250, Rarity.COMMON,
            ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/iron")));
    public static final DeferredItem<HandbellItem> GOLD_HANDBELL = handbell("gold", 64, Rarity.COMMON,
            ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/gold")));
    public static final DeferredItem<HandbellItem> AMETHYST_HANDBELL = handbell("amethyst", 200, Rarity.UNCOMMON,
            ItemTags.create(Identifier.fromNamespaceAndPath("c", "gems/amethyst")));
    public static final DeferredItem<HandbellItem> PRISMARINE_HANDBELL = handbell("prismarine", 300, Rarity.UNCOMMON,
            ItemTags.create(Identifier.fromNamespaceAndPath("c", "gems/prismarine")));

    public static final DeferredItem<BellAccessoryItem> CHIME_PIN = ITEMS.registerItem("chime_pin",
            BellAccessoryItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<BellAccessoryItem> BELL_ANKLET = ITEMS.registerItem("bell_anklet",
            BellAccessoryItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<TuningHammerItem> TUNING_HAMMER = ITEMS.registerItem("tuning_hammer",
            TuningHammerItem::new, p -> p.durability(160)
                    .repairable(ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/iron"))));

    /** Lowers every element resistance while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> DISCORD = EFFECTS.register("discord", DiscordEffect::new);
    /** Stops the mob picking a target while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> LULLED = EFFECTS.register("lulled",
            () -> new CampanileEffect(MobEffectCategory.NEUTRAL, 0xB9D7E8));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TuningAnvilBlockEntity>> TUNING_ANVIL_BE =
            BLOCK_ENTITIES.register("tuning_anvil", () -> new BlockEntityType<>(TuningAnvilBlockEntity::new, TUNING_ANVIL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BelfryChimeBlockEntity>> BELFRY_CHIME_BE =
            BLOCK_ENTITIES.register("belfry_chime", () -> new BlockEntityType<>(BelfryChimeBlockEntity::new, BELFRY_CHIME.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("campanile",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.campanile"))
                    .icon(() -> new ItemStack(IRON_HANDBELL.get()))
                    .displayItems((params, output) -> {
                        for (DeferredItem<?> item : List.of(COPPER_HANDBELL, IRON_HANDBELL, GOLD_HANDBELL, AMETHYST_HANDBELL,
                                PRISMARINE_HANDBELL, TUNING_HAMMER, TUNING_ANVIL_ITEM, BELFRY_CHIME_ITEM, CHIME_PIN, BELL_ANKLET)) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
        EFFECTS.register(modBus);
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
    }

    public static List<Item> handbells() {
        return List.of(COPPER_HANDBELL.get(), IRON_HANDBELL.get(), GOLD_HANDBELL.get(), AMETHYST_HANDBELL.get(),
                PRISMARINE_HANDBELL.get());
    }

    public static List<Item> tunables() {
        List<Item> out = new ArrayList<>(handbells());
        out.add(CHIME_PIN.get());
        out.add(BELL_ANKLET.get());
        return out;
    }

    private static DeferredItem<HandbellItem> handbell(String metal, int durability, Rarity rarity, TagKey<Item> repair) {
        return ITEMS.registerItem(metal + "_handbell", HandbellItem::new,
                p -> p.durability(durability).rarity(rarity).repairable(repair));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Campanile.id(name)));
    }

    /** MobEffect's constructor is protected. */
    public static final class CampanileEffect extends MobEffect {
        public CampanileEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private CampanileRegistry() {
    }
}
