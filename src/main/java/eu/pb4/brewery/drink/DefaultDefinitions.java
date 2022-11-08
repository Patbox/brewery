package eu.pb4.brewery.drink;

import com.mojang.serialization.RecordBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;


import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class DefaultDefinitions {
    public static void createBrews(BiConsumer<String, DrinkType> consumer) {
        consumer.accept("beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.beer"),
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

        consumer.accept("dark_beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.dark_beer"),
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

        consumer.accept("wheat_beer", DrinkType.create(
                        Text.translatable("drinktype.brewery.wheat_beer"),
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

        consumer.accept("wine", DrinkType.create(
                        Text.translatable("drinktype.brewery.wine"),
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

        consumer.accept("glow_wine", DrinkType.create(
                        Text.translatable("drinktype.brewery.glow_wine"),
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

        consumer.accept("mead", DrinkType.create(
                        Text.translatable("drinktype.brewery.mead"),
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

        consumer.accept("apple_mead", DrinkType.create(
                        Text.translatable("drinktype.brewery.apple_mead"),
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

        consumer.accept("whiskey", DrinkType.create(
                        Text.translatable("drinktype.brewery.whiskey"),
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

        consumer.accept("cider", DrinkType.create(
                        Text.translatable("drinktype.brewery.cider"),
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

        consumer.accept("apple_liquor", DrinkType.create(
                        Text.translatable("drinktype.brewery.apple_liquor"),
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

        consumer.accept("rum", DrinkType.create(
                        Text.translatable("drinktype.brewery.rum"),
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

        consumer.accept("vodka", DrinkType.create(
                        Text.translatable("drinktype.brewery.vodka"),
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
                                ConsumptionEffect.TeleportRandom.of("quality * 10")
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
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 15", "0", false)
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
                                ConsumptionEffect.of(StatusEffects.RESISTANCE, "quality * 20", "0", false)
                        ),
                        ExpressionUtil.defaultBoiling(5, 1d / 2),
                        List.of(
                                DrinkType.BrewIngredient.of(1, Items.JUNGLE_LEAVES)
                        ),
                        DrinkInfo.defaults(5, -1, "", List.of())
                )
        );
    }

    public static AlcoholValueEffect createEffects() {
        return new AlcoholValueEffect(false, List.of(
                new AlcoholValueEffect.Value(50, 20 * 4, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.WEAKNESS, "4", "min(max((userAlcoholLevel - 60) / 30, 0), 2)", true, false, false)
                )),
                new AlcoholValueEffect.Value(60, 20 * 4, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.SLOWNESS, "4", "min(max((userAlcoholLevel - 70) / 30, 0), 2)", true, false, false)
                )),
                new AlcoholValueEffect.Value(70, 20 * 16, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.NAUSEA, "16", "0", true, false, false)
                )),
                new AlcoholValueEffect.Value(70, 20 * 6, List.of(
                        ConsumptionEffect.Velocity.of("random() - 0.5", "random() / 2 - 0.25", "random() - 0.5", "(0.3 + random() / 10) * ((userAlcoholLevel - 70) / 80 + 1)")
                )),
                new AlcoholValueEffect.Value(90, 20 * 16, List.of(
                        ConsumptionEffect.Potion.of(StatusEffects.DARKNESS, "16", "0", true, false, false)
                )),
                new AlcoholValueEffect.Value(90, 20 * 4, List.of(
                        ConsumptionEffect.Damage.of("brewery.alcohol_poisoning", "(userAlcoholLevel - 110) / 10 + 1")
                ))
        ), Map.of(Items.BREAD, 3d, Items.MILK_BUCKET, 10d));
    }
}
