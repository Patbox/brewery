package eu.pb4.brewery.other;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class BrewUtils {

    public static MutableComponent fromTime(double seconds) {
        var secondsDis = (int) (seconds % 60);
        var minutes = (int) ((seconds % 1200) / (60));
        var days = (int) (seconds / 1200);

        var t = Component.empty();

        if (days > 0) {
            t.append(Component.translatable("text.brewery.mc_days", days));
        }

        if (minutes > 0) {
            if (!t.getSiblings().isEmpty()) {
                t.append(" ");
            }

            t.append(Component.translatable("text.brewery.minutes", minutes));
        }

        if (seconds > 0) {
            if (!t.getSiblings().isEmpty()) {
                t.append(" ");
            }

            t.append(Component.translatable("text.brewery.seconds", secondsDis));
        }

        if (t.getSiblings().isEmpty()) {
            t.append(Component.translatable("text.brewery.seconds", seconds));
        }

        return t;
    }

    public static MutableComponent fromTimeShort(double seconds) {
        var minutes = seconds / 60;
        var days = seconds / 1200;

        if (days >= 1) {
            return Component.translatable("text.brewery.mc_days", ((int) (days * 100)) / 100d);
        }

        if (minutes >= 1) {
            return Component.translatable("text.brewery.minutes", ((int) (minutes * 100)) / 100d);
        }

        return Component.translatable("text.brewery.seconds", (int) seconds);
    }


    public static Identifier tryParsingId(String type, Identifier fallback) {
        var x = Identifier.tryParse(type);
        return x != null ? x : fallback;
    }
}
