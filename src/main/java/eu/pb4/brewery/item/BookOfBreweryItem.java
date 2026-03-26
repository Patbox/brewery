package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.GenericModInfo;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.*;

public class BookOfBreweryItem extends Item implements PolymerItem {
    public BookOfBreweryItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user instanceof ServerPlayer player) {
            new Gui(player, hand).open();
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.WRITTEN_BOOK;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }

    public static void build(Collection<Map.Entry<Identifier, DrinkType>> input, double barrelAgingMultiplier, double cookingTimeMultiplier) {
        var builder = new BookElementBuilder();
        Gui.BOOKS.clear();
        var types = input.stream().filter(x -> x.getValue().info().isPresent()).sorted(Comparator.comparing(x -> x.getValue().looks().nameSelector().select(7).text().getString())).toList();

        var container = FabricLoader.getInstance().getModContainer(BreweryInit.MOD_ID).get();

        {
            var contributors = new ArrayList<String>();
            contributors.addAll(container.getMetadata().getAuthors().stream().map(Person::getName).toList());
            contributors.addAll(container.getMetadata().getContributors().stream().map(Person::getName).toList());

            //noinspection DataFlowIssue
            builder.addPage(
                    ComponentUtils.formatList(List.of(GenericModInfo.getIconBook()), Component.literal("\n")),
                    Component.empty(),
                    Component.empty().append(Component.translatable("item.brewery.book_of_brewery")
                                    .setStyle(Style.EMPTY.withShadowColor(ARGB.scaleRGB(ChatFormatting.DARK_BLUE.getColor(), 0.6f) | 0xFF000000))
                                    .withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.BLUE))

                            .append(Component.literal(" \uD83E\uDDEA").withStyle(ChatFormatting.DARK_RED)),
                    Component.empty(),
                    Component.translatable("text.brewery.about.version").withStyle(ChatFormatting.DARK_GREEN)
                            .append(Component.literal(container.getMetadata().getVersion().getFriendlyString()).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))),
                    Component.empty(),
                    Component.literal("")
                            .append(Component.translatable("[%s]", Component.translatable("text.brewery.about.contributors"))
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)
                                            .withHoverEvent(new HoverEvent.ShowText(
                                                    Component.literal(String.join(", ", contributors)
                                                    ))
                                            )))
                            .append("")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))
            );
        }

        builder.addPage(
                Component.translatable("polydex.brewery.cooking_cauldron").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GREEN),
                Component.empty(),
                Component.translatable("polydex.brewery.cooking_cauldron.text")
        );

        builder.addPage(
                Component.translatable("polydex.brewery.aging_with_barrel").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.DARK_RED),
                Component.empty(),
                Component.translatable("polydex.brewery.aging_with_barrel.text")
        );

        builder.addPage(
                Component.translatable("polydex.brewery.building_barrel").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.GOLD),
                Component.empty(),
                Component.literal("§8 1§f⏹§a⏹§f⏹⏹§a⏹   §8|   2§f⏹§b⏹⏹⏹⏹"),
                Component.literal("§f ⏹⏹⏹⏹⏹⏹   §8|   §f⏹⏹§c⏹⏹⏹⏹"),
                Component.literal("§f ⏹⏹§a⏹§f⏹⏹§a⏹   §8|   §f⏹⏹§b⏹⏹⏹⏹"),
                Component.literal("§8 -----------------"),
                Component.literal("§8 3§f⏹§c⏹⏹⏹⏹   §8|   4§f⏹§b⏹⏹⏹⏹"),
                Component.literal("§f ⏹§d⏹§c⏹§f⏹⏹§c⏹   §8|   §f⏹⏹§c⏹⏹⏹⏹"),
                Component.literal("§f ⏹⏹§c⏹⏹⏹⏹   §8|   §f⏹⏹§b⏹⏹⏹⏹"),

                Component.translatable("polydex.brewery.building_barrel.view"),
                Component.empty().append(Component.literal("§a⏹")).append(" - ").append(Component.translatable("polydex.brewery.building_barrel.fence")),
                Component.empty().append(Component.literal("§b⏹")).append(" - ").append(Component.translatable("polydex.brewery.building_barrel.stair")),
                Component.empty().append(Component.literal("§c⏹")).append(" - ").append(Component.translatable("polydex.brewery.building_barrel.planks")),
                Component.empty().append(Component.literal("§d⏹")).append(" - ").append(Component.translatable("block.brewery.barrel_spigot"))
        );

        builder.addPage(
                Component.translatable("polydex.brewery.distillation").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.DARK_GREEN),
                Component.empty(),
                Component.translatable("polydex.brewery.distillation.text")
        );


        var indexEntries = new ArrayList<Component>();
        indexEntries.add(Component.translatable("polydex.brewery.recipes").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.RED));
        indexEntries.add(Component.empty());

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
                        .withStyle(x -> x.withClickEvent(new ClickEvent.ChangePage(1001 + finalIndex)).withUnderlined(true)));

                if (indexEntries.size() == 12) {
                    builder.addPage(indexEntries.toArray(new Component[0]));
                    indexEntries.clear();
                }
            }
        }

        if (!indexEntries.isEmpty()) {
            builder.addPage(indexEntries.toArray(new Component[0]));
        }
        builder.setComponent(DataComponents.WRITTEN_BOOK_CONTENT, builder.getComponent(DataComponents.WRITTEN_BOOK_CONTENT).markResolved());

        Gui.indexBook = builder.asStack();
    }

    private static int buildInfo(Identifier id, DrinkType type, double barrelAgingMultiplier, double cookingTimeMultiplier) {
        var builder = new BookElementBuilder();

        var list = new ArrayList<Component>();

        list.add(Component.empty().append(Component.literal("\uD83E\uDDEA ").withStyle(x -> x.withColor(type.looks().colorSelector().select(7)))).append(type.looks().nameSelector().select(7).text().copy().withStyle(x -> x.withBold(true).withUnderlined(true))));
        list.add(Component.empty());
        var info = type.info().get();

        if (!type.ingredients().isEmpty()) {
            list.add(Component.translatable("polydex.brewery.ingredients").withStyle(x -> x.withBold(true).withUnderlined(true).withColor(ChatFormatting.GOLD)));
            list.add(Component.empty());
            for (var i : type.ingredients()) {
                if (i.items().size() == 1) {
                    list.add(Component.literal(i.count() + " × ").append(i.items().get(0).getName(i.items().getFirst().getDefaultInstance())));
                } else {
                    var text = Component.translatable("polydex.brewery.any_of").append("\n");

                    for (var item : i.items()) {
                        text.append(item.getName(item.getDefaultInstance())).append("\n");
                    }

                    list.add(Component.literal(i.count() + " × ").append(Component.translatable("polydex.brewery.any_of_the_list").withStyle(ChatFormatting.ITALIC))
                            .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(text)))
                    );
                }
            }
            if (info.bestCookingTime() > 0) {
                list.add(Component.empty());
                list.add(Component.translatable("polydex.brewery.cook_for", BrewUtils.fromTimeShort(info.bestCookingTime() / cookingTimeMultiplier)));
            }
            list.add(Component.empty());
        }

        if (!type.barrelInfo().isEmpty() && !info.bestBarrelType().isEmpty()) {
            list.add(Component.translatable("polydex.brewery.aging").withStyle(ChatFormatting.UNDERLINE, ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
            list.add(Component.empty());

            if (info.bestBarrelType().contains("*")) {
                list.add(Component.translatable("polydex.brewery.any_barrel"));
            } else {
                if (info.bestBarrelType().size() == 1) {
                    list.add(Component.translatable("container.brewery." + info.bestBarrelType().get(0) + "_barrel"));
                } else {
                    list.add(Component.translatable("polydex.brewery.one_of_barrel"));
                    for (var b : info.bestBarrelType()) {
                        list.add(Component.translatable("container.brewery." + b + "_barrel" ));
                    }
                }
            }

            if (info.bestBarrelAge() > 0) {
                list.add(Component.translatable("polydex.brewery.age_in_barrel_for", BrewUtils.fromTimeShort(info.bestBarrelAge() / barrelAgingMultiplier)));
            }
            list.add(Component.empty());
        }

        if (type.requireDistillation()) {
            list.add(Component.translatable("polydex.brewery.require_distillation"));
            list.add(Component.empty());
        }

        var x = new ArrayList<Component>();
        for (var t : list) {
            x.add(t);
            if (x.size() == 10) {
                builder.addPage(x.toArray(new Component[0]));
                x.clear();
            }
        }

        if (!x.isEmpty()) {
            builder.addPage(x.toArray(new Component[0]));
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
        private final InteractionHand hand;

        public Gui(ServerPlayer player, InteractionHand hand) {
            super(player, indexBook);
            this.stack = player.getItemInHand(hand);
            this.hand = hand;
            this.setPage(Math.min(stack.getOrDefault(BrewComponents.BOOK_PAGE, 0),
                    indexBook.get(DataComponents.WRITTEN_BOOK_CONTENT).getPages(false).size()));
        }

        @Override
        public void onTakeBookButton() {
            if (this.book != indexBook) {
                playSoundToPlayer(this.player, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1);
                var page = this.stack.getOrDefault(BrewComponents.BOOK_PAGE, 0);
                this.book = indexBook;
                this.containerMenu.broadcastChanges();
                this.setPage(page);
            } else {
                this.close();
            }
        }

        @Override
        public void setPage(int page) {
            playSoundToPlayer(this.player, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1);
            if (page >= 1000 && BOOKS.size() > page - 1000) {
                this.book = BOOKS.get(page - 1000);
                this.containerMenu.broadcastChanges();
                super.setPage(0);
                return;
            }

            super.setPage(page);
            if (this.book == indexBook && this.stack == this.player.getItemInHand(hand)) {
                this.stack.set(BrewComponents.BOOK_PAGE, page);
            }
        }

        public static void playSoundToPlayer(Player player, SoundEvent soundEvent, SoundSource category, float volume, float pitch) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent), category, player, volume, pitch, player.getRandom().nextLong()));
            }
        }
    }
}
