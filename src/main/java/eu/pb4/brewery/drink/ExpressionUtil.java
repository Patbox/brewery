package eu.pb4.brewery.drink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.objecthunter.exp4j.function.Function;

public class ExpressionUtil {
    public static final String AGE_KEY = "age";
    public static final String QUALITY_KEY = "quality";
    public static final Codec<WrappedExpression> COMMON_EXPRESSION = ExpressionUtil.createCodec(AGE_KEY, QUALITY_KEY);
    public static final Function[] FUNCTIONS = new Function[] {
            new Function("max", 2) {
                @Override
                public double apply(double... args) {
                    return Math.max(args[0], args[1]);
                }
            },
            new Function("min", 2) {
                @Override
                public double apply(double... args) {
                    return Math.min(args[0], args[1]);
                }
            },
            new Function("clamp", 3) {
                @Override
                public double apply(double... args) {
                    if (args[0] < args[1]) {
                        return args[1];
                    } else if (args[0] > args[2]) {
                        return args[2];
                    } else {
                        return args[0];
                    }
                }
            },
            new Function("defaultQualityFunction", 4) {
                @Override
                public double apply(double... args) {
                    var day = args[3] / 1200;

                    if (day <= 0) {
                        return 0;
                    } else if (day >= (args[0] + args[1] + args[2])) {
                        return -1;
                    } else if (day >= args[0] && day <= args[0] + args[1]) {
                        return 10;
                    } else {
                        double x;

                        if (day < args[0]) {
                            x = day / (Math.max(args[0], 0.001));
                        } else {
                            x = (args[2] + args[0] + args[1] - day) / (Math.max(args[2], 0.001));
                        }

                        return (3 * x * x - 2 * x * x * x) * 10;
                    }
                }
            }
    };

    public static String defaultQuality(double mcDaysIdeal, double lockedTime, double qualityFalloff) {
        return "defaultQualityFunction(" + mcDaysIdeal + ", " + lockedTime + ", " + qualityFalloff + ", age)";
    }

    public static String defaultQuality(double mcDaysIdeal, double lockedTime) {
        return "defaultQualityFunction(" + mcDaysIdeal + ", " + lockedTime + ", " + mcDaysIdeal + ", age)";
    }

    public static String defaultBoiling(double minutes, double percent) {
        return "cos(clamp((age / 60 - " + minutes + ") * " + percent + ", -2, 2))";
    }

    public static Codec<WrappedExpression> createCodec(String... args) {
        return Codec.STRING.comapFlatMap(string -> DataResult.success(WrappedExpression.create(string, args)), x -> x.input());
    }
}
