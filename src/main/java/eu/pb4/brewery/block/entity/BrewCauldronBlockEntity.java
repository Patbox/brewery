package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewCauldronBlock;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class BrewCauldronBlockEntity extends BlockEntity implements TickableContents {
    private NbtList inventory = new NbtList();
    private long lastTicked = -1;
    private double timeCooking = 0;
    private ElementHolder elementHolder;
    private TextDisplayElement[] textDisplayElement = new TextDisplayElement[4];

    public BrewCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.CAULDRON, pos, state);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        if (t instanceof BrewCauldronBlockEntity cauldron && world instanceof ServerWorld world1) {
            var currentTime = world.getTime();

            if (cauldron.lastTicked == -1) {
                cauldron.lastTicked = currentTime;
                return;
            }

            if (cauldron.elementHolder == null) {
                cauldron.elementHolder = new ElementHolder();;
                var matrix = new Matrix4f();
                var seconds = (int) (cauldron.timeCooking / 20) % 60;
                var minutes = (int) (cauldron.timeCooking / (20 * 60));
                var text = Text.literal(minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
                for (int i = 0; i < 4; i++) {
                    var element = cauldron.textDisplayElement[i] = new TextDisplayElement();
                    element.setTransformation(matrix.rotationY(i * MathHelper.HALF_PI).translate(0,-0.14f ,0.51f));
                    element.setDisplayHeight(1f);
                    element.setDisplayWidth(1f);
                    element.setText(text);
                    element.setViewRange(0.3f);
                    cauldron.elementHolder.addElement(element);
                }

                ChunkAttachment.of(cauldron.elementHolder, world1, cauldron.pos);
            }

            if (BrewCauldronBlock.isValid(pos, state, world)) {
                cauldron.timeCooking += world.getGameRules().getBoolean(BrewGameRules.AGE_UNLOADED) ? (currentTime - cauldron.lastTicked) : 1;

                world1.spawnParticles(ParticleTypes.BUBBLE_POP,
                        0.4 * world.random.nextFloat(),
                        (6.0D + (double)state.get(LeveledCauldronBlock.LEVEL) * 3.0D) / 16.0D,
                        0.4 * world.random.nextFloat(),
                0, 0, 0, 0, 0);

                if (cauldron.timeCooking % 20 == 0) {
                    var seconds = (int) (cauldron.timeCooking / 20) % 60;
                    var minutes = (int) (cauldron.timeCooking / (20 * 60));
                    var text = Text.literal(minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
                    for (int i = 0; i < 4; i++) {
                        cauldron.textDisplayElement[i].setText(text);
                    }
                    cauldron.elementHolder.tick();
                }
            }

            cauldron.lastTicked = currentTime;
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (this.elementHolder != null) {
            this.elementHolder.destroy();
        }
        this.elementHolder = null;
    }

    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("LastTicked", this.lastTicked);
        nbt.put("Ingredients", this.inventory);
        nbt.putDouble("CookingTime", this.timeCooking);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lastTicked = nbt.getLong("LastTicked");
        this.inventory = nbt.getList("Ingredients", NbtElement.COMPOUND_TYPE);
        this.timeCooking = nbt.getDouble("CookingTime");
    }

    public void addIngredients(List<ItemEntity> entities) {
        var items = new ArrayList<ItemStack>();
        for (var entity : entities) {
            if (!entity.getStack().isEmpty()) {
                items.add(entity.getStack());
            }

            entity.discard();
        }

        for (var item : items) {
            this.inventory.add(item.writeNbt(new NbtCompound()));
        }
    }

    public boolean onUse(PlayerEntity player, Hand hand) {
        var stack = player.getStackInHand(hand);

        if (!stack.isEmpty() && stack.isOf(Items.GLASS_BOTTLE)) {
            stack.decrement(1);
            var agingMultiplier = this.world.getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get();

            var ingredients = new ArrayList<ItemStack>();

            for (var nbt : this.inventory) {
                ingredients.add(ItemStack.fromNbt((NbtCompound) nbt));
            }
            var heatSource = this.getWorld().getBlockState(this.pos.down()).getBlock();
            var types = DrinkUtils.findTypes(ingredients, null, heatSource);

            var age = this.timeCooking * agingMultiplier;

            {
                double quality = Double.MIN_VALUE;
                DrinkType match = null;

                for (var type : types) {
                    if (!type.requireDistillation()) {
                        var q = type.cookingQualityMult().expression()
                                .setVariable(ExpressionUtil.AGE_KEY, age / 20d)
                                .evaluate();

                        if (q > quality) {
                            quality = q;
                            match = type;
                        }
                    }
                }

                if (match == null || quality < 0) {
                    var out = new ItemStack(BrewItems.INGREDIENT_MIXTURE);

                    out.getOrCreateNbt().putDouble(DrinkUtils.AGE_COOK_NBT, age);
                    out.getOrCreateNbt().put("Ingredients", this.inventory.copy());
                    out.getOrCreateNbt().putString(DrinkUtils.HEAT_SOURCE_NBT, Registries.BLOCK.getId(heatSource).toString());

                    player.giveItemStack(out);
                } else {
                    player.giveItemStack(DrinkUtils.createDrink(BreweryInit.DRINK_TYPE_ID.get(match), 0, quality * 10, 0, Registries.BLOCK.getId(heatSource)));
                }
            }

            var level = this.getCachedState().get(LeveledCauldronBlock.LEVEL) - 1;

            if (level == 0) {
                this.world.setBlockState(this.pos, Blocks.CAULDRON.getDefaultState());

            } else {
                this.world.setBlockState(this.pos,this.getCachedState().with(LeveledCauldronBlock.LEVEL, level));
            }

            return true;
        }


        return false;
    }

    @Override
    public void tickContents(double ticksPassed) {
        this.timeCooking += ticksPassed;
    }
}
