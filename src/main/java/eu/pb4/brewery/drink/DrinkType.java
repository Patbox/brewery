package eu.pb4.brewery.drink;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.brewery.other.FloatSelector;
import eu.pb4.brewery.other.WrappedText;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.pb4.brewery.BreweryInit.id;

public record DrinkType(Looks looks,
                        Ingredient requiredContainer,
                        List<BarrelInfo> barrelInfo, WrappedExpression baseQuality,
                        WrappedExpression drinkingTime,
                        WrappedExpression alcoholicValue, List<ConsumptionEffect> consumptionEffects,
                        WrappedExpression cookingQualityMult, List<BrewIngredient> ingredients, int distillationRuns,
                        List<ConsumptionEffect> unfinishedEffects, Optional<DrinkInfo> info, boolean showQuality,
                        Optional<RegistryEntryList<Block>> heatSource) {
    public static MapCodec<DrinkType> CODEC_V4 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecordCodecBuilder.<Looks>mapCodec(instance2 -> instance2.group(
                    FloatSelector.createQualityCodec(WrappedText.CODEC, null).fieldOf("name").forGetter(Looks::nameSelector),
                    FloatSelector.createQualityCodec(TextColor.CODEC, null).fieldOf("color").forGetter(Looks::colorSelector),
                    FloatSelector.createQualityCodec(ItemLookData.CODEC, ItemLookData.DEFAULT).optionalFieldOf("visual", FloatSelector.of(ItemLookData.DEFAULT)).forGetter(Looks::visualsSelector),
                    FloatSelector.createQualityCodec(TextColor.CODEC, null).optionalFieldOf("unfinished_color").forGetter(Looks::unfinishedColorSelector),
                    FloatSelector.createQualityCodec(ItemLookData.CODEC, null).optionalFieldOf("unfinished_visual").forGetter(Looks::unfinishedVisualsSelector),
                    TextColor.CODEC.optionalFieldOf("failed_color").forGetter(Looks::failedColorSelector),
                    ItemLookData.CODEC.optionalFieldOf("failed_visual").forGetter(Looks::failedVisualsSelector)
            ).apply(instance2, Looks::new)).forGetter(DrinkType::looks),
            Ingredient.CODEC.optionalFieldOf("required_container", Ingredient.ofItem(Items.GLASS_BOTTLE)).forGetter(DrinkType::requiredContainer),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            ExpressionUtil.COMMON_EXPRESSION.optionalFieldOf("drinking_time").xmap(x -> x.orElse(ExpressionUtil.DEFAULT_DRINKING_TIME), Optional::of).forGetter(DrinkType::drinkingTime),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("effects", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.INT.optionalFieldOf("distillation_runs", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));
    public static MapCodec<DrinkType> CODEC_V3 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecordCodecBuilder.<Looks>mapCodec(instance2 -> instance2.group(
                    FloatSelector.createSingularCodec(WrappedText.CODEC).fieldOf("name").forGetter(Looks::nameSelector),
                    FloatSelector.createSingularCodec(TextColor.CODEC).fieldOf("color").forGetter(Looks::colorSelector),
                    FloatSelector.createSingularCodec(ItemLookData.CODEC_LEGACY).optionalFieldOf("visual", FloatSelector.of(ItemLookData.DEFAULT)).forGetter(Looks::visualsSelector),
                    MapCodec.unit(Optional.<FloatSelector<TextColor>>empty()).forGetter(Looks::unfinishedColorSelector),
                    MapCodec.unit(Optional.<FloatSelector<ItemLookData>>empty()).forGetter(Looks::unfinishedVisualsSelector),
                    MapCodec.unit(Optional.<TextColor>empty()).forGetter(Looks::failedColorSelector),
                    MapCodec.unit(Optional.<ItemLookData>empty()).forGetter(Looks::failedVisualsSelector)
            ).apply(instance2, Looks::new)).forGetter(DrinkType::looks),
            MapCodec.unit(Ingredient.ofItem(Items.GLASS_BOTTLE)).forGetter(DrinkType::requiredContainer),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            MapCodec.unit(ExpressionUtil.LEGACY_DRINKING_TIME).forGetter(DrinkType::drinkingTime),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("effects", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.INT.optionalFieldOf("distillation_runs", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));

    public static MapCodec<DrinkType> CODEC_V2 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecordCodecBuilder.<Looks>mapCodec(instance2 -> instance2.group(
                    FloatSelector.createSingularCodec(WrappedText.CODEC).fieldOf("name").forGetter(Looks::nameSelector),
                    FloatSelector.createSingularCodec(TextColor.CODEC).fieldOf("color").forGetter(Looks::colorSelector),
                    FloatSelector.createSingularCodec(ItemLookData.CODEC_LEGACY).optionalFieldOf("visual", FloatSelector.of(ItemLookData.DEFAULT)).forGetter(Looks::visualsSelector),
                    MapCodec.unit(Optional.<FloatSelector<TextColor>>empty()).forGetter(Looks::unfinishedColorSelector),
                    MapCodec.unit(Optional.<FloatSelector<ItemLookData>>empty()).forGetter(Looks::unfinishedVisualsSelector),
                    MapCodec.unit(Optional.<TextColor>empty()).forGetter(Looks::failedColorSelector),
                    MapCodec.unit(Optional.<ItemLookData>empty()).forGetter(Looks::failedVisualsSelector)
            ).apply(instance2, Looks::new)).forGetter(DrinkType::looks),
            MapCodec.unit(Ingredient.ofItem(Items.GLASS_BOTTLE)).forGetter(DrinkType::requiredContainer),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            MapCodec.unit(ExpressionUtil.LEGACY_DRINKING_TIME).forGetter(DrinkType::drinkingTime),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.INT.optionalFieldOf("distillation_runs", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));

    public static MapCodec<DrinkType> CODEC_V1 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RecordCodecBuilder.<Looks>mapCodec(instance2 -> instance2.group(
                    FloatSelector.createSingularCodec(WrappedText.CODEC).fieldOf("name").forGetter(Looks::nameSelector),
                    FloatSelector.createSingularCodec(TextColor.CODEC).fieldOf("color").forGetter(Looks::colorSelector),
                    FloatSelector.createSingularCodec(ItemLookData.CODEC_LEGACY).optionalFieldOf("visual", FloatSelector.of(ItemLookData.DEFAULT)).forGetter(Looks::visualsSelector),
                    MapCodec.unit(Optional.<FloatSelector<TextColor>>empty()).forGetter(Looks::unfinishedColorSelector),
                    MapCodec.unit(Optional.<FloatSelector<ItemLookData>>empty()).forGetter(Looks::unfinishedVisualsSelector),
                    MapCodec.unit(Optional.<TextColor>empty()).forGetter(Looks::failedColorSelector),
                    MapCodec.unit(Optional.<ItemLookData>empty()).forGetter(Looks::failedVisualsSelector)
            ).apply(instance2, Looks::new)).forGetter(DrinkType::looks),
            MapCodec.unit(Ingredient.ofItem(Items.GLASS_BOTTLE)).forGetter(DrinkType::requiredContainer),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            MapCodec.unit(ExpressionUtil.LEGACY_DRINKING_TIME).forGetter(DrinkType::drinkingTime),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.BOOL.xmap(x -> x ? 1 : 0, i -> i == 1).optionalFieldOf("require_distillation", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));
    public static final Codec<DrinkType> CODEC = new MapCodec.MapCodecCodec<>(new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("version"));
        }

        @Override
        public <T> DataResult<DrinkType> decode(DynamicOps<T> ops, MapLike<T> input) {
            var version = ops.getNumberValue(input.get("version"), 0).intValue();
            return switch (version) {
                case 1 -> DrinkType.CODEC_V1.decode(ops, input);
                case 2 -> DrinkType.CODEC_V2.decode(ops, input);
                case 3 -> DrinkType.CODEC_V3.decode(ops, input);
                default -> DrinkType.CODEC_V4.decode(ops, input);
            };
        }

        @Override
        public <T> RecordBuilder<T> encode(DrinkType input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return CODEC_V4.encode(input, ops, prefix.add("version", ops.createInt(4)));
        }
    });

    public static DrinkType create(Identifier identifier, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, DrinkInfo info) {
        return create(identifier, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, false, List.of(), info);
    }


    public static DrinkType create(Identifier identifier, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, boolean requireDistillation, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info) {
        return create(identifier, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, requireDistillation ? 1 : 0, unfinishedEffects, info);

    }

    public static DrinkType create(Identifier identifier, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info) {
        return create(identifier, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, distillationRuns, unfinishedEffects, info, Optional.empty());
    }

    public static DrinkType create(Identifier identifier, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info, TagKey<Block> heatSource) {
        return create(identifier, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, distillationRuns, unfinishedEffects, info, Optional.of(RegistryEntryList.of(Registries.BLOCK, heatSource)));
    }

    public static DrinkType create(Identifier identifier, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info, Optional<RegistryEntryList<Block>> heatSource) {
        return new DrinkType(new Looks(FloatSelector.of(WrappedText.of(Text.translatable(Util.createTranslationKey("drinktype", identifier)))),
                FloatSelector.of(color),
                FloatSelector.of(ItemLookData.DEFAULT.withResourcePackModel(identifier.withPrefixedPath("brewery_drink/"))),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()),
                Ingredient.ofItem(Items.GLASS_BOTTLE),
                barrelInfo, WrappedExpression.create(quality, ExpressionUtil.AGE_KEY),
                ExpressionUtil.DEFAULT_DRINKING_TIME,
                WrappedExpression.createDefault(alcoholicValue), consumptionEffects, WrappedExpression.create(cookingTime, ExpressionUtil.AGE_KEY), ingredients, distillationRuns, unfinishedEffects, Optional.of(info), true, heatSource);
    }



    public WrappedText name(ItemStack stack) {
        return this.looks.nameSelector.select((float) DrinkUtils.getQuality(stack));
    }
    public int color(ItemStack stack) {
        var quality = (float) DrinkUtils.getQuality(stack);
        if (!this.isFinished(stack)) {
            return this.looks.unfinishedColorSelector.map(textColorFloatSelector -> textColorFloatSelector.select(quality).getRgb())
                    .orElseGet(() -> ColorHelper.lerp(0.5f, this.looks.colorSelector.select(quality).getRgb(), 0x385dc6));
        }
        return this.looks.colorSelector.select(quality).getRgb();
    }
    public ItemLookData visuals(ItemStack stack) {
        var quality = (float) DrinkUtils.getQuality(stack);
        if (!this.isFinished(stack) && this.looks.unfinishedVisualsSelector.isPresent()) {
            return this.looks.unfinishedVisualsSelector.get().select(quality);
        }

        return this.looks.visualsSelector.select(quality);
    }

    public int failedColor() {
        return this.looks.failedColorSelector.orElseGet(() -> TextColor.fromRgb(0x051a0a)).getRgb();
    }
    public ItemLookData failedVisuals() {
        return this.looks.failedVisualsSelector.orElseGet(() -> this.looks.visualsSelector.select(0f));
    }

    public boolean requireDistillation() {
        return this.distillationRuns > 0;
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
        return DrinkUtils.getAgeInSeconds(itemStack) >= 0 && (!this.requireDistillation() || DrinkUtils.getDistillationStatus(itemStack));
    }

    public double drinkingTime(ItemStack stack, LivingEntity entity) {
        return this.drinkingTime.expression()
                .setVariable(ExpressionUtil.AGE_KEY, DrinkUtils.getAgeInSeconds(stack))
                .setVariable(ExpressionUtil.QUALITY_KEY, DrinkUtils.getQuality(stack))
                .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(entity).getModifiedAlcoholLevel())
                .evaluate();
    }


    public record Looks(FloatSelector<WrappedText> nameSelector,
                        FloatSelector<TextColor> colorSelector,
                        FloatSelector<ItemLookData> visualsSelector,
                        Optional<FloatSelector<TextColor>> unfinishedColorSelector,
                        Optional<FloatSelector<ItemLookData>> unfinishedVisualsSelector,
                        Optional<TextColor> failedColorSelector,
                        Optional<ItemLookData> failedVisualsSelector) {

    }

    public record BarrelInfo(Identifier type, WrappedExpression qualityChange, int baseTime) {
        public static final Identifier ANY = id("any_barrel");
        public static final Identifier NONE = id("none");
        protected static Codec<Identifier> TYPE_CODEC = Codec.STRING.xmap(x -> x.equals("*") ? ANY : BrewUtils.tryParsingId(x, NONE), x -> x.equals(ANY) ? "*" : x.toString());
        public static Codec<BarrelInfo> CODEC_V1 = RecordCodecBuilder.create(instance -> instance.group(TYPE_CODEC.fieldOf("type").forGetter(BarrelInfo::type), ExpressionUtil.COMMON_EXPRESSION.fieldOf("quality_value").forGetter(BarrelInfo::qualityChange), Codecs.POSITIVE_INT.fieldOf("reveal_time").forGetter(BarrelInfo::baseTime)).apply(instance, BarrelInfo::new));

        public static BarrelInfo of(String type, String qualityChange, int baseTime) {
            return of(type.equals("*") ? ANY : Identifier.of(type), qualityChange, baseTime);
        }

        public static BarrelInfo of(Identifier type, String qualityChange, int baseTime) {
            return new BarrelInfo(type, WrappedExpression.createDefault(qualityChange), baseTime);
        }
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public record ItemLookData(Identifier defaultModel, Optional<Identifier> resourcePackModel, Optional<ComponentMap> components, boolean particles,
                               RegistryEntry<SoundEvent> soundEvent, UseAction animation) {
        public static final ItemLookData DEFAULT = new ItemLookData(Identifier.ofVanilla("potion"), Optional.empty(), Optional.empty(),
                false, SoundEvents.ENTITY_GENERIC_DRINK, UseAction.DRINK);
        public static Codec<ItemLookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("model", DEFAULT.defaultModel).forGetter(ItemLookData::defaultModel),
                Identifier.CODEC.optionalFieldOf("resource_pack_model").forGetter(ItemLookData::resourcePackModel),
                ComponentMap.CODEC.optionalFieldOf("components").forGetter(ItemLookData::components),
                Codec.BOOL.optionalFieldOf("particles", DEFAULT.particles).forGetter(ItemLookData::particles),
                SoundEvent.ENTRY_CODEC.optionalFieldOf("sound_event", DEFAULT.soundEvent).forGetter(ItemLookData::soundEvent),
                UseAction.CODEC.optionalFieldOf("animation", DEFAULT.animation).forGetter(ItemLookData::animation)
        ).apply(instance, ItemLookData::new));

        public static Codec<ItemLookData> CODEC_LEGACY = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("item", DEFAULT.defaultModel).forGetter(ItemLookData::defaultModel),
                Identifier.CODEC.optionalFieldOf("model").forGetter(ItemLookData::resourcePackModel),
                ComponentMap.CODEC.optionalFieldOf("components").forGetter(ItemLookData::components),
                Codec.BOOL.optionalFieldOf("particles", DEFAULT.particles).forGetter(ItemLookData::particles),
                SoundEvent.ENTRY_CODEC.optionalFieldOf("sound_event", DEFAULT.soundEvent).forGetter(ItemLookData::soundEvent),
                UseAction.CODEC.optionalFieldOf("animation", DEFAULT.animation).forGetter(ItemLookData::animation)
        ).apply(instance, ItemLookData::new));

        public ItemLookData withModel(Identifier identifier) {
            return new ItemLookData(identifier, resourcePackModel, components, particles, soundEvent, animation);
        }
        public ItemLookData withResourcePackModel(Identifier identifier) {
            return new ItemLookData(defaultModel, Optional.ofNullable(identifier), components, particles, soundEvent, animation);
        }
    }

    public record BrewIngredient(List<Item> items, int count, ItemStack returnedItemStack) {
        public static Codec<BrewIngredient> CODEC_V1 = RecordCodecBuilder.create(instance -> instance.group(Codec.list(Registries.ITEM.getCodec()).fieldOf("items").forGetter(BrewIngredient::items), Codec.INT.optionalFieldOf("count", 1).forGetter(BrewIngredient::count), ItemStack.CODEC.optionalFieldOf("dropped_stack", ItemStack.EMPTY).forGetter(BrewIngredient::returnedItemStack)).apply(instance, BrewIngredient::new));

        public static BrewIngredient of(int count, Item... items) {
            return new BrewIngredient(List.of(items), count, ItemStack.EMPTY);
        }

        public static BrewIngredient of(int count, ItemStack stack, Item... items) {
            return new BrewIngredient(List.of(items), count, stack);
        }

        public static BrewIngredient of(Item... items) {
            return new BrewIngredient(List.of(items), 1, ItemStack.EMPTY);
        }
    }
}
