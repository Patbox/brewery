package eu.pb4.brewery.drink;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import eu.pb4.brewery.other.FloatSelector;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ConsumptionEffect {

    static void register(Identifier identifier, MapCodec<ConsumptionEffect> codec) {
        EFFECTS.put(identifier.toString(), codec);
    }
    Codec<ConsumptionEffect> CODEC = Codec.lazyInitialized(() -> ConsumptionEffect.EFFECTS.getCodec(Codec.STRING).dispatch(ConsumptionEffect::codec, Function.identity()));
    Codecs.IdMapper<String, MapCodec<? extends ConsumptionEffect>> EFFECTS = Util.make(new Codecs.IdMapper<>(), self -> {
        self.put("potion", Potion.CODEC);
        self.put("teleport_random", TeleportRandom.CODEC);
        self.put("execute_command", ExecuteCommand.CODEC);
        self.put("random", Random.CODEC);
        self.put("set_on_fire", SetOnFire.CODEC);
        self.put("freeze", Freeze.CODEC);
        self.put("set_alcohol_level", SetAlcoholLevel.CODEC);
        self.put("add_alcohol_level", AddAlcoholLevel.CODEC);
        self.put("delayed", Delayed.CODEC);
        self.put("attributes", Attributes.CODEC);
        self.put("consume_effects", ConsumeEffects.CODEC);
        self.put("velocity", Velocity.CODEC);
        self.put("damage", Damage.CODEC);
        self.put("quality_select", QualitySelect.CODEC);
    });

    static ConsumptionEffect of(RegistryEntry<StatusEffect> effect, String time, String value, boolean locked) {
        return new ConsumptionEffect.Potion(effect, WrappedExpression.createDefaultCE(time), WrappedExpression.createDefaultCE(value), locked, false, true);
    }

    void apply(LivingEntity user, double age, double quality);

    MapCodec<? extends ConsumptionEffect> codec();

    record Potion(RegistryEntry<StatusEffect> effect, WrappedExpression time, WrappedExpression value, boolean locked,
                  boolean particles, boolean showIcon) implements ConsumptionEffect {
        public static MapCodec<Potion> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Registries.STATUS_EFFECT.getEntryCodec().fieldOf("effect").forGetter(Potion::effect),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("time").forGetter(Potion::time),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("value", WrappedExpression.createDefaultCE("0")).forGetter(Potion::value),
                        Codec.BOOL.optionalFieldOf("locked", true).forGetter(Potion::locked),
                        Codec.BOOL.optionalFieldOf("particles", false).forGetter(Potion::particles),
                        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(Potion::showIcon)
                ).apply(instance, Potion::new));

        public static ConsumptionEffect of(RegistryEntry<StatusEffect> effect, String time, String value, boolean locked, boolean particles, boolean showIcon) {
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
                user.getEntityWorld().getServer().getCommandManager().executeWithPrefix(user.getCommandSource((ServerWorld) user.getEntityWorld()).withLevel(4).withOutput(CommandOutput.DUMMY), this.command);
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record ConsumeEffects(List<ConsumeEffect> effects, WrappedExpression apply) implements ConsumptionEffect {
        public static MapCodec<ConsumeEffects> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codecs.listOrSingle(ConsumeEffect.CODEC).fieldOf("entries").forGetter(ConsumeEffects::effects),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("apply_check", WrappedExpression.createDefaultCE("1")).forGetter(ConsumeEffects::apply)
                ).apply(instance, ConsumeEffects::new));

        public static ConsumptionEffect of(List<ConsumeEffect> effects, String applyCheck) {
            return new ConsumeEffects(effects, WrappedExpression.createDefaultCE(applyCheck));
        }

        public static ConsumptionEffect of(List<ConsumeEffect> effects) {
            return of(effects, "1");
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.apply.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                for (var effect : this.effects) {
                    effect.onConsume(user.getEntityWorld(), ItemStack.EMPTY, user);
                }
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record Random(List<ConsumptionEffect> effects, WrappedExpression apply) implements ConsumptionEffect {
        public static MapCodec<Random> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codecs.listOrSingle(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Random::effects),
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record AddAlcoholLevel(WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<AddAlcoholLevel> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, "current").fieldOf("value").forGetter(AddAlcoholLevel::value)
                ).apply(instance, AddAlcoholLevel::new));

        public static ConsumptionEffect of(String value) {
            return new AddAlcoholLevel(WrappedExpression.create(value, ExpressionUtil.AGE_KEY, ExpressionUtil.QUALITY_KEY, "current"));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.value.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .setVariable("current", AlcoholManager.of(user).alcoholLevel)
                    .evaluate();

            AlcoholManager.of(user).alcoholLevel = Math.max(AlcoholManager.of(user).alcoholLevel + value, 0);
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record QualitySelect(FloatSelector<List<ConsumptionEffect>> effects) implements ConsumptionEffect {
        public static MapCodec<QualitySelect> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        FloatSelector.createQualityCodec(Codecs.listOrSingle(ConsumptionEffect.CODEC), null).fieldOf("entries").forGetter(QualitySelect::effects)
                ).apply(instance, QualitySelect::new));

        public void apply(LivingEntity user, double age, double quality) {
            for (var effect : this.effects.select((float) quality)) {
                effect.apply(user, age, quality);
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record Delayed(WrappedExpression time, List<ConsumptionEffect> effects) implements ConsumptionEffect {
        public static MapCodec<Delayed> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("delay").forGetter(Delayed::time),
                        Codecs.listOrSingle(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Delayed::effects)
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
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }

    record Attributes(WrappedExpression time, List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> effects) implements ConsumptionEffect {
        public static MapCodec<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> ATTRIBUTE_PAIR = Codec.mapPair(
                Registries.ATTRIBUTE.getEntryCodec().fieldOf("type"),
                EntityAttributeModifier.MAP_CODEC);


        public static MapCodec<Attributes> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("time").forGetter(Attributes::time),
                        Codec.list(ATTRIBUTE_PAIR.codec()).fieldOf("entries").forGetter(Attributes::effects)
                ).apply(instance, Attributes::new));

        public static ConsumptionEffect of(List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> effects, String time) {
            return new Attributes(WrappedExpression.createDefaultCE(time), effects);
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.time.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate() * 20;

            if (value >= 0) {
                AlcoholManager.of(user).addTimedAttributes((int) value, age, quality, this.effects);
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
                    double h = MathHelper.clamp(user.getY() + user.getRandom().nextDouble() - 0.5d, user.getEntityWorld().getBottomY(), user.getEntityWorld().getBottomY() + ((ServerWorld) user.getEntityWorld()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    Vec3d vec3d = user.getEntityPos();
                    if (user.teleport(g, h, j, true)) {
                        user.getEntityWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
                        SoundEvent soundEvent = user instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                        user.getEntityWorld().playSound(null, d, e, f, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        user.playSound(soundEvent, 1.0F, 1.0F);
                        break;
                    }
                }
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
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
                Vec3d vec;
                if (this.normalized.isPresent()) {
                    var value = this.normalized.get().expression()
                            .setVariable(ExpressionUtil.AGE_KEY, age)
                            .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                            .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                            .evaluate();

                    if (value == 0) {
                        return;
                    }
                    vec = new Vec3d(x, y, z).normalize().multiply(Math.min(value, 100));
                    user.addVelocity(vec.x, vec.y, vec.z);
                } else {
                    vec = new Vec3d(x, y, z);
                    vec = vec.normalize().multiply(Math.min(vec.length(), 100));
                    user.addVelocity(vec.x, vec.y, vec.z);
                }

                if (user instanceof ServerPlayerEntity player) {
                    //player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                    player.networkHandler.sendPacket(new ExplosionS2CPacket(player.getEntityPos().add(0, -99999, 0), 0, 0, Optional.of(vec), ParticleTypes.UNDERWATER, Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY), Pool.empty()));
                }
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }


    record Damage(RegistryEntry<DamageType> type, WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<Damage> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        RegistryFixedCodec.of(RegistryKeys.DAMAGE_TYPE).fieldOf("id").forGetter(Damage::type),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("value").forGetter(Damage::value)
                ).apply(instance, Damage::new));
        public static ConsumptionEffect of(MinecraftServer server, RegistryKey<DamageType> type, String value) {
            return new Damage(server.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(type), WrappedExpression.createDefaultCE(value));
        }

        public static ConsumptionEffect of(MinecraftServer server, Identifier type, String value) {
            return new Damage(server.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, type)), WrappedExpression.createDefaultCE(value));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.value.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                var source = new DamageSource(type);

                user.damage((ServerWorld) user.getEntityWorld(), source, (float) value);
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }
}
