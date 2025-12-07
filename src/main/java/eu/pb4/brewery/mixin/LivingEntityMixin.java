package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.duck.LivingEntityExt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityExt {
    @Unique
    private AlcoholManager brewery$alcoholManager = new AlcoholManager((LivingEntity) (Object) this);

    public LivingEntityMixin(EntityType<?> type, Level world) {
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

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void brewery$writeData(ValueOutput view, CallbackInfo ci) {
        this.brewery$alcoholManager.writeData(view);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void brewery$readData(ValueInput view, CallbackInfo ci) {
        this.brewery$alcoholManager.readData(view);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void brewery$tick(CallbackInfo ci) {
        this.brewery$alcoholManager.tick();
    }


}
