package com.holybuckets.combat.config;

import com.holybuckets.combat.Constants;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModConfig {

    public static final String MSG_SYNC = Constants.MOD_ID + ":config_sync";

    private static ModConfig INSTANCE = new ModConfig();
    public final Set<Item> dashItems = new HashSet<>();

    private ModConfig() {}

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerLogin(ModConfig::onPlayerLogin);
    }

    public static ModConfig initOnServerStart(DodgeSwingConfig config) {
        return load(config.dashItems);
    }

    public static ModConfig load(Collection<String> itemIds) {
        ModConfig config = new ModConfig();
        for (String itemId : itemIds) {
            if (itemId == null || itemId.isBlank()) continue;
            Item item = HBUtil.ItemUtil.itemNameToItem(itemId.trim());
            if (item != null && item != Items.AIR)
                config.dashItems.add(item);
        }
        INSTANCE = config;
        return config;
    }

    public static ModConfig reset() {
        return load(List.of());
    }

    public boolean isDashItem(Item item) {
        return dashItems.contains(item);
    }

    public String serialize() {
        return dashItems.stream()
            .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
            .collect(Collectors.joining(","));
    }

    public static ModConfig deserialize(String content) {
        if (content == null || content.isBlank()) return reset();
        return load(List.of(content.split(",")));
    }

    private static void onPlayerLogin(PlayerLoginEvent event) {
        SimpleStringMessage.createAndFire(event.getPlayer(), MSG_SYNC, INSTANCE.serialize());
    }

}
