package eu.pb4.brewery.item.comp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record CookingData(double time, List<ItemStack> ingredients, Block heatSource, ItemStack container) {
    public static final Codec<CookingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("time", 0d).forGetter(CookingData::time),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(CookingData::ingredients),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("heat_source", Blocks.AIR).forGetter(CookingData::heatSource),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("container", Items.GLASS_BOTTLE.getDefaultInstance()).forGetter(CookingData::container)
    ).apply(instance, CookingData::new));
    public static final CookingData DEFAULT = new CookingData(0, List.of(), Blocks.AIR, Items.GLASS_BOTTLE.getDefaultInstance());
}
