package eu.pb4.brewery.drink;

import com.mojang.datafixers.util.Pair;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.other.FloatSelector;
import eu.pb4.brewery.other.WrappedText;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;


import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DefaultDefinitions {
    public static void createBrews(BiConsumer<String, Function<Identifier, DrinkType>> consumer) {
        consumer.accept("beer", (id) -> DrinkType.create(
                        id,
                        TextColor.fromRgb(0xffd333),
                        List.of(
                                DrinkType.BarrelInfo.of("oak", "quality", 60 * 5),
                                DrinkType.BarrelInfo.of("dark_oak", "quality", 60 * 5),
                                DrinkType.BarrelInfo.of("*", "quality * 0.8", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3.5, 2, 5),
                        "6 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 6", "quality / 5 - 1.2", false)
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 3.5, "oak", List.of())
                )
        );

        consumer.accept("dark_beer", (id) -> DrinkType.create(
                        id,
                        TextColor.fromRgb(0x650013),
                        List.of(
                                DrinkType.BarrelInfo.of("dark_oak", "quality", 60 * 7)
                        ),
                        ExpressionUtil.defaultQuality(8, 3, 18),
                        "7 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 8", "quality / 5 - 1.2", false)
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 8, "dark_oak", List.of())
                )
        );

        consumer.accept("wheat_beer", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xffb84d),
                        List.of(
                                DrinkType.BarrelInfo.of("birch", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3, 2, 4),
                        "5 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SATURATION, "(quality - 5) * 4", "quality / 5 - 1.4", false)
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(8, 3.5, "birch", List.of())

                )
        );

        consumer.accept("wine", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0x722F37),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(30, 30, 240),
                        "8 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.REGENERATION, "(quality - 5) * 15", "0", false)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(5, Items.SWEET_BERRIES)
                        ),
                        DrinkInfo.defaults(5, 30, "*", List.of())
                )
        );

        consumer.accept("glow_wine", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xfffdd1),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(30, 30, 240),
                        "8 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.REGENERATION, "(quality - 5) * 15", "0", false),
                                ConsumptionEffect.of(StatusEffects.GLOWING, "(quality - 3) * 30", "0", true)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(5, Items.GLOW_BERRIES)
                        ),
                        DrinkInfo.defaults(5, 30, "*", List.of())
                )
        );

        consumer.accept("mead", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xffed91),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(4, 1, 6),
                        "9 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SPEED, "(quality - 3) * 10", "(quality - 2) / 3", false)
                        ),
                        ExpressionUtil.defaultBoiling(3, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(3, Items.HONEY_BOTTLE)
                        ),
                        DrinkInfo.defaults(3, 4, "*", List.of())

                )
        );

        consumer.accept("apple_mead", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xffdc91),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(4, 2, 8),
                        "11 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.SPEED, "(quality - 3) * 12", "quality / 3", false)
                        ),
                        ExpressionUtil.defaultBoiling(3, 1d / 3),
                        List.of(
                                DrinkType.BrewIngredient.of(3, Items.HONEY_BOTTLE),
                                DrinkType.BrewIngredient.of(Items.APPLE)
                        ),
                        DrinkInfo.defaults(3, 4, "*", List.of())
                )
        );

        consumer.accept("whiskey", (id) -> DrinkType.create(
                id,
                        TextColor.fromFormatting(Formatting.GOLD),
                        List.of(
                                DrinkType.BarrelInfo.of("dark_oak", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(18, 2, 50),
                        "26 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.ABSORPTION, "(quality - 4) * 12", "quality / 2 - 2", false)
                        ),
                        ExpressionUtil.defaultBoiling(10, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(10, Items.WHEAT)
                        ),
                        DrinkInfo.defaults(10, 18, "dark_oak", List.of())
                )
        );

        consumer.accept("cider", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xf86820),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(3.5, 5),
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

        consumer.accept("apple_liquor", (id) -> DrinkType.create(
                id,
                        TextColor.fromFormatting(Formatting.RED),
                        List.of(
                                DrinkType.BarrelInfo.of("acacia", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(10, 8, 120),
                        "30 * (0.75 + quality / 40)",
                        List.of(

                        ),
                        ExpressionUtil.defaultBoiling(16, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(12, Items.APPLE)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4", true)
                        ),
                        DrinkInfo.defaults(16, 10, "acacia", List.of())

                )
        );

        consumer.accept("rum", (id) -> DrinkType.create(
                id,
                        TextColor.fromFormatting(Formatting.RED),
                        List.of(
                                DrinkType.BarrelInfo.of("*", "quality", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(16, 5, 100),
                        "30 * (0.75 + quality / 40)",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.FIRE_RESISTANCE, "(quality - 4) * 60", "0", false),
                                ConsumptionEffect.of(StatusEffects.POISON, "(6 - quality) * 10", "0", false),
                                ConsumptionEffect.SetOnFire.of("(quality - 2) * 10")
                        ),
                        ExpressionUtil.defaultBoiling(8, 1d / 4),
                        List.of(
                                DrinkType.BrewIngredient.of(18, Items.SUGAR)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4", true)
                        ),
                        DrinkInfo.defaults(8, 16, "*", List.of())
                )
        );

        consumer.accept("vodka", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xEEEEFF),
                        List.of(),
                        "10",
                        "50",
                        List.of(
                                ConsumptionEffect.Delayed.of(
                                        List.of(
                                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "10", "0", true)
                                        ),
                                        "60 * quality"
                                )
                        ),
                        ExpressionUtil.defaultBoilingVodka(20, 0.5),
                        List.of(
                                DrinkType.BrewIngredient.of(6, Items.POTATO)
                        ),
                        true,
                        List.of(
                                ConsumptionEffect.of(StatusEffects.POISON, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.DARKNESS, "60", "4", true),
                                ConsumptionEffect.of(StatusEffects.BLINDNESS, "60", "4", true)
                        ),
                        DrinkInfo.defaults(20, -1, "", List.of())
                )
        );

        consumer.accept("chorus_brew", (id) -> DrinkType.create(
                id,
                        TextColor.fromFormatting(Formatting.DARK_PURPLE),
                        List.of(
                                DrinkType.BarrelInfo.of("warped", "quality", 60 * 5),
                                DrinkType.BarrelInfo.of("*", "quality * 0.4", 60 * 5)
                        ),
                        ExpressionUtil.defaultQuality(2, 2, 5),
                        "0",
                        List.of(
                                ConsumptionEffect.TeleportRandom.of("quality * 10")
                        ),
                        ExpressionUtil.defaultBoiling(16, 1d / 10),
                        List.of(
                                DrinkType.BrewIngredient.of(16, Items.CHORUS_FRUIT)
                        ),
                        DrinkInfo.defaults(16, 2.5, "warped", List.of())
                )
        );


        consumer.accept("tea", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0x993D00),
                        List.of(),
                        "10",
                        "0",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 15", "0", false)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                                DrinkType.BrewIngredient.of(1, Items.OAK_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );

        consumer.accept("green_tea", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xD9F959),
                        List.of(),
                        "10",
                        "0",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 20", "0", false)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                                DrinkType.BrewIngredient.of(1, Items.JUNGLE_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );

        consumer.accept("cherry_tea", (id) -> DrinkType.create(
                id,
                        TextColor.fromRgb(0xc92a0a),
                        List.of(),
                        "10",
                        "0",
                        List.of(
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 20", "0", false),
                                ConsumptionEffect.of(StatusEffects.REGENERATION, "quality * 1.5", "0", false)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                            DrinkType.BrewIngredient.of(1, Items.CHERRY_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );

        if (true) {
            consumer.accept("the_testificate", (id) -> new DrinkType(
                    new DrinkType.Looks(
                            FloatSelector.of(0, 10, WrappedText.of("<red>Failed Experiment"), WrappedText.of("<gold>You tried"),
                                    WrappedText.of("<yellow>So close"), WrappedText.of("<green>The Testificate")),
                            FloatSelector.of(0, 10, TextColor.fromFormatting(Formatting.RED), TextColor.fromFormatting(Formatting.YELLOW), TextColor.fromFormatting(Formatting.GREEN)),
                            FloatSelector.of(0, 10, DrinkType.ItemLookData.DEFAULT.withModel(Identifier.ofVanilla("dirt")),
                                    DrinkType.ItemLookData.DEFAULT.withModel(Identifier.ofVanilla("tnt")),
                                    DrinkType.ItemLookData.DEFAULT.withModel(Identifier.ofVanilla("potion")).withResourcePackModel(Identifier.ofVanilla("splash_potion"))
                            ),
                            Optional.empty(),
                            Optional.of(FloatSelector.of(DrinkType.ItemLookData.DEFAULT.withModel(Identifier.ofVanilla("lingering_potion")))),
                            Optional.empty(),
                            Optional.empty()
                    ),
                    Ingredient.ofItem(Items.GLASS_BOTTLE),
                    List.of(),
                    WrappedExpression.createDefault("10"),
                    WrappedExpression.createDefault("0.8 * 10 / max(quality, 0.1)"),
                    WrappedExpression.createDefault("0"),
                    List.of(
                            ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 15", "0", false)
                    ),
                    WrappedExpression.createDefault("10"),
                    List.of(
                            DrinkType.BrewIngredient.of(1, Items.DARK_OAK_LEAVES)
                    ),
                    1,
                    List.of(),
                    Optional.empty(),
                    true,
                    Optional.empty()
            ));
        }
    }

    public static AlcoholValueEffect createEffects(MinecraftServer server) {
        return new AlcoholValueEffect(false, List.of(
                new AlcoholValueEffect.Value(50, 20 * 4, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.WEAKNESS, "4", "min(max((userAlcoholLevel - 60) / 30, 0), 2)", true, false, false)
                )),
                new AlcoholValueEffect.Value(60, 20 * 4, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.SLOWNESS, "4", "min(max((userAlcoholLevel - 70) / 30, 0), 2)", true, false, false)
                )),
                new AlcoholValueEffect.Value(80, 20 * 16, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.NAUSEA, "32", "0", true, false, false)
                )),
                new AlcoholValueEffect.Value(70, 4, List.of(
                        ConsumptionEffect.Velocity.of("random() - 0.5", "random() / 2 - 0.49", "random() - 0.5", "min((0.3 + random() / 10) * ((userAlcoholLevel - 70) / 70) * 0.7, 0.2)")
                )),
                new AlcoholValueEffect.Value(140, 20 * 16, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.DARKNESS, "16", "0", true, false, false)
                )),
                new AlcoholValueEffect.Value(110, 20 * 4, List.of(
                        ConsumptionEffect.Damage.of(server, BreweryInit.id("alcohol_poisoning"), "(userAlcoholLevel - 110) / 10 + 1")
                ))
        ), Map.of(Items.BREAD, 3d, Items.MILK_BUCKET, 10d),
                Map.of(
                        Items.GLASS_BOTTLE, Identifier.ofVanilla("potion"),
                        Items.BOWL, Identifier.ofVanilla("suspicious_soup"),
                        Items.BUCKET, Identifier.ofVanilla("water_bucket")
                ));
    }
}
