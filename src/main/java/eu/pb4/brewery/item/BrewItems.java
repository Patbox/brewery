package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.item.debug.BlockTickerItem;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

import java.util.function.Function;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewItems {
    public static final Item BOOK_ITEM = register("book_of_brewery", BookOfBreweryItem::new);


    public static final PolymerBlockItem BARREL_SPIGOT = register("barrel_spigot", (s) -> new PolymerBlockItem(
            BrewBlocks.BARREL_SPIGOT, s.maxCount(16).useBlockPrefixedTranslationKey(), Items.TRIPWIRE_HOOK
    ));

    public static final DrinkItem DRINK_ITEM = register("drink_bottle", DrinkItem::new);
    public static final Item FAILED_DRINK_ITEM = register("failed_drink_bottle", FailedDrinkItem::new);
    public static final Item INGREDIENT_MIXTURE = register("ingredient_mixture", IngredientMixtureItem::new);
    public static final BlockTickerItem DEBUG_BLOCK_TICKER = register("debug/block_ticker", BlockTickerItem::new);

    public static final ItemGroup ITEM_GROUP = ItemGroup.create(null, -1)
            .displayName(Text.literal("Brewery"))
            .icon(Items.BARREL::getDefaultStack)

            .entries((ctx, e) -> {
                e.add(BOOK_ITEM);
                e.add(BARREL_SPIGOT);

                for (var entry : BreweryInit.DRINK_TYPES.entrySet()) {
                    e.add(DrinkUtils.createDrink(entry.getKey(), 0, 10, entry.getValue().distillationRuns(), Blocks.AIR));
                }

                if (BreweryInit.IS_DEV) {
                    e.add(DEBUG_BLOCK_TICKER.create(20));
                    e.add(DEBUG_BLOCK_TICKER.create(60 * 20));
                    e.add(DEBUG_BLOCK_TICKER.create(60 * 60 * 20));
                    e.add(DEBUG_BLOCK_TICKER.create(24000));
                    e.add(DEBUG_BLOCK_TICKER.create(24000 * 7));
                }
            })
            .build();

    public static void register() {
        PolymerItemGroupUtils.registerPolymerItemGroup(id("items"), ITEM_GROUP);
    }

    private static <T extends Item> T register(String path, Function<Item.Settings, T> block) {
        return Registry.register(Registries.ITEM, id(path), block.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id(path)))));
    }
}
