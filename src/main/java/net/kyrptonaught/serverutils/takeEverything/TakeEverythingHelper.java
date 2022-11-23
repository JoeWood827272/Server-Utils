package net.kyrptonaught.serverutils.takeEverything;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Wearable;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;

public class TakeEverythingHelper {
    public static boolean takeEverything(ServerPlayerEntity player) {
        if (!TakeEverythingMod.getConfigStatic().Enabled || player.currentScreenHandler instanceof PlayerScreenHandler) {
            return false;
        }

        if (player.isSpectator() && !TakeEverythingMod.getConfigStatic().worksInSpectator) {
            return false;
        }

        for (int i = 0; i < player.currentScreenHandler.slots.size(); i++) {
            Slot slot = player.currentScreenHandler.slots.get(i);
            if (!(slot.inventory instanceof PlayerInventory) && slot.canTakeItems(player) && !slot.getStack().isEmpty()) {
                player.currentScreenHandler.onSlotClick(i, 0, SlotActionType.QUICK_MOVE, player);
            }
        }
        player.playerScreenHandler.updateToClient();
        return true;
    }

    public static boolean canEquip(PlayerEntity player, ItemStack armor) {
        EquipmentSlot equipmentSlot = getEquipSlot(armor);
        if(equipmentSlot == null)
            return false;
        ItemStack equipped = player.getEquippedStack(equipmentSlot);
        return equipped.isEmpty();
    }

    public static boolean canSwap(PlayerEntity player, ItemStack armor, boolean alwaysSwap) {
        EquipmentSlot equipmentSlot = getEquipSlot(armor);
        if(equipmentSlot == null)
            return false;
        ItemStack equipped = player.getEquippedStack(equipmentSlot);
        return !hasBinding(equipped) && (alwaysSwap || (!hasEnchants(player, equipped) && isBetter(equipped, armor)));
    }


    public static boolean isBetter(ItemStack og, ItemStack newStack) {
        if (newStack.isOf(Items.ELYTRA))
            return true;

        if (og.isOf(Items.ELYTRA))
            return false;

        if (og.getItem() instanceof ArmorItem && newStack.getItem() instanceof ArmorItem)
            return ((ArmorItem) newStack.getItem()).getProtection() >= ((ArmorItem) newStack.getItem()).getProtection();

        if (og.getItem() instanceof ArmorItem)
            return false;

        if (newStack.getItem() instanceof ArmorItem)
            return true;

        return true;
    }

    public static ItemStack equipOrSwapArmor(PlayerEntity player, ItemStack armor, boolean alwaysSwap) {
        EquipmentSlot equipmentSlot = getEquipSlot(armor);
        if(equipmentSlot == null)
            return ItemStack.EMPTY;

        ItemStack equipped = player.getEquippedStack(equipmentSlot);
        if (canEquip(player, armor)) {
            player.equipStack(equipmentSlot, armor.copy());
            playSound(armor, (ServerPlayerEntity) player);
            armor.setCount(0);
        } else if (canSwap(player, armor, alwaysSwap)) {
            ItemStack copy = equipped.copy();
            player.equipStack(equipmentSlot, armor.copy());
            playSound(armor, (ServerPlayerEntity) player);
            armor.setCount(0);
            return copy;
        }
        return ItemStack.EMPTY;
    }

    public static EquipmentSlot getEquipSlot(ItemStack stack) {
        EquipmentSlot slot = PlayerEntity.getPreferredEquipmentSlot(stack);

        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
            return null;
        return slot;
    }

    public static boolean isSwappableItem(ItemStack stack) {
        return getEquipSlot(stack) != null;
    }

    public static Boolean hasBinding(ItemStack stack) {
        return EnchantmentHelper.hasBindingCurse(stack);
    }

    public static Boolean hasEnchants(PlayerEntity playerEntity, ItemStack stack) {
        if (TakeEverythingConfig.SWAP_IGNORE_ENCHANTS.contains(playerEntity.getUuidAsString()))
            return false;
        return stack.hasEnchantments();
    }

    public static void playSound(ItemStack stack, ServerPlayerEntity player) {
        SoundEvent soundEvent = stack.getEquipSound();
        if (stack.isEmpty() || soundEvent == null) {
            return;
        }
        player.playSound(soundEvent, player.getSoundCategory(), 1, 1);
    }
}
