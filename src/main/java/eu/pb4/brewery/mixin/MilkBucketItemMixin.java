package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.AlcoholManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MilkBucketItem.class)
public class MilkBucketItemMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void brewery$drinkMilk(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        AlcoholManager.of(user).eat(stack);
    }
}
