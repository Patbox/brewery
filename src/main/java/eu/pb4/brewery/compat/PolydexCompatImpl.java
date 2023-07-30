package eu.pb4.brewery.compat;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.item.BrewItems;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static eu.pb4.brewery.BreweryInit.id;

public class PolydexCompatImpl {
    public static final PolydexCategory CATEGORY = PolydexCategory.of(id("drink"));

    public static void init() {
        PolydexPage.register(PolydexCompatImpl::createPages);
        PolydexEntry.registerEntryCreator(BrewItems.DRINK_ITEM, PolydexCompatImpl::createEntries);
        PolymerResourcePackUtils.addModAssets(BreweryInit.MOD_ID);
    }

    private static PolydexEntry createEntries(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            return PolydexEntry.of(id("drink_bottle/" + BreweryInit.DRINK_TYPE_ID.get(type).toUnderscoreSeparatedString()), stack, PolydexCompatImpl::isPartOf);
        }
        return PolydexEntry.of(stack);
    }

    private static boolean isPartOf(PolydexEntry entry, PolydexStack<?> polydexStack) {
        if (polydexStack.getBacking() instanceof ItemStack checked && entry.stack().getBacking() instanceof ItemStack base) {
            return DrinkUtils.getType(checked) == DrinkUtils.getType(base) && checked.isOf(base.getItem());
        }
        return false;
    }

    private static void createPages(MinecraftServer server, Consumer<PolydexPage> pageConsumer) {
        BreweryInit.DRINK_TYPES.forEach((id, type) -> {
            pageConsumer.accept(new BrewingPage(id, type));
        });
    }

    public static class BrewingPage implements PolydexPage {
        private static final Text TEXTURE = Text.literal("0").setStyle(Style.EMPTY.withFont(id("gui")));

        public static final ItemStack ICON = new GuiElementBuilder(Items.BARREL).setName(Text.translatable("polydex.brewery.brewing_recipe")).asStack();
        public static final ItemStack INGREDIENTS = new GuiElementBuilder(Items.CAULDRON).setName(Text.translatable("polydex.brewery.ingredients").formatted(Formatting.GOLD)).asStack();
        public static final ItemStack REQUIRE_DISTILLATION = new GuiElementBuilder(Items.BREWING_STAND).setName(Text.translatable("polydex.brewery.require_distillation").formatted(Formatting.YELLOW)).asStack();
        public static final ItemStack[] ANY_BARREL;

        static {
            var stacks = new ArrayList<ItemStack>();
            var e = new GuiElementBuilder();
            e.setName(Text.translatable("polydex.brewery.any_barrel").formatted(Formatting.YELLOW));
            for (var barrel : BrewBlocks.BARREL_MATERIALS) {
                e.setItem(barrel.planks().asItem());
                stacks.add(e.asStack());
            }

            ANY_BARREL = stacks.toArray(new ItemStack[0]);
        }

        private final DrinkType type;
        private final List<PolydexIngredient<?>> ingredients;
        private final Identifier identifier;
        private final Identifier typeId;

        public BrewingPage(Identifier identifier, DrinkType type) {
            this.identifier = identifier.withPrefixedPath("brewery_drink/");
            this.typeId = identifier;
            this.type = type;
            List<PolydexIngredient<?>> list = new ArrayList<>();
            for (DrinkType.BrewIngredient x : type.ingredients()) {
                list.add(PolydexIngredient.of(Ingredient.ofItems(x.items().toArray(new ItemConvertible[0])), x.count()));
            }
            this.ingredients = list;
        }

        @Override
        public @Nullable Text texture(ServerPlayerEntity player) {
            return TEXTURE;
        }

        @Override
        public Identifier identifier() {
            return this.identifier;
        }

        @Override
        public ItemStack typeIcon(ServerPlayerEntity serverPlayerEntity) {
            return ICON;
        }

        @Override
        public ItemStack entryIcon(@Nullable PolydexEntry polydexEntry, ServerPlayerEntity serverPlayerEntity) {
            return DrinkUtils.createDrink(this.typeId, 0, 10, this.type.distillationRuns(), new Identifier("air"));
        }

        @Override
        public void createPage(@Nullable PolydexEntry polydexEntry, ServerPlayerEntity serverPlayerEntity, PageBuilder layer) {
            int i = 0;

            layer.set(4,0, INGREDIENTS);

            for (var x : this.ingredients) {
                layer.setIngredient((i % 7) + 1, (i / 7) + 1, x);
                i++;

                if (i == 14) {
                    break;
                }
            }

            while (i < 14) {
                layer.setEmpty((i % 7) + 1, (i / 7) + 1);
                i++;
            }

            int key = 2;
            layer.setOutput(4, 3, DrinkUtils.createDrink(this.typeId, 0, 10, this.type.distillationRuns(), new Identifier("air")));

            if (!type.barrelInfo().isEmpty()) {
                ItemStack[] element;
                var universal = type.getBarrelInfo(DrinkType.BarrelInfo.ANY);

                if (universal != null) {
                    element = ANY_BARREL;
                } else if (type.barrelInfo().size() == 1) {
                    var data = type.barrelInfo().get(0);

                    var material = BrewBlocks.BARREL_MATERIAL_MAP.get(data.type());

                    element = new ItemStack[] {
                            new GuiElementBuilder(material.planks().asItem()).setName(Text.translatable("container.brewery." + material.type().toString().replace("minecraft:", "") + "_barrel").formatted(Formatting.YELLOW)).asStack()
                    };
                } else {
                    var lore = new ArrayList<Text>();
                    var item = new ArrayList<Item>();

                    for (var data : type.barrelInfo()) {
                        var material = BrewBlocks.BARREL_MATERIAL_MAP.get(data.type());
                        item.add(material.planks().asItem());
                        lore.add(Text.translatable("container.brewery." + material.type() + "_barrel"));
                    }
                    var b = new GuiElementBuilder();
                    b.setLore(lore);
                    b.setName(Text.translatable("polydex.brewery.one_of_barrel").formatted(Formatting.YELLOW));

                    var list = new ArrayList<ItemStack>();
                    for (var a : item) {
                        b.setItem(a);
                        list.add(b.asStack());
                    }
                    element = list.toArray(new ItemStack[0]);
                }

                layer.set(key++, 4, element);
            }

            if (type.requireDistillation()) {
                layer.set(key++, 4, REQUIRE_DISTILLATION);
            }
        }

        @Override
        public List<PolydexIngredient<?>> ingredients() {
            return this.ingredients;
        }

        @Override
        public List<PolydexCategory> categories() {
            return List.of(CATEGORY);
        }

        @Override
        public boolean isOwner(MinecraftServer minecraftServer, PolydexEntry polydexEntry) {
            if (polydexEntry.stack().getBacking() instanceof ItemStack stack) {
                return DrinkUtils.getType(stack) == this.type;
            }
            return false;
        }
    }
}
