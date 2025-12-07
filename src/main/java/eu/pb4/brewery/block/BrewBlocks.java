package eu.pb4.brewery.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewBlocks {
    private static final BlockBehaviour.StateArgumentPredicate<EntityType<?>> BLOCK_SPAWNS = (state, world, pos, type) -> false;

    public static final List<BarrelMaterial> BARREL_MATERIALS = new ArrayList<>();
    public static final Map<Identifier, BarrelMaterial> BARREL_MATERIAL_MAP = new HashMap<>();
    public static final Map<Identifier, BrewBarrelPartBlock> BARREL_PARTS = new HashMap<>();
    public static final List<Tuple<BarrelMaterial, BlockPattern>> BARREL_PATTERNS = new ArrayList<>();


    public static void registerBarrel(Identifier identifier, Component name, Block planks, Block stairsBlock, Block fenceBlock) {
        var material = new BarrelMaterial(identifier, name, planks, stairsBlock, fenceBlock);
        var blockId = (identifier.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? "" : identifier.getNamespace() + "/") + identifier.getPath() + "_barrel_part";
        BARREL_MATERIALS.add(material);
        BARREL_MATERIAL_MAP.put(identifier, material);
        BARREL_PARTS.put(identifier, register(blockId,
                BlockBehaviour.Properties.ofFullCopy(planks),
            (s) -> new BrewBarrelPartBlock(s.isValidSpawn(BLOCK_SPAWNS).noLootTable(), material)));

        BARREL_PATTERNS.add(new Tuple<>(material, BlockPatternBuilder.start()
                .aisle("/#/", "###", "/#/", "I I")
                .aisle("/#/", "# #", "/#/", "   ")
                .aisle("/#/", "# #", "/#/", "   ")
                .aisle("/#/", "###", "/#/", "I I")
                .where('#', (c) -> c.getState().getBlock() == material.planks())
                .where('/', (c) -> c.getState().getBlock() == material.stair())
                .where('I', (c) -> c.getState().getBlock() == material.fence())
                .build()));
    }

    private static void registerBarrel(String identifier, Block planks, Block stairsBlock, Block fenceBlock) {
        registerBarrel(Identifier.parse(identifier), Component.translatable("container.brewery." + identifier + "_barrel"), planks, stairsBlock, fenceBlock);
    }

    public static final Block BARREL_SPIGOT = register("barrel_spigot", BlockBehaviour.Properties.of().instabreak().noCollision().isRedstoneConductor(Blocks::never), BrewSpigotBlock::new);
    public static final Block CAULDRON = register("cauldron", BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).overrideLootTable(Blocks.CAULDRON.getLootTable()), BrewCauldronBlock::new);

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
        registerBarrel("cherry", Blocks.CHERRY_PLANKS, Blocks.CHERRY_STAIRS, Blocks.CHERRY_FENCE);
        registerBarrel("bamboo", Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_FENCE);
        registerBarrel("pale_oak", Blocks.PALE_OAK_PLANKS, Blocks.PALE_OAK_STAIRS, Blocks.PALE_OAK_FENCE);
    }

    private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> block) {
        return register(path, BlockBehaviour.Properties.of(), block);
    }
    private static <T extends Block> T register(String path, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> block) {
        return Registry.register(BuiltInRegistries.BLOCK, id(path), block.apply(settings.setId(ResourceKey.create(Registries.BLOCK, id(path)))));
    }
}
