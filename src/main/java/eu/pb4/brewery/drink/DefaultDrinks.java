package eu.pb4.brewery.drink;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;


import java.util.List;
import java.util.function.BiConsumer;

public class DefaultDrinks {
    public static void create(BiConsumer<String, DrinkType> consumer) {
        consumer.accept("beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.beer"),
                        TextColor.fromRgb(0xffd333),
                        List.of(
                                DrinkType.BarrelInfo.of("oak", "quality", 60 * 5),
                                DrinkType.BarrelInfo.of("*", "quality * 0.8", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3.5, 2, 5),
                        "6 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 3", "quality / 5 - 1.2")
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 3.5, "oak", List.of())
                )
        );

        consumer.accept("dark_beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.dark_beer"),
                        TextColor.fromRgb(0x650013),
                        List.of(
                                DrinkType.BarrelInfo.of("dark_oak", "quality", 60 * 7)
                        ),
                        ExpressionUtil.defaultQuality(8, 3, 18),
                        "7 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 3", "quality / 5 - 1.2")
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 8, "dark_oak", List.of())
                )
        );

        consumer.accept("wheat_beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.wheat_beer"),
                        TextColor.fromRgb(0xffb84d),
                        List.of(
                                DrinkType.BarrelInfo.of("birch", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3, 2, 4),
                        "5 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 2", "quality / 5 - 1.4")
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 3.5, "birch", List.of())

                )
        );

        consumer.accept("wine", DrinkType.create(
                        Text.translatable("drinktype.brewery.wine"),
                        TextColor.fromRgb(0x722F37),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(30, 30, 120),
                        "8 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.REGENERATION, "(quality - 5) * 10", "1")
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(5, Items.SWEET_BERRIES)
                        ),
                        DrinkInfo.defaults(5, 30, "*", List.of())
                )
        );

        consumer.accept("mead", DrinkType.create(
                        Text.translatable("drinktype.brewery.mead"),
                        TextColor.fromRgb(0xffed91),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(4, 1, 2),
                        "9 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SPEED, "(quality - 3) * 10", "(quality - 2) / 3")
                        ),
                        ExpressionUtil.defaultBoiling(3, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(3, Items.HONEY_BOTTLE)
                        ),
                        DrinkInfo.defaults(3, 4, "*", List.of())

                )
        );

        consumer.accept("apple_mead", DrinkType.create(
                        Text.translatable("drinktype.brewery.apple_mead"),
                        TextColor.fromRgb(0xffdc91),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(4, 1, 2),
                        "11 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SPEED, "(quality - 3) * 12", "quality / 3")
                        ),
                        ExpressionUtil.defaultBoiling(3, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(3, Items.HONEY_BOTTLE),
                                DrinkType.BrewIngredient.of(Items.APPLE)
                        ),
                        DrinkInfo.defaults(3, 4, "*", List.of())
                )
        );

        consumer.accept("whiskey", DrinkType.create(
                        Text.translatable("drinktype.brewery.whiskey"),
                        TextColor.fromFormatting(Formatting.GOLD),
                        List.of(
                                DrinkType.BarrelInfo.of("dark_oak", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(18, 2, 40),
                        "26 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.ABSORPTION, "(quality - 4) * 12", "quality / 2 - 2")
                        ),
                        ExpressionUtil.defaultBoiling(10, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(10, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(10, 18, "dark_oak", List.of())
                )
        );

        consumer.accept("cider", DrinkType.create(
                        Text.translatable("drinktype.brewery.cider"),
                        TextColor.fromRgb(0xf86820),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3.5, 1),
                        "7 * (0.75 + quality / 40)",
                        List.of(
                        ),
                        ExpressionUtil.defaultBoiling(7, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(14, Items.APPLE)
                        ),
                        DrinkInfo.defaults(7, 3.5, "*", List.of())
                )
        );

        consumer.accept("apple_liquor", DrinkType.create(
                        Text.translatable("drinktype.brewery.apple_liquor"),
                        TextColor.fromFormatting(Formatting.RED),
                        List.of(
                                DrinkType.BarrelInfo.of("acacia", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(6, 2, 5),
                        "14 * (0.75 + quality / 40)",
                        List.of(

                        ),
                        ExpressionUtil.defaultBoiling(16, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(12, Items.APPLE)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4")
                        ),
                        DrinkInfo.defaults(16, 6, "acacia", List.of())

                )
        );

        consumer.accept("rum", DrinkType.create(
                        Text.translatable("drinktype.brewery.rum"),
                        TextColor.fromFormatting(Formatting.RED),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(14, 4, 100),
                        "30 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.FIRE_RESISTANCE, "(quality - 4) * 60", "0"),
                                ConsumptionEffect.of(StatusEffects.POISON, "(6 - quality) * 10", "0")
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(18, Items.SUGAR)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4")
                        ),
                        DrinkInfo.defaults(8, 14, "*", List.of())
                )
        );

        consumer.accept("vodka", DrinkType.create(
                        Text.translatable("drinktype.brewery.vodka"),
                        TextColor.fromRgb(0xEEEEFF),
                        List.of(),
                        "10",
                        "50",
                        List.of(),
                        ExpressionUtil.defaultQuality(20, 0) + " / 10",
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.POTATO)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4"),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4")
                        ),
                        DrinkInfo.defaults(20 / 60d * 1200, -1, "", List.of())
                )
        );

        consumer.accept("chorus_brew", DrinkType.create(
                        Text.translatable("drinktype.brewery.chorus_brew"),
                        TextColor.fromFormatting(Formatting.DARK_PURPLE),
                        List.of(
                                DrinkType.BarrelInfo.of("warped", "quality", 60 * 5),
                                DrinkType.BarrelInfo.of("*", "quality * 0.4", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(2, 2, 5),
                        "0",
                        List.of(
                                new ConsumptionEffect.TeleportRandom(WrappedExpression.createDefault("quality * 8"))
                        ),
                        ExpressionUtil.defaultBoiling(16, 1d / 10),
                        List.of(
                                DrinkType.BrewIngredient.of(16, Items.CHORUS_FRUIT)
                        ),
                        DrinkInfo.defaults(16, 2.5, "warped", List.of())
                )
        );

        consumer.accept("tea", DrinkType.create(
                        Text.translatable("drinktype.brewery.tea"),
                        TextColor.fromRgb(0x993D00),
                        List.of(),
                        "10",
                        "0",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 15", "0")
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                                DrinkType.BrewIngredient.of(1, Items.OAK_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );

        consumer.accept("green_tea", DrinkType.create(
                        Text.translatable("drinktype.brewery.green_tea"),
                        TextColor.fromRgb(0xD9F959),
                        List.of(),
                        "10",
                        "0",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 20", "0")
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                                DrinkType.BrewIngredient.of(1, Items.JUNGLE_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );
    }
}
