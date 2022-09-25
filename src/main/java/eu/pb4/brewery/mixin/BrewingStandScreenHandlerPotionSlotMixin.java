package eu.pb4.brewery.mixin;

import eu.pb4.brewery.drink.DrinkUtils;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/screen/BrewingStandScreenHandler$PotionSlot")
public class BrewingStandScreenHandlerPotionSlotMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void brewery$matches(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (DrinkUtils.canBeDistillated(stack)) {
            cir.setReturnValue(true);
        }
    }
}
