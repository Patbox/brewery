package eu.pb4.brewery.block;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record BarrelMaterial(Identifier type, Component name, Block planks, Block stair, Block fence) {
    public static final BarrelMaterial EMPTY = new BarrelMaterial(Identifier.parse("void"), Component.empty(), Blocks.BEDROCK, Blocks.ANDESITE_STAIRS, Blocks.NETHER_BRICK_FENCE);
}
