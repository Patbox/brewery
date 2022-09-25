package eu.pb4.brewery.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public record BarrelMaterial(String type, Block planks, Block stair, Block fence) {
    public static final BarrelMaterial EMPTY = new BarrelMaterial("void", Blocks.BEDROCK, Blocks.ANDESITE_STAIRS, Blocks.NETHER_BRICK_FENCE);
}
