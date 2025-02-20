package eu.pb4.brewery;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.block.BrewCauldronBlock;
import eu.pb4.brewery.block.entity.BrewBlockEntities;
import eu.pb4.brewery.compat.PolydexCompatImpl;
import eu.pb4.brewery.drink.AlcoholValueEffect;
import eu.pb4.brewery.drink.DefaultDefinitions;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.item.BookOfBreweryItem;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.other.BrewCommands;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewNetworking;
import eu.pb4.brewery.other.FloatSelector;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class BreweryInit implements ModInitializer {
    public static final String MOD_ID = "brewery";
    public static final Map<Identifier, DrinkType> DRINK_TYPES = new Object2ObjectOpenHashMap<>();
    public static final Map<DrinkType, Identifier> DRINK_TYPE_ID = new Reference2ObjectOpenHashMap<>();
    public static final List<AlcoholValueEffect.Value> ALCOHOL_EFFECTS = new ArrayList<>();
    public static final Object2DoubleMap<Item> ITEM_ALCOHOL_REMOVAL_VALUES = new Object2DoubleOpenHashMap<>();
    public static final Map<Item, Identifier> CONTAINER_TO_INGMIX_MODEL = new IdentityHashMap<>();
    public static Ingredient containerIngredient = Ingredient.ofItems(Items.GLASS_BOTTLE);

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final boolean DISPLAY_DEV = IS_DEV && true;
    public static final boolean USE_GENERATOR = IS_DEV && true;

    private static ServerWorld overworld = null;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Nullable
    public static ServerWorld getOverworld() {
        return overworld;
    }

    @Override
    public void onInitialize() {
        /*FloatSelector.createQualityCodec(Codec.STRING, "").decode(JsonOps.INSTANCE,
                JsonParser.parseString("""
                        [ "red", "yellow", "green", "blue"]
                        """)).result().get().getFirst().entries().forEach(x -> System.out.println(x.valueForPass() + ": " + x.result()));*/

        GenericModInfo.build(FabricLoader.getInstance().getModContainer(MOD_ID).get());
        PolymerResourcePackUtils.addModAssets(BreweryInit.MOD_ID);

        BrewBlocks.register();
        BrewComponents.register();
        BrewBlockEntities.register();
        BrewItems.register();
        BrewGameRules.register();

        BrewNetworking.register();

        var id = id("early_reload");

        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(id, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STARTING.register(id, BreweryInit::loadDrinks);
        ServerLifecycleEvents.SERVER_STARTED.register(id, BreweryInit::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register((s) -> {
            overworld = null;
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.addPhaseOrdering(id, Event.DEFAULT_PHASE);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(id, (x, y, z) -> {
            BreweryInit.loadDrinks(x);
            BookOfBreweryItem.build(
                    DRINK_TYPES.entrySet(),
                    x.getOverworld().getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get(),
                    x.getOverworld().getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get()
            );
        });


        CommandRegistrationCallback.EVENT.register(BrewCommands::register);

        //CauldronBehavior.WATER_CAULDRON_BEHAVIOR.put(Items.STICK, BrewCauldronBlock::tryReplaceCauldron);

        UseBlockCallback.EVENT.register(BrewCauldronBlock::handleUseEvent);

        if (FabricLoader.getInstance().isModLoaded("polydex")) {
            PolydexCompatImpl.init();
        }
    }

    public static void clearData() {
        DRINK_TYPES.clear();
        DRINK_TYPE_ID.clear();
        ITEM_ALCOHOL_REMOVAL_VALUES.clear();
        ALCOHOL_EFFECTS.clear();
        CONTAINER_TO_INGMIX_MODEL.clear();
        containerIngredient = Ingredient.ofItems(Items.GLASS_BOTTLE);
    }

    public static void addDrink(Identifier identifier, DrinkType type) {
        DRINK_TYPES.put(identifier, type);
        DRINK_TYPE_ID.put(type, identifier);
    }

    private static void loadDrinks(MinecraftServer server) {
        var ops = RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager());
        clearData();

        for (var res : server.getResourceManager().findResources("brewery_drinks", (x) -> x.getPath().endsWith(".json")).entrySet()) {
            var id = Identifier.of(res.getKey().getNamespace(), res.getKey().getPath().substring("brewery_drinks/".length(), res.getKey().getPath().length() - 5));

            try {
                var drinkType = DrinkType.CODEC.decode(ops, JsonParser.parseReader(res.getValue().getReader())).getOrThrow();

                addDrink(id, drinkType.getFirst());
            } catch (Throwable e) {
                DRINK_TYPE_ID.remove(DRINK_TYPES.remove(id));
                LOGGER.warn("{} isn't valid brewery definition!", res.getKey().toString());
                e.printStackTrace();
            }
        }

        for (var res : server.getResourceManager().findResources("", (x) -> x.getPath().equals("brewery_effects.json")).entrySet()) {
            try {
                var effects = AlcoholValueEffect.CODEC.decode(ops, JsonParser.parseReader(res.getValue().getReader())).result().get().getFirst();

                if (effects.replace()) {
                    ALCOHOL_EFFECTS.clear();
                    ITEM_ALCOHOL_REMOVAL_VALUES.clear();
                    CONTAINER_TO_INGMIX_MODEL.clear();
                }
                CONTAINER_TO_INGMIX_MODEL.putAll(effects.ingredientMixModels());
                ALCOHOL_EFFECTS.addAll(effects.entries());
                ITEM_ALCOHOL_REMOVAL_VALUES.putAll(effects.itemReduction());
            } catch (Throwable e) {
                LOGGER.warn("{} isn't valid brewery effect definition!", res.getKey().toString());
                e.printStackTrace();
            }
        }


        if (USE_GENERATOR) {
            var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String potionModelJson = "";
            try {
                potionModelJson = Files.readString(PolymerCommonUtils.getClientJarRoot().resolve("assets/minecraft/items/potion.json"));
            } catch (Throwable e) {
                e.printStackTrace();
            }

            {
                var dir = FabricLoader.getInstance().getGameDir().resolve("../src/main/resources/data/brewery/brewery_drinks/");
                var dirModel = FabricLoader.getInstance().getGameDir().resolve("../src/main/resources/assets/brewery/items/");
                String finalPotionModelJson = potionModelJson;
                DefaultDefinitions.createBrews((key, drinkType) -> {
                    var id = Identifier.of("brewery", key);
                    addDrink(id, drinkType.apply(id));

                    if (id.getPath().equals("the_testificate")) {
                        return;
                    }

                    try {
                        Files.writeString(dirModel.resolve( "brewery_drink/" +key + ".json"), finalPotionModelJson);
                        Files.writeString(dir.resolve(key + ".json"), gson.toJson(DrinkType.CODEC.encodeStart(ops, drinkType.apply(id)).getOrThrow()));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
            }

            {
                var effects = DefaultDefinitions.createEffects(server);

                if (effects.replace()) {
                    ALCOHOL_EFFECTS.clear();
                }

                ALCOHOL_EFFECTS.addAll(effects.entries());
                try {
                    Files.writeString(FabricLoader.getInstance().getGameDir().resolve("../src/main/resources/data/brewery/brewery_effects.json"),
                            gson.toJson(AlcoholValueEffect.CODEC.encodeStart(ops, effects).result().get()
                            ));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        var list = new ArrayList<Item>();

        for (var drink : DRINK_TYPES.values()) {
            //noinspection deprecation
            drink.requiredContainer().getMatchingItems().map(RegistryEntry::value).forEach(list::add);
        }

        containerIngredient = Ingredient.ofItems(list.stream());
    }

    private static void onServerStarted(MinecraftServer server) {
        CardboardWarning.checkAndAnnounce();
        overworld = server.getOverworld();

        BookOfBreweryItem.build(
                DRINK_TYPES.entrySet(),
                server.getOverworld().getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get(),
                server.getOverworld().getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get()
        );
    }
}
