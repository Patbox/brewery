package eu.pb4.brewery.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public record BarrelMaterial(Identifier type, Text name, Block planks, Block stair, Block fence) {
    public static final BarrelMaterial EMPTY = new BarrelMaterial(new Identifier("void"), Text.empty(), Blocks.BEDROCK, Blocks.ANDESITE_STAIRS, Blocks.NETHER_BRICK_FENCE);
}
