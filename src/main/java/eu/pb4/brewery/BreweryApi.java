package eu.pb4.brewery;

import com.mojang.serialization.MapCodec;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.ConsumptionEffect;
import net.minecraft.block.Block;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BreweryApi {
    private BreweryApi(){}
    public static void registerBarrelType(Identifier identifier, Text text, Block planks, Block stairs, Block fence) {
        BrewBlocks.registerBarrel(identifier, text, planks, stairs, fence);
    }

    public static void registerConsumptionEffect(Identifier identifier, MapCodec<ConsumptionEffect> codec) {
        ConsumptionEffect.register(identifier, codec);
    }
}
