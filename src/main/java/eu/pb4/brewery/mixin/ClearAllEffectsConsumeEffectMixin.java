package eu.pb4.brewery.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import eu.pb4.brewery.duck.MobInstanceExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

@Mixin(ClearAllStatusEffectsConsumeEffect.class)
public class ClearAllEffectsConsumeEffectMixin {

    @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z", shift = At.Shift.BEFORE))
    private void brewery$storeEffects(Level world, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir,
                                      @Share("storedEffects") LocalRef<List<MobEffectInstance>> storedEffects) {
        var list = new ArrayList<MobEffectInstance>();

        for (var effect : user.getActiveEffects()) {
            if (((MobInstanceExt) effect).brewery$isLocked()) {
                list.add(effect);
            }
        }
        storedEffects.set(list);
    }

    @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z", shift = At.Shift.AFTER))
    private void brewery$restoreEffects(Level world, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir,
                                        @Share("storedEffects") LocalRef<List<MobEffectInstance>> storedEffects) {
        for (var effect : storedEffects.get()) {
            user.addEffect(effect);
        }
    }
}
