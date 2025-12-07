package eu.pb4.brewery.other;

import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewGameRules {
    public static CustomGameRuleCategory CATEGORY = new CustomGameRuleCategory(id("gamerules"), Text.literal("Brewery").formatted(Formatting.YELLOW, Formatting.BOLD));

    public static GameRule<Boolean> AGE_UNLOADED = register(
            id("aging_unloaded"), GameRuleBuilder.forBoolean(true)
    );

    public static GameRule<Double> BARREL_AGING_MULTIPLIER = register(
            id("barrel_aging_multiplier"), GameRuleBuilder.forDouble(1).minValue(0d)
    );

    public static GameRule<Double> CAULDRON_COOKING_TIME_MULTIPLIER = register(
            id("cauldron_cooking_time_multiplier"), GameRuleBuilder.forDouble(1).minValue(0d)
    );

    public static GameRule<Double> ALCOHOL_MULTIPLIER = register(
            id("alcohol_value_multiplier"), GameRuleBuilder.forDouble(1).minValue(0d)
    );

    public static GameRule<Boolean> SHOW_AGE = register(
            id("show_age"), GameRuleBuilder.forBoolean(true)
    );

    public static GameRule<Boolean> SHOW_QUALITY = register(
            id("show_quality"), GameRuleBuilder.forBoolean(true)
    );

    private static <T> GameRule<T> register(Identifier identifier, GameRuleBuilder<T> t) {
        return Registry.register(Registries.GAME_RULE, identifier, t.category(CATEGORY).build());
    }

    public static void register() {
    }
}
