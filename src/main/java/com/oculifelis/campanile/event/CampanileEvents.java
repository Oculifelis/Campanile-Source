package com.oculifelis.campanile.event;

import com.fundamentum.damage.Elements;
import com.oculifelis.campanile.CampanileRegistry;
import com.oculifelis.campanile.bell.Accessories;
import com.oculifelis.campanile.bell.Accessory;
import com.oculifelis.campanile.bell.Ring;
import com.oculifelis.campanile.bell.Tuning;
import com.oculifelis.campanile.bell.Tunings;
import com.oculifelis.campanile.item.BellAccessoryItem;
import com.oculifelis.campanile.item.HandbellItem;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/** Lulled stops mobs choosing targets; worn bells ring when their wearer is hurt. */
public final class CampanileEvents {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(CampanileEvents::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(CampanileEvents::onDamaged);
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && event.getEntity().hasEffect(CampanileRegistry.LULLED)) {
            event.setCanceled(true);
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Item item : CampanileRegistry.tunables()) {
            if (item instanceof BellAccessoryItem) {
                tryRing(level, player, item, event.getSource());
            }
        }
    }

    private static void tryRing(ServerLevel level, Player player, Item item, DamageSource source) {
        Optional<SlotResult> found = CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.findFirstCurio(item));
        if (found.isEmpty()) {
            return;
        }
        ItemStack stack = found.get().stack();
        Accessory acc = Accessories.of(item);
        Identifier tuningId = HandbellItem.tuning(stack);
        if (acc == null || tuningId == null || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        Tuning tuning = Tunings.get(tuningId);
        if (tuning == null || !triggered(acc, tuning, source)) {
            return;
        }
        Ring.ringStack(level, player.position().add(0.0D, 0.5D, 0.0D), player, acc.radius(), acc.strength(), stack,
                acc.sound(), acc.pitch());
        player.getCooldowns().addCooldown(stack, acc.cooldown());
    }

    public static boolean triggered(Accessory acc, Tuning tuning, DamageSource source) {
        if (acc.triggers().contains(Accessory.Trigger.MOB_HIT) && source.getEntity() instanceof Mob) {
            return true;
        }
        if (acc.triggers().contains(Accessory.Trigger.FALL) && source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }
        if (acc.triggers().contains(Accessory.Trigger.ELEMENT) && tuning.target() instanceof Tuning.ElementTarget et) {
            Elements.Element want = et.resolve();
            if (want != null) {
                for (Elements.Element e : Elements.of(source)) {
                    if (e.isA(want)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private CampanileEvents() {
    }
}
