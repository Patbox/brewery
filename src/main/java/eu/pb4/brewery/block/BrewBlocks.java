package eu.pb4.brewery.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewBlocks {
    private static final AbstractBlock.TypedContextPredicate<EntityType<?>> BLOCK_SPAWNS = (state, world, pos, type) -> false;

    public static final List<BarrelMaterial> BARREL_MATERIALS = new ArrayList<>();
    public static final Map<Identifier, BarrelMaterial> BARREL_MATERIAL_MAP = new HashMap<>();
    public static final Map<Identifier, BrewBarrelPartBlock> BARREL_PARTS = new HashMap<>();
    public static final List<Pair<BarrelMaterial, BlockPattern>> BARREL_PATTERNS = new ArrayList<>();


    public static void registerBarrel(Identifier identifier, Text name, Block planks, Block stairsBlock, Block fenceBlock) {
        var material = new BarrelMaterial(identifier, name, planks, stairsBlock, fenceBlock);

        BARREL_MATERIALS.add(material);
        BARREL_MATERIAL_MAP.put(identifier, material);
        BARREL_PARTS.put(identifier, register(id( (identifier.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? "" : identifier.getNamespace() + "/") + identifier.getPath() + "_barrel_part"),
                new BrewBarrelPartBlock(
                        AbstractBlock.Settings.copy(planks)
                                .allowsSpawning(BLOCK_SPAWNS).dropsNothing(),
                        material)
        ));

        BARREL_PATTERNS.add(new Pair<>(material, BlockPatternBuilder.start()
                .aisle(new String[]{"/#/", "###", "/#/", "I I"})
                .aisle(new String[]{"/#/", "# #", "/#/", "   "})
                .aisle(new String[]{"/#/", "# #", "/#/", "   "})
                .aisle(new String[]{"/#/", "###", "/#/", "I I"})
                .where('#', (c) -> c.getBlockState().getBlock() == material.planks())
                .where('/', (c) -> c.getBlockState().getBlock() == material.stair())
                .where('I', (c) -> c.getBlockState().getBlock() == material.fence())
                .build()));
    }

    private static void registerBarrel(String identifier, Block planks, Block stairsBlock, Block fenceBlock) {
        registerBarrel(new Identifier(identifier), Text.translatable("container.brewery." + identifier + "_barrel"), planks, stairsBlock, fenceBlock);
    }

    public static final Block BARREL_SPIGOT = register("barrel_spigot", new BrewSpigotBlock(AbstractBlock.Settings.of(Material.WOOD)));
    public static final Block CAULDRON = register("cauldron", new BrewCauldronBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON).dropsLike(Blocks.CAULDRON)));

    public static void register() {
        registerBarrel("oak", Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_FENCE);
        registerBarrel("birch", Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_FENCE);
        registerBarrel("spruce", Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE);
        registerBarrel("jungle", Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_FENCE);
        registerBarrel("acacia", Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_FENCE);
        registerBarrel("dark_oak", Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE);
        registerBarrel("mangrove", Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_FENCE);
        registerBarrel("warped", Blocks.WARPED_PLANKS, Blocks.WARPED_STAIRS, Blocks.WARPED_FENCE);
        registerBarrel("crimson", Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_FENCE);
    }

    private static <T extends Block> T register(String path, T block) {
        return Registry.register(Registries.BLOCK, id(path), block);
    }

    private static <T extends Block> T register(Identifier path, T block) {
        return Registry.register(Registries.BLOCK, path, block);
    }
}
