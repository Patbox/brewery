package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.other.LegacyNbtHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class BrewBarrelPartBlockEntity extends BlockEntity {
    private BlockPos container;

    public BrewBarrelPartBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.BARREL_PART, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (this.container != null) {
            nbt.put("Container", LegacyNbtHelper.fromBlockPos(this.container));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (nbt.contains("Container")) {
            this.container = LegacyNbtHelper.toBlockPos(nbt.getCompoundOrEmpty("Container"));
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
