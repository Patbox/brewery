package eu.pb4.brewery.item.debug;

import eu.pb4.brewery.block.entity.TickableContents;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

public class BlockTickerItem extends Item implements PolymerItem {
    public BlockTickerItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var be = context.getLevel().getBlockEntity(context.getClickedPos());

        if (be instanceof TickableContents tickableContents) {
            tickableContents.tickContents(context.getItemInHand().getOrDefault(BrewComponents.TICK_COUNT, 0));
        }

        return super.useOn(context);
    }

    @Override
    public Component getName(ItemStack stack) {
        var tick = stack.getOrDefault(BrewComponents.TICK_COUNT, 0);
        return Component.literal("debug/BlockTickerItem [" + tick + " ticks | "
                + ((int) (tick / 20d / 60d * 100) / 100d) + " minutes | "
                + ((int) (tick / 24000d * 100) / 100d) + " days]");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public ItemStack create(int ticks) {
        var stack = new ItemStack(this);
        stack.set(BrewComponents.TICK_COUNT, ticks);
        return stack;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.STICK;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }
}
