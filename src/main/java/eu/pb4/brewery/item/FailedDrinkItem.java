package eu.pb4.brewery.item;

import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().saturationModifier(-0.2f).build(),
                ConsumableComponent.builder()
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20), 0.95f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 10 * 20), 0.80f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20), 0.60f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20, 1), 0.30f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10 * 20), 0.60f))
                        .consumeSeconds(32 / 20f * 3)
                        .consumeParticles(true)
                        .useAction(UseAction.DRINK)
                        .build()
        ));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!user.isInCreativeMode()) {
            var cookingData = stack.get(BrewComponents.COOKING_DATA);
            if (cookingData != null) {
                user.giveOrDropStack(cookingData.container().copy());
            }
        }
        return super.finishUsing(stack, world, user);
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

        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }


    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);
        var color = type != null ? type.failedColor() : 0x051a0a;
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(color), List.of(), Optional.empty()));

        out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                FloatList.of(0, (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                BooleanList.of(false, false),
                List.of(type != null ? DrinkUtils.getTypeId(stack).toString() : "", DrinkUtils.getBarrelType(stack), "failed_drink"),
                IntList.of(color, type != null ? type.color(stack) : color))
        );
    }
}
