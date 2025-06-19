package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.duck.LivingEntityExt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityExt {
    @Unique
    private AlcoholManager brewery$alcoholManager = new AlcoholManager((LivingEntity) (Object) this);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public AlcoholManager brewery$getAlcoholManager() {
        return this.brewery$alcoholManager;
    }

    @Override
    public void brewery$setAlcoholManager(AlcoholManager manager) {
        this.brewery$alcoholManager = manager;
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void brewery$writeData(WriteView view, CallbackInfo ci) {
        this.brewery$alcoholManager.writeData(view);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void brewery$readData(ReadView view, CallbackInfo ci) {
        this.brewery$alcoholManager.readData(view);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void brewery$tick(CallbackInfo ci) {
        this.brewery$alcoholManager.tick();
    }


}
