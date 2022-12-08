package eu.pb4.brewery.drink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.List;
import java.util.Map;

public record AlcoholValueEffect(boolean replace, List<Value> entries, Map<Item, Double> itemReduction) {
    public static Codec<AlcoholValueEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("replace").forGetter(AlcoholValueEffect::replace),
                    Codec.list(Value.CODEC).fieldOf("entries").forGetter(AlcoholValueEffect::entries),
                    Codec.unboundedMap(Registries.ITEM.getCodec(), Codec.DOUBLE).optionalFieldOf("alcohol_reduction_items", Map.of()).forGetter(AlcoholValueEffect::itemReduction)

            ).apply(instance, AlcoholValueEffect::new)
    );


    public record Value(double minimumValue, int rate, List<ConsumptionEffect> effects) {
        public static Codec<Value> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("minimumValue").forGetter(Value::minimumValue),
                        Codec.DOUBLE.fieldOf("rate").xmap(x -> (int) (x.doubleValue() * 20), x -> x / 20d).forGetter(Value::rate),
                        Codec.list(ConsumptionEffect.CODEC).fieldOf("effects").forGetter(Value::effects)
                ).apply(instance, Value::new)
        );
    }
}
