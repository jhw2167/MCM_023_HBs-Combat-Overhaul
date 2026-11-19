package com.holybuckets.combat.mixin;

import com.holybuckets.combat.config.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRendererDash {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hbs_combat$hideOffhandWithDashItem(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                                    float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                                    MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (hand != InteractionHand.OFF_HAND) return;
        if (ModConfig.getInstance().isDashItem(player.getMainHandItem().getItem())) ci.cancel();
    }
}
