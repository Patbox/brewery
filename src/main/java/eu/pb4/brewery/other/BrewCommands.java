package eu.pb4.brewery.other;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.GenericModInfo;
import eu.pb4.brewery.block.entity.TickableContents;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.drink.DefaultDefinitions;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Function;

import static eu.pb4.brewery.BreweryInit.id;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BrewCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
                literal("brewery")
                        .executes(BrewCommands::about)
                        .then(literal("create")
                                .requires(FabricPermissionBridge.require(id("create"), PermissionLevel.GAMEMASTERS))
                                .then(argument("type", IdentifierArgument.id())
                                        .suggests((context, builder) -> {
                                            Iterable<Identifier> candidates = BreweryInit.DRINK_TYPES.keySet()::iterator;
                                            var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                                            SharedSuggestionProvider.filterResources(candidates, remaining, Function.identity(), id -> {
                                                builder.suggest(id.toString(), BreweryInit.DRINK_TYPES.get(id).looks().nameSelector().select(7).text());
                                            });
                                            return builder.buildFuture();
                                        })
                                        .executes(BrewCommands::createDrink)
                                        .then(argument("quality", DoubleArgumentType.doubleArg(0, 10))
                                                .executes(BrewCommands::createDrink)
                                                .then(argument("age", DoubleArgumentType.doubleArg())
                                                        .executes(BrewCommands::createDrink)
                                                        .then(argument("distillated", BoolArgumentType.bool())
                                                                .executes(BrewCommands::createDrink)
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("force_age")
                                .requires(FabricPermissionBridge.require(id("force_age"), PermissionLevel.GAMEMASTERS))
                                .then(argument("position", BlockPosArgument.blockPos())
                                        .then(argument("time", TimeArgument.time())
                                                .executes(BrewCommands::ageContainer)
                                        )
                                )
                        )
                        .then(literal("stats")
                                .requires(FabricPermissionBridge.require(id("stats"), PermissionLevel.GAMEMASTERS))
                                .executes((ctx) -> showStats(ctx, ctx.getSource().getPlayerOrException()))
                                .then(argument("player", EntityArgument.entity()).requires(FabricPermissionBridge.require(id("stats_others"), PermissionLevel.GAMEMASTERS))
                                        .executes(ctx -> showStats(ctx, EntityArgument.getEntity(ctx, "player"))))
                        )
                        .then(literal("set")
                                .requires(FabricPermissionBridge.require(id("set"), PermissionLevel.GAMEMASTERS))
                                .then(argument("target", EntityArgument.entities())
                                        .then(argument("alcohol", DoubleArgumentType.doubleArg())
                                                .then(argument("quality", FloatArgumentType.floatArg(0, 10))
                                                        .executes(BrewCommands::setAlcoholValue)

                                                )
                                        )
                                )


                        ).then(literal("dump_defaults")
                                .requires(FabricPermissionBridge.require(id("dump_defaults"), PermissionLevel.OWNERS))
                                .executes((ctx) -> dumpDefaultDefinitions())
                        )
        );
    }

    private static int setAlcoholValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var alcohol = DoubleArgumentType.getDouble(ctx, "alcohol");
        var quality = FloatArgumentType.getFloat(ctx, "quality");
        int r = 0;
        for (var entity : EntityArgument.getEntities(ctx, "target")) {
            if (entity instanceof LivingEntity livingEntity) {
                var m = AlcoholManager.of(livingEntity);
                m.alcoholLevel = alcohol;
                m.quality = quality;
                r++;
            }
        }
        return r;
    }

    private static int ageContainer(CommandContext<CommandSourceStack> ctx) {
        var pos = BlockPosArgument.getBlockPos(ctx, "position");
        var time = ctx.getArgument("time", Integer.class);

        if (ctx.getSource().getLevel().getBlockEntity(pos) instanceof TickableContents tickableContents) {
            tickableContents.tickContents(time);
        }
        return 0;
    }

    private static int dumpDefaultDefinitions() {
        var dir = FabricLoader.getInstance().getGameDir().resolve("brewery_defaults_dump");

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            var gson = new GsonBuilder().setPrettyPrinting().create();

            DefaultDefinitions.createBrews((key, type) -> {
                try {
                    Files.writeString(dir.resolve(key + ".json"), gson.toJson(DrinkType.CODEC.encodeStart(JsonOps.INSTANCE, type.apply(id(key))).getOrThrow()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return 0;
    }

    private static int showStats(CommandContext<CommandSourceStack> ctx, Entity player) {
        if (player instanceof LivingEntity livingEntity) {
            var m = AlcoholManager.of(livingEntity);
            ctx.getSource().sendSuccess(() -> Component.translatable("text.brewery.stats", player.getDisplayName(), m.alcoholLevel, m.quality), false);
        }
        return 0;
    }

    private static int createDrink(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "type");
        var type = BreweryInit.DRINK_TYPES.get(id);

        if (type == null) {
            context.getSource().sendFailure(Component.literal("Invalid type!"));
            return -1;
        }

        double quality;

        try {
            quality = DoubleArgumentType.getDouble(context, "quality");
        } catch (Throwable t) {
            quality = 10;
        }

        int age;

        try {
            age = (int) (DoubleArgumentType.getDouble(context, "age") * 20);
        } catch (Throwable e) {
            age = 0;
        }

        int distillated;

        try {
            distillated = BoolArgumentType.getBool(context, "distillated") ? type.distillationRuns() : 0;
        } catch (Throwable e) {
            distillated = type.distillationRuns();
        }

        context.getSource().getPlayerOrException().addItem(DrinkUtils.createDrink(id, age, quality, distillated, Blocks.AIR));

        return 1;
    }


    private static int about(CommandContext<CommandSourceStack> context) {
        for (var text : context.getSource().getEntity() instanceof ServerPlayer ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendSuccess(() -> text, false);
        }

        return 1;
    }
}
