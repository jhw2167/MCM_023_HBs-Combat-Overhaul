package com.holybuckets.combat.config;

import com.holybuckets.combat.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;

import java.util.List;


@Config(Constants.MOD_ID)
public class DodgeSwingConfig {

    @Comment("List of Minecraft items that trigger a dash on use which shift clicking.")
    @NestedType(String.class)
    public List<String> dashItems = List.of( "minecraft:snowball");

    @Comment("List of blocks that slow down player dashes when standing on them.")
    @NestedType(String.class)
    public List<String> slowBlocks = List.of("minecraft:soul_sand", "minecraft:honey_block");

}