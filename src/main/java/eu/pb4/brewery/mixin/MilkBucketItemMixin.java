package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MilkBucketItem.class)
public class MilkBucketItemMixin {
    @Unique
    private List<StatusEffectInstance> brewery$storedEffects;

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void brewery$drinkMilk(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        AlcoholManager.of(user).eat(stack);
    }

    @Inject(method = "finishUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z", shift = At.Shift.BEFORE))
    private void brewery$storeEffects(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        var list = new ArrayList<StatusEffectInstance>();

        for (var effect : user.getStatusEffects()) {
            if (((StatusEffectInstanceExt) effect).brewery$isLocked()) {
                list.add(effect);
            }
        }
        this.brewery$storedEffects = list;
    }

    @Inject(method = "finishUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z", shift = At.Shift.AFTER))
    private void brewery$restoreEffects(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        for (var effect : this.brewery$storedEffects) {
            user.addStatusEffect(effect);
        }
        this.brewery$storedEffects = null;
    }
}
