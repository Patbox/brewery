package eu.pb4.brewery.drink;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import eu.pb4.brewery.other.TypeMapCodec;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.Optional;

public interface ConsumptionEffect extends TypeMapCodec.CodecContainer<ConsumptionEffect> {

    static void register(Identifier identifier, MapCodec<ConsumptionEffect> codec) {
        ((TypeMapCodec) CODEC).register(identifier.toString(), codec);
    }

    MapCodec<ConsumptionEffect> MAP_CODEC = new TypeMapCodec<>((self) -> {
        self.register("potion", (MapCodec<ConsumptionEffect>) (Object) Potion.CODEC);
        self.register("teleport_random", (MapCodec<ConsumptionEffect>) (Object) TeleportRandom.CODEC);
        self.register("execute_command", (MapCodec<ConsumptionEffect>) (Object) ExecuteCommand.CODEC);
        self.register("random", (MapCodec<ConsumptionEffect>) (Object) Random.CODEC);
        self.register("set_on_fire", (MapCodec<ConsumptionEffect>) (Object) SetOnFire.CODEC);
        self.register("freeze", (MapCodec<ConsumptionEffect>) (Object) Freeze.CODEC);
        self.register("set_alcohol_level", (MapCodec<ConsumptionEffect>) (Object) SetAlcoholLevel.CODEC);
        self.register("delayed", (MapCodec<ConsumptionEffect>) (Object) Delayed.CODEC);
        self.register("velocity", (MapCodec<ConsumptionEffect>) (Object) Velocity.CODEC);
        self.register("damage", (MapCodec<ConsumptionEffect>) (Object) Damage.CODEC);
    });

    Codec<ConsumptionEffect> CODEC = new MapCodec.MapCodecCodec<>(MAP_CODEC);

    static ConsumptionEffect of(StatusEffect effect, String time, String value, boolean locked) {
        return new ConsumptionEffect.Potion(effect, WrappedExpression.createDefaultCE(time), WrappedExpression.createDefaultCE(value), locked, false, true);
    }

    void apply(LivingEntity user, double age, double quality);

    MapCodec<ConsumptionEffect> codec();

    record Potion(StatusEffect effect, WrappedExpression time, WrappedExpression value, boolean locked,
                  boolean particles, boolean showIcon) implements ConsumptionEffect {
        public static MapCodec<Potion> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Registries.STATUS_EFFECT.createEntryCodec().comapFlatMap(
                                        x -> DataResult.success(x.value()),
                                        x -> Registries.STATUS_EFFECT.getEntry(Registries.STATUS_EFFECT.getRawId(x)).get())
                                .fieldOf("effect").forGetter(Potion::effect),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("time").forGetter(Potion::time),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("value", WrappedExpression.createDefaultCE("0")).forGetter(Potion::value),
                        Codec.BOOL.optionalFieldOf("locked", true).forGetter(Potion::locked),
                        Codec.BOOL.optionalFieldOf("particles", false).forGetter(Potion::particles),
                        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(Potion::showIcon)
                ).apply(instance, Potion::new));

        public static ConsumptionEffect of(StatusEffect effect, String time, String value, boolean locked, boolean particles, boolean showIcon) {
            return new ConsumptionEffect.Potion(effect, WrappedExpression.createDefaultCE(time), WrappedExpression.createDefaultCE(value), locked, particles, showIcon);
        }

        public void apply(LivingEntity user, double age, double quality) {
            var time = this.time().expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate() * 20;

            var level = this.value().expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (time >= 0 && level >= 0) {
                var instance = new StatusEffectInstance(this.effect(), (int) time, (int) level, false, this.particles, this.showIcon);
                ((StatusEffectInstanceExt) instance).brewery$setLocked(this.locked);
                user.setStatusEffect(instance, user);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record ExecuteCommand(String command, WrappedExpression apply) implements ConsumptionEffect {
        public static MapCodec<ExecuteCommand> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("command").forGetter(ExecuteCommand::command),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("apply_check", WrappedExpression.createDefaultCE("1")).forGetter(ExecuteCommand::apply)
                ).apply(instance, ExecuteCommand::new));

        public static ConsumptionEffect of(String command, String applyCheck) {
            return new ExecuteCommand(command, WrappedExpression.createDefaultCE(applyCheck));
        }

        public static ConsumptionEffect of(String command) {
            return of(command, "1");
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.apply.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                user.getServer().getCommandManager().executeWithPrefix(user.getCommandSource().withLevel(4).withOutput(CommandOutput.DUMMY), this.command);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record Random(List<ConsumptionEffect> effects, WrappedExpression apply) implements ConsumptionEffect {
        public static MapCodec<Random> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.list(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Random::effects),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("apply_check", WrappedExpression.createDefaultCE("1")).forGetter(Random::apply)
                ).apply(instance, Random::new));

        public static ConsumptionEffect of(List<ConsumptionEffect> effects, String applyCheck) {
            return new Random(effects, WrappedExpression.createDefaultCE(applyCheck));
        }

        public static ConsumptionEffect of(List<ConsumptionEffect> effects) {
            return of(effects, "1");
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.apply.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                this.effects.get(user.getRandom().nextInt(this.effects.size())).apply(user, age, quality);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record SetOnFire(WrappedExpression time) implements ConsumptionEffect {
        public static MapCodec<SetOnFire> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current")
                                .fieldOf("time").forGetter(SetOnFire::time)
                ).apply(instance, SetOnFire::new));

        public static ConsumptionEffect of(String time) {
            return new SetOnFire(WrappedExpression.create(time, ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current"));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.time.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .setVariable("current", user.getFireTicks() / 20d)
                    .evaluate() * 20;

            if (value >= 0) {
                user.setFireTicks((int) value);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record Freeze(WrappedExpression time) implements ConsumptionEffect {
        public static MapCodec<Freeze> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current")
                                .fieldOf("time").forGetter(Freeze::time)
                ).apply(instance, Freeze::new));

        public static ConsumptionEffect of(String time) {
            return new Freeze(WrappedExpression.create(time, ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current"));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.time.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .setVariable("current", user.getFireTicks() / 20d)
                    .evaluate() * 20;

            if (value >= 0) {
                user.setFrozenTicks((int) value);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record SetAlcoholLevel(WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<SetAlcoholLevel> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current").fieldOf("value").forGetter(SetAlcoholLevel::value)
                ).apply(instance, SetAlcoholLevel::new));

        public static ConsumptionEffect of(String value) {
            return new SetAlcoholLevel(WrappedExpression.create(value, ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, "current"));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.value.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .setVariable("current", AlcoholManager.of(user).alcoholLevel)
                    .evaluate();

            if (value >= 0) {
                AlcoholManager.of(user).alcoholLevel = value;
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }

    record Delayed(WrappedExpression time, List<ConsumptionEffect> effects) implements ConsumptionEffect {
        public static MapCodec<Delayed> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("delay").forGetter(Delayed::time),
                        Codec.list(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Delayed::effects)
                ).apply(instance, Delayed::new));

        public static ConsumptionEffect of(List<ConsumptionEffect> effects, String time) {
            return new Delayed(WrappedExpression.createDefaultCE(time), effects);
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.time.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate() * 20;

            if (value >= 0) {
                AlcoholManager.of(user).addDelayedEffect((int) value, age, quality, this.effects);
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
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("distance").forGetter(TeleportRandom::distance)
                ).apply(instance, TeleportRandom::new));

        public static ConsumptionEffect of(String distance) {
            return new TeleportRandom(WrappedExpression.createDefaultCE(distance));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var distance = this.distance.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (distance > 0) {
                double d = user.getX();
                double e = user.getY();
                double f = user.getZ();

                for (int i = 0; i < 16; ++i) {
                    double g = user.getX() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    double h = MathHelper.clamp(user.getY() + user.getRandom().nextDouble() - 0.5d, user.getWorld().getBottomY(), user.getWorld().getBottomY() + ((ServerWorld) user.getWorld()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    if (user.hasVehicle()) {
                        user.stopRiding();
                    }

                    Vec3d vec3d = user.getPos();
                    if (user.teleport(g, h, j, true)) {
                        user.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
                        SoundEvent soundEvent = user instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                        user.getWorld().playSound(null, d, e, f, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
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

    record Velocity(WrappedExpression x, WrappedExpression y,
                    WrappedExpression z, Optional<WrappedExpression> normalized) implements ConsumptionEffect {
        public static MapCodec<Velocity> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("x").forGetter(Velocity::x),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("y").forGetter(Velocity::y),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("z").forGetter(Velocity::z),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("normalized").forGetter(Velocity::normalized)
                ).apply(instance, Velocity::new));

        public static ConsumptionEffect of(String x, String y, String z) {
            return new Velocity(WrappedExpression.createDefaultCE(x), WrappedExpression.createDefaultCE(y), WrappedExpression.createDefaultCE(z), Optional.empty());
        }

        public static ConsumptionEffect of(String x, String y, String z, String normalized) {
            return new Velocity(WrappedExpression.createDefaultCE(x), WrappedExpression.createDefaultCE(y), WrappedExpression.createDefaultCE(z), Optional.of(WrappedExpression.createDefaultCE(normalized)));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var x = this.x.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            var y = this.y.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            var z = this.z.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (x != 0 || y != 0 || z != 0) {
                if (this.normalized.isPresent()) {
                    var value = this.normalized.get().expression()
                            .setVariable(ExpressionUtil.AGE_KEY, age)
                            .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                            .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                            .evaluate();

                    if (value == 0) {
                        return;
                    }
                    var vec = new Vec3d(x, y, z).normalize().multiply(value);
                    user.addVelocity(vec.x, vec.y, vec.z);
                } else {
                    user.addVelocity(x, y, z);
                }

                if (user instanceof ServerPlayerEntity player) {
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                }
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }


    record Damage(RegistryEntry<DamageType> type, WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<Damage> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        RegistryFixedCodec.of(RegistryKeys.DAMAGE_TYPE).fieldOf("id").forGetter(Damage::type),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("value").forGetter(Damage::value)
                ).apply(instance, Damage::new));
        public static ConsumptionEffect of(MinecraftServer server, RegistryKey<DamageType> type, String value) {
            return new Damage(server.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(type).get(), WrappedExpression.createDefaultCE(value));
        }

        public static ConsumptionEffect of(MinecraftServer server, Identifier type, String value) {
            return new Damage(server.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, type)).get(), WrappedExpression.createDefaultCE(value));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.value.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                var source = new DamageSource(type);

                user.damage(source, (float) value);
            }
        }

        @Override
        public MapCodec<ConsumptionEffect> codec() {
            return (MapCodec<ConsumptionEffect>) (Object) CODEC;
        }
    }
}
