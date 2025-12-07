package eu.pb4.brewery.drink;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.duck.MobInstanceExt;
import eu.pb4.brewery.other.FloatSelector;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ConsumptionEffect {

    static void register(Identifier identifier, MapCodec<ConsumptionEffect> codec) {
        EFFECTS.put(identifier.toString(), codec);
    }
    Codec<ConsumptionEffect> CODEC = Codec.lazyInitialized(() -> ConsumptionEffect.EFFECTS.codec(Codec.STRING).dispatch(ConsumptionEffect::codec, Function.identity()));
    ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ConsumptionEffect>> EFFECTS = Util.make(new ExtraCodecs.LateBoundIdMapper<>(), self -> {
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

    static ConsumptionEffect of(Holder<MobEffect> effect, String time, String value, boolean locked) {
        return new ConsumptionEffect.Potion(effect, WrappedExpression.createDefaultCE(time), WrappedExpression.createDefaultCE(value), locked, false, true);
    }

    void apply(LivingEntity user, double age, double quality);

    MapCodec<? extends ConsumptionEffect> codec();

    record Potion(Holder<MobEffect> effect, WrappedExpression time, WrappedExpression value, boolean locked,
                  boolean particles, boolean showIcon) implements ConsumptionEffect {
        public static MapCodec<Potion> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Potion::effect),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("time").forGetter(Potion::time),
                        ExpressionUtil.COMMON_CE_EXPRESSION.optionalFieldOf("value", WrappedExpression.createDefaultCE("0")).forGetter(Potion::value),
                        Codec.BOOL.optionalFieldOf("locked", true).forGetter(Potion::locked),
                        Codec.BOOL.optionalFieldOf("particles", false).forGetter(Potion::particles),
                        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(Potion::showIcon)
                ).apply(instance, Potion::new));

        public static ConsumptionEffect of(Holder<MobEffect> effect, String time, String value, boolean locked, boolean particles, boolean showIcon) {
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
                var instance = new MobEffectInstance(this.effect(), (int) time, (int) level, false, this.particles, this.showIcon);
                ((MobInstanceExt) instance).brewery$setLocked(this.locked);
                user.forceAddEffect(instance, user);
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
                user.level().getServer().getCommands().performPrefixedCommand(user.createCommandSourceStackForNameResolution((ServerLevel) user.level())
                        .withPermission(x -> true).withSource(CommandSource.NULL), this.command);
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
                        ExtraCodecs.compactListCodec(ConsumeEffect.CODEC).fieldOf("entries").forGetter(ConsumeEffects::effects),
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
                    effect.apply(user.level(), ItemStack.EMPTY, user);
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
                        ExtraCodecs.compactListCodec(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Random::effects),
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
                    .setVariable("current", user.getRemainingFireTicks() / 20d)
                    .evaluate() * 20;

            if (value >= 0) {
                user.setRemainingFireTicks((int) value);
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
                    .setVariable("current", user.getRemainingFireTicks() / 20d)
                    .evaluate() * 20;

            if (value >= 0) {
                user.setTicksFrozen((int) value);
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
                        FloatSelector.createQualityCodec(ExtraCodecs.compactListCodec(ConsumptionEffect.CODEC), null).fieldOf("entries").forGetter(QualitySelect::effects)
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
                        ExtraCodecs.compactListCodec(ConsumptionEffect.CODEC).fieldOf("entries").forGetter(Delayed::effects)
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

    record Attributes(WrappedExpression time, List<Pair<Holder<Attribute>, AttributeModifier>> effects) implements ConsumptionEffect {
        public static MapCodec<Pair<Holder<Attribute>, AttributeModifier>> ATTRIBUTE_PAIR = Codec.mapPair(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("type"),
                AttributeModifier.MAP_CODEC);


        public static MapCodec<Attributes> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("time").forGetter(Attributes::time),
                        Codec.list(ATTRIBUTE_PAIR.codec()).fieldOf("entries").forGetter(Attributes::effects)
                ).apply(instance, Attributes::new));

        public static ConsumptionEffect of(List<Pair<Holder<Attribute>, AttributeModifier>> effects, String time) {
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
                    double h = Mth.clamp(user.getY() + user.getRandom().nextDouble() - 0.5d, user.level().getMinY(), user.level().getMinY() + ((ServerLevel) user.level()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5D) * distance;
                    Vec3 vec3d = user.position();
                    if (user.randomTeleport(g, h, j, true)) {
                        user.level().gameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Context.of(user));
                        SoundEvent soundEvent = user instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
                        user.level().playSound(null, d, e, f, soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
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
                Vec3 vec;
                if (this.normalized.isPresent()) {
                    var value = this.normalized.get().expression()
                            .setVariable(ExpressionUtil.AGE_KEY, age)
                            .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                            .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                            .evaluate();

                    if (value == 0) {
                        return;
                    }
                    vec = new Vec3(x, y, z).normalize().scale(Math.min(value, 100));
                    user.push(vec.x, vec.y, vec.z);
                } else {
                    vec = new Vec3(x, y, z);
                    vec = vec.normalize().scale(Math.min(vec.length(), 100));
                    user.push(vec.x, vec.y, vec.z);
                }

                if (user instanceof ServerPlayer player) {
                    //player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                    player.connection.send(new ClientboundExplodePacket(player.position().add(0, -99999, 0), 0, 0, Optional.of(vec), ParticleTypes.UNDERWATER, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), WeightedList.of()));
                }
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }


    record Damage(Holder<DamageType> type, WrappedExpression value) implements ConsumptionEffect {
        public static MapCodec<Damage> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        RegistryFixedCodec.create(Registries.DAMAGE_TYPE).fieldOf("id").forGetter(Damage::type),
                        ExpressionUtil.COMMON_CE_EXPRESSION.fieldOf("value").forGetter(Damage::value)
                ).apply(instance, Damage::new));
        public static ConsumptionEffect of(MinecraftServer server, ResourceKey<DamageType> type, String value) {
            return new Damage(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), WrappedExpression.createDefaultCE(value));
        }

        public static ConsumptionEffect of(MinecraftServer server, Identifier type, String value) {
            return new Damage(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, type)), WrappedExpression.createDefaultCE(value));
        }

        public void apply(LivingEntity user, double age, double quality) {
            var value = this.value.expression()
                    .setVariable(ExpressionUtil.AGE_KEY, age)
                    .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                    .setVariable(ExpressionUtil.USER_ALCOHOL_LEVEL_KEY, AlcoholManager.of(user).getModifiedAlcoholLevel())
                    .evaluate();

            if (value >= 0) {
                var source = new DamageSource(type);

                user.hurtServer((ServerLevel) user.level(), source, (float) value);
            }
        }

        @Override
        public MapCodec<? extends ConsumptionEffect> codec() {
            return CODEC;
        }
    }
}
