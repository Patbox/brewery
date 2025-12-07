package eu.pb4.brewery.block;

import eu.pb4.brewery.BreweryInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class BrewBlockTags {
    public static final TagKey<Block> IS_HEAT_SOURCE = TagKey.create(Registries.BLOCK, BreweryInit.id("is_heat_source"));
}
