package eu.pb4.brewery.drink;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.brewery.other.WrappedText;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.pb4.brewery.BreweryInit.id;

public record DrinkType(WrappedText name, TextColor color, List<BarrelInfo> barrelInfo, WrappedExpression baseQuality, WrappedExpression alcoholicValue,
                        List<ConsumptionEffect> consumptionEffects, WrappedExpression cookingQualityMult, List<BrewIngredient> ingredients,
                        boolean requireDistillation, List<ConsumptionEffect> unfinishedEffects, Optional<DrinkInfo> info) {
    public static final Codec<DrinkType> CODEC = new MapCodec.MapCodecCodec<>(new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(Stream.of(ops.createString("version")), DrinkType.CODEC_V1.keys(ops));
        }

        @Override
        public <T> DataResult<DrinkType> decode(DynamicOps<T> ops, MapLike<T> input) {
            var version = ops.getNumberValue(input.get("version"), 0).intValue();
            return switch (version) {
                case 0 -> DrinkType.CODEC_V0.decode(ops, input);
                case 1 -> DrinkType.CODEC_V1.decode(ops, input);

                default -> DataResult.error("Unsupported brew version: " + version + "!");
            };
        }

        @Override
        public <T> RecordBuilder<T> encode(DrinkType input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return CODEC_V1.encode(input, ops, prefix.add("version", ops.createInt(1)));
        }
    });
    public static MapCodec<DrinkType> CODEC_V1 = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    WrappedText.CODEC.fieldOf("name").forGetter(DrinkType::name),
                    TextColor.CODEC.fieldOf("color").forGetter(DrinkType::color),
                    Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
                    ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
                    Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
                    Codec.BOOL.optionalFieldOf("require_distillation", false).forGetter(DrinkType::requireDistillation),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
                    DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info)
                    ).apply(instance, DrinkType::new));

    public static MapCodec<DrinkType> CODEC_V0 = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    WrappedText.CODEC.fieldOf("name").forGetter(DrinkType::name),
                    TextColor.CODEC.fieldOf("color").forGetter(DrinkType::color),
                    Codec.list(BarrelInfo.CODEC_V0).fieldOf("barrels").forGetter(DrinkType::barrelInfo),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("baseQuality").forGetter(DrinkType::baseQuality),
                    ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholicValue").forGetter(DrinkType::alcoholicValue),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cookingQuality").forGetter(DrinkType::cookingQualityMult),
                    Codec.list(BrewIngredient.CODEC_V0).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
                    Codec.BOOL.optionalFieldOf("requireDistillation", false).forGetter(DrinkType::requireDistillation),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinishedEffects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
                    DrinkInfo.OLD_CODEC.optionalFieldOf("info").forGetter(DrinkType::info)
            ).apply(instance, DrinkType::new));

    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, DrinkInfo info) {
        return create(name, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, false, List.of(), info);
    }


    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue,
                                   List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients,
                                   boolean requireDistillation, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info) {
        return new DrinkType(WrappedText.of(name), color, barrelInfo,
                WrappedExpression.create(quality, ExpressionUtil.AGE_KEY),
                WrappedExpression.createDefault(alcoholicValue),
                consumptionEffects,
                WrappedExpression.create(cookingTime, ExpressionUtil.AGE_KEY),
                ingredients, requireDistillation, unfinishedEffects, Optional.of(info)
        );
    }

    @Nullable
    public BarrelInfo getBarrelInfo(Identifier barrelType) {
        BarrelInfo def = null;
        for (var info : this.barrelInfo) {
            if (info.type.equals(barrelType)) {
                return info;
            } else if (info.type.equals(BarrelInfo.ANY)) {
                def = info;
            }
        }
        return def;
    }

    public boolean isFinished(ItemStack itemStack) {
        return DrinkUtils.getAgeInSeconds(itemStack) >= 0 && (!this.requireDistillation || DrinkUtils.getDistillationStatus(itemStack));
    }

    public record BarrelInfo(Identifier type, WrappedExpression qualityChange, int baseTime) {
        public static final Identifier ANY = id("any_barrel");
        public static final Identifier NONE = id("none");

        public static BarrelInfo of(String type, String qualityChange, int baseTime) {
            return of(type.equals("*") ? ANY : new Identifier(type), qualityChange, baseTime);
        }

        public static BarrelInfo of(Identifier type, String qualityChange, int baseTime) {
            return new BarrelInfo(type, WrappedExpression.createDefault(qualityChange), baseTime);
        }

        protected static Codec<Identifier> TYPE_CODEC = Codec.STRING.xmap(x -> x.equals("*") ? ANY : BrewUtils.tryParsingId(x, NONE), x -> x.equals(ANY) ? "*" : x.toString());

        public static Codec<BarrelInfo> CODEC_V1 = RecordCodecBuilder.create(instance ->
                instance.group(
                        TYPE_CODEC.fieldOf("type").forGetter(BarrelInfo::type),
                        ExpressionUtil.COMMON_EXPRESSION.fieldOf("quality_value").forGetter(BarrelInfo::qualityChange),
                        Codecs.POSITIVE_INT.fieldOf("reveal_time").forGetter(BarrelInfo::baseTime)
                ).apply(instance, BarrelInfo::new));

        public static Codec<BarrelInfo> CODEC_V0 = RecordCodecBuilder.create(instance ->
                instance.group(
                        TYPE_CODEC.fieldOf("type").forGetter(BarrelInfo::type),
                        ExpressionUtil.COMMON_EXPRESSION.fieldOf("qualityChange").forGetter(BarrelInfo::qualityChange),
                        Codecs.POSITIVE_INT.fieldOf("minimalTime").forGetter(BarrelInfo::baseTime)
                    ).apply(instance, BarrelInfo::new));
    }

    public record BrewIngredient(List<Item> items, int count, ItemStack returnedItemStack) {
        public static BrewIngredient of(int count, Item... items) {
            return new BrewIngredient(List.of(items), count, ItemStack.EMPTY);
        }

        public static BrewIngredient of(int count, ItemStack stack, Item... items) {
            return new BrewIngredient(List.of(items), count, stack);
        }

        public static BrewIngredient of(Item... items) {
            return new BrewIngredient(List.of(items), 1, ItemStack.EMPTY);
        }

        public static Codec<BrewIngredient> CODEC_V1 = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.list(Registry.ITEM.getCodec()).fieldOf("items").forGetter(BrewIngredient::items),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(BrewIngredient::count),
                        ItemStack.CODEC.optionalFieldOf("dropped_stack", ItemStack.EMPTY).forGetter(BrewIngredient::returnedItemStack)
                ).apply(instance, BrewIngredient::new));

        public static Codec<BrewIngredient> CODEC_V0 = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.list(Registry.ITEM.getCodec()).fieldOf("items").forGetter(BrewIngredient::items),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(BrewIngredient::count),
                        ItemStack.CODEC.optionalFieldOf("returnedItemStack", ItemStack.EMPTY).forGetter(BrewIngredient::returnedItemStack)
                ).apply(instance, BrewIngredient::new));
    }
}
