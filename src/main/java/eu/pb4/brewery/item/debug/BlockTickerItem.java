package eu.pb4.brewery.item.debug;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.entity.TickableContents;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

public class BlockTickerItem extends Item implements PolymerItem {
    public BlockTickerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var be = context.getWorld().getBlockEntity(context.getBlockPos());

        if (be instanceof TickableContents tickableContents) {
            tickableContents.tickContents(context.getStack().getOrCreateNbt().getInt("TickCount"));
        }

        return super.useOnBlock(context);
    }

    @Override
    public Text getName(ItemStack stack) {
        var tick = stack.getOrCreateNbt().getInt("TickCount");
        return Text.literal("debug/BlockTickerItem [" + tick + " ticks | "
                + ((int) (tick / 20d / 60d * 100) / 100d) + " minutes | "
                + ((int) (tick / 24000d * 100) / 100d) + " days]");
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    public ItemStack create(int ticks) {
        var stack = new ItemStack(this);
        stack.getOrCreateNbt().putInt("TickCount", ticks);
        return stack;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.STICK;
    }
}
