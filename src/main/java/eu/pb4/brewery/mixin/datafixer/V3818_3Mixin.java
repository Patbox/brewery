package eu.pb4.brewery.mixin.datafixer;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V3818_3;

@Mixin(V3818_3.class)
public class V3818_3Mixin {
    @Inject(method = "components", at = @At("TAIL"))
    private static void addCustomComponents(Schema schema, CallbackInfoReturnable<SequencedMap<String, Supplier<TypeTemplate>>> cir) {
        cir.getReturnValue().put("brewery:cooking_data", () -> DSL.optionalFields(
                "ingredients", DSL.list(References.ITEM_STACK.in(schema)),
                "heat_source", References.BLOCK_NAME.in(schema)
        ));
    }
}
