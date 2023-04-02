package eu.pb4.brewery.block;

import eu.pb4.brewery.BreweryInit;
import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;

public class BrewBlockTags {
    public static final TagKey<Block> IS_HEAT_SOURCE = TagKey.of(Registry.BLOCK_KEY, BreweryInit.id("is_heat_source"));
}
