package com.holybuckets.combat.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IDashManager {

    public void tryDash(Player p, int invSlot);
}
