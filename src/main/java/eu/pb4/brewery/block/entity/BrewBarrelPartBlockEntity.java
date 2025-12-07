package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.other.LegacyNbtHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class BrewBarrelPartBlockEntity extends BlockEntity {
    private BlockPos container;

    public BrewBarrelPartBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.BARREL_PART, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (this.container != null) {
            view.store("Container", CompoundTag.CODEC, LegacyNbtHelper.fromBlockPos(this.container));
        }
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.container = view.read("Container", CompoundTag.CODEC).map(LegacyNbtHelper::toBlockPos).orElse(null);
    }

    public void setContainer(BlockPos pos) {
        this.container = pos;

        this.setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null && this.container != null) {
            level.destroyBlock(this.container, true);
        }
    }

    @Nullable
    public BlockPos getContainer() {
        return this.container;
    }

    public BrewBarrelSpigotBlockEntity getContainerBe() {
        return this.level.getBlockEntity(this.getContainer()) instanceof BrewBarrelSpigotBlockEntity be ? be : null;
    }
}
