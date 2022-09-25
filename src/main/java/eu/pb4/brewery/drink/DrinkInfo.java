package eu.pb4.brewery.drink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.other.WrappedText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public record DrinkInfo(double bestCookingTime, double bestBarrelAge, List<String> bestBarrelType, List<WrappedText> additionalInfo) {
    public static Codec<DrinkInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.DOUBLE.optionalFieldOf("bestCookingTime", -1d).forGetter(DrinkInfo::bestCookingTime),
                Codec.DOUBLE.optionalFieldOf("bestBarrelAge", -1d).forGetter(DrinkInfo::bestBarrelAge),
                    Codec.list(Codec.STRING).optionalFieldOf("bestBarrelType", List.of()).forGetter(DrinkInfo::bestBarrelType),
                Codec.list(WrappedText.CODEC).optionalFieldOf("texts", List.of()).forGetter(DrinkInfo::additionalInfo)
            ).apply(instance, DrinkInfo::new));

    public static DrinkInfo defaults(double bestCookingTimeMinutes, double bestBarrelAgeDays, String bestBarrelType, List<Text> texts) {
        return new DrinkInfo(bestCookingTimeMinutes * 60, bestBarrelAgeDays * 1200, List.of(bestBarrelType), texts.stream().map(x -> WrappedText.of(x)).collect(Collectors.toList()));
    }

    public static DrinkInfo defaults(double bestCookingTimeMinutes, double bestBarrelAgeDays, List<Text> texts) {
        return defaults(bestCookingTimeMinutes, bestBarrelAgeDays, "*", texts);
    }
}
