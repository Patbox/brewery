package eu.pb4.brewery.item;

import com.mojang.serialization.Codec;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewComponents {
    public static final ComponentType<Integer> BOOK_PAGE = register("book_page",
            ComponentType.<Integer>builder().codec(Codec.INT).build());

    public static final ComponentType<Integer> TICK_COUNT = register("tick_count",
            ComponentType.<Integer>builder().codec(Codec.INT).build());

    public static final ComponentType<CookingData> COOKING_DATA = register("cooking_data",
            ComponentType.<CookingData>builder().codec(CookingData.CODEC).build());

    public static final ComponentType<BrewData> BREW_DATA = register("brew_data",
            ComponentType.<BrewData>builder().codec(BrewData.CODEC).build());

    public static void register() {
    }

    private static <T> ComponentType<T> register(String path, ComponentType<T> block) {
        PolymerComponent.registerDataComponent(block);
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id(path), block);
    }
}
