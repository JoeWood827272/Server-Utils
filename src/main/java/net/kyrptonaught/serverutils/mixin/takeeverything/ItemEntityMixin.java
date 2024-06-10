package net.kyrptonaught.serverutils.mixin.takeeverything;

import net.kyrptonaught.serverutils.takeEverything.TakeEverythingHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.kyrptonaught.serverutils.ServerUtilsMod.TakeEverythingModule;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z"))
    public boolean autoEquip(PlayerInventory instance, ItemStack stack) {
        if (TakeEverythingHelper.isSwappableItem(instance.player, stack)) {
            if (TakeEverythingHelper.canEquip(instance.player, stack)) {
                TakeEverythingHelper.equipOrSwapArmor(instance.player, stack, false);
                return true;
            }
            if (TakeEverythingHelper.canSwap(instance.player, stack, false)) {
                stack = TakeEverythingHelper.equipOrSwapArmor(instance.player, stack, false);
                instance.insertStack(stack);
                if (!stack.isEmpty() && !TakeEverythingModule.getConfig().deleteItemNotDrop) {
                    World world = instance.player.getWorld();
                    ItemEntity itemEntity = new ItemEntity(world, this.getX(), this.getY(), this.getZ(), stack);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
                return true;
            }
        }
        return instance.insertStack(stack);
    }
}
