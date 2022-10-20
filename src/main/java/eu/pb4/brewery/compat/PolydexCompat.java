package eu.pb4.brewery.compat;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.item.BookOfBreweryItem;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.PageEntry;
import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.sgui.api.elements.*;
import eu.pb4.sgui.api.gui.layered.Layer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PolydexCompat {
    public static void init() {
        ItemPageView.register(PolydexCompat::createPages);
    }

    private static Collection<PageEntry<?>> createPages(MinecraftServer server, ItemEntry itemEntry, Collection<Recipe<?>> recipes) {
        if (itemEntry.item() == BrewItems.DRINK_ITEM) {
            var type = DrinkUtils.getType(itemEntry.stack());

            if (type != null && !type.ingredients().isEmpty()) {
                return List.of(new PageEntry<>(BrewingPage.INSTANCE, type));
            }
        }

        return null;
    }

    public static class BrewingPage implements ItemPageView<DrinkType> {
        public static final BrewingPage INSTANCE = new BrewingPage();

        public static final GuiElement ICON = new GuiElementBuilder(Items.BARREL).setName(Text.translatable("polydex.brewery.brewing_recipe")).build();
        public static final GuiElement INGREDIENTS = new GuiElementBuilder(Items.CAULDRON).setName(Text.translatable("polydex.brewery.ingredients").formatted(Formatting.GOLD)).build();
        public static final GuiElement REQUIRE_DISTILLATION = new GuiElementBuilder(Items.BREWING_STAND).setName(Text.translatable("polydex.brewery.require_distillation").formatted(Formatting.YELLOW)).build();
        public static final AnimatedGuiElement ANY_BARREL;

        static {
            var e = new AnimatedGuiElementBuilder();
            e.setInterval(20);
            for (var barrel : BrewBlocks.BARREL_MATERIALS) {
                e.setItem(barrel.planks().asItem());
                e.setName(Text.translatable("polydex.brewery.any_barrel").formatted(Formatting.YELLOW));
                e.saveItemStack();
            }

            ANY_BARREL = e.build();
        }

        @Override
        public GuiElement getIcon(ItemEntry itemEntry, DrinkType type, ServerPlayerEntity serverPlayerEntity, Runnable runnable) {
            return ICON;
        }

        @Override
        public void renderLayer(ItemEntry itemEntry, DrinkType type, ServerPlayerEntity serverPlayerEntity, Layer layer, Runnable runnable) {
            int i = 0;

            layer.setSlot(4, INGREDIENTS);

            for (var x : type.ingredients()) {
                GuiElementInterface element;

                if (x.items().size() == 1) {
                    element = new GuiElement(new ItemStack(x.items().get(0), x.count()), GuiElementInterface.EMPTY_CALLBACK);
                } else {
                    var list = new ArrayList<ItemStack>();
                    for (var item : x.items()) {
                        list.add(new ItemStack(item, x.count()));
                    }

                    element = new AnimatedGuiElement(list.toArray(new ItemStack[0]), 20, false, GuiElementInterface.EMPTY_CALLBACK);
                }

                layer.setSlot(((i / 7) * 9 + i % 7) + 9 + 1, element);
                i++;

                if (i == 14) {
                    break;
                }
            }

            while (i < 14) {
                layer.setSlot(((i / 7) * 9 + i % 7) + 9 + 1, GuiElement.EMPTY);
                i++;
            }

            int key = 3 * 9 + 2;

            if (!type.barrelInfo().isEmpty()) {
                GuiElementInterface element;
                var universal = type.getBarrelInfo(DrinkType.BarrelInfo.ANY);

                if (universal != null) {
                    element = ANY_BARREL;
                } else if (type.barrelInfo().size() == 1) {
                    var data = type.barrelInfo().get(0);

                    var material = BrewBlocks.BARREL_MATERIAL_MAP.get(data.type());

                    element = new GuiElementBuilder(material.planks().asItem()).setName(Text.translatable("container.brewery." + material.type() + "_barrel").formatted(Formatting.YELLOW)).build();
                } else {
                    var lore = new ArrayList<Text>();
                    var item = new ArrayList<Item>();

                    for (var data : type.barrelInfo()) {
                        var material = BrewBlocks.BARREL_MATERIAL_MAP.get(data.type());
                        item.add(material.planks().asItem());
                        lore.add(Text.translatable("container.brewery." + material.type() + "_barrel"));
                    }

                    var b = new AnimatedGuiElementBuilder();
                    b.setInterval(20);
                    for (int a = 0; i < type.barrelInfo().size(); a++) {
                        b.setItem(item.get(a));
                        b.setLore(lore);
                        b.setName(Text.translatable("polydex.brewery.one_of_barrel").formatted(Formatting.YELLOW));
                        b.saveItemStack();
                    }
                    element = b.build();
                }

                layer.setSlot(key++, element);
            }

            if (type.requireDistillation()) {
                layer.setSlot(key++, REQUIRE_DISTILLATION);
            }

            layer.setSlot(layer.getHeight() * 9 - 1, new GuiElementBuilder(Items.BOOK)
                    .setName(Text.translatable("polydex.brewery.open_book").formatted(Formatting.GREEN))
                    .hideFlags()
                    .setCallback((x, y, z, d) -> {
                        d.close(false);
                        BookOfBreweryItem.openEntry(d.getPlayer(), BreweryInit.DRINK_TYPE_ID.get(type), () -> {
                            d.open();
                        });
                    })

            );
        }
    }
}
