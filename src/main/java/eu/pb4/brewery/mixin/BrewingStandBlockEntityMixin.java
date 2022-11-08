package eu.pb4.brewery.mixin;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.IngredientMixtureItem;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private static void brewery$canCraft(DefaultedList<ItemStack> slots, CallbackInfoReturnable<Boolean> cir) {
        if (slots.get(3).isEmpty()) {
            for (var i = 0; i < 3; i++) {
                if (DrinkUtils.canBeDistillated(slots.get(i))) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private static void brewery$craft(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {
        if (slots.get(3).isEmpty()) {
            for (var i = 0; i < 3; i++) {
                var stack = slots.get(i);
                if (DrinkUtils.canBeDistillated(stack)) {
                    if (stack.isOf(BrewItems.INGREDIENT_MIXTURE)) {
                        var ingredients = IngredientMixtureItem.getIngredients(stack);
                        var types = DrinkUtils.findTypes(ingredients, null);

                        if (types.isEmpty()) {
                            slots.set(i, new ItemStack(BrewItems.FAILED_DRINK_ITEM));
                        } else {
                            double quality = Double.MIN_VALUE;
                            DrinkType match = null;

                            for (var type : types) {
                                if (type.requireDistillation()) {
                                    var q = type.cookingQualityMult().expression()
                                            .setVariable(ExpressionUtil.AGE_KEY, stack.getNbt().getDouble(DrinkUtils.AGE_COOK_NBT) / 20d)
                                            .evaluate();

                                    if (q > quality) {
                                        quality = q;
                                        match = type;
                                    }
                                }
                            }

                            if (match == null || quality < 0) {
                                slots.set(i, new ItemStack(BrewItems.FAILED_DRINK_ITEM));
                            } else {
                                var drink = new ItemStack(BrewItems.DRINK_ITEM);
                                drink.setNbt(stack.getNbt());
                                drink.getOrCreateNbt().putString(DrinkUtils.TYPE_NBT, BreweryInit.DRINK_TYPE_ID.get(match).toString());

                                drink.getOrCreateNbt().putDouble(DrinkUtils.QUALITY_NBT, quality * 10);

                                drink.getOrCreateNbt().putInt(DrinkUtils.DISTILLATED_NBT, drink.getOrCreateNbt().getInt(DrinkUtils.DISTILLATED_NBT) + 1);
                                slots.set(i, drink);
                            }
                        }
                    } else {
                        stack.getOrCreateNbt().putInt(DrinkUtils.DISTILLATED_NBT, stack.getOrCreateNbt().getInt(DrinkUtils.DISTILLATED_NBT) + 1);
                    }
                }
            }

            world.syncWorldEvent(1035, pos, 0);
            ci.cancel();
        }
    }

    @Shadow
    public abstract ItemStack getStack(int slot);

    @Inject(method = "isValid", at = @At("TAIL"), cancellable = true)
    private void brewery$isValid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (DrinkUtils.canBeDistillated(stack)) {
            cir.setReturnValue(this.getStack(slot).isEmpty());
        }
    }
}
