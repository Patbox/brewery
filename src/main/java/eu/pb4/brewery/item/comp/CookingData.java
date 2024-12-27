package eu.pb4.brewery.item.comp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.List;

public record CookingData(double time, List<ItemStack> ingredients, Block heatSource, ItemStack container) {
    public static final Codec<CookingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("time", 0d).forGetter(CookingData::time),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(CookingData::ingredients),
            Registries.BLOCK.getCodec().optionalFieldOf("heat_source", Blocks.AIR).forGetter(CookingData::heatSource),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("container", Items.GLASS_BOTTLE.getDefaultStack()).forGetter(CookingData::container)
    ).apply(instance, CookingData::new));
    public static final CookingData DEFAULT = new CookingData(0, List.of(), Blocks.AIR, Items.GLASS_BOTTLE.getDefaultStack());
}
