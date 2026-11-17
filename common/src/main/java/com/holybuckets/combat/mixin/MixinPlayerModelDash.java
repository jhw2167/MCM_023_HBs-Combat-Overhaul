package com.holybuckets.combat.mixin;

import com.holybuckets.combat.client.ClientDashRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Animate the player model when dashing
@Mixin(PlayerModel.class)
public abstract class MixinPlayerModelDash {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void applyDashPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                          float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        ClientDashRenderer.applyPose((PlayerModel<?>) (Object) this, player, ageInTicks);
    }
}
