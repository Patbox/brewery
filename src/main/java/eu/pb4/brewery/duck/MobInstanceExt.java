package eu.pb4.brewery.duck;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.effect.MobEffectInstance;

public interface MobInstanceExt {
    static Codec<MobEffectInstance> codec(Codec<MobEffectInstance> codec) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<MobEffectInstance, T>> decode(DynamicOps<T> ops, T input) {
                var decoded = codec.decode(ops, input);

                if (decoded.result().isPresent()) {
                    ops.get(input, "brewery:locked").result().ifPresent(x -> {
                         ((MobInstanceExt) decoded.getOrThrow().getFirst()).brewery$setLocked(ops.getBooleanValue(x).getOrThrow());
                    });
                }

                return decoded;
            }

            @Override
            public <T> DataResult<T> encode(MobEffectInstance input, DynamicOps<T> ops, T prefix) {
                var encoded = codec.encode(input, ops, prefix);

                if (encoded.result().isPresent() && ((MobInstanceExt) input).brewery$isLocked()) {
                    ops.set(encoded.getOrThrow(), "brewery:locked", ops.createBoolean(true));
                }

                return encoded;
            }
        };
    }

    void brewery$setLocked(boolean value);
    boolean brewery$isLocked();
}
