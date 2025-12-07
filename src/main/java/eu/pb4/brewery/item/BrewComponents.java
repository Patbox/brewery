package eu.pb4.brewery.item;

import com.mojang.serialization.Codec;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewComponents {
    public static final DataComponentType<Integer> BOOK_PAGE = register("book_page",
            DataComponentType.<Integer>builder().persistent(Codec.INT).build());

    public static final DataComponentType<Integer> TICK_COUNT = register("tick_count",
            DataComponentType.<Integer>builder().persistent(Codec.INT).build());

    public static final DataComponentType<CookingData> COOKING_DATA = register("cooking_data",
            DataComponentType.<CookingData>builder().persistent(CookingData.CODEC).build());

    public static final DataComponentType<BrewData> BREW_DATA = register("brew_data",
            DataComponentType.<BrewData>builder().persistent(BrewData.CODEC).build());

    public static void register() {
    }

    private static <T> DataComponentType<T> register(String path, DataComponentType<T> block) {
        PolymerComponent.registerDataComponent(block);
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(path), block);
    }
}
