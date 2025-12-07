package eu.pb4.brewery.drink;

import com.mojang.datafixers.util.Pair;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.duck.LivingEntityExt;
import eu.pb4.brewery.other.BrewGameRules;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AlcoholManager {
    public static final AlcoholManager FALLBACK = new AlcoholManager(null);
    @Nullable
    private final LivingEntity entity;
    public double alcoholLevel = 0;
    public double quality = -1;

    private final List<DelayedEffect> delayedEffects = new ArrayList<>();
    private final List<DelayedEffect> delayedEffectsNext = new ArrayList<>();
    private final List<TimedAttributes> timedAttributes = new ArrayList<>();


    public AlcoholManager(@Nullable LivingEntity entity) {
        this.entity = entity;
    }

    public void drink(DrinkType type, double quality, double alcoholicValue) {
        if (this.entity == null) {
            return;
        }
        if (this.entity.level() instanceof ServerLevel world) {
            var multiplier = world.getGameRules().get(BrewGameRules.ALCOHOL_MULTIPLIER);

            this.alcoholLevel = Math.max(this.alcoholLevel + alcoholicValue * multiplier, alcoholicValue * multiplier);
            if (this.quality == -1) {
                this.quality = quality;
            } else {
                this.quality = (this.quality + quality) / 2;
            }
        }
    }

    public void eat(ItemStack stack) {
        if (this.alcoholLevel > 0) {
            this.alcoholLevel -= BreweryInit.ITEM_ALCOHOL_REMOVAL_VALUES.getDouble(stack.getItem());
        }
    }

    public static AlcoholManager of(LivingEntity entity) {
        return entity != null ? ((LivingEntityExt) entity).brewery$getAlcoholManager() : FALLBACK;
    }

    public void writeData(ValueOutput view) {
        view.putDouble("brewery:alcohol_level", this.alcoholLevel);
        view.putDouble("brewery:quality", this.quality);

        var list = view.childrenList("brewery:delayed_effects");
        var list2 = view.childrenList("brewery:timed_attributes");

        for (var effect : this.delayedEffects) {
            effect.writeData(list.addChild());
        }
        for (var effect : this.timedAttributes) {
            effect.writeData(list2.addChild());
        }
    }

    public void readData(ValueInput view) {
        this.alcoholLevel = view.getDoubleOr("brewery:alcohol_level",0);
        this.quality = view.getDoubleOr("brewery:quality", 0);

        this.delayedEffects.clear();
        for (var effect : view.childrenListOrEmpty("brewery:delayed_effects")) {
            this.delayedEffects.add(DelayedEffect.readData(effect));
        }

        for (var effect : view.childrenListOrEmpty("brewery:timed_attributes")) {
            this.timedAttributes.add(TimedAttributes.readData(effect));
        }
    }
    
    public void tick() {
        if (this.entity == null) {
            return;
        }
        if (!this.delayedEffectsNext.isEmpty()) {
            this.delayedEffects.addAll(this.delayedEffectsNext);
            this.delayedEffectsNext.clear();
        }

        this.delayedEffects.removeIf(this::applyDelayedEffects);
        this.timedAttributes.removeIf(this::applyTimedAttributes);

        if (!this.delayedEffectsNext.isEmpty()) {
            this.delayedEffects.addAll(this.delayedEffectsNext);
            this.delayedEffectsNext.clear();
        }

        if (this.alcoholLevel > 0) {
            this.alcoholLevel -= 0.0012 * this.quality;

            var level = this.getModifiedAlcoholLevel();

            for (var effects : BreweryInit.ALCOHOL_EFFECTS) {
                if (level >= effects.minimumValue() && this.entity.tickCount % effects.rate() == 0) {
                    for (var effect : effects.effects()) {
                        effect.apply(this.entity, 0, this.quality);
                    }
                }
            }
        }
    }

    private boolean applyTimedAttributes(TimedAttributes timedAttributes) {
        if (this.entity == null) {
            return false;
        }
        if (--timedAttributes.ticksLeft > 0) {
            for (var effect : timedAttributes.attributes) {
                var x = this.entity.getAttribute(effect.getFirst());
                if (x != null && !x.hasModifier(effect.getSecond().id())) {
                    x.addTransientModifier(effect.getSecond());
                }
            }

            return false;
        }

        for (var effect : timedAttributes.attributes) {
            var x = this.entity.getAttribute(effect.getFirst());
            if (x != null && x.hasModifier(effect.getSecond().id())) {
                x.removeModifier(effect.getSecond());
            }
        }
        return true;
    }

    private boolean applyDelayedEffects(DelayedEffect delayedEffect) {
        if (this.entity == null) {
            return false;
        }
        if ((--delayedEffect.ticksLeft) <= 0) {
            for (var effect : delayedEffect.effects) {
                effect.apply(this.entity, delayedEffect.age, delayedEffect.quality);
            }

            return true;
        }
        return false;
    }

    public void addDelayedEffect(int ticks, double drinkAge, double drinkQuality, List<ConsumptionEffect> effects) {
        this.delayedEffectsNext.add(new DelayedEffect(ticks, drinkQuality, drinkAge, List.copyOf(effects)));
    }

    public void addTimedAttributes(int ticks, double drinkAge, double drinkQuality, List<Pair<Holder<Attribute>, AttributeModifier>> effects) {
        this.timedAttributes.add(new TimedAttributes(ticks, drinkQuality, drinkAge, List.copyOf(effects)));
    }

    public double getModifiedAlcoholLevel() {
        return this.alcoholLevel - (this.quality - 8) * 5;
    }

    private static class TimedAttributes {
        public int ticksLeft;
        private final double quality;
        private final double age;
        private final List<Pair<Holder<Attribute>, AttributeModifier>> attributes;

        public TimedAttributes(int ticks, double quality, double age, List<Pair<Holder<Attribute>, AttributeModifier>> effects) {
            this.ticksLeft = ticks;
            this.quality = quality;
            this.age = age;
            this.attributes = effects;
        }


        public void writeData(ValueOutput view) {
            view.putInt("ticks", this.ticksLeft);
            view.putDouble("quality", this.quality);
            view.putDouble("age", this.age);

            var list = view.list("entries", ConsumptionEffect.Attributes.ATTRIBUTE_PAIR.codec());
            for (var effect : this.attributes) {
                list.add(effect);
            }
        }

        public static TimedAttributes readData(ValueInput view) {
            var ticks = view.getIntOr("ticks", 0);
            var quality = view.getDoubleOr("quality", 0);
            var age = view.getDoubleOr("age", 0);

            var list = new ArrayList<Pair<Holder<Attribute>, AttributeModifier>>();

            for (var effect : view.listOrEmpty("entries", ConsumptionEffect.Attributes.ATTRIBUTE_PAIR.codec())) {
                list.add(effect);
            }

            return new TimedAttributes(ticks, quality, age, list);
        }
    }

    private static class DelayedEffect {
        public int ticksLeft;
        private final double quality;
        private final double age;
        private final List<ConsumptionEffect> effects;

        public DelayedEffect(int ticks, double quality, double age, List<ConsumptionEffect> effects) {
            this.ticksLeft = ticks;
            this.quality = quality;
            this.age = age;
            this.effects = effects;
        }


        public void writeData(ValueOutput view) {
            view.putInt("ticks", this.ticksLeft);
            view.putDouble("quality", this.quality);
            view.putDouble("age", this.age);

            var list = view.list("entries", ConsumptionEffect.CODEC);
            for (var effect : this.effects) {
                list.add(effect);
            }
        }

        public static DelayedEffect readData(ValueInput view) {
            var ticks = view.getIntOr("ticks", 0);
            var quality = view.getDoubleOr("quality", 0);
            var age = view.getDoubleOr("age", 0);

            var list = new ArrayList<ConsumptionEffect>();

            for (var effect : view.listOrEmpty("entries", ConsumptionEffect.CODEC)) {
                list.add(effect);
            }

            return new DelayedEffect(ticks, quality, age, list);
        }
    }
}
