package eu.pb4.brewery.block.entity;

import com.mojang.serialization.Codec;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BarrelMaterial;
import eu.pb4.brewery.block.BrewBarrelPartBlock;
import eu.pb4.brewery.block.BrewBlocks;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.IngredientMixtureItem;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BrewBarrelSpigotBlockEntity extends RandomizableContainerBlockEntity implements TickableContents, WorldlyContainer {
    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private final LongSet parts = new LongArraySet();
    private NonNullList<ItemStack> inventory;
    private long lastTicked = -1;
    private int loadedTime;
    private boolean requestUpdate;
    private BarrelMaterial material  = BarrelMaterial.EMPTY;

    public BrewBarrelSpigotBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BrewBlockEntities.BARREL_SPIGOT, blockPos, blockState);
        this.inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof BrewBarrelSpigotBlockEntity)) {
            return;
        }

        var barrel = (BrewBarrelSpigotBlockEntity) t;

        barrel.loadedTime++;

        if (barrel.requestUpdate && world.getGameTime() % 20 == 0) {
            barrel.updateAges();
        }
    }

    private void requestUpdate() {
        this.requestUpdate = true;
    }

    public void updateAges() {
        var currentTime = level.getGameTime();


        if (this.lastTicked == -1) {
            this.lastTicked = level.getGameTime();
            return;
        }

        var agingMultiplier = ((ServerLevel) level).getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER);

        this.tickContents(((ServerLevel) level).getGameRules().get(BrewGameRules.AGE_UNLOADED) ? (currentTime - this.lastTicked) * agingMultiplier : this.loadedTime * agingMultiplier);

        this.lastTicked = currentTime;
        this.loadedTime = 0;
        this.requestUpdate = false;
    }

    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.trySaveLootTable(view)) {
            ContainerHelper.saveAllItems(view, this.inventory);
        }

        view.store("Parts", Codec.LONG_STREAM, Arrays.stream(this.parts.toLongArray()));
        view.putString("BarrelType", this.material.type().toString());
        view.putLong("LastTicked", this.lastTicked);
    }

    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(view)) {
            ContainerHelper.loadAllItems(view, this.inventory);
        }

        this.parts.clear();
        view.read("Parts", Codec.LONG_STREAM).orElse(LongStream.empty()).forEach(this.parts::add);

        this.material = BrewBlocks.BARREL_MATERIAL_MAP.get(Identifier.parse(view.getStringOr("BarrelType", "")));
        this.lastTicked = view  .getLongOr("LastTicked", 0);
    }

    public void tickContents(double l) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            var stack = this.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(BrewItems.INGREDIENT_MIXTURE) || stack.is(BrewItems.DRINK_ITEM)) {
                    var age = DrinkUtils.getAgeInTicks(stack, 0) + l;
                    var ageSec = age / 20d;

                    var oldType = DrinkUtils.getType(stack);

                    if (oldType == null && stack.has(BrewComponents.BREW_DATA) && Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().isPresent()) {
                        continue;
                    }

                    var ingredients = IngredientMixtureItem.getIngredients(stack);
                    var types = DrinkUtils.findTypes(ingredients, this.material.type(), DrinkUtils.getHeatSource(stack), DrinkUtils.getContainer(stack));

                    if (types.isEmpty() && oldType == null) {
                        this.setItem(i, stack.transmuteCopy(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                    } else {
                        double quality = -1;
                        DrinkType match = null;
                        //boolean isMatchGeneric = true;

                        for (var type : types) {
                            var barrelInfo = type.getBarrelInfo(this.material.type());
                            if (barrelInfo != null && ageSec >= barrelInfo.baseTime()) {
                                var newAge = ageSec - barrelInfo.baseTime();

                                var q = barrelInfo.qualityChange().expression()
                                        .setVariable(ExpressionUtil.QUALITY_KEY, type.baseQuality().expression().setVariable(ExpressionUtil.AGE_KEY, newAge).evaluate())
                                        .setVariable(ExpressionUtil.AGE_KEY, newAge)
                                        .evaluate();

                                if (q > quality) {
                                    quality = q;
                                    match = type;
                                }
                            }
                        }
                        if (match == null && oldType != null) {
                            match = oldType;

                        }

                        if (match == null) {
                            stack.update(BrewComponents.BREW_DATA, BrewData.DEFAULT, x -> x.withAge(age));
                        } else if (quality < 0) {
                            this.setItem(i, stack.transmuteCopy(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                        } else {
                            if (stack.is(BrewItems.INGREDIENT_MIXTURE)) {
                                var nStack = new ItemStack(BrewItems.DRINK_ITEM);
                                nStack.set(BrewComponents.COOKING_DATA, stack.get(BrewComponents.COOKING_DATA));
                                stack = nStack;
                                this.setItem(i, stack);
                            }
                            var barrelInfo = match.getBarrelInfo(this.material.type());

                            if (barrelInfo != null) {
                                var mult = match.cookingQualityMult().expression().setVariable("age", stack.getOrDefault(BrewComponents.COOKING_DATA, CookingData.DEFAULT).time() / 20).evaluate();

                                if (quality * mult >= 0) {
                                    stack.set(BrewComponents.BREW_DATA, new BrewData(Optional.of(BreweryInit.DRINK_TYPE_ID.get(match)), Math.min(quality * mult, 10), this.material.type().toString(), 0, age));
                                } else {
                                    this.setItem(i, stack.transmuteCopy(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return this.material.name();
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return null;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    public void addPart(BlockPos pos) {
        this.parts.add(pos.asLong());
        this.setChanged();
    }

    public LongSet getParts() {
        return this.parts;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);

        if (this.level != null) {
            for (var part : this.iterableParts()) {
                var partState = level.getBlockState(part);

                if (partState.getBlock() instanceof BrewBarrelPartBlock block) {
                    level.setBlockAndUpdate(part, partState.getValue(BrewBarrelPartBlock.SHAPE).state.apply(block.barrelMaterial));
                }
            }
        }
    }

    public Iterable<BlockPos.MutableBlockPos> iterableParts() {
        var pos = new BlockPos.MutableBlockPos();
        return () -> new java.util.Iterator<>() {
            final LongIterator iter = BrewBarrelSpigotBlockEntity.this.parts.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public BlockPos.MutableBlockPos next() {
                var val = iter.nextLong();
                return pos.set(BlockPos.getX(val), BlockPos.getY(val), BlockPos.getZ(val));
            }
        };
    }

    public void setBarrelType(BarrelMaterial material) {
        this.material = material;
    }

    public void openGui(ServerPlayer player) {
        this.updateAges();
        new Gui(player);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return canInsert(stack);
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.is(BrewItems.DRINK_ITEM)) {

            var type = DrinkUtils.getType(stack);
            var barrelType = DrinkUtils.getBarrelType(stack);
            return type != null && !type.barrelInfo().isEmpty() &&
                    (barrelType.isEmpty() || barrelType.equals(this.material.type()) || DrinkUtils.getAgeInTicks(stack) <= 0);
        } else {
            return stack.is(BrewItems.INGREDIENT_MIXTURE);
        }
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(BrewBarrelSpigotBlockEntity.this.getDisplayName());

            for (int i = 0; i < BrewBarrelSpigotBlockEntity.this.getContainerSize(); i++) {
                this.setSlotRedirect(i, new Slot(BrewBarrelSpigotBlockEntity.this, i, 0, 0) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return BrewBarrelSpigotBlockEntity.this.canInsert(stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return 1;
                    }
                });
            }
            this.open();
        }


        @Override
        public void onTick() {
            if (BrewBarrelSpigotBlockEntity.this.isRemoved()
                    || BrewBarrelSpigotBlockEntity.this.getBlockPos().distToCenterSqr(this.player.getX(), this.player.getY(), this.player.getZ()) > 20 * 20) {
                this.close();
            }

            BrewBarrelSpigotBlockEntity.this.requestUpdate();
            super.onTick();
        }
    }
}
