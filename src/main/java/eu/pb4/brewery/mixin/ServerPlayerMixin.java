package eu.pb4.brewery.mixin;

import eu.pb4.brewery.duck.LivingEntityExt;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void  brewery$copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        if (alive) {
            ((LivingEntityExt) this).brewery$setAlcoholManager(((LivingEntityExt) oldPlayer).brewery$getAlcoholManager());
        }
    }
}
