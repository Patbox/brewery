package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.duck.LivingEntityExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements LivingEntityExt {
    @Unique
    private AlcoholManager brewery$alcoholManager = new AlcoholManager((LivingEntity) (Object) this);

    @Override
    public AlcoholManager brewery$getAlcoholManager() {
        return this.brewery$alcoholManager;
    }

    @Override
    public void brewery$setAlcoholManager(AlcoholManager manager) {
        this.brewery$alcoholManager = manager;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void brewery$writeData(NbtCompound nbt, CallbackInfo ci) {
        this.brewery$alcoholManager.writeNbt(nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void brewery$readData(NbtCompound nbt, CallbackInfo ci) {
        this.brewery$alcoholManager.readNbt(nbt);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void brewery$tick(CallbackInfo ci) {
        this.brewery$alcoholManager.tick();
    }

    @Inject(method = "eatFood", at = @At("TAIL"))
    private void brewery$eat(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        this.brewery$alcoholManager.eat(stack);
    }
}
