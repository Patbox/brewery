package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().saturationModifier(-0.2f)
                .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20), 0.95f)
                .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 10 * 20), 0.80f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20), 0.60f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20, 1), 0.30f)
                .statusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10 * 20), 0.60f)
                //.consumeSeconds(32 / 20f * 3)
                .build()));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!user.isInCreativeMode()) {
            var cookingData = stack.get(BrewComponents.COOKING_DATA);
            if (cookingData != null) {
                if (user instanceof ServerPlayerEntity player) {
                    player.getInventory().offerOrDrop(cookingData.container().copy());
                }
            }
        }
        return super.finishUsing(stack, world, user);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            var visuals = type.failedVisuals();

            if (Registries.ITEM.containsId(visuals.defaultModel())) {
                return Registries.ITEM.get(visuals.defaultModel());
            }

            //return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION;
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            var visuals = type.failedVisuals();
            if (visuals.resourcePackModel().isPresent()) {
                var val = BreweryInit.RESOURCE_PACK_MODELS.getOrDefault(visuals.defaultModel(), Map.of()).get(visuals.resourcePackModel().get());
                if (val != null) {
                    return val.value();
                }
            }
        }

        return -1;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    /*@Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            var visuals = type.failedVisuals();
            return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }*/

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, lookup, player);
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            type.failedVisuals().components().ifPresent(out::applyComponentsFrom);
        }
        var color = type != null ? type.failedColor() : 0x051a0a;
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(color), List.of()));
        return out;
    }
}
