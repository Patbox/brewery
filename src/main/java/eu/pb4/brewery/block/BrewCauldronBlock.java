package eu.pb4.brewery.block;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.entity.BrewCauldronBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BrewCauldronBlock extends BlockWithEntity implements PolymerBlock {
    public static final TagKey<Item> START_CAULDRON_COOKING = TagKey.of(RegistryKeys.ITEM, BreweryInit.id("start_cauldron_cooking"));

    protected BrewCauldronBlock(Settings settings) {
        super(settings);
    }

    public static boolean isValid(BlockPos pos, BlockState state, World world) {
        if (!state.isOf(Blocks.WATER_CAULDRON) && !state.isOf(BrewBlocks.CAULDRON)) {
            return false;
        }

        var blockBelow = world.getBlockState(pos.down());

        boolean logicCheck = true;
        if (blockBelow.getBlock() instanceof CampfireBlock) {
            logicCheck = blockBelow.get(CampfireBlock.LIT);
        }


        return blockBelow.isIn(BrewBlockTags.IS_HEAT_SOURCE) && logicCheck;
    }

    public static ActionResult tryReplaceCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) {
        if (stack.isIn(START_CAULDRON_COOKING) && isValid(pos, state, world)) {
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
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return Items.CAULDRON.getDefaultStack();
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

    public static ActionResult handleUseEvent(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        return tryReplaceCauldron(world.getBlockState(blockHitResult.getBlockPos()), world, blockHitResult.getBlockPos(), playerEntity, hand, playerEntity.getStackInHand(hand));
    }
}
