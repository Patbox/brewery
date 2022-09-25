package eu.pb4.brewery.drink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.other.WrappedText;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record DrinkType(WrappedText name, TextColor color, List<BarrelInfo> barrelInfo, WrappedExpression baseQuality, WrappedExpression alcoholicValue,
                        List<ConsumptionEffect> consumptionEffects, WrappedExpression cookingQualityMult, List<BrewIngredient> ingredients,
                        boolean requireDistillation, List<ConsumptionEffect> unfinishedEffects, Optional<DrinkInfo> info) {
    public static Codec<DrinkType> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    WrappedText.CODEC.fieldOf("name").forGetter(DrinkType::name),
                    TextColor.CODEC.fieldOf("color").forGetter(DrinkType::color),
                    Codec.list(BarrelInfo.CODEC).fieldOf("barrels").forGetter(DrinkType::barrelInfo),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("baseQuality").forGetter(DrinkType::baseQuality),
                    ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholicValue").forGetter(DrinkType::alcoholicValue),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("effects", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
                    ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cookingQuality").forGetter(DrinkType::cookingQualityMult),
                    Codec.list(BrewIngredient.CODEC).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
                    Codec.BOOL.optionalFieldOf("requireDistillation", false).forGetter(DrinkType::requireDistillation),
                    Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinishedEffects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
                    DrinkInfo.CODEC.optionalFieldOf("info").forGetter(DrinkType::info)
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
    public BarrelInfo getBarrelInfo(String barrelType) {
        BarrelInfo def = null;
        for (var info : this.barrelInfo) {
            if (info.type.equals(barrelType)) {
                return info;
            } else if (info.type.equals("*")) {
                def = info;
            }
        }
        return def;
    }

    public boolean isFinished(ItemStack itemStack) {
        return DrinkUtils.getAgeInSeconds(itemStack) >= 0 && (!this.requireDistillation || DrinkUtils.getDistillationStatus(itemStack));
    }

    public record BarrelInfo(String type, WrappedExpression qualityChange, int baseTime) {
        public static BarrelInfo of(String type, String qualityChange, int baseTime) {
            return new BarrelInfo(type, WrappedExpression.createDefault(qualityChange), baseTime);
        }

        public static Codec<BarrelInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("type").forGetter(BarrelInfo::type),
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

        public static Codec<BrewIngredient> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.list(Registry.ITEM.getCodec()).fieldOf("items").forGetter(BrewIngredient::items),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(BrewIngredient::count),
                        ItemStack.CODEC.optionalFieldOf("returnedItemStack", ItemStack.EMPTY).forGetter(BrewIngredient::returnedItemStack)
                ).apply(instance, BrewIngredient::new));
    }
}
