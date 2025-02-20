package eu.pb4.brewery.other;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record FloatSelector<T>(List<Entry<T>> entries) {

    public T select(float value) {
        var diff = Float.POSITIVE_INFINITY;
        Entry<T> curr = this.entries.getLast();
        for (var entry : entries) {
            var d = Math.abs(entry.valueForPass - value);
            if (d < diff) {
                curr = entry;
                diff = d;
            }
        }

        return curr.result();
    }


    public static <T> FloatSelector<T> of(T result) {
        return new FloatSelector<>(List.of(Entry.of(result)));
    }

    public static <T> FloatSelector<T> of(float min, float max, T... results) {
        var list = new ArrayList<Entry<T>>(results.length);
        for (int i = 0; i < results.length; i++) {
            list.add(new Entry<>(min + (max - min) * (i) / (results.length - 1), results[i]));
        }
        return new FloatSelector<>(list);
    }

    public static <T> Codec<FloatSelector<T>> createSingularCodec(Codec<T> codec) {
        return codec.xmap(FloatSelector::of, x -> x.entries.getFirst().result);
    }

    public static <T> Codec<FloatSelector<T>> createQualityCodec(Codec<T> codec, T defaultValue) {
        return createCodec(0, 10, codec, defaultValue);
    }

    public static <T> Codec<FloatSelector<T>> createCodec(float min, float max, Codec<T> codec, T defaultValue) {
        var entryFull = RecordCodecBuilder.<Entry<T>>create(instaince -> instaince.group(
                Codec.FLOAT.optionalFieldOf("for", Float.NEGATIVE_INFINITY).forGetter(Entry<T>::valueForPass),
                codec.fieldOf("result").forGetter(Entry<T>::result)
        ).apply(instaince, Entry<T>::new));

        var entryFullList = entryFull.listOf();
        var entryList = Codec.either(entryFull, codec).listOf();
        return new Codec<>() {
            @Override
            public <D> DataResult<Pair<FloatSelector<T>, D>> decode(DynamicOps<D> ops, D input) {
                var value = codec.decode(ops, input);
                if (value.isSuccess()) {
                    return value.map(x -> x.mapFirst(FloatSelector::of));
                }

                var result = entryList.decode(ops, input);
                if (result.isError() && defaultValue != null) {
                    return DataResult.success(new Pair<>(FloatSelector.of(defaultValue), input));
                }

                return result.map(x -> x.mapFirst(list -> {
                    var out = new ArrayList<Entry<T>>();
                    for (int i = 0; i < list.size(); i++) {
                        var either = list.get(i);
                        if (either.left().isPresent()) {
                            out.add(either.left().get());
                        } else {
                            if (i == 0) {
                                out.add(new Entry<>(min, either.right().get()));
                            } else if (i == list.size() - 1) {
                                out.add(new Entry<>(max, either.right().get()));
                            } else {
                                float previous = out.get(i - 1).valueForPass;
                                float nextValue = max;
                                int nextPos = list.size() - 1;
                                for (int b = i + 1; b < list.size(); b++) {
                                    var next = list.get(b);
                                    if (next.left().isPresent()) {
                                        nextValue = next.left().get().valueForPass;
                                        nextPos = b;
                                    }
                                }
                                float val = (nextValue - previous) / (nextPos - i + 1);
                                for (; i < nextPos; i++) {
                                    var next = list.get(i);
                                    //noinspection OptionalGetWithoutIsPresent
                                    out.add(new Entry<>(previous + val * i, next.right().get()));
                                }
                                i--;
                            }
                        }
                    }

                    out.sort(Comparator.comparing(Entry::valueForPass));
                    return new FloatSelector<>(out);
                }));
            }

            @Override
            public <D> DataResult<D> encode(FloatSelector<T> input, DynamicOps<D> ops, D prefix) {
                if (input.entries.size() == 1) {
                    return codec.encode(input.entries.getFirst().result, ops, prefix);
                }

                return entryFullList.encode(input.entries(), ops, prefix);
            }
        };
    }

    public record Entry<T>(float valueForPass, T result) {
        public static <T> Entry<T> of(T result) {
            return new Entry<>(Float.NEGATIVE_INFINITY, result);
        }
    };
}
