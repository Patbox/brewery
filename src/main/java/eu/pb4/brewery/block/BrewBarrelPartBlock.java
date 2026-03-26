package eu.pb4.brewery.block;

import eu.pb4.brewery.block.entity.BrewBarrelSpigotBlockEntity;
import eu.pb4.brewery.block.entity.BrewBarrelPartBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.*;
import java.util.function.Function;

public final class BrewBarrelPartBlock extends Block implements PolymerBlock, EntityBlock, WorldlyContainerHolder {
    public final BarrelMaterial barrelMaterial;
    public static final Property<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public BrewBarrelPartBlock(Properties settings, BarrelMaterial material) {
        super(settings);
        this.barrelMaterial = material;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return state.getValue(SHAPE).state.apply(this.barrelMaterial);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof BrewBarrelPartBlockEntity redirect && redirect.getContainer() != null) {
            blockEntity = world.getBlockEntity(redirect.getContainer());
        }


        if (blockEntity instanceof BrewBarrelSpigotBlockEntity barrelBlock && !(player.distanceToSqr((double)pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D)) {
            barrelBlock.openGui((ServerPlayer) player);
            world.playSound(null,
                    barrelBlock.getBlockPos().getX() + 0.5,
                    barrelBlock.getBlockPos().getY() + 0.5,
                    barrelBlock.getBlockPos().getZ() + 0.5, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var target = state.getValue(SHAPE).state.apply(this.barrelMaterial);
        return target.getCloneItemStack(world, pos, includeData);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return state.getValue(SHAPE).state.apply(this.barrelMaterial).getDrops(builder);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewBarrelPartBlockEntity(pos, state);
    }

    @Override
    public MutableComponent getName() {
        return this.barrelMaterial.name().copy();
    }

    public BlockState getState(Block block, int x, int y, Direction direction) {
        if (block == this.barrelMaterial.planks()) {
            return this.defaultBlockState().setValue(SHAPE, Shape.SIDE_BLOCK);
        } else if (block == this.barrelMaterial.fence()) {
            return this.defaultBlockState().setValue(SHAPE, Shape.LEG);
        } else if (block == this.barrelMaterial.stair()) {
            var ohgod
                    = Shape.STAIR_SHAPES.get(direction.getClockWise().getAxis());

            var pair1 = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                    ? x == 0 ? ohgod.getA() : ohgod.getB()
                    : x == 0 ? ohgod.getB() : ohgod.getA();

            return this.defaultBlockState().setValue(SHAPE, y == 0 ? pair1.getA() : pair1.getB());
        }


        return null;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor world, BlockPos pos) {

        if(world.getBlockEntity(pos) instanceof BrewBarrelPartBlockEntity be) {
            return be.getContainerBe();
        }

        return null;
    }

    public enum Shape implements StringRepresentable {
        SIDE_BLOCK(x -> x.planks().defaultBlockState()),
        LEG(x -> x.fence().defaultBlockState()),
        WEST_BOTTOM(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP)),
        EAST_BOTTOM(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP)),
        NORTH_BOTTOM(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP)),
        SOUTH_BOTTOM(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP)),
        WEST_TOP(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.BOTTOM)),
        EAST_TOP(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.BOTTOM)),
        NORTH_TOP(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.BOTTOM)),
        SOUTH_TOP(x -> x.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.BOTTOM)),
        ;


        public static final Map<Direction.Axis, Tuple<Tuple<Shape, Shape>, Tuple<Shape, Shape>>> STAIR_SHAPES = new HashMap<>();

        static {
            var map0 = new HashMap<Direction, Tuple<Shape, Shape>>();

            for (var value : Shape.values()) {
                var state = value.state.apply(BarrelMaterial.EMPTY);
                if (state.getBlock() == BarrelMaterial.EMPTY.stair()) {
                    var pair = map0.computeIfAbsent(state.getValue(StairBlock.FACING), (x) -> new Tuple<>(null, null));

                    if (state.getValue(StairBlock.HALF) == Half.TOP) {
                        pair.setB(value);
                    } else {
                        pair.setA(value);
                    }
                }
            }

            for (var entry : map0.entrySet()) {
                var state = entry.getValue().getA().state.apply(BarrelMaterial.EMPTY).getValue(StairBlock.FACING);
                var pair = STAIR_SHAPES.computeIfAbsent(state.getAxis(), (x) -> new Tuple<>(null,null));

                if (state.getAxis() == Direction.Axis.Z) {
                    if (state.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                        pair.setA(entry.getValue());
                    } else {
                        pair.setB(entry.getValue());
                    }
                } else {
                    if (state.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                        pair.setA(entry.getValue());
                    } else {
                        pair.setB(entry.getValue());
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
        public String getSerializedName() {
            if (this.name == null) {
                this.name = this.name().toLowerCase(Locale.ROOT);
            }
            return this.name;
        }
    }
}
