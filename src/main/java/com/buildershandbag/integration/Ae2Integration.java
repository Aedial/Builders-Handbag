package com.buildershandbag.integration;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.core.localization.GuiText;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.helpers.BaseActionSource;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.item.ItemHandbag;


/**
 * Wireless handler for the Handbag's AE2 Security Station link.
 */
public final class Ae2Integration implements IWirelessTermHandler {

    private static final Ae2Integration WIRELESS_HANDLER = new Ae2Integration();

    private Ae2Integration() {
    }

    /**
     * Registers the handbag's AE2 wireless handler.
     */
    public static void registerWirelessHandler() {
        AEApi.instance().registries().wireless().registerWirelessHandler(WIRELESS_HANDLER);
    }

    /**
     * Pulls the requested material from the linked network.
     */
    public static int refill(EntityPlayer player, ItemStack handbag, ItemStack material, int requestedAmount) {
        if (!HandbagServerConfig.integrations.enableAe2Refill || requestedAmount <= 0
                || handbag.isEmpty() || material.isEmpty() || !isLinked(handbag)) {
            return 0;
        }

        WirelessTerminalGuiObject wireless = new WirelessTerminalGuiObject(
            WIRELESS_HANDLER, handbag, player, player.world, -1, 0, 0);
        if (!wireless.rangeCheck()) return 0;

        IMEMonitor<IAEItemStack> monitor = wireless.getInventory(
            AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
        if (monitor == null) return 0;

        IAEItemStack request = AEItemStack.fromItemStack(material.copy());
        if (request == null) return 0;
        request.setStackSize(requestedAmount);

        IEnergyGrid energy = wireless.getActionableNode().getGrid().getCache(IEnergyGrid.class);
        IAEItemStack extracted = Platform.poweredExtraction(
            energy, monitor, request, new BaseActionSource(), Actionable.MODULATE);

        return extracted == null ? 0 : (int) Math.min(requestedAmount, extracted.getStackSize());
    }

    public static boolean isLinked(ItemStack handbag) {
        return !handbag.isEmpty() && !WIRELESS_HANDLER.getEncryptionKey(handbag).isEmpty();
    }

    @SideOnly(Side.CLIENT)
    public static void addTooltip(ItemStack handbag, List<String> tooltip) {
        if (!HandbagServerConfig.integrations.enableAe2Refill) {
            tooltip.add(I18n.format("tooltip.buildershandbag.ae2_disabled"));
            return;
        }

        tooltip.add(isLinked(handbag)
            ? TextFormatting.GREEN + GuiText.Linked.getLocal()
            : TextFormatting.RED + GuiText.Unlinked.getLocal());
    }

    @Override
    public boolean canHandle(ItemStack stack) {
        return HandbagServerConfig.integrations.enableAe2Refill && (stack.getItem() instanceof ItemHandbag);
    }

    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack stack) {
        return true;
    }

    @Override
    public IConfigManager getConfigManager(ItemStack target) {
        ConfigManager manager = new ConfigManager((config, settingName, newValue) -> config.writeToNBT(Platform.openNbtData(target)));

        manager.readFromNBT(Platform.openNbtData(target).copy());
        return manager;
    }

    @Override
    public IGuiHandler getGuiHandler(ItemStack stack) {
        return null;
    }

    @Override
    public String getEncryptionKey(ItemStack stack) {
        return Platform.openNbtData(stack).getString("encryptionKey");
    }

    @Override
    public void setEncryptionKey(ItemStack stack, String encryptionKey, String name) {
        NBTTagCompound tag = Platform.openNbtData(stack);
        tag.setString("encryptionKey", encryptionKey);
        tag.setString("name", name);
    }
}
