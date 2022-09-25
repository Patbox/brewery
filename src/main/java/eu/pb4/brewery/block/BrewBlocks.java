package eu.pb4.brewery.block;

import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewBlocks {
    private static final AbstractBlock.TypedContextPredicate<EntityType<?>> BLOCK_SPAWNS = (state, world, pos, type) -> false;

    public static final List<BarrelMaterial> BARREL_MATERIALS = List.of(
            new BarrelMaterial("oak", Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_FENCE),
            new BarrelMaterial("birch", Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_FENCE),
            new BarrelMaterial("spruce", Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE),
            new BarrelMaterial("jungle", Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_FENCE),
            new BarrelMaterial("acacia", Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_FENCE),
            new BarrelMaterial("dark_oak", Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE),
            new BarrelMaterial("mangrove", Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_FENCE),
            new BarrelMaterial("warped", Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS, Blocks.WARPED_FENCE),
            new BarrelMaterial("crimson", Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_FENCE)

    );

    public static final Map<String, BarrelMaterial> BARREL_MATERIAL_MAP = new HashMap<>();

    public static final Map<String, BrewBarrelPartBlock> BARREL_PARTS = new HashMap<>();
    public static final Block BARREL_SPIGOT = register("barrel_spigot", new BrewSpigotBlock(AbstractBlock.Settings.of(Material.WOOD)));
    public static final Block CAULDRON = register("cauldron", new BrewCauldronBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON).dropsLike(Blocks.CAULDRON)));

    public static void register() {
        for (var mat : BARREL_MATERIALS) {
            BARREL_PARTS.put(mat.type(), register(mat.type() + "_barrel_part",
                    new BrewBarrelPartBlock(
                            AbstractBlock.Settings.copy(mat.planks())
                                    .allowsSpawning(BLOCK_SPAWNS).dropsNothing(),
                            mat)
            ));
        }
    }
    private static <T extends Block> T register(String path, T block) {
        return Registry.register(Registry.BLOCK, id(path), block);
    }

    static {
        for (var barrel : BARREL_MATERIALS) {
            BARREL_MATERIAL_MAP.put(barrel.type(), barrel);
        }
    }
}
