package eu.pb4.brewery.item;

import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.item.debug.BlockTickerItem;
import eu.pb4.polymer.api.item.PolymerBlockItem;
import eu.pb4.polymer.api.item.PolymerItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewItems {
    public static final ItemGroup ITEM_GROUP = PolymerItemGroup.create(id("group"), Text.literal("Brewery"), () -> Items.BARREL.getDefaultStack());

    public static final Item BOOK_ITEM = register("book_of_brewery", new BookOfBreweryItem(new Item.Settings().group(ITEM_GROUP)));


    public static final PolymerBlockItem BARREL_SPIGOT = register("barrel_spigot", new PolymerBlockItem(
            BrewBlocks.BARREL_SPIGOT, new Item.Settings().group(ITEM_GROUP).maxCount(16), Items.TRIPWIRE_HOOK
    ));

    public static final DrinkItem DRINK_ITEM = register("drink_bottle", new DrinkItem(new Item.Settings().group(ITEM_GROUP)));
    public static final Item FAILED_DRINK_ITEM = register("failed_drink_bottle", new FailedDrinkItem(new Item.Settings().group(ITEM_GROUP)));
    public static final Item INGREDIENT_MIXTURE = register("ingredient_mixture", new IngredientMixtureItem(new Item.Settings()));
    public static final Item DEBUG_BLOCK_TICKER = register("debug/block_ticker", new BlockTickerItem(new Item.Settings()));

    public static void register() {

    }

    private static <T extends Item> T register(String path, T block) {
        return Registry.register(Registry.ITEM, id(path), block);
    }
}
