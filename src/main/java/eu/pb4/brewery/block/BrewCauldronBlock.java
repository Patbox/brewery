package eu.pb4.brewery.block;

import eu.pb4.brewery.block.entity.BrewBarrelSpigotBlockEntity;
import eu.pb4.brewery.block.entity.BrewCauldronBlockEntity;
import eu.pb4.polymer.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BrewCauldronBlock extends BlockWithEntity implements PolymerBlock {
    protected BrewCauldronBlock(Settings settings) {
        super(settings);
    }

    public static boolean isValid(BlockPos pos, BlockState state, World world) {
        if (!state.isOf(Blocks.WATER_CAULDRON) && !state.isOf(BrewBlocks.CAULDRON)) {
            return false;
        }

        var blockBelow = world.getBlockState(pos.down());
        return blockBelow.isIn(BlockTags.FIRE) || blockBelow.isIn(BlockTags.CAMPFIRES);
    }

    public static ActionResult tryReplaceCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) {
        if (stack.isOf(Items.STICK) && isValid(pos, state, world)) {
            var ingredients = world.getEntitiesByClass(
                    ItemEntity.class,
                    new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.2, pos.getZ() + 1),
                    (x) -> true
            );


            if (!ingredients.isEmpty()) {
                world.setBlockState(pos, BrewBlocks.CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, state.get(LeveledCauldronBlock.LEVEL)));

                var be = world.getBlockEntity(pos);
                if (be instanceof BrewCauldronBlockEntity cauldron) {
                    cauldron.addIngredients(ingredients);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LeveledCauldronBlock.LEVEL);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var be = world.getBlockEntity(pos);
        if (be instanceof BrewCauldronBlockEntity blockEntity) {
            return blockEntity.onUse(player, hand) ? ActionResult.SUCCESS : ActionResult.FAIL;
        }


        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.WATER_CAULDRON;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, state.get(LeveledCauldronBlock.LEVEL));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BrewCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return BrewCauldronBlockEntity::ticker;
    }
}
