package eu.pb4.brewery.other;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BrewUtils {

    public static MutableText fromTime(double seconds) {
        var secondsDis = (int) (seconds % 60);
        var minutes = (int) ((seconds % 1200) / (60));
        var days = (int) (seconds / 1200);

        var t = Text.empty();

        if (days > 0) {
            t.append(Text.translatable("text.brewery.mc_days", days));
        }

        if (minutes > 0) {
            if (!t.getSiblings().isEmpty()) {
                t.append(" ");
            }

            t.append(Text.translatable("text.brewery.minutes", minutes));
        }

        if (seconds > 0) {
            if (!t.getSiblings().isEmpty()) {
                t.append(" ");
            }

            t.append(Text.translatable("text.brewery.seconds", secondsDis));
        }

        if (t.getSiblings().isEmpty()) {
            t.append(Text.translatable("text.brewery.seconds", seconds));
        }

        return t;
    }

    public static MutableText fromTimeShort(double seconds) {
        var minutes = seconds / 60;
        var days = seconds / 1200;

        if (days >= 1) {
            return Text.translatable("text.brewery.mc_days", ((int) (days * 100)) / 100d);
        }

        if (minutes >= 1) {
            return Text.translatable("text.brewery.minutes", ((int) (minutes * 100)) / 100d);
        }

        return Text.translatable("text.brewery.seconds", (int) seconds);
    }


    public static Identifier tryParsingId(String type, Identifier fallback) {
        var x = Identifier.tryParse(type);
        return x != null ? x : fallback;
    }
}
