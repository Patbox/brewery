package eu.pb4.brewery.item;

import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Properties settings) {
        super(settings.food(new FoodProperties.Builder().alwaysEdible().saturationModifier(-0.2f).build(),
                Consumable.builder()
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 30 * 20), 0.95f))
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NAUSEA, 10 * 20), 0.80f))
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10 * 20), 0.60f))
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10 * 20, 1), 0.30f))
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20), 0.60f))
                        .consumeSeconds(32 / 20f * 3)
                        .hasConsumeParticles(true)
                        .animation(ItemUseAnimation.DRINK)
                        .build()
        ));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!user.hasInfiniteMaterials()) {
            var cookingData = stack.get(BrewComponents.COOKING_DATA);
            if (cookingData != null) {
                user.handleExtraItemsCreatedOnUse(cookingData.container().copy());
            }
        }
        return super.finishUsingItem(stack, world, user);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            var visuals = type.failedVisuals();
            return visuals.resourcePackModel().isPresent() && PolymerResourcePackUtils.hasMainPack(context) ? visuals.resourcePackModel().get() : visuals.defaultModel();
        }

        return Items.POTION.components().get(DataComponents.ITEM_MODEL);
    }


    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);
        var color = type != null ? type.failedColor() : 0x051a0a;
        out.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(),
                Optional.of(color), List.of(), Optional.empty()));

        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                FloatList.of(0, (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                BooleanList.of(false, false),
                List.of(type != null ? DrinkUtils.getTypeId(stack).toString() : "", DrinkUtils.getBarrelType(stack), "failed_drink"),
                IntList.of(color, type != null ? type.color(stack) : color))
        );
    }
}
