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
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

public class DrinkItem extends Item implements PolymerItem {
    public DrinkItem(Settings settings) {
        super(settings.maxCount(1).component(DataComponentTypes.CONSUMABLE, new ConsumableComponent(32 / 20f, UseAction.DRINK, SoundEvents.ENTITY_GENERIC_DRINK, false, List.of())));
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

        if (type == null) {
            return UseAction.DRINK;
        } else {
            return type.visuals(stack).animation();
        }
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
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            int color = type.color(stack);
            var visual = type.visuals(stack);
            out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));

            if (visual.components().isPresent()) {
                out.applyComponentsFrom(visual.components().get());
            }

            out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                    BooleanList.of(type.isFinished(stack), DrinkUtils.getDistillationStatus(stack)),
                    List.of(DrinkUtils.getTypeId(stack).toString(), DrinkUtils.getBarrelType(stack), type.isFinished(stack) ? "finished_drink" : "unfinished_drink"),
                    IntList.of(color, color)
            ));

            out.set(DataComponentTypes.CONSUMABLE, new ConsumableComponent((float) type.drinkingTime(stack, context.getPlayer()), visual.animation(), visual.soundEvent(), visual.particles(), List.of()));
        } else {
            out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                    BooleanList.of(false, DrinkUtils.getDistillationStatus(stack)),
                    List.of("", DrinkUtils.getBarrelType(stack), "unknown_drink"),
                    IntList.of(0x385dc6, 0x385dc6))
            );
        }
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            var visuals = type.visuals(stack);
            return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }
}
