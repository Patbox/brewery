package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

public class IngredientMixtureItem extends Item implements PolymerItem {
    public IngredientMixtureItem(Item.Properties settings) {
        super(settings);
    }

    public static List<ItemStack> getIngredients(ItemStack stack) {
        if (stack.has(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).ingredients().stream().map(ItemStackTemplate::create).toList();
        }
        return List.of();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        var world = BreweryInit.getOverworld();

        if (stack.has(BrewComponents.COOKING_DATA) && world != null && world.getGameRules().get(BrewGameRules.SHOW_AGE)) {
            var time = Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).time();
            double mult = world.getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER);

            var age = DrinkUtils.getAgeInSeconds(stack) / mult;

            if (age > 0) {
                textConsumer.accept(Component.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).withStyle(ChatFormatting.GRAY)));
            }

            textConsumer.accept(Component.translatable("text.brewery.cooked_for", BrewUtils.fromTimeShort(time / 20d / mult).withStyle(ChatFormatting.GRAY)));
            for (var ingredient : getIngredients(stack)) {
                textConsumer.accept(Component.empty().append("" + ingredient.getCount()).append(" × ").append(ingredient.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        var model = BreweryInit.CONTAINER_TO_INGMIX_MODEL.get(DrinkUtils.getContainer(stack).getItem());
        if (model != null) {
            return model;
        }

        return Items.POTION.components().get(DataComponents.ITEM_MODEL);
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        out.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(),
                Optional.of(3694022), List.of(), Optional.empty()));

        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                BooleanList.of(false, DrinkUtils.getDistillationStatus(stack)),
                List.of("", DrinkUtils.getBarrelType(stack), "mixture"),
                IntList.of(3694022, 3694022)
        ));
    }
}
