package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.item.debug.BlockTickerItem;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import java.util.function.Function;

import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewItems {
    public static final Item BOOK_ITEM = register("book_of_brewery", BookOfBreweryItem::new);


    public static final PolymerBlockItem BARREL_SPIGOT = register("barrel_spigot", (s) -> new PolymerBlockItem(
            BrewBlocks.BARREL_SPIGOT, s.stacksTo(16).useBlockDescriptionPrefix(), Items.TRIPWIRE_HOOK
    ));

    public static final DrinkItem DRINK_ITEM = register("drink_bottle", DrinkItem::new);
    public static final Item FAILED_DRINK_ITEM = register("failed_drink_bottle", FailedDrinkItem::new);
    public static final Item INGREDIENT_MIXTURE = register("ingredient_mixture", IngredientMixtureItem::new);
    public static final BlockTickerItem DEBUG_BLOCK_TICKER = register("debug/block_ticker", BlockTickerItem::new);

    public static final CreativeModeTab ITEM_GROUP = CreativeModeTab.builder(null, -1)
            .title(Component.literal("Brewery"))
            .icon(Items.BARREL::getDefaultInstance)

            .displayItems((ctx, e) -> {
                e.accept(BOOK_ITEM);
                e.accept(BARREL_SPIGOT);

                for (var entry : BreweryInit.DRINK_TYPES.entrySet()) {
                    e.accept(DrinkUtils.createDrink(entry.getKey(), 0, 10, entry.getValue().distillationRuns(), Blocks.AIR));
                }

                if (BreweryInit.IS_DEV) {
                    e.accept(DEBUG_BLOCK_TICKER.create(20));
                    e.accept(DEBUG_BLOCK_TICKER.create(60 * 20));
                    e.accept(DEBUG_BLOCK_TICKER.create(60 * 60 * 20));
                    e.accept(DEBUG_BLOCK_TICKER.create(24000));
                    e.accept(DEBUG_BLOCK_TICKER.create(24000 * 7));
                }
            })
            .build();

    public static void register() {
        PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(id("items"), ITEM_GROUP);
    }

    private static <T extends Item> T register(String path, Function<Item.Properties, T> block) {
        return Registry.register(BuiltInRegistries.ITEM, id(path), block.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)))));
    }
}
