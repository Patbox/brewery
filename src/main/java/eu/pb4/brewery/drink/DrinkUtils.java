package eu.pb4.brewery.drink;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DrinkUtils {
    @Nullable
    public static DrinkType getType(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            if (Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().isPresent()) {
                return BreweryInit.DRINK_TYPES.get(Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().get());
            }
        }

        return null;
    }

    @Nullable
    public static Identifier getTypeId(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            if (Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().isPresent()) {
                return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().get();
            }
        }

        return null;
    }

    public static double getQuality(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).quality();
        }

        return -1;
    }

    public static String getBarrelType(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).barrelType();
        }

        return "";
    }

    public static boolean getDistillationStatus(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            var type = getType(stack);
            if (type != null) {
                return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).distillations() >= type.distillationRuns();
            }
        }

        return false;
    }

    public static int getDistillationCount(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).distillations();
        }

        return 0;
    }

    public static Block getHeatSource(ItemStack stack) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).heatSource();
        }

        return Blocks.FIRE;
    }


    public static ItemStack getContainer(ItemStack stack) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).container();
        }

        return new ItemStack(Items.GLASS_BOTTLE);
    }

    public static boolean canBeDistillated(ItemStack stack) {
        var type = DrinkUtils.getType(stack);
        var already = DrinkUtils.getDistillationStatus(stack);
        return !already && ((type != null && type.requireDistillation()) || stack.isOf(BrewItems.INGREDIENT_MIXTURE));
    }

    public static double getAgeInTicks(ItemStack stack) {
        return getAgeInTicks(stack, Double.MIN_VALUE);
    }

    public static double getAgeInTicks(ItemStack stack, double defaultValue) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).age();
        }

        return defaultValue;
    }

    public static double getCookingAgeInTicks(ItemStack stack) {
        return getCookingInTicks(stack, Double.MIN_VALUE);
    }

    public static double getCookingInTicks(ItemStack stack, double defaultValue) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).time();
        }

        return defaultValue;
    }

    public static double getAgeInSeconds(ItemStack stack) {
        return getAgeInTicks(stack) / 20d;
    }

    public static double getCookingAgeInSeconds(ItemStack stack) {
        return getCookingAgeInTicks(stack) / 20d;
    }

    public static ItemStack createDrink(Identifier type, int age, double quality, int distillated, Block heatingSource) {
        return createDrink(type, age, quality, distillated, new CookingData(0, List.of(), heatingSource, ItemStack.EMPTY));
    }
    public static ItemStack createDrink(Identifier type, int age, double quality, int distillated, CookingData cookingData) {
        var stack = new ItemStack(BrewItems.DRINK_ITEM);

        stack.set(BrewComponents.BREW_DATA, new BrewData(Optional.of(type), quality, "", distillated, age));
        stack.set(BrewComponents.COOKING_DATA, cookingData);
        return stack;
    }

    public static List<DrinkType> findTypes(List<ItemStack> ingredients, Identifier barrelType, Block heatSource, ItemStack container) {
        if (ingredients.isEmpty()) {
            return List.of();
        }
        var list = new ArrayList<DrinkType>();
        base:
        for (var type : BreweryInit.DRINK_TYPES.values()) {
            if (((barrelType == null && type.barrelInfo().isEmpty()) || (barrelType != null && type.getBarrelInfo(barrelType) != null))
                    && !type.ingredients().isEmpty() && (type.heatSource().isEmpty() || type.heatSource().get().contains(Registries.BLOCK.getEntry(heatSource)))) {
                var ing = new ArrayList<ItemStack>(ingredients.size());
                for (var i : ingredients) {
                    ing.add(new ItemStack(i.getItem(), i.getCount()));
                }

                for (var ingredient : type.ingredients()) {
                    int count = ingredient.count();
                    for (var stack : ing) {
                        if (!stack.isEmpty() && ingredient.items().contains(stack.getItem())) {
                            count -= stack.getCount();
                            if (count < 0) {
                                continue base;
                            } else {
                                stack.setCount(0);
                            }
                        }
                    }

                    if (count != 0) {
                        continue base;
                    }
                }
                list.add(type);
            }
        }

        return list;
    }
}
