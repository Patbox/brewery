package eu.pb4.brewery.item;

import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import org.jetbrains.annotations.Nullable;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().saturationModifier(-0.2f)
                .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20), 0.95f)
                .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 10 * 20), 0.80f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20), 0.60f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20, 1), 0.30f)
                .statusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10 * 20), 0.60f)
                .build()));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.POTION;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var out =  PolymerItem.super.getPolymerItemStack(itemStack, player);
        out.getOrCreateNbt().putInt("CustomPotionColor", 0x051a0a);
        return out;
    }
}
