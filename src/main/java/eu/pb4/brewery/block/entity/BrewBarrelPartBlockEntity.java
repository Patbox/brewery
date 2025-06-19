package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.other.LegacyNbtHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class BrewBarrelPartBlockEntity extends BlockEntity {
    private BlockPos container;

    public BrewBarrelPartBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.BARREL_PART, pos, state);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (this.container != null) {
            view.put("Container", NbtCompound.CODEC, LegacyNbtHelper.fromBlockPos(this.container));
        }
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);
        this.container = view.read("Container", NbtCompound.CODEC).map(LegacyNbtHelper::toBlockPos).orElse(null);
    }

    public void setContainer(BlockPos pos) {
        this.container = pos;

        this.markDirty();
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null && this.container != null) {
            world.breakBlock(this.container, true);
        }
    }

    @Nullable
    public BlockPos getContainer() {
        return this.container;
    }

    public BrewBarrelSpigotBlockEntity getContainerBe() {
        return this.world.getBlockEntity(this.getContainer()) instanceof BrewBarrelSpigotBlockEntity be ? be : null;
    }
}
