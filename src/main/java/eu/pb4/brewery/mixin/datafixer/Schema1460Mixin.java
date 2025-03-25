package eu.pb4.brewery.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public abstract class Schema1460Mixin extends Schema {
    @Shadow
    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {}

    public Schema1460Mixin(int versionKey, Schema parent) {
        super(versionKey, parent);
    }



    @Inject(method = "registerBlockEntities", at = @At("RETURN"))
    private void registerBreweryBlockEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        var map = cir.getReturnValue();
        registerInventory(schema, map, mod("cauldron"));
        registerInventory(schema, map, mod("barrel_spigot"));

        schema.registerSimple(map, mod("barrel_part"));
    }

    @Unique
    private static String mod(String path) {
        return "brewery:" + path;
    }

    /*@ModifyArg(method = "method_5259", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DSL;optionalFields([Lcom/mojang/datafixers/util/Pair;)Lcom/mojang/datafixers/types/templates/TypeTemplate;"))
    private static Pair<String, TypeTemplate>[] addCustomComponents(Pair<String, TypeTemplate>[] components,
                                                                    @Local(argsOnly = true) Schema schema) {
        var list = new ArrayList<>(List.of(components));
        list.add(Pair.of("Ingredients", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
        return list.toArray(components);
    }*/
}