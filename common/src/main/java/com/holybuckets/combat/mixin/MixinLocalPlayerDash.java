package com.holybuckets.combat.mixin;

import com.holybuckets.combat.client.ClientDashManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerDash {

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void hbs_combat$suppressDashInput(CallbackInfo ci) {
        ClientDashManager.suppressInput((LocalPlayer) (Object) this);
    }
}
