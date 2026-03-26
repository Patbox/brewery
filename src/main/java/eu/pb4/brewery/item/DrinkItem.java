package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DrinkItem extends Item implements PolymerItem {
    public DrinkItem(Properties settings) {
        super(settings.stacksTo(1).component(DataComponents.CONSUMABLE, new Consumable(32 / 20f, ItemUseAnimation.DRINK, SoundEvents.GENERIC_DRINK, false, List.of())));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        var type = DrinkUtils.getType(stack);

        if (type == null) {
            return 32 * 3;
        } else {
            return (int) (type.drinkingTime(stack, user) * 20);
        }
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        if (type == null) {
            return ItemUseAnimation.DRINK;
        } else {
            return type.visuals(stack).animation();
        }
    }

    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        Player playerEntity = user instanceof Player ? (Player) user : null;
        if (playerEntity instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) playerEntity, stack);
        }

        if (!world.isClientSide()) {
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
            playerEntity.awardStat(Stats.ITEM_USED.get(this));
        }
        user.gameEvent(GameEvent.DRINK);

        var cookingData = stack.get(BrewComponents.COOKING_DATA);
        stack.consume(1, playerEntity);

        if (playerEntity == null || !playerEntity.isCreative()) {
            if (cookingData != null) {
                if (stack.isEmpty()) {
                    return cookingData.container().map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
                }

                if (playerEntity != null) {
                    playerEntity.getInventory().add(cookingData.container().map(ItemStackTemplate::create).orElse(ItemStack.EMPTY));
                }
            }
        }

        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            if (type.isFinished(stack)) {
                return type.name(stack).text();
            } else {
                return Component.translatable("item.brewery.ingredient_mixture_specific", type.name(stack).text());
            }
        } else {
            var id = DrinkUtils.getType(stack);

            Component text;

            if (id != null) {
                text = id.name(stack).text();
            } else {
                text = Component.literal("<Unknown>");
            }

            return Component.translatable(this.getDescriptionId(), text);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag typex) {
        var world = BreweryInit.getOverworld();
        if (world != null) {
            var type = DrinkUtils.getType(stack);
            if (type != null && type.showQuality() && world.getGameRules().get(BrewGameRules.SHOW_QUALITY)) {
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

                textConsumer.accept(Component.translatable("text.brewery.quality", Component.empty()
                        .append(Component.literal(stars.toString()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(antistars.toString()).withStyle(ChatFormatting.DARK_GRAY))
                ));
            }

            if (world.getGameRules().get(BrewGameRules.SHOW_AGE)) {
                double mult = world != null ? world.getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER) : 1;

                var age = DrinkUtils.getAgeInSeconds(stack) / mult;
                if (age > 0) {
                    textConsumer.accept(Component.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).withStyle(ChatFormatting.GRAY)));
                }
            }

            if (BreweryInit.DISPLAY_DEV) {
                textConsumer.accept(Component.literal("== DEV ==").withStyle(ChatFormatting.AQUA));
                textConsumer.accept(Component.literal("BrewType: ").append(stack.getOrDefault(BrewComponents.BREW_DATA, BrewData.DEFAULT).type().toString()).withStyle(ChatFormatting.GRAY));
                textConsumer.accept(Component.literal("BrewQuality: ").append("" + DrinkUtils.getQuality(stack)).withStyle(ChatFormatting.GRAY));
                textConsumer.accept(Component.literal("BrewAge: ").append("" + DrinkUtils.getAgeInTicks(stack)).withStyle(ChatFormatting.GRAY));
                textConsumer.accept(Component.literal("BrewDistillated: ").append("" + DrinkUtils.getDistillationStatus(stack)).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            int color = type.color(stack);
            var visual = type.visuals(stack);
            out.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));

            if (visual.components().isPresent()) {
                out.applyComponents(visual.components().get());
            }

            out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                    BooleanList.of(type.isFinished(stack), DrinkUtils.getDistillationStatus(stack)),
                    List.of(DrinkUtils.getTypeId(stack).toString(), DrinkUtils.getBarrelType(stack), type.isFinished(stack) ? "finished_drink" : "unfinished_drink"),
                    IntList.of(color, color)
            ));

            out.set(DataComponents.CONSUMABLE, new Consumable((float) type.drinkingTime(stack, PolymerCommonUtils.getPlayer(context)), visual.animation(), visual.soundEvent(), visual.particles(), List.of()));
        } else {
            out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                    BooleanList.of(false, DrinkUtils.getDistillationStatus(stack)),
                    List.of("", DrinkUtils.getBarrelType(stack), "unknown_drink"),
                    IntList.of(0x385dc6, 0x385dc6))
            );
        }
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider provider) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            var visuals = type.visuals(stack);
            return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION.components().get(DataComponents.ITEM_MODEL);
    }
}
