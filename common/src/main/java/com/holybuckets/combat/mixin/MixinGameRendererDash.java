package com.holybuckets.combat.mixin;

import com.holybuckets.combat.client.ClientDashRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Give the user a little lean on their dash using bobHurt
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRendererDash {

    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void dashCameraTilt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ClientDashRenderer.applyCameraTilt(poseStack, partialTick);
    }
}
