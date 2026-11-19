package com.holybuckets.combat.mixin;

import com.holybuckets.combat.DodgeSwingMain;
import com.holybuckets.combat.config.ModConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This is the best way to hook into ItemUse logic so we can
 * cancel the use of an item inteded to be throwable. Wish there was a better way but its fine
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackDash {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onItemUsed(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!player.isShiftKeyDown()) return;
        ItemStack stack = (ItemStack) (Object) this;
        if (!ModConfig.getInstance().isDashItem(stack.getItem())) return;

        if (level.isClientSide && DodgeSwingMain.DASH_MANAGER != null) {
            int invSlot = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            DodgeSwingMain.DASH_MANAGER.tryDash(player, invSlot);
        }
        cir.setReturnValue(InteractionResultHolder.consume(stack));
    }
}
