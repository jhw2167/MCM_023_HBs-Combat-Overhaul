package com.holybuckets.combat.mixin;

import com.holybuckets.combat.client.ClientDashRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FOV mixin for dashing. zoom it a little when the player dashes.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayerDash {

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void hbs_combat$dashFov(CallbackInfoReturnable<Float> cir) {
        float factor = ClientDashRenderer.getFovFactor((AbstractClientPlayer) (Object) this);
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValue() * factor);
    }
}
