package com.holybuckets.combat.config;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

public class ModConfig {

    private static ModConfig INSTANCE;
    public Set<Item> dashItems = new HashSet<>();

    private ModConfig() {}


    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public static ModConfig initOnServerStart(DodgeSwingConfig config) {
        INSTANCE = new ModConfig();
        for(String itemId : config.dashItems) {
            Item item = HBUtil.ItemUtil.itemNameToItem(itemId);
            if (item!=Items.AIR)
                INSTANCE.dashItems.add(item);
        }
        return INSTANCE;
    }

}
