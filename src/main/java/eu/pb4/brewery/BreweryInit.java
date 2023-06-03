package eu.pb4.brewery;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.block.BrewCauldronBlock;
import eu.pb4.brewery.block.entity.BrewBlockEntities;
import eu.pb4.brewery.compat.PolydexCompat;
import eu.pb4.brewery.drink.AlcoholValueEffect;
import eu.pb4.brewery.drink.DefaultDefinitions;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.item.BookOfBreweryItem;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.other.BrewCommands;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewNetworking;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BreweryInit implements ModInitializer {
    public static final String MOD_ID = "brewery";
    public static final Map<Identifier, DrinkType> DRINK_TYPES = new Object2ObjectOpenHashMap<>();
    public static final Map<DrinkType, Identifier> DRINK_TYPE_ID = new Object2ObjectOpenCustomHashMap<>(Util.identityHashStrategy());
    public static final List<AlcoholValueEffect.Value> ALCOHOL_EFFECTS = new ArrayList<>();
    public static final Object2DoubleMap<Item> ITEM_ALCOHOL_REMOVAL_VALUES = new Object2DoubleOpenHashMap<>();

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final boolean DISPLAY_DEV = IS_DEV && false;
    public static final boolean USE_GENERATOR = IS_DEV && false;

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        GenericModInfo.build(FabricLoader.getInstance().getModContainer(MOD_ID).get());

        BrewBlocks.register();
        BrewBlockEntities.register();
        BrewItems.register();
        BrewGameRules.register();

        BrewNetworking.register();

        ServerLifecycleEvents.SERVER_STARTED.register(BreweryInit::loadDrinks);
        ServerLifecycleEvents.SERVER_STARTED.register((s) -> CardboardWarning.checkAndAnnounce());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((x, y, z) -> BreweryInit.loadDrinks(x));

        CommandRegistrationCallback.EVENT.register(BrewCommands::register);

        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.put(Items.STICK, BrewCauldronBlock::tryReplaceCauldron);

        if (FabricLoader.getInstance().isModLoaded("polydex")) {
            PolydexCompat.init();
        }
    }

    public static void clearData() {
        DRINK_TYPES.clear();
        DRINK_TYPE_ID.clear();
        ITEM_ALCOHOL_REMOVAL_VALUES.clear();
        ALCOHOL_EFFECTS.clear();
    }

    public static void addDrink(Identifier identifier, DrinkType type) {
        DRINK_TYPES.put(identifier, type);
        DRINK_TYPE_ID.put(type, identifier);
    }

    private static void loadDrinks(MinecraftServer server) {
        var ops = RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager());
        clearData();

        for (var res : server.getResourceManager().findResources("brewery_drinks", (x) -> x.getPath().endsWith(".json")).entrySet()) {
            var id = new Identifier(res.getKey().getNamespace(), res.getKey().getPath().substring("brewery_drinks/".length(), res.getKey().getPath().length() - 5));

            try {
                var drinkType = DrinkType.CODEC.decode(ops, JsonParser.parseReader(res.getValue().getReader())).getOrThrow(false, (x) -> {});

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
                }

                ALCOHOL_EFFECTS.addAll(effects.entries());
                ITEM_ALCOHOL_REMOVAL_VALUES.putAll(effects.itemReduction());
            } catch (Throwable e) {
                LOGGER.warn("{} isn't valid brewery effect definition!", res.getKey().toString());
                e.printStackTrace();
            }
        }


        if (USE_GENERATOR) {
            var gson = new GsonBuilder().setPrettyPrinting().create();

            {
                var dir = FabricLoader.getInstance().getGameDir().resolve("../src/main/resources/data/brewery/brewery_drinks/");


                DefaultDefinitions.createBrews((key, drinkType) -> {
                    var id = new Identifier("brewery:" + key);
                    addDrink(id, drinkType);

                    try {
                        Files.writeString(dir.resolve(key + ".json"), gson.toJson(DrinkType.CODEC.encodeStart(ops, drinkType).getOrThrow(false, (x) -> {
                        })));
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

        BookOfBreweryItem.build(
                DRINK_TYPES.entrySet(),
                server.getOverworld().getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get(),
                server.getOverworld().getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get()
        );
    }
}
