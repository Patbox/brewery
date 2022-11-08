package eu.pb4.brewery.drink;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.duck.LivingEntityExt;
import eu.pb4.brewery.other.BrewGameRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;

import java.util.ArrayList;
import java.util.List;

public final class AlcoholManager {
    private final LivingEntity entity;
    public double alcoholLevel = 0;
    public double quality = -1;

    private final List<DelayedEffect> delayedEffects = new ArrayList<>();
    
    public AlcoholManager(LivingEntity entity) {
        this.entity = entity;
    }

    public void drink(DrinkType type, double quality, double alcoholicValue) {
        var multiplier = this.entity.world.getGameRules().get(BrewGameRules.ALCOHOL_MULTIPLIER).get();

        this.alcoholLevel = Math.max(this.alcoholLevel + alcoholicValue * multiplier, alcoholicValue * multiplier);
        if (this.quality == -1) {
            this.quality = quality;
        } else {
            this.quality = (this.quality + quality) / 2;
        }
    }

    public void eat(ItemStack stack) {
        if (this.alcoholLevel > 0) {
            this.alcoholLevel -= BreweryInit.ITEM_ALCOHOL_REMOVAL_VALUES.getDouble(stack.getItem());
        }
    }

    public static AlcoholManager of(LivingEntity entity) {
        return ((LivingEntityExt) entity).brewery$getAlcoholManager();
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putDouble("brewery:alcohol_level", this.alcoholLevel);
        nbt.putDouble("brewery:quality", this.quality);

        var list = new NbtList();
        for (var effect : this.delayedEffects) {
            list.add(effect.toNbt());
        }
        nbt.put("brewery:delayed_effects", list);
    }

    public void readNbt(NbtCompound nbt) {
        this.alcoholLevel = nbt.getDouble("brewery:alcohol_level");
        this.quality = nbt.getDouble("brewery:quality");

        this.delayedEffects.clear();
        for (var effect : nbt.getList("brewery:delayed_effects", NbtElement.COMPOUND_TYPE)) {
            this.delayedEffects.add(DelayedEffect.fromNbt((NbtCompound) effect));
        }
    }
    
    public void tick() {
        this.delayedEffects.removeIf(this::applyDelayedEffects);

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

    private boolean applyDelayedEffects(DelayedEffect delayedEffect) {
        if ((--delayedEffect.ticksLeft) <= 0) {
            for (var effect : delayedEffect.effects) {
                effect.apply(this.entity, delayedEffect.age, delayedEffect.quality);
            }

            return true;
        }
        return false;
    }

    public void addDelayedEffect(int ticks, double drinkAge, double drinkQuality, List<ConsumptionEffect> effects) {
        this.delayedEffects.add(new DelayedEffect(ticks, drinkQuality, drinkAge, List.copyOf(effects)));
    }

    public double getModifiedAlcoholLevel() {
        return this.alcoholLevel - (this.quality - 8) * 5;
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


        public NbtCompound toNbt() {
            var nbt = new NbtCompound();
            nbt.putInt("ticks", this.ticksLeft);
            nbt.putDouble("quality", this.quality);
            nbt.putDouble("age", this.age);

            var list = new NbtList();
            for (var effect : this.effects) {
                list.add(ConsumptionEffect.CODEC.encodeStart(NbtOps.INSTANCE, effect).result().get());
            }

            nbt.put("entries", list);
            return nbt;
        }

        public static DelayedEffect fromNbt(NbtCompound nbt) {
            var ticks = nbt.getInt("ticks");
            var quality = nbt.getDouble("quality");
            var age = nbt.getDouble("age");

            var list = new ArrayList<ConsumptionEffect>();
            for (var effect : nbt.getList("entries", NbtElement.COMPOUND_TYPE)) {
                var x = ConsumptionEffect.CODEC.decode(NbtOps.INSTANCE, effect).result();
                if (x.isPresent()) {
                    list.add(x.get().getFirst());
                }
            }

            return new DelayedEffect(ticks, quality, age, list);
        }
    }
}
