package eu.pb4.brewery.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class BrewBarrelPartBlockEntity extends BlockEntity {
    private BlockPos container;

    public BrewBarrelPartBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.BARREL_PART, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.container != null) {
            nbt.put("Container", NbtHelper.fromBlockPos(this.container));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("Container", NbtElement.COMPOUND_TYPE)) {
            this.container = NbtHelper.toBlockPos(nbt.getCompound("Container"));
        }
    }

    public void setContainer(BlockPos pos) {
        this.container = pos;

        this.markDirty();
    }


    @Nullable
    public BlockPos getContainer() {
        return this.container;
    }

    public BrewBarrelSpigotBlockEntity getContainerBe() {
        return this.world.getBlockEntity(this.getContainer()) instanceof BrewBarrelSpigotBlockEntity be ? be : null;
    }
}
