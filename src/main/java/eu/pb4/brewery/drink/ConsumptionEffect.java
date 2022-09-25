package eu.pb4.brewery.drink;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.event.GameEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public interface ConsumptionEffect {
    static ConsumptionEffect of(StatusEffect effect, String time, String value) {
        return new ConsumptionEffect.Potion(effect, WrappedExpression.createDefault(time), WrappedExpression.createDefault(value));
    }

    Codec<ConsumptionEffect> CODEC = new MapCodec.MapCodecCodec<>(new MapCodec<>() {
        private static BiMap<String, MapCodec<ConsumptionEffect>> SUB_CODECS = HashBiMap.create();

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("type"));
        }

        @Override
        public <T> DataResult<ConsumptionEffect> decode(DynamicOps<T> ops, MapLike<T> input) {
            var type = ops.getStringValue(input.get("type")).result();

            if (type.isEmpty()) {
                return DataResult.error("Missing type");
            }

            var data = SUB_CODECS.get(type.get().toLowerCase(Locale.ROOT));
            if (data != null) {
                return data.decode(ops, input);
            }

            return DataResult.error("Invalid type");
        }

        @Override
        public <T> RecordBuilder<T> encode(ConsumptionEffect input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return input.codec().encode(input, ops, prefix.add("type", ops.createString(SUB_CODECS.inverse().get(input.codec()))));
        }

        static {
            SUB_CODECS.put("potion", (MapCodec<ConsumptionEffect>) (Object) Potion.CODEC);
            SUB_CODECS.put("teleport_random", (MapCodec<ConsumptionEffect>) (Object) TeleportRandom.CODEC);
        }
    });

    void apply(LivingEntity user, double age, double quality);

    MapCodec<ConsumptionEffect> codec();

    record Potion(StatusEffect effect, WrappedExpression time, WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<Potion> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Registry.STATUS_EFFECT.createEntryCodec().comapFlatMap(
                                        x -> DataResult.success(x.value()),
                                        x -> Registry.STATUS_EFFECT.getEntry(Registry.STATUS_EFFECT.getRawId(x)).get())
                                .fieldOf("effect").forGetter(Potion::effect),
                        ExpressionUtil.COMMON_EXPRESSION.fieldOf("time").forGetter(Potion::time),
                        ExpressionUtil.COMMON_EXPRESSION.fieldOf("value").forGetter(Potion::value)
                ).apply(instance, Potion::new));

        public void apply(LivingEntity user, double age, double quality) {
            var time = this.time().expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .evaluate() * 20;

            var level = this.value().expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .evaluate();

            if (time >= 0 && level >= 0) {
                user.setStatusEffect(new StatusEffectInstance(this.effect(), (int) time, (int) level), user);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }


    record TeleportRandom(WrappedExpression distance) implements ConsumptionEffect {
        public static MapCodec<TeleportRandom> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_EXPRESSION.fieldOf("distance").forGetter(TeleportRandom::distance)
                ).apply(instance, TeleportRandom::new));

        public void apply(LivingEntity user, double age, double quality) {
            var distance = this.distance.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .evaluate();

            if (distance > 0) {
                double d = user.getX();
                double e = user.getY();
                double f = user.getZ();

                for(int i = 0; i < 16; ++i) {
                    double g = user.getX() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    double h = MathHelper.clamp(user.getY() + user.getRandom().nextDouble() - 0.5d, user.world.getBottomY(), user.world.getBottomY() + ((ServerWorld)user.world).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    if (user.hasVehicle()) {
                        user.stopRiding();
                    }

                    Vec3d vec3d = user.getPos();
                    if (user.teleport(g, h, j, true)) {
                        user.world.emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
                        SoundEvent soundEvent = user instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                        user.world.playSound(null, d, e, f, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        user.playSound(soundEvent, 1.0F, 1.0F);
                        break;
                    }
                }
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }
}
