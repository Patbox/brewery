package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DrinkItem extends Item implements PolymerItem {
    public DrinkItem(Settings settings) {
        super(settings.maxCount(1).food(new FoodComponent.Builder().alwaysEdible().build()));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        var type = DrinkUtils.getType(stack);

        if (type == null) {
            return 32 * 3;
        } else {
            return (int) (type.drinkingTime(stack, user) * 20);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        //if (type == null) {
            return UseAction.DRINK;
        //} else {
            //return type.visuals(stack).animation();
        //}
    }

    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        PlayerEntity playerEntity = user instanceof PlayerEntity ? (PlayerEntity) user : null;
        if (playerEntity instanceof ServerPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity) playerEntity, stack);
        }

        if (!world.isClient) {
            try {
                var type = DrinkUtils.getType(stack);

                if (type != null) {
                    var age = DrinkUtils.getAgeInSeconds(stack);
                    var quality = DrinkUtils.getQuality(stack);

                    var alcoholicValue = type.alcoholicValue().expression()
                            .setVariable(ExpressionUtil.AGE_KEY, age)
                            .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                            .evaluate();

                    AlcoholManager.of(user).drink(type, quality, alcoholicValue);

                    for (var effect : (type.isFinished(stack) ? type.consumptionEffects() : type.unfinishedEffects())) {
                        effect.apply(user, age, quality);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (playerEntity != null) {
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        user.emitGameEvent(GameEvent.DRINK);

        var cookingData = stack.get(BrewComponents.COOKING_DATA);
        stack.decrementUnlessCreative(1, playerEntity);

        if (playerEntity == null || !playerEntity.isCreative()) {
            if (cookingData != null) {
                if (stack.isEmpty()) {
                    return cookingData.container().copy();
                }

                if (playerEntity != null) {
                    playerEntity.getInventory().insertStack(cookingData.container().copy());
                }
            }
        }

        return stack;
    }

    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public Text getName(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            if (type.isFinished(stack)) {
                return type.name(stack).text();
            } else {
                return Text.translatable("item.brewery.ingredient_mixture_specific", type.name(stack).text());
            }
        } else {
            var id = DrinkUtils.getType(stack);

            Text text;

            if (id != null) {
                text = id.name(stack).text();
            } else {
                text = Text.literal("<Unknown>");
            }

            return Text.translatable(this.getTranslationKey(), text);
        }
    }

    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType typex) {
        var world = BreweryInit.getOverworld();
        if (world != null) {
            var type = DrinkUtils.getType(stack);
            if (type != null && type.showQuality() && world.getGameRules().getBoolean(BrewGameRules.SHOW_QUALITY)) {
                var quality
                        = DrinkUtils.getQuality(stack);
                var starCount = (Math.round((quality / 2) * 10)) / 10d;

                StringBuilder stars = new StringBuilder();
                StringBuilder antistars = new StringBuilder();

                while (starCount >= 1) {
                    stars.append("⭐");
                    starCount--;
                }

                if (starCount > 0) {
                    stars.append("☆");
                }

                var starsLeft = 5 - stars.length();
                for (int i = 0; i < starsLeft; i++) {
                    antistars.append("☆");
                }

                tooltip.add(Text.translatable("text.brewery.quality", Text.empty()
                        .append(Text.literal(stars.toString()).formatted(Formatting.YELLOW))
                        .append(Text.literal(antistars.toString()).formatted(Formatting.DARK_GRAY))
                ));
            }

            if (world.getGameRules().getBoolean(BrewGameRules.SHOW_AGE)) {
                double mult = world != null ? world.getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get() : 1;

                var age = DrinkUtils.getAgeInSeconds(stack) / mult;
                if (age > 0) {
                    tooltip.add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).formatted(Formatting.GRAY)));
                }
            }

            if (BreweryInit.DISPLAY_DEV) {
                tooltip.add(Text.literal("== DEV ==").formatted(Formatting.AQUA));
                tooltip.add(Text.literal("BrewType: ").append(stack.getOrDefault(BrewComponents.BREW_DATA, BrewData.DEFAULT).type().toString()).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewQuality: ").append("" + DrinkUtils.getQuality(stack)).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewAge: ").append("" + DrinkUtils.getAgeInTicks(stack)).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewDistillated: ").append("" + DrinkUtils.getDistillationStatus(stack)).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            var visuals = type.visuals(itemStack);

            if (Registries.ITEM.containsId(visuals.defaultModel())) {
                return Registries.ITEM.get(visuals.defaultModel());
            }
        }

        return Items.POTION;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            var visuals = type.visuals(itemStack);
            if (visuals.resourcePackModel().isPresent()) {
                var val = BreweryInit.RESOURCE_PACK_MODELS.getOrDefault(visuals.defaultModel(), Map.of()).get(visuals.resourcePackModel().get());
                if (val != null) {
                    return val.value();
                }
            }
        }

        return -1;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack stack, TooltipType context, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(stack, context, lookup, player);
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            int color = type.color(stack);
            var visual = type.visuals(stack);
            out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of()));

            if (visual.components().isPresent()) {
                out.applyComponentsFrom(visual.components().get());
            }


            out.set(DataComponentTypes.FOOD, new FoodComponent(0, 0, true, (float) type.drinkingTime(stack, player), Optional.empty(), List.of()));
        } else {
            out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(0x385dc6), List.of()));
        }

        return out;
    }

    /*@Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            var visuals = type.visuals(stack);
            return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }*/
}
