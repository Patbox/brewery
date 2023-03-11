package eu.pb4.brewery.other;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.*;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TypeMapCodec<A extends TypeMapCodec.CodecContainer<A>> extends MapCodec<A > {
    private final BiMap<String, MapCodec<A>> codecs = HashBiMap.create();
    private Consumer<TypeMapCodec<A>> initConsumer;

    public TypeMapCodec() {}
    public TypeMapCodec(Consumer<TypeMapCodec<A>> consumer) {
        this.initConsumer = consumer;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString("type"));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        if (this.initConsumer != null) {
            this.initConsumer.accept(this);
        }
        var type = ops.getStringValue(input.get("type")).result();

        if (type.isEmpty()) {
            return DataResult.error(() -> "Missing type");
        }

        try {
            var data = codecs.get(type.get().toLowerCase(Locale.ROOT));
            if (data != null) {
                return data.decode(ops, input);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }


        return DataResult.error(() -> "Invalid type");
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (this.initConsumer != null) {
            this.initConsumer.accept(this);
        }
        return input.codec().encode(input, ops, prefix.add("type", ops.createString(codecs.inverse().get(input.codec()))));
    }

    public interface CodecContainer<A> {
        MapCodec<A> codec();
    }

    public void register(String string, MapCodec<A> codec) {
        this.codecs.put(string, codec);
    }
}
