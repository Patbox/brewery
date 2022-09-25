package eu.pb4.brewery.other;

import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewGameRules {
    public static CustomGameRuleCategory CATEGORY = new CustomGameRuleCategory(id("gamerules"), Text.literal("Brewery").formatted(Formatting.YELLOW, Formatting.BOLD));

    public static GameRules.Key<GameRules.BooleanRule> AGE_UNLOADED = GameRuleRegistry.register(
            id("aging_unloaded").toString(), CATEGORY, GameRuleFactory.createBooleanRule(true)
    );

    public static GameRules.Key<DoubleRule> BARREL_AGING_MULTIPLIER = GameRuleRegistry.register(
            id("barrel_aging_multiplier").toString(), CATEGORY, GameRuleFactory.createDoubleRule(1, 0)
    );

    public static GameRules.Key<DoubleRule> CAULDRON_COOKING_TIME_MULTIPLIER = GameRuleRegistry.register(
            id("cauldron_cooking_time_multiplier").toString(), CATEGORY, GameRuleFactory.createDoubleRule(1, 0)
    );

    public static GameRules.Key<DoubleRule> ALCOHOL_MULTIPLIER = GameRuleRegistry.register(
            id("alcohol_value_multiplier").toString(), CATEGORY, GameRuleFactory.createDoubleRule(1, 0)
    );

    public static void register() {
    }
}
