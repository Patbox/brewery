package eu.pb4.brewery;

import com.mojang.serialization.MapCodec;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.ConsumptionEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class BreweryApi {
    private BreweryApi(){}
    public static void registerBarrelType(Identifier identifier, Component text, Block planks, Block stairs, Block fence) {
        BrewBlocks.registerBarrel(identifier, text, planks, stairs, fence);
    }

    public static void registerConsumptionEffect(Identifier identifier, MapCodec<ConsumptionEffect> codec) {
        ConsumptionEffect.register(identifier, codec);
    }
}
