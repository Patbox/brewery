package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BarrelMaterial;
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
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

public final class BrewBarrelSpigotBlockEntity extends LootableContainerBlockEntity implements TickableContents, SidedInventory {
    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private final LongSet parts = new LongArraySet();
    private DefaultedList<ItemStack> inventory;
    private long lastTicked = -1;
    private int loadedTime;
    private boolean requestUpdate;
    private BarrelMaterial material  = BarrelMaterial.EMPTY;

    public BrewBarrelSpigotBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BrewBlockEntities.BARREL_SPIGOT, blockPos, blockState);
        this.inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof BrewBarrelSpigotBlockEntity)) {
            return;
        }

        var barrel = (BrewBarrelSpigotBlockEntity) t;

        barrel.loadedTime++;

        if (barrel.requestUpdate && world.getTime() % 20 == 0) {
            barrel.updateAges();
        }
    }

    private void requestUpdate() {
        this.requestUpdate = true;
    }

    public void updateAges() {
        var currentTime = world.getTime();


        if (this.lastTicked == -1) {
            this.lastTicked = world.getTime();
            return;
        }

        var agingMultiplier = ((ServerWorld) world).getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get();

        this.tickContents(((ServerWorld) world).getGameRules().getBoolean(BrewGameRules.AGE_UNLOADED) ? (currentTime - this.lastTicked) * agingMultiplier : this.loadedTime * agingMultiplier);

        this.lastTicked = currentTime;
        this.loadedTime = 0;
        this.requestUpdate = false;
    }

    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (!this.writeLootTable(nbt)) {
            Inventories.writeNbt(nbt, this.inventory, lookup);
        }

        nbt.put("Parts", new NbtLongArray(this.parts.toLongArray()));
        nbt.putString("BarrelType", this.material.type().toString());
        nbt.putLong("LastTicked", this.lastTicked);
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.readLootTable(nbt)) {
            Inventories.readNbt(nbt, this.inventory, lookup);
        }

        this.parts.clear();
        for (var part : nbt.getLongArray("Parts").orElse(new long[0])) {
            this.parts.add(part);
        }

        this.material = BrewBlocks.BARREL_MATERIAL_MAP.get(Identifier.of(nbt.getString("BarrelType", "")));
        this.lastTicked = nbt.getLong("LastTicked", 0);
    }

    public void tickContents(double l) {
        for (int i = 0; i < this.size(); i++) {
            var stack = this.getStack(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(BrewItems.INGREDIENT_MIXTURE) || stack.isOf(BrewItems.DRINK_ITEM)) {
                    var age = DrinkUtils.getAgeInTicks(stack, 0) + l;
                    var ageSec = age / 20d;

                    var oldType = DrinkUtils.getType(stack);

                    if (oldType == null && stack.contains(BrewComponents.BREW_DATA) && Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().isPresent()) {
                        continue;
                    }

                    var ingredients = IngredientMixtureItem.getIngredients(stack);
                    var types = DrinkUtils.findTypes(ingredients, this.material.type(), DrinkUtils.getHeatSource(stack), DrinkUtils.getContainer(stack));

                    if (types.isEmpty() && oldType == null) {
                        this.setStack(i, stack.copyComponentsToNewStack(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
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
                            stack.apply(BrewComponents.BREW_DATA, BrewData.DEFAULT, x -> x.withAge(age));
                        } else if (quality < 0) {
                            this.setStack(i, stack.copyComponentsToNewStack(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                        } else {
                            if (stack.isOf(BrewItems.INGREDIENT_MIXTURE)) {
                                var nStack = new ItemStack(BrewItems.DRINK_ITEM);
                                nStack.set(BrewComponents.COOKING_DATA, stack.get(BrewComponents.COOKING_DATA));
                                stack = nStack;
                                this.setStack(i, stack);
                            }
                            var barrelInfo = match.getBarrelInfo(this.material.type());

                            if (barrelInfo != null) {
                                var mult = match.cookingQualityMult().expression().setVariable("age", stack.getOrDefault(BrewComponents.COOKING_DATA, CookingData.DEFAULT).time() / 20).evaluate();

                                if (quality * mult >= 0) {
                                    stack.set(BrewComponents.BREW_DATA, new BrewData(Optional.of(BreweryInit.DRINK_TYPE_ID.get(match)), Math.min(quality * mult, 10), this.material.type().toString(), 0, age));
                                } else {
                                    this.setStack(i, stack.copyComponentsToNewStack(BrewItems.FAILED_DRINK_ITEM, stack.getCount()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Text getContainerName() {
        return this.material.name();
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return null;
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    public int size() {
        return 27;
    }

    public void addPart(BlockPos pos) {
        this.parts.add(pos.asLong());
        this.markDirty();
    }

    public LongSet getParts() {
        return this.parts;
    }

    public Iterable<BlockPos.Mutable> iterableParts() {
        var pos = new BlockPos.Mutable();
        return () -> new java.util.Iterator<>() {
            final LongIterator iter = BrewBarrelSpigotBlockEntity.this.parts.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public BlockPos.Mutable next() {
                var val = iter.nextLong();
                return pos.set(BlockPos.unpackLongX(val), BlockPos.unpackLongY(val), BlockPos.unpackLongZ(val));
            }
        };
    }

    public void setBarrelType(BarrelMaterial material) {
        this.material = material;
    }

    public void openGui(ServerPlayerEntity player) {
        this.updateAges();
        new Gui(player);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return canInsert(stack);
    }

    public boolean canInsert(ItemStack stack) {
        if (stack.isOf(BrewItems.DRINK_ITEM)) {

            var type = DrinkUtils.getType(stack);
            var barrelType = DrinkUtils.getBarrelType(stack);
            return type != null && !type.barrelInfo().isEmpty() &&
                    (barrelType.isEmpty() || barrelType.equals(this.material.type()) || DrinkUtils.getAgeInTicks(stack) <= 0);
        } else {
            return stack.isOf(BrewItems.INGREDIENT_MIXTURE);
        }
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(BrewBarrelSpigotBlockEntity.this.getDisplayName());

            for (int i = 0; i < BrewBarrelSpigotBlockEntity.this.size(); i++) {
                this.setSlotRedirect(i, new Slot(BrewBarrelSpigotBlockEntity.this, i, 0, 0) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return BrewBarrelSpigotBlockEntity.this.canInsert(stack);
                    }

                    @Override
                    public int getMaxItemCount() {
                        return 1;
                    }

                    @Override
                    public int getMaxItemCount(ItemStack stack) {
                        return 1;
                    }
                });
            }
            this.open();
        }


        @Override
        public void onTick() {
            if (BrewBarrelSpigotBlockEntity.this.isRemoved()
                    || BrewBarrelSpigotBlockEntity.this.getPos().getSquaredDistanceFromCenter(this.player.getX(), this.player.getY(), this.player.getZ()) > 20 * 20) {
                this.close();
            }

            BrewBarrelSpigotBlockEntity.this.requestUpdate();
            super.onTick();
        }
    }
}
