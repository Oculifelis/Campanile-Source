package com.oculifelis.campanile.client;

import com.oculifelis.campanile.Campanile;
import com.oculifelis.campanile.CampanileRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Third person: a handbell is held out in front, the arm raised 75 degrees, and the bell hangs straight down from the
 * fist. The held model's thirdperson display (rotation 15 about X) cancels the arm's tilt so the bell stays upright.
 */
@EventBusSubscriber(modid = Campanile.MOD_ID, value = Dist.CLIENT)
public final class HandbellPose {
    /** How far the arm is raised from hanging, radians (75 degrees). Must match 90 minus the model's display X rotation. */
    static final float RAISE = (float) Math.toRadians(75.0);

    public static final EnumProxy<HumanoidModel.ArmPose> HELD_OUT = new EnumProxy<>(HumanoidModel.ArmPose.class, false, false,
            (IArmPoseTransformer) HandbellPose::heldOut);

    private static void heldOut(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart a = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        a.xRot = -RAISE;
        a.yRot = arm == HumanoidArm.RIGHT ? -0.1F : 0.1F;
        a.zRot = 0.0F;
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HELD_OUT.getValue();
            }
        }, CampanileRegistry.handbells().toArray(new net.minecraft.world.item.Item[0]));
    }

    private HandbellPose() {}
}
