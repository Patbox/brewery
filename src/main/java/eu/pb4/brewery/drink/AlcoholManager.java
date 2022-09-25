package eu.pb4.brewery.drink;

import eu.pb4.brewery.duck.LivingEntityExt;
import eu.pb4.brewery.other.BrewGameRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.event.KeyListener;

public class AlcoholManager {
    private final LivingEntity entity;
    public double alcoholLevel;
    public double quality;
    
    public AlcoholManager(LivingEntity entity) {
        this.entity = entity;
    }

    public void drink(DrinkType type, double quality, double alcoholicValue) {
        var multiplier = this.entity.world.getGameRules().get(BrewGameRules.ALCOHOL_MULTIPLIER).get();

        this.alcoholLevel = Math.max(this.alcoholLevel + alcoholicValue * multiplier, alcoholicValue * multiplier);
        this.quality = (this.quality + quality) / 2;
    }

    public void eat(ItemStack stack) {
        if (this.alcoholLevel > 0) {
            if (stack.isOf(Items.BREAD)) {
                this.alcoholLevel -= 5;
            } else if (stack.isOf(Items.MILK_BUCKET)) {
                this.alcoholLevel -= 30;
            }
        }
    }

    public static AlcoholManager of(LivingEntity entity) {
        return ((LivingEntityExt) entity).brewery$getAlcoholManager();
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putDouble("brewery:alcohol_level", this.alcoholLevel);
        nbt.putDouble("brewery:quality", this.quality);
    }

    public void readNbt(NbtCompound nbt) {
        this.alcoholLevel = nbt.getDouble("brewery:alcohol_level");
        this.quality = nbt.getDouble("brewery:quality");
    }
    
    public void tick() {
        if (this.alcoholLevel > 0) {
            this.alcoholLevel -= 0.01;
            var appliedLevel = this.alcoholLevel - (this.quality - 8) * 5;
            if (appliedLevel > 50 && this.entity.age % 80 == 0) {
                this.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, (int) Math.max(appliedLevel / 10 - 60, 0), true, false, false));

                if (appliedLevel > 60) {
                    this.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, (int) Math.max(appliedLevel / 10 - 70, 0), true, false, false));
                }

                if (appliedLevel > 70) {
                    this.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, true, false, false));
                    var velocity = new Vec3d(Math.random() - 0.5, 0, Math.random() - 0.5).normalize().multiply(0.4);
                    this.entity.addVelocity(velocity.x, velocity.y, velocity.z);
                    if (this.entity instanceof ServerPlayerEntity player) {
                        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                    }
                }

                if (appliedLevel > 90) {
                    this.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, true, false, false));
                    this.entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0, true, false, false));
                }

                if (appliedLevel > 110) {
                    this.entity.damage(new DamageSource("brewery.alcohol_poisoning") {}.setUsesMagic(), (float) ((appliedLevel - 110) / 10 + 1));
                }
            }
        }
    }
}
