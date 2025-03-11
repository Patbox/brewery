package eu.pb4.brewery.mixin;

import eu.pb4.brewery.BreweryInit;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Registries.class)
public class RegistriesMixin {
    @Inject(method = "freezeRegistries", at = @At("TAIL"))
    private static void loadModelData(CallbackInfo ci) {
        BreweryInit.loadStaticResPackData();
    }
}
