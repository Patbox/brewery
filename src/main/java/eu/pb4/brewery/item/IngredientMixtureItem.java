package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class IngredientMixtureItem extends Item implements PolymerItem {
    public IngredientMixtureItem(Item.Settings settings) {
        super(settings);
    }

    public static List<ItemStack> getIngredients(ItemStack stack) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).ingredients();
        }
        return List.of();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        var world = BreweryInit.getOverworld();

        if (stack.contains(BrewComponents.COOKING_DATA) && world != null && world.getGameRules().getValue(BrewGameRules.SHOW_AGE)) {
            var time = Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).time();
            double mult = world.getGameRules().getValue(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER);

            var age = DrinkUtils.getAgeInSeconds(stack) / mult;

            if (age > 0) {
                textConsumer.accept(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).formatted(Formatting.GRAY)));
            }

            textConsumer.accept(Text.translatable("text.brewery.cooked_for", BrewUtils.fromTimeShort(time / 20d / mult).formatted(Formatting.GRAY)));
            for (var ingredient : getIngredients(stack)) {
                textConsumer.accept(Text.empty().append("" + ingredient.getCount()).append(" × ").append(ingredient.getName()).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var model = BreweryInit.CONTAINER_TO_INGMIX_MODEL.get(DrinkUtils.getContainer(stack).getItem());
        if (model != null) {
            return model;
        }

        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(3694022), List.of(), Optional.empty()));

        out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                FloatList.of((float) DrinkUtils.getQuality(stack), (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                BooleanList.of(false, DrinkUtils.getDistillationStatus(stack)),
                List.of("", DrinkUtils.getBarrelType(stack), "mixture"),
                IntList.of(3694022, 3694022)
        ));
    }
}
