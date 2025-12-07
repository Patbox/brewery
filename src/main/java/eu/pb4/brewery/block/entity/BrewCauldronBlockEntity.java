package eu.pb4.brewery.block.entity;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.block.BrewCauldronBlock;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BrewCauldronBlockEntity extends BlockEntity implements TickableContents {
    private List<ItemStack> inventory = new ArrayList<>();
    private long lastTicked = -1;
    private double timeCooking = 0;
    private ElementHolder elementHolder;
    private final TextDisplayElement[] textDisplayElement = new TextDisplayElement[4];

    public BrewCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(BrewBlockEntities.CAULDRON, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        if (t instanceof BrewCauldronBlockEntity cauldron && world instanceof ServerLevel world1) {
            var currentTime = world.getGameTime();

            if (cauldron.lastTicked == -1) {
                cauldron.lastTicked = currentTime;
                return;
            }

            if (cauldron.elementHolder == null) {
                cauldron.elementHolder = new ElementHolder();;
                var matrix = new Matrix4f();
                var seconds = (int) (cauldron.timeCooking / 20) % 60;
                var minutes = (int) (cauldron.timeCooking / (20 * 60));
                var text = Component.literal(minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
                for (int i = 0; i < 4; i++) {
                    var element = cauldron.textDisplayElement[i] = new TextDisplayElement();
                    element.setTransformation(matrix.rotationY(i * Mth.HALF_PI).translate(0,-0.14f ,0.51f));
                    element.setDisplayHeight(1f);
                    element.setDisplayWidth(1f);
                    element.setText(text);
                    element.setViewRange(0.3f);
                    cauldron.elementHolder.addElement(element);
                }

                ChunkAttachment.of(cauldron.elementHolder, world1, cauldron.worldPosition);
            }

            if (BrewCauldronBlock.isValid(pos, state, world)) {
                cauldron.timeCooking += world1.getGameRules().get(BrewGameRules.AGE_UNLOADED) ? (currentTime - cauldron.lastTicked) : 1;

                world1.sendParticles(ParticleTypes.BUBBLE_POP,
                        0.4 * world.random.nextFloat(),
                        (6.0D + (double)state.getValue(LayeredCauldronBlock.LEVEL) * 3.0D) / 16.0D,
                        0.4 * world.random.nextFloat(),
                0, 0, 0, 0, 0);

                if (cauldron.timeCooking % 20 == 0) {
                    var seconds = (int) (cauldron.timeCooking / 20) % 60;
                    var minutes = (int) (cauldron.timeCooking / (20 * 60));
                    var text = Component.literal(minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
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
    public void setRemoved() {
        super.setRemoved();
        if (this.elementHolder != null) {
            this.elementHolder.destroy();
        }
        this.elementHolder = null;
    }

    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putLong("LastTicked", this.lastTicked);
        var list = view.list("Ingredients", ItemStack.OPTIONAL_CODEC);
        for (var stack : this.inventory) {
            list.add(stack);
        }

        view.putDouble("CookingTime", this.timeCooking);
    }

    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.lastTicked = view.getLongOr("LastTicked", 0);
        this.inventory.clear();
        for (var item : view.listOrEmpty("Ingredients", ItemStack.OPTIONAL_CODEC)) {
            this.inventory.add(item);
        }
        this.timeCooking = view.getDoubleOr("CookingTime", 0);
    }

    public void addIngredients(List<ItemEntity> entities) {
        for (var entity : entities) {
            if (!entity.getItem().isEmpty()) {
                var stack = entity.getItem().copy();
                for (var existing : this.inventory) {
                    if (ItemStack.isSameItemSameComponents(stack, existing)) {
                        var count = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        existing.grow(count);
                        stack.shrink(count);
                    }
                    if (stack.isEmpty()) {
                        break;
                    }
                }
                if (!stack.isEmpty()) {
                    this.inventory.add(stack);
                }
            }

            entity.discard();
        }
    }

    public boolean onUse(Player player) {
        var stack = player.getMainHandItem();

        if (!stack.isEmpty() && BreweryInit.containerIngredient.test(stack)) {
            var container = stack.copyWithCount(1);
            stack.shrink(1);
            var agingMultiplier = ((ServerLevel) this.level).getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER);

            var ingredients = new ArrayList<ItemStack>();

            for (var nbt : this.inventory) {
                ingredients.add(nbt.copy());
            }
            var heatSource = this.getLevel().getBlockState(this.worldPosition.below()).getBlock();
            var types = DrinkUtils.findTypes(ingredients, null, heatSource, container);

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

                var stacks = new ArrayList<ItemStack>();
                for (var st : this.inventory) {
                    stacks.add(st.copy());
                }

                var cookingData = new CookingData(age, stacks, heatSource, container);
                if (match == null || quality < 0) {
                    var out = new ItemStack(BrewItems.INGREDIENT_MIXTURE);
                    out.set(BrewComponents.COOKING_DATA, cookingData);
                    player.addItem(out);
                } else {
                    player.addItem(DrinkUtils.createDrink(BreweryInit.DRINK_TYPE_ID.get(match), 0, quality * 10, 0, cookingData));
                }
            }

            var level = this.getBlockState().getValue(LayeredCauldronBlock.LEVEL) - 1;

            if (level == 0) {
                this.level.setBlockAndUpdate(this.worldPosition, Blocks.CAULDRON.defaultBlockState());

            } else {
                this.level.setBlockAndUpdate(this.worldPosition,this.getBlockState().setValue(LayeredCauldronBlock.LEVEL, level));
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
