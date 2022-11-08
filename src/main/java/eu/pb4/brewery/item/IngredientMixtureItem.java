package eu.pb4.brewery.item;

import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class IngredientMixtureItem extends Item implements PolymerItem {
    public IngredientMixtureItem(Item.Settings settings) {
        super(settings);
    }

    public static List<ItemStack> getIngredients(ItemStack stack) {
        if (stack.hasNbt()) {
            var list = new ArrayList<ItemStack>();
            for (var nbt : stack.getNbt().getList("Ingredients", NbtElement.COMPOUND_TYPE)) {
                list.add(ItemStack.fromNbt((NbtCompound) nbt));
            }

            return list;
        }
        return List.of();
    }

    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.hasNbt() && world.getGameRules().getBoolean(BrewGameRules.SHOW_AGE)) {
            var time = stack.getNbt().getDouble(DrinkUtils.AGE_COOK_NBT);
            double mult = world != null ? world.getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get() : 1;

            var age = DrinkUtils.getAgeInSeconds(stack) / mult;

            if (age > 0) {
                tooltip.add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).formatted(Formatting.GRAY)));
            }

            tooltip.add(Text.translatable("text.brewery.cooked_for", BrewUtils.fromTimeShort(time / 20d / mult).formatted(Formatting.GRAY)));
            for (var ingredient : getIngredients(stack)) {
                tooltip.add(Text.empty().append("" + ingredient.getCount()).append(" × ").append(ingredient.getName()).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.POTION;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, player);
        out.getOrCreateNbt().putInt("CustomPotionColor", 3694022);
        return out;
    }
}
