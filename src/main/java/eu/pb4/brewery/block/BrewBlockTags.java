package eu.pb4.brewery.block;

import eu.pb4.brewery.BreweryInit;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class BrewBlockTags {
    public static final TagKey<Block> IS_HEAT_SOURCE = TagKey.of(RegistryKeys.BLOCK, BreweryInit.id("is_heat_source"));
}
