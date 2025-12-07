package eu.pb4.brewery.mixin.datafixer;

import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Inject(method = "fixItemStack", at = @At("TAIL"))
    private static void fixCustomStacks(ItemStackComponentizationFix.ItemStackData data, Dynamic dynamic, CallbackInfo ci) {
        if (data.is("brewery:ingredient_mixture") || data.is("brewery:drink_bottle")) {
            data.setComponent("brewery:cooking_data", dynamic.emptyMap()
                    .set("time", data.removeTag("BrewCookAge").result().orElse(dynamic.createDouble(0)))
                    .set("heat_source", data.removeTag("BrewHeatSource").result().orElse(dynamic.createString("air")))
                    .set("ingredients", data.removeTag("Ingredients").result().orElse(dynamic.emptyList()))
            );
        }
        if (data.is("brewery:drink_bottle")) {
            data.setComponent("brewery:brew_data", dynamic.emptyMap()
                    .set("age", data.removeTag("BrewAge").result().orElse(dynamic.createDouble(0)))
                    .set("quality", data.removeTag("BrewQuality").result().orElse(dynamic.createDouble(0)))
                    .set("barrel", data.removeTag("BrewBarrelType").result().orElse(dynamic.createString("")))
                    .set("type", data.removeTag("BrewType").result().orElse(dynamic.createString("")))
                    .set("distillation_runs", data.removeTag("BrewDistillated").result().orElse(dynamic.createInt(0)))
            );
        }
    }
}
