package com.holybuckets.combat.mixin;

import com.holybuckets.combat.client.ClientDashManager;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerDash {

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void hbs_combat$dashPastLedges(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide) return;
        if (ClientDashManager.getActive(player) != null) cir.setReturnValue(movement);
    }
}
