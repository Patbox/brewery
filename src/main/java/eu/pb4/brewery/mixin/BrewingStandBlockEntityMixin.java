package eu.pb4.brewery.mixin;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.IngredientMixtureItem;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements Inventory {
    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private static void brewery$canCraft(BrewingRecipeRegistry brewingRecipeRegistry, DefaultedList<ItemStack> slots, CallbackInfoReturnable<Boolean> cir) {
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
                        var types = DrinkUtils.findTypes(ingredients, null, DrinkUtils.getHeatSource(stack), DrinkUtils.getContainer(stack));

                        if (types.isEmpty()) {
                            slots.set(i, stack.copyComponentsToNewStack(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                        } else {
                            double quality = Double.MIN_VALUE;
                            DrinkType match = null;

                            for (var type : types) {
                                if (type.requireDistillation()) {
                                    var q = type.cookingQualityMult().expression()
                                            .setVariable(ExpressionUtil.AGE_KEY, stack.getOrDefault(BrewComponents.COOKING_DATA, CookingData.DEFAULT).time() / 20)
                                            .evaluate();

                                    if (q > quality) {
                                        quality = q;
                                        match = type;
                                    }
                                }
                            }

                            if (match == null || quality < 0) {
                                slots.set(i, stack.copyComponentsToNewStack(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                            } else {
                                var drink = new ItemStack(BrewItems.DRINK_ITEM);
                                drink.set(BrewComponents.COOKING_DATA, stack.get(BrewComponents.COOKING_DATA));
                                drink.set(BrewComponents.BREW_DATA, new BrewData(Optional.of(BreweryInit.DRINK_TYPE_ID.get(match)), quality * 10, "", 1, 0));
                                slots.set(i, drink);
                            }
                        }
                    } else {
                        stack.apply(BrewComponents.BREW_DATA, BrewData.DEFAULT, BrewData::distillate);
                    }
                }
            }

            world.syncWorldEvent(1035, pos, 0);
            ci.cancel();
        }
    }

    @Inject(method = "isValid", at = @At("TAIL"), cancellable = true)
    private void brewery$isValid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (DrinkUtils.canBeDistillated(stack)) {
            cir.setReturnValue(this.getStack(slot).isEmpty());
        }
    }
}
