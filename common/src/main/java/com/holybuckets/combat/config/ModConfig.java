package com.holybuckets.combat.config;

import com.holybuckets.combat.Constants;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModConfig {

    public static final String MSG_SYNC = Constants.MOD_ID + ":config_sync";

    public static final String SYNC_SEPARATOR = "|";

    private static ModConfig INSTANCE = new ModConfig();
    public final Set<Item> dashItems = new HashSet<>();
    public final Set<Block> slowBlocks = new HashSet<>();

    private ModConfig() {}

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnPlayerLogin(ModConfig::onPlayerLogin);
    }

    public static ModConfig initOnServerStart(DodgeSwingConfig config) {
        return load(config.dashItems, config.slowBlocks);
    }

    public static ModConfig load(Collection<String> itemIds, Collection<String> blockIds) {
        ModConfig config = new ModConfig();
        for (String itemId : itemIds) {
            if (itemId == null || itemId.isBlank()) continue;
            Item item = HBUtil.ItemUtil.itemNameToItem(itemId.trim());
            if (item != null && item != Items.AIR)
                config.dashItems.add(item);
        }
        for (String blockId : blockIds) {
            if (blockId == null || blockId.isBlank()) continue;
            Block block = HBUtil.BlockUtil.blockNameToBlock(blockId.trim());
            if (block != null && block != Blocks.AIR)
                config.slowBlocks.add(block);
        }
        INSTANCE = config;
        return config;
    }

    public static ModConfig reset() {
        return load(List.of(), List.of());
    }

    public boolean isDashItem(Item item) {
        return dashItems.contains(item);
    }

    public boolean isSlowBlock(Block block) {
        return slowBlocks.contains(block);
    }

    public String serialize() {
        String items = dashItems.stream()
            .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
            .collect(Collectors.joining(","));
        String blocks = slowBlocks.stream()
            .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
            .collect(Collectors.joining(","));
        return items + SYNC_SEPARATOR + blocks;
    }

    public static ModConfig deserialize(String content) {
        if (content == null || content.isBlank()) return reset();
        String[] parts = content.split("\\" + SYNC_SEPARATOR, -1);
        List<String> blocks = parts.length > 1 ? List.of(parts[1].split(",")) : List.of();
        return load(List.of(parts[0].split(",")), blocks);
    }

    private static void onPlayerLogin(PlayerLoginEvent event) {
        SimpleStringMessage.createAndFire(event.getPlayer(), MSG_SYNC, INSTANCE.serialize());
    }

}
