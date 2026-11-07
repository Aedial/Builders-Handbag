package com.buildershandbag.storage;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.buildershandbag.Tags;
import com.buildershandbag.config.HandbagServerConfig;


/**
 * Pickup absorption and periodic cleanup of configured outputs in player inventories.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class HandbagAbsorptionEvents {

    private static final int SCAN_INTERVAL = 600;

    private HandbagAbsorptionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || player.isSpectator() || !HandbagServerConfig.isPickupAbsorptionEnabled()) return;

        EntityItem entity = event.getItem();
        if (entity.isDead || entity.cannotPickup()) return;

        // Let vanilla handle drops reserved for another player
        if (entity.getOwner() != null && !entity.getOwner().equals(player.getName())) return;

        ItemStack stack = entity.getItem();
        int accepted = HandbagAbsorption.absorb(CarriedHandbags.get(player), stack, true);
        if (accepted <= 0) return;

        stack.shrink(accepted);
        entity.setItem(stack);
        if (stack.isEmpty()) event.setResult(Event.Result.ALLOW);

        HandbagAbsorption.sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.world.isRemote || player.isSpectator()
                || !HandbagServerConfig.isInventoryCleanupEnabled()
                || Math.floorMod(player.ticksExisted + player.getEntityId(), SCAN_INTERVAL) != 0) return;

        List<ItemStack> handbags = CarriedHandbags.get(player);
        if (handbags.isEmpty()) return;

        boolean changed = false;
        int armorStart = player.inventory.mainInventory.size();
        int armorEnd = armorStart + player.inventory.armorInventory.size();
        // Reuse configured outputs already carried by the player
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            if (slot >= armorStart && slot < armorEnd) continue;

            ItemStack stack = player.inventory.getStackInSlot(slot);
            int accepted = HandbagAbsorption.absorb(handbags, stack, false);
            if (accepted <= 0) continue;

            stack.shrink(accepted);
            if (stack.isEmpty()) player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);

            changed = true;
        }
        if (changed) HandbagAbsorption.sync(player);
    }
}
