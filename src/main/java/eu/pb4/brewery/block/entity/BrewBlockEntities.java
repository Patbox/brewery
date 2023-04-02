package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.polymer.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewBlockEntities {
    public static BlockEntityType<BrewBarrelSpigotBlockEntity> BARREL_SPIGOT;
    public static BlockEntityType<BrewBarrelPartBlockEntity> BARREL_PART;
    public static BlockEntityType<BrewCauldronBlockEntity> CAULDRON;

    public static void register() {
        var redirect = FabricBlockEntityTypeBuilder.create(BrewBarrelPartBlockEntity::new);
        for (var block : BrewBlocks.BARREL_PARTS.values()) {
            redirect.addBlock(block);
        }

        BARREL_SPIGOT = register("barrel_spigot", FabricBlockEntityTypeBuilder.create(BrewBarrelSpigotBlockEntity::new, BrewBlocks.BARREL_SPIGOT).build());
        BARREL_PART = register("barrel_part", redirect.build());

        CAULDRON = register("cauldron", FabricBlockEntityTypeBuilder.create(BrewCauldronBlockEntity::new, BrewBlocks.CAULDRON).build());
    }


    private static <T extends BlockEntity> BlockEntityType<T> register(String path, BlockEntityType<T> block) {
        Registry.register(Registry.BLOCK_ENTITY_TYPE, id(path), block);
        PolymerBlockUtils.registerBlockEntity(block);
        return block;
    }
}
