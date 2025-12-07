package eu.pb4.brewery.item.comp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record BrewData(Optional<Identifier> type, double quality, String barrelType, int distillations, double age) {
    public static final Codec<BrewData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.lenientOptionalFieldOf("type").forGetter(BrewData::type),
            Codec.DOUBLE.optionalFieldOf("quality", 10d).forGetter(BrewData::quality),
            Codec.STRING.optionalFieldOf("barrel", "").forGetter(BrewData::barrelType),
            Codec.INT.optionalFieldOf("distillation_runs", 0).forGetter(BrewData::distillations),
            Codec.DOUBLE.optionalFieldOf("age", 0d).forGetter(BrewData::age)
    ).apply(instance, BrewData::new));
    public static final BrewData DEFAULT = new BrewData(Optional.empty(), 0, "", 0, 0);

    public BrewData distillate() {
        return new BrewData(type, quality, barrelType, distillations + 1, age);
    }

    public BrewData withAge(double age) {
        return new BrewData(type, quality, barrelType, distillations, age);
    }
}
