package com.buildershandbag.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import com.buildershandbag.integration.HandbagConfigurationOption;
import com.buildershandbag.integration.HandbagConfigurationProvider;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketHandbagOptionsSync;


/**
 * Server-authoritative handbag container. Only the configuration material is a
 * real inventory slot, the 4x9 configured outputs are client-rendered.
 */
public class ContainerHandbag extends Container {

    private static final int MATERIAL_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final EntityPlayer player;
    private final EnumHand hand;
    private final InventoryBasic configurationMaterial = new InventoryBasic("Handbag", false, 1);

    private ItemStack lastOptionMaterial = ItemStack.EMPTY;
    private List<HandbagConfigurationOption> clientOptions = Collections.emptyList();

    public ContainerHandbag(InventoryPlayer playerInventory, EnumHand hand) {
        this.player = playerInventory.player;
        this.hand = hand;

        addSlotToContainer(new SlotConfigurationMaterial(configurationMaterial, 0, 8, 112));
        bindPlayerInventory(playerInventory, 8, 167);
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
        return playerIn == player && playerIn.getHeldItem(hand).getItem() == ItemRegistry.HANDBAG;
    }

    @Override
    public void addListener(@Nonnull IContainerListener listener) {
        super.addListener(listener);
        if (player.world.isRemote) return;

        lastOptionMaterial = getConfigurationMaterial().copy();
        syncOptions();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (player.world.isRemote) return;

        ItemStack currentMaterial = getConfigurationMaterial();
        if (sameStack(lastOptionMaterial, currentMaterial)) return;

        lastOptionMaterial = currentMaterial.copy();
        syncOptions();
    }

    /**
     * Transfers a stack from the material slot to the player inventory, or from
     * the player inventory to the material slot if it is a valid configuration
     * material.
     */
    @Override
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        if (index < 0 || index >= inventorySlots.size() || isHeldHandSlot(index)) return ItemStack.EMPTY;

        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index == MATERIAL_SLOT) {
            if (!mergeItemStack(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) return ItemStack.EMPTY;
        } else if (HandbagConfigurationProvider.isConfigurationMaterial(stack)) {
            if (!mergeItemStack(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(playerIn, stack);

        return original;
    }

    @Override
    @Nonnull
    public ItemStack slotClick(int slotId, int dragType, @Nonnull ClickType clickType, @Nonnull EntityPlayer playerIn) {
        // We lock the hand slot to prevent the player from moving the handbag while the GUI is open
        if (hand == EnumHand.MAIN_HAND && (isHeldHandSlot(slotId)
                || clickType == ClickType.SWAP && dragType == player.inventory.currentItem)) {
            return ItemStack.EMPTY;
        }

        return super.slotClick(slotId, dragType, clickType, playerIn);
    }

    @Override
    public void onContainerClosed(@Nonnull EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        if (playerIn.world.isRemote) return;

        ItemStack material = configurationMaterial.removeStackFromSlot(0);
        if (!material.isEmpty()) playerIn.inventory.placeItemBackInInventory(playerIn.world, material);
    }

    public EnumHand getHand() {
        return hand;
    }

    public ItemStack getConfigurationMaterial() {
        return configurationMaterial.getStackInSlot(0);
    }

    /**
     * Moves configuration material into a handbag entry after the server has
     * validated the requested output option.
     */
    public void consumeConfigurationMaterial(int amount) {
        ItemStack material = getConfigurationMaterial();
        if (amount <= 0 || material.isEmpty()) return;

        material.shrink(amount);
        if (material.isEmpty()) configurationMaterial.setInventorySlotContents(0, ItemStack.EMPTY);
    }

    public List<HandbagConfigurationOption> getClientOptions() {
        return clientOptions;
    }

    public void setClientOptions(List<HandbagConfigurationOption> options) {
        if (options == null || options.isEmpty()) {
            clientOptions = Collections.emptyList();
            return;
        }

        clientOptions = Collections.unmodifiableList(new ArrayList<>(options));
    }

    private void bindPlayerInventory(InventoryPlayer playerInventory, int x, int y) {
        // Internal inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }

        // Hotbar
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, x + column * 18, y + 58));
        }
    }

    private boolean isHeldHandSlot(int containerSlot) {
        return hand == EnumHand.MAIN_HAND
            && containerSlot == PLAYER_INVENTORY_START + 27 + player.inventory.currentItem;
    }

    private void syncOptions() {
        List<HandbagConfigurationOption> options = HandbagConfigurationProvider.getOptions(getConfigurationMaterial());

        PacketHandbagOptionsSync packet = new PacketHandbagOptionsSync(hand, options);
        for (Object listener : listeners) {
            if (!(listener instanceof EntityPlayerMP)) continue;

            HandbagNetwork.INSTANCE.sendTo(packet, (EntityPlayerMP) listener);
        }
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        return first.isEmpty() && second.isEmpty()
            || !first.isEmpty()
            && !second.isEmpty()
            && ItemStack.areItemsEqual(first, second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }
}
