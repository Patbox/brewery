package eu.pb4.brewery.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.entity.BrewCauldronBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

public class BrewCauldronBlock extends BaseEntityBlock implements PolymerBlock {
    public static final TagKey<Item> START_CAULDRON_COOKING = TagKey.create(Registries.ITEM, BreweryInit.id("start_cauldron_cooking"));
    private static final MapCodec<BrewCauldronBlock> CODEC = simpleCodec(BrewCauldronBlock::new);
    protected BrewCauldronBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static boolean isValid(BlockPos pos, BlockState state, Level world) {
        if (!state.is(Blocks.WATER_CAULDRON) && !state.is(BrewBlocks.CAULDRON)) {
            return false;
        }

        var blockBelow = world.getBlockState(pos.below());

        boolean logicCheck = true;
        if (blockBelow.getBlock() instanceof CampfireBlock) {
            logicCheck = blockBelow.getValue(CampfireBlock.LIT);
        }


        return blockBelow.is(BrewBlockTags.IS_HEAT_SOURCE) && logicCheck;
    }

    public static InteractionResult tryReplaceCauldron(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(START_CAULDRON_COOKING) && isValid(pos, state, world)) {
            var ingredients = world.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.2, pos.getZ() + 1),
                    (x) -> true
            );


            if (!ingredients.isEmpty()) {
                world.setBlockAndUpdate(pos, BrewBlocks.CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL)));

                var be = world.getBlockEntity(pos);
                if (be instanceof BrewCauldronBlockEntity cauldron) {
                    cauldron.addIngredients(ingredients);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return Items.CAULDRON.getDefaultInstance();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LayeredCauldronBlock.LEVEL);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var be = world.getBlockEntity(pos);
        if (be instanceof BrewCauldronBlockEntity blockEntity) {
            return blockEntity.onUse(player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }


        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return BrewCauldronBlockEntity::ticker;
    }

    public static InteractionResult handleUseEvent(Player playerEntity, Level world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (world instanceof ServerLevel) {
            return tryReplaceCauldron(world.getBlockState(blockHitResult.getBlockPos()), world, blockHitResult.getBlockPos(), playerEntity, hand, playerEntity.getItemInHand(hand));
        }
        return InteractionResult.PASS;
    }
}
