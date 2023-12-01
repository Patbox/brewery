package eu.pb4.brewery.block;

import eu.pb4.brewery.block.entity.BrewBarrelSpigotBlockEntity;
import eu.pb4.brewery.block.entity.BrewBarrelPartBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public final class BrewBarrelPartBlock extends Block implements PolymerBlock, BlockEntityProvider, InventoryProvider {
    public final BarrelMaterial barrelMaterial;
    public static final Property<Shape> SHAPE = EnumProperty.of("shape", Shape.class);

    public BrewBarrelPartBlock(Settings settings, BarrelMaterial material) {
        super(settings);
        this.barrelMaterial = material;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return state.get(SHAPE).state.apply(this.barrelMaterial).getBlock();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return state.get(SHAPE).state.apply(this.barrelMaterial);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND) {
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof BrewBarrelPartBlockEntity redirect && redirect.getContainer() != null) {
                blockEntity = world.getBlockEntity(redirect.getContainer());
            }


            if (blockEntity instanceof BrewBarrelSpigotBlockEntity barrelBlock && !(player.squaredDistanceTo((double)pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D)) {
                barrelBlock.openGui((ServerPlayerEntity) player);
                world.playSound(null,
                        barrelBlock.getPos().getX() + 0.5,
                        barrelBlock.getPos().getY() + 0.5,
                        barrelBlock.getPos().getZ() + 0.5, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var target = state.get(SHAPE).state.apply(this.barrelMaterial);
        return target.getBlock().getPickStack(world, pos, target);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof BrewBarrelPartBlockEntity redirect && redirect.getContainer() != null) {
                world.breakBlock(redirect.getContainer(), true);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return state.get(SHAPE).state.apply(this.barrelMaterial).getDroppedStacks(builder);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BrewBarrelPartBlockEntity(pos, state);
    }

    @Override
    public MutableText getName() {
        return this.barrelMaterial.name().copy();
    }

    public BlockState getState(Block block, int x, int y, Direction direction) {
        if (block == this.barrelMaterial.planks()) {
            return this.getDefaultState().with(SHAPE, Shape.SIDE_BLOCK);
        } else if (block == this.barrelMaterial.fence()) {
            return this.getDefaultState().with(SHAPE, Shape.LEG);
        } else if (block == this.barrelMaterial.stair()) {
            var ohgod
                    = Shape.STAIR_SHAPES.get(direction.rotateYClockwise().getAxis());

            var pair1 = direction.getDirection() == Direction.AxisDirection.NEGATIVE
                    ? x == 0 ? ohgod.getLeft() : ohgod.getRight()
                    : x == 0 ? ohgod.getRight() : ohgod.getLeft();

            return this.getDefaultState().with(SHAPE, y == 0 ? pair1.getLeft() : pair1.getRight());
        }


        return null;
    }

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {

        if(world.getBlockEntity(pos) instanceof BrewBarrelPartBlockEntity be) {
            return be.getContainerBe();
        }

        return null;
    }

    public enum Shape implements StringIdentifiable {
        SIDE_BLOCK(x -> x.planks().getDefaultState()),
        LEG(x -> x.fence().getDefaultState()),
        WEST_BOTTOM(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.EAST).with(StairsBlock.HALF, BlockHalf.TOP)),
        EAST_BOTTOM(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.WEST).with(StairsBlock.HALF, BlockHalf.TOP)),
        NORTH_BOTTOM(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.SOUTH).with(StairsBlock.HALF, BlockHalf.TOP)),
        SOUTH_BOTTOM(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.TOP)),
        WEST_TOP(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.EAST).with(StairsBlock.HALF, BlockHalf.BOTTOM)),
        EAST_TOP(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.WEST).with(StairsBlock.HALF, BlockHalf.BOTTOM)),
        NORTH_TOP(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.SOUTH).with(StairsBlock.HALF, BlockHalf.BOTTOM)),
        SOUTH_TOP(x -> x.stair().getDefaultState().with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.BOTTOM)),
        ;


        public static final Map<Direction.Axis, Pair<Pair<Shape, Shape>, Pair<Shape, Shape>>> STAIR_SHAPES = new HashMap<>();

        static {
            var map0 = new HashMap<Direction, Pair<Shape, Shape>>();

            for (var value : Shape.values()) {
                var state = value.state.apply(BarrelMaterial.EMPTY);
                if (state.getBlock() == BarrelMaterial.EMPTY.stair()) {
                    var pair = map0.computeIfAbsent(state.get(StairsBlock.FACING), (x) -> new Pair<>(null, null));

                    if (state.get(StairsBlock.HALF) == BlockHalf.TOP) {
                        pair.setRight(value);
                    } else {
                        pair.setLeft(value);
                    }
                }
            }

            for (var entry : map0.entrySet()) {
                var state = entry.getValue().getLeft().state.apply(BarrelMaterial.EMPTY).get(StairsBlock.FACING);
                var pair = STAIR_SHAPES.computeIfAbsent(state.getAxis(), (x) -> new Pair<>(null,null));

                if (state.getAxis() == Direction.Axis.Z) {
                    if (state.getDirection() == Direction.AxisDirection.NEGATIVE) {
                        pair.setLeft(entry.getValue());
                    } else {
                        pair.setRight(entry.getValue());
                    }
                } else {
                    if (state.getDirection() == Direction.AxisDirection.POSITIVE) {
                        pair.setLeft(entry.getValue());
                    } else {
                        pair.setRight(entry.getValue());
                    }
                }

            }
        }

        public final Function<BarrelMaterial, BlockState> state;
        private String name;

        Shape(Function<BarrelMaterial, BlockState> state) {
            this.state = state;
        }

        @Override
        public String asString() {
            if (this.name == null) {
                this.name = this.name().toLowerCase(Locale.ROOT);
            }
            return this.name;
        }
    }
}
