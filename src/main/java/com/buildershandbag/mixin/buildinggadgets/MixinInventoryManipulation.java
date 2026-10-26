package com.buildershandbag.mixin.buildinggadgets;

import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.direwolf20.buildinggadgets.common.tools.InventoryManipulation;
import com.direwolf20.buildinggadgets.common.tools.NetworkIO;
import com.direwolf20.buildinggadgets.common.tools.GadgetUtils;
import com.direwolf20.buildinggadgets.common.integration.IItemAccess;

import com.buildershandbag.integration.BuildingGadgetsInventory;
import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.item.HandbagItemHandler;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = InventoryManipulation.class, remap = false)
public abstract class MixinInventoryManipulation {

    @Inject(method = "countItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
        + "Lnet/minecraft/world/World;)I", at = @At("HEAD"), cancellable = true)
    private static void buildershandbag$countExactRemote(ItemStack requested, EntityPlayer player, World world,
            CallbackInfoReturnable<Integer> callback) {
        ItemStack target = BuildingGadgetsTarget.resolve(requested, player);
        if (!HandbagOutputs.requiresExactMatch(target)) return;

        callback.setReturnValue(InventoryManipulation.countItem(target, player, (gadget, stack) ->
            BuildingGadgetsInventory.countItems(GadgetUtils.getRemoteInventory(gadget, world, player), stack, player)));
    }

    @Redirect(
        method = "countItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation$IRemoteInventoryProvider;)I",
        at = @At(value = "INVOKE", target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "countInContainer(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/item/Item;I)I")
    )
    private static int buildershandbag$countExactContainer(IItemHandler inventory, Item item, int meta,
            ItemStack requested, EntityPlayer player, InventoryManipulation.IRemoteInventoryProvider remote) {
        return BuildingGadgetsInventory.countItems(inventory, requested, player);
    }

    @Redirect(
        method = "countItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation$IRemoteInventoryProvider;)I",
        at = @At(value = "INVOKE", target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "findItem(Lnet/minecraft/item/Item;ILnet/minecraftforge/items/IItemHandler;)Ljava/util/List;")
    )
    private static List<Integer> buildershandbag$findExactItems(Item item, int meta, IItemHandler inventory,
            ItemStack requested, EntityPlayer player, InventoryManipulation.IRemoteInventoryProvider remote) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack available = inventory.getStackInSlot(slot);
            if (available.isEmpty()) continue;

            boolean matches = HandbagOutputs.requiresExactMatch(requested)
                ? HandbagOutputs.sameOutput(available, requested)
                : available.getItem() == item && available.getMetadata() == meta;
            if (matches) slots.add(slot);
        }

        return slots;
    }

    @Inject(method = "extractFromInventory", at = @At("HEAD"), cancellable = true)
    private static void buildershandbag$extractExactItems(IItemHandler inventory, ItemStack requested, int amount,
            EntityPlayer player, CallbackInfoReturnable<Integer> callback) {
        if (inventory instanceof IItemAccess || !HandbagOutputs.requiresExactMatch(requested)) return;

        callback.setReturnValue(BuildingGadgetsInventory.extractExact(inventory, requested, amount));
    }

    @Inject(method = "findInvContainers", at = @At("RETURN"))
    private static void buildershandbag$countHandbags(EntityPlayer player,
            CallbackInfoReturnable<List<IItemHandler>> callback) {
        List<IItemHandler> inventories = callback.getReturnValue();
        if (inventories.removeIf(inventory -> inventory instanceof HandbagItemHandler)) {
            inventories.add(new BuildingGadgetsInventory());
        }
    }

    @Inject(method = "collectInventories", at = @At("RETURN"))
    private static void buildershandbag$collectHandbags(ItemStack gadget, EntityPlayer player, World world,
            NetworkIO.Operation operation, CallbackInfoReturnable<List<Pair<Object, IItemHandler>>> callback) {
        boolean supplied = false;
        ListIterator<Pair<Object, IItemHandler>> iterator = callback.getReturnValue().listIterator();
        while (iterator.hasNext()) {
            Pair<Object, IItemHandler> entry = iterator.next();
            if (!(entry.getRight() instanceof HandbagItemHandler)) continue;

            HandbagItemHandler handler = (HandbagItemHandler) entry.getRight();
            iterator.remove();
            if (operation == NetworkIO.Operation.INSERT) {
                iterator.add(Pair.of(entry.getLeft(), handler.getInput()));
            } else if (!supplied) {
                iterator.add(Pair.of(entry.getLeft(), new BuildingGadgetsInventory()));
                supplied = true;
            }
        }
    }

    @ModifyVariable(
        method = "countItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation$IRemoteInventoryProvider;)I",
        at = @At("HEAD"), argsOnly = true, ordinal = 0
    )
    private static ItemStack buildershandbag$countSelected(ItemStack requested, ItemStack original,
            EntityPlayer player, InventoryManipulation.IRemoteInventoryProvider remote) {
        return BuildingGadgetsTarget.resolve(requested, player);
    }

    @ModifyVariable(method = "useItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static ItemStack buildershandbag$extractSelected(ItemStack requested, ItemStack original,
            EntityPlayer player, int amount, World world) {
        return BuildingGadgetsTarget.resolve(requested, player);
    }
}
