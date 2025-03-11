package eu.pb4.brewery.other;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class BCodecs {
    public static <E> Codec<List<E>> listOrSingle(Codec<E> entryCodec) {
        return listOrSingle(entryCodec, entryCodec.listOf());
    }

    public static <E> Codec<List<E>> listOrSingle(Codec<E> entryCodec, Codec<List<E>> listCodec) {
        return Codec.either(listCodec, entryCodec).xmap((either) -> {
            return (List)either.map((list) -> {
                return list;
            }, List::of);
        }, (list) -> {
            return list.size() == 1 ? Either.right(list.get(0)) : Either.left(list);
        });
    }

    public static <I, E> Codec<E> idChecked(Codec<I> idCodec, Function<I, E> idToElement, Function<E, I> elementToId) {
        return idCodec.flatXmap((id) -> {
            E object = idToElement.apply(id);
            return object == null ? DataResult.error(() -> {
                return "Unknown element id: " + String.valueOf(id);
            }) : DataResult.success(object);
        }, (element) -> {
            I object = elementToId.apply(element);
            return object == null ? DataResult.error(() -> {
                return "Element with unknown id: " + String.valueOf(element);
            }) : DataResult.success(object);
        });
    }

    public static class IdMapper<I, V> {
        private final BiMap<I, V> values = HashBiMap.create();

        public IdMapper() {
        }

        public Codec<V> getCodec(Codec<I> idCodec) {
            BiMap<V, I> biMap = this.values.inverse();
            BiMap var10001 = this.values;
            Objects.requireNonNull(var10001);
            Function var3 = var10001::get;
            Objects.requireNonNull(biMap);
            return BCodecs.idChecked(idCodec, var3, biMap::get);
        }

        public IdMapper<I, V> put(I id, V value) {
            Objects.requireNonNull(value, () -> {
                return "Value for " + String.valueOf(id) + " is null";
            });
            this.values.put(id, value);
            return this;
        }
    }
}
