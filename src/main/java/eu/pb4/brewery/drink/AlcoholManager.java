package eu.pb4.brewery.drink;

import com.mojang.datafixers.util.Pair;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.duck.LivingEntityExt;
import eu.pb4.brewery.other.BrewGameRules;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
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
        if (this.entity.getWorld() instanceof ServerWorld world) {
            var multiplier = world.getGameRules().get(BrewGameRules.ALCOHOL_MULTIPLIER).get();

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

    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putDouble("brewery:alcohol_level", this.alcoholLevel);
        nbt.putDouble("brewery:quality", this.quality);

        var list = new NbtList();
        var list2 = new NbtList();
        for (var effect : this.delayedEffects) {
            list.add(effect.toNbt(lookup));
        }
        for (var effect : this.timedAttributes) {
            list.add(effect.toNbt(lookup));
        }
        nbt.put("brewery:delayed_effects", list);
        nbt.put("brewery:timed_attributes", list2);
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.alcoholLevel = nbt.getDouble("brewery:alcohol_level",0);
        this.quality = nbt.getDouble("brewery:quality", 0);

        this.delayedEffects.clear();
        for (var effect : nbt.getListOrEmpty("brewery:delayed_effects")) {
            this.delayedEffects.add(DelayedEffect.fromNbt((NbtCompound) effect, lookup));
        }

        for (var effect : nbt.getListOrEmpty("brewery:timed_attributes")) {
            this.timedAttributes.add(TimedAttributes.fromNbt((NbtCompound) effect, lookup));
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
                if (level >= effects.minimumValue() && this.entity.age % effects.rate() == 0) {
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
                var x = this.entity.getAttributeInstance(effect.getFirst());
                if (x != null && !x.hasModifier(effect.getSecond().id())) {
                    x.addTemporaryModifier(effect.getSecond());
                }
            }

            return false;
        }

        for (var effect : timedAttributes.attributes) {
            var x = this.entity.getAttributeInstance(effect.getFirst());
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

    public void addTimedAttributes(int ticks, double drinkAge, double drinkQuality, List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> effects) {
        this.timedAttributes.add(new TimedAttributes(ticks, drinkQuality, drinkAge, List.copyOf(effects)));
    }

    public double getModifiedAlcoholLevel() {
        return this.alcoholLevel - (this.quality - 8) * 5;
    }

    private static class TimedAttributes {
        public int ticksLeft;
        private final double quality;
        private final double age;
        private final List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> attributes;

        public TimedAttributes(int ticks, double quality, double age, List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> effects) {
            this.ticksLeft = ticks;
            this.quality = quality;
            this.age = age;
            this.attributes = effects;
        }


        public NbtCompound toNbt(RegistryWrapper.WrapperLookup lookup) {
            var nbt = new NbtCompound();
            nbt.putInt("ticks", this.ticksLeft);
            nbt.putDouble("quality", this.quality);
            nbt.putDouble("age", this.age);

            var list = new NbtList();
            var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            for (var effect : this.attributes) {
                list.add(ConsumptionEffect.Attributes.ATTRIBUTE_PAIR.codec().encodeStart(ops, effect).getOrThrow());
            }

            nbt.put("entries", list);
            return nbt;
        }

        public static TimedAttributes fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
            var ticks = nbt.getInt("ticks", 0);
            var quality = nbt.getDouble("quality", 0);
            var age = nbt.getDouble("age", 0);

            var list = new ArrayList<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>>();
            var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

            for (var effect : nbt.getListOrEmpty("entries")) {
                ConsumptionEffect.Attributes.ATTRIBUTE_PAIR.codec().decode(ops, effect).ifSuccess(x -> list.add(x.getFirst()));
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


        public NbtCompound toNbt(RegistryWrapper.WrapperLookup lookup) {
            var nbt = new NbtCompound();
            nbt.putInt("ticks", this.ticksLeft);
            nbt.putDouble("quality", this.quality);
            nbt.putDouble("age", this.age);

            var list = new NbtList();
            var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            for (var effect : this.effects) {
                list.add(ConsumptionEffect.CODEC.encodeStart(ops, effect).result().get());
            }

            nbt.put("entries", list);
            return nbt;
        }

        public static DelayedEffect fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
            var ticks = nbt.getInt("ticks", 0);
            var quality = nbt.getDouble("quality", 0);
            var age = nbt.getDouble("age", 0);

            var list = new ArrayList<ConsumptionEffect>();
            var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

            for (var effect : nbt.getListOrEmpty("entries")) {
                var x = ConsumptionEffect.CODEC.decode(ops, effect).result();
                if (x.isPresent()) {
                    list.add(x.get().getFirst());
                }
            }

            return new DelayedEffect(ticks, quality, age, list);
        }
    }
}
