package eu.pb4.brewery.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import eu.pb4.brewery.duck.MobInstanceExt;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceMixin implements MobInstanceExt {
    @Unique
    private boolean brewery$locked;

    @Override
    public void brewery$setLocked(boolean value) {
        this.brewery$locked = value;
    }

    @Override
    public boolean brewery$isLocked() {
        return this.brewery$locked;
    }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<MobEffectInstance> replaceCodec(Codec<MobEffectInstance> codec) {
        return MobInstanceExt.codec(codec);
    }
}
