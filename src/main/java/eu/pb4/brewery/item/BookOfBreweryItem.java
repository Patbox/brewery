package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;
import java.util.stream.Collectors;

public class BookOfBreweryItem extends Item implements PolymerItem {
    public BookOfBreweryItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity player) {
            new Gui(player, hand).open();
            return ActionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.WRITTEN_BOOK;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }

    public static void build(Collection<Map.Entry<Identifier, DrinkType>> input, double barrelAgingMultiplier, double cookingTimeMultiplier) {
        var builder = new BookElementBuilder();
        Gui.BOOKS.clear();
        var types = input.stream().filter(x -> x.getValue().info().isPresent()).sorted(Comparator.comparing(x -> x.getValue().looks().nameSelector().select(7).text().getString())).toList();

        var container = FabricLoader.getInstance().getModContainer(BreweryInit.MOD_ID).get();

        {
            var contributors = new ArrayList<String>();
            contributors.addAll(container.getMetadata().getAuthors().stream().map((p) -> p.getName()).collect(Collectors.toList()));
            contributors.addAll(container.getMetadata().getContributors().stream().map((p) -> p.getName()).collect(Collectors.toList()));

            builder.addPage(
                    Text.empty(),
                    Text.empty().append(Text.translatable("item.brewery.book_of_brewery").formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.DARK_BLUE))
                            .append(Text.literal(" \uD83E\uDDEA").formatted(Formatting.DARK_RED)),
                    Text.empty(),
                    Text.translatable("text.brewery.about.version").formatted(Formatting.DARK_GREEN)
                            .append(Text.literal(container.getMetadata().getVersion().getFriendlyString()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))),
                    Text.empty(),
                    Text.literal("")
                            .append(Text.translatable("[%s]", Text.translatable("text.brewery.about.contributors"))
                                    .setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                    Text.literal(String.join(", ", contributors)
                                                    ))
                                            )))
                            .append("")
                            .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
            );
        }

        builder.addPage(
                Text.translatable("polydex.brewery.cooking_cauldron").formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.GREEN),
                Text.empty(),
                Text.translatable("polydex.brewery.cooking_cauldron.text")
        );

        builder.addPage(
                Text.translatable("polydex.brewery.aging_with_barrel").formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.DARK_RED),
                Text.empty(),
                Text.translatable("polydex.brewery.aging_with_barrel.text")
        );

        builder.addPage(
                Text.translatable("polydex.brewery.building_barrel").formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.GOLD),
                Text.empty(),
                Text.literal("§8 1§f⏹§a⏹§f⏹⏹§a⏹   §8|   2§f⏹§b⏹⏹⏹⏹"),
                Text.literal("§f ⏹⏹⏹⏹⏹⏹   §8|   §f⏹⏹§c⏹⏹⏹⏹"),
                Text.literal("§f ⏹⏹§a⏹§f⏹⏹§a⏹   §8|   §f⏹⏹§b⏹⏹⏹⏹"),
                Text.literal("§8 -----------------"),
                Text.literal("§8 3§f⏹§c⏹⏹⏹⏹   §8|   4§f⏹§b⏹⏹⏹⏹"),
                Text.literal("§f ⏹§d⏹§c⏹§f⏹⏹§c⏹   §8|   §f⏹⏹§c⏹⏹⏹⏹"),
                Text.literal("§f ⏹⏹§c⏹⏹⏹⏹   §8|   §f⏹⏹§b⏹⏹⏹⏹"),

                Text.translatable("polydex.brewery.building_barrel.view"),
                Text.empty().append(Text.literal("§a⏹")).append(" - ").append(Text.translatable("polydex.brewery.building_barrel.fence")),
                Text.empty().append(Text.literal("§b⏹")).append(" - ").append(Text.translatable("polydex.brewery.building_barrel.stair")),
                Text.empty().append(Text.literal("§c⏹")).append(" - ").append(Text.translatable("polydex.brewery.building_barrel.planks")),
                Text.empty().append(Text.literal("§d⏹")).append(" - ").append(Text.translatable("block.brewery.barrel_spigot"))
        );


        var indexEntries = new ArrayList<Text>();
        indexEntries.add(Text.translatable("polydex.brewery.recipes").formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.RED));
        indexEntries.add(Text.empty());

        for (var e : types) {
            var type = e.getValue();
            int index = -1;
            try {
                index = buildInfo(e.getKey(), e.getValue(), barrelAgingMultiplier, cookingTimeMultiplier);
            } catch (Throwable e2) {
                e2.printStackTrace();
            }
            if (index != -1) {
                int finalIndex = index;
                indexEntries.add(type.looks().nameSelector().select(7).text().copy()
                        .styled(x -> x.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(1001 + finalIndex))).withUnderline(true)));

                if (indexEntries.size() == 12) {
                    builder.addPage(indexEntries.toArray(new Text[0]));
                    indexEntries.clear();
                }
            }
        }

        if (!indexEntries.isEmpty()) {
            builder.addPage(indexEntries.toArray(new Text[0]));
        }
        builder.setComponent(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.getComponent(DataComponentTypes.WRITTEN_BOOK_CONTENT).asResolved());

        Gui.indexBook = builder.asStack();
    }

    private static int buildInfo(Identifier id, DrinkType type, double barrelAgingMultiplier, double cookingTimeMultiplier) {
        var builder = new BookElementBuilder();

        var list = new ArrayList<Text>();

        list.add(Text.empty().append(Text.literal("\uD83E\uDDEA ").styled(x -> x.withColor(type.looks().colorSelector().select(7)))).append(type.looks().nameSelector().select(7).text().copy().styled(x -> x.withBold(true).withUnderline(true))));
        list.add(Text.empty());
        var info = type.info().get();

        if (!type.ingredients().isEmpty()) {
            list.add(Text.translatable("polydex.brewery.ingredients").styled(x -> x.withBold(true).withUnderline(true).withColor(Formatting.GOLD)));
            list.add(Text.empty());
            for (var i : type.ingredients()) {
                if (i.items().size() == 1) {
                    list.add(Text.literal(i.count() + " × ").append(i.items().get(0).getName()));
                } else {
                    var text = Text.translatable("polydex.brewery.any_of").append("\n");

                    for (var item : i.items()) {
                        text.append(item.getName()).append("\n");
                    }

                    list.add(Text.literal(i.count() + " × ").append(Text.translatable("polydex.brewery.any_of_the_list").formatted(Formatting.ITALIC))
                            .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text)))
                    );
                }
            }
            if (info.bestCookingTime() > 0) {
                list.add(Text.empty());
                list.add(Text.translatable("polydex.brewery.cook_for", BrewUtils.fromTimeShort(info.bestCookingTime() / cookingTimeMultiplier)));
            }
            list.add(Text.empty());
        }

        if (!type.barrelInfo().isEmpty() && !info.bestBarrelType().isEmpty()) {
            list.add(Text.translatable("polydex.brewery.aging").formatted(Formatting.UNDERLINE, Formatting.DARK_GREEN, Formatting.BOLD));
            list.add(Text.empty());

            if (info.bestBarrelType().contains("*")) {
                list.add(Text.translatable("polydex.brewery.any_barrel"));
            } else {
                if (info.bestBarrelType().size() == 1) {
                    list.add(Text.translatable("container.brewery." + info.bestBarrelType().get(0) + "_barrel"));
                } else {
                    list.add(Text.translatable("polydex.brewery.one_of_barrel"));
                    for (var b : info.bestBarrelType()) {
                        list.add(Text.translatable("container.brewery." + b + "_barrel" ));
                    }
                }
            }

            if (info.bestBarrelAge() > 0) {
                list.add(Text.translatable("polydex.brewery.age_in_barrel_for", BrewUtils.fromTimeShort(info.bestBarrelAge() / barrelAgingMultiplier)));
            }
            list.add(Text.empty());
        }

        if (type.requireDistillation()) {
            list.add(Text.translatable("polydex.brewery.require_distillation"));
            list.add(Text.empty());
        }

        var x = new ArrayList<Text>();
        for (var t : list) {
            x.add(t);
            if (x.size() == 10) {
                builder.addPage(x.toArray(new Text[0]));
                x.clear();
            }
        }

        if (!x.isEmpty()) {
            builder.addPage(x.toArray(new Text[0]));
            x.clear();
        }

        for (var text : info.additionalInfo()) {
            builder.addPage(text.text().copy());
        }

        builder.setTitle(id.toString());
        builder.setAuthor("Brewery");

        Gui.BOOKS.add(builder.asStack());
        return Gui.BOOKS.size() - 1;
    }

    public static final class Gui extends BookGui {
        public static final List<ItemStack> BOOKS = new ArrayList<>();

        public static ItemStack indexBook;
        private final ItemStack stack;
        private final Hand hand;

        public Gui(ServerPlayerEntity player, Hand hand) {
            super(player, indexBook);
            this.stack = player.getStackInHand(hand);
            this.hand = hand;
            this.setPage(Math.min(stack.getOrDefault(BrewComponents.BOOK_PAGE, 0),
                    indexBook.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).getPages(false).size()));
        }

        @Override
        public void onTakeBookButton() {
            if (this.book != indexBook) {
                this.player.playSoundToPlayer(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1);
                var page = this.stack.getOrDefault(BrewComponents.BOOK_PAGE, 0);
                this.book = indexBook;
                this.screenHandler.sendContentUpdates();
                this.setPage(page);
            } else {
                this.close();
            }
        }

        @Override
        public void setPage(int page) {
            this.player.playSoundToPlayer(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1);
            if (page >= 1000 && BOOKS.size() > page - 1000) {
                this.book = BOOKS.get(page - 1000);
                this.screenHandler.sendContentUpdates();
                super.setPage(0);
                return;
            }

            super.setPage(page);
            if (this.book == indexBook && this.stack == this.player.getStackInHand(hand)) {
                this.stack.set(BrewComponents.BOOK_PAGE, page);
            }
        }
    }
}
