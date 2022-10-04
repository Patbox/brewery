package eu.pb4.brewery.mixin;

import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public class StatusEffectInstanceMixin implements StatusEffectInstanceExt {
    @Unique
    private boolean brewery$locked;

    @Override
    public void brewery$setLocked(boolean value) {
        this.brewery$locked = value;
    }

    @Override
    public boolean brewery$isLocked() {
        return this.brewery$locked;
    }

    @Inject(method = "writeTypelessNbt", at = @At("HEAD"))
    private void brewery$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("brewery:locked", this.brewery$locked);
    }

    @Inject(method = "fromNbt(Lnet/minecraft/entity/effect/StatusEffect;Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/entity/effect/StatusEffectInstance;", at = @At("TAIL"))
    private static void brewery$writeNbt(StatusEffect type, NbtCompound nbt, CallbackInfoReturnable<StatusEffectInstance> cir) {
        ((StatusEffectInstanceExt) cir.getReturnValue()).brewery$setLocked(nbt.getBoolean("brewery:locked"));
    }
}
