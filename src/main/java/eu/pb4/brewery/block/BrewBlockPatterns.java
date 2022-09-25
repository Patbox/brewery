package eu.pb4.brewery.block;

import eu.pb4.brewery.block.BarrelMaterial;
import eu.pb4.brewery.block.BrewBlocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class BrewBlockPatterns {
    public static final List<Pair<BarrelMaterial, BlockPattern>> BARREL_PATTERNS = new ArrayList<>();



    public static void register() {
        for (var mat : BrewBlocks.BARREL_MATERIALS) {
            BARREL_PATTERNS.add(new Pair<>(mat, createBarrel(mat)));
        }
    }

    public static BlockPattern createBarrel(BarrelMaterial material) {
        return BlockPatternBuilder.start()
                .aisle(new String[]{"/#/", "###", "/#/", "I I"})
                .aisle(new String[]{"/#/", "# #", "/#/", "   "})
                .aisle(new String[]{"/#/", "# #", "/#/", "   "})
                .aisle(new String[]{"/#/", "###", "/#/", "I I"})
                .where('#', (c) -> c.getBlockState().getBlock() == material.planks())
                .where('/', (c) -> c.getBlockState().getBlock() == material.stair())
                .where('I', (c) -> c.getBlockState().getBlock() == material.fence())
                .build();
    }
}
