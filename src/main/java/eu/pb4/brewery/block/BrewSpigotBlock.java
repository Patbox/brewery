package eu.pb4.brewery.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.brewery.block.entity.BrewBarrelSpigotBlockEntity;
import eu.pb4.brewery.block.entity.BrewBlockEntities;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

public final class BrewSpigotBlock extends HorizontalDirectionalBlock implements PolymerBlock, EntityBlock, BlockWithElementHolder {
    private static final MapCodec<BrewSpigotBlock> CODEC = simpleCodec(BrewSpigotBlock::new);

    public BrewSpigotBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var facing = state.getValue(FACING);

        for (var pattern : BrewBlocks.BARREL_PATTERNS) {
            var result = pattern.getB().matches(world, pos.relative(facing)
                    .offset(facing.getStepZ(), 1, -facing.getStepX()), facing, Direction.UP);
            if (result != null) {
                var mat = pattern.getA();
                var partBlockType = BrewBlocks.BARREL_PARTS.get(mat.type());

                var be = world.getBlockEntity(pos);
                if (be instanceof BrewBarrelSpigotBlockEntity spigotBlock) {
                    spigotBlock.setBarrelType(pattern.getA());
                    for (var x = 0; x < result.getWidth(); x++) {
                        for (var y = 0; y < result.getHeight(); y++) {
                            for (var z = 0; z < result.getDepth(); z++) {
                                var blockPosition = result.getBlock(x, y, z);
                                var partState = partBlockType.getState(blockPosition.getState().getBlock(), x, y, facing);
                                if (partState != null) {
                                    world.setBlock(blockPosition.getPos(), partState, 2);
                                    spigotBlock.addPart(blockPosition.getPos());
                                    world.getBlockEntity(blockPosition.getPos(), BrewBlockEntities.BARREL_PART).get().setContainer(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace().getAxis() != Direction.Axis.Y) {
            for (var pattern : BrewBlocks.BARREL_PATTERNS) {
                var result = pattern.getB().matches(ctx.getLevel(), ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite())
                        .offset(-ctx.getClickedFace().getStepZ(), 1, ctx.getClickedFace().getStepX()), ctx.getClickedFace().getOpposite(), Direction.UP);
                if (result != null) {
                    return this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
                }
            }
        }

        return null;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,  BlockHitResult hit) {
        var blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof BrewBarrelSpigotBlockEntity barrelBlock && barrelBlock.stillValid(player)) {
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
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos,  moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewBarrelSpigotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return BrewBarrelSpigotBlockEntity::ticker;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        var holder = new ElementHolder();
        var m = new Matrix4f();

        for (var i = 0; i < 3; i++) {
            for (var o = 0; o < 2; o++) {
                var a = new ItemDisplayElement(PolymerUtils.createPlayerHead("ewogICJ0aW1lc3RhbXAiIDogMTY3ODU0MTAyNzMwNiwKICAicHJvZmlsZUlkIiA6ICI4MTc1MTc4NzQ2MjE0NmY2YjllOWM3MTYyMWRiODQwZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFwcGVhcmFuY2UiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmY3NTVjNDc1YTcwODcxMGQwNGRiZDk0YjNiZDI5MDZlMDFhODMwY2IwNGE2Y2QyYWExY2JhOTk2YmU3OGYyZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"));
                a.setItemDisplayContext(ItemDisplayContext.FIXED);
                a.setDisplayHeight(5f);
                a.setDisplayWidth(5f);
                a.setViewRange(0.5f);
                a.setTransformation(m.identity().rotateY(Mth.HALF_PI-initialBlockState.getValue(FACING).get2DDataValue() * Mth.HALF_PI)
                        .translate(-1.3f - i * 1.2f, 0, 0).scale(2)
                        .rotateX(o * Mth.HALF_PI)
                        .scale(0.15f, 3.001f, 2.001f));
                holder.addElement(a);
            }
        }
        return holder;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, state.getValue(FACING).getOpposite());
    }
}
