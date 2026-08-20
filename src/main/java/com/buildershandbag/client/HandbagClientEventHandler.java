package com.buildershandbag.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;
import com.buildershandbag.client.overlay.OverlayMessageRenderer;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketCycleHandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Client-only held-item scrolling and non-GUI overlay rendering.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class HandbagClientEventHandler {

    private HandbagClientEventHandler() {
    }

    @SubscribeEvent
    public static void onMouseInput(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (event.getDwheel() == 0 || player == null || !player.isSneaking() || minecraft.currentScreen != null) return;

        EnumHand hand = getHeldHandbagHand(player);
        if (hand == null || HandbagStorage.getConfigurations(player.getHeldItem(hand)).isEmpty()) return;

        HandbagNetwork.INSTANCE.sendToServer(new PacketCycleHandbagConfiguration(hand, event.getDwheel() < 0));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onOverlayRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;

        OverlayMessageRenderer.render(
            event.getResolution().getScaledWidth(),
            event.getResolution().getScaledHeight());

        // TODO: Ideally want to show something above the hotbar,
        //       with the current selection and the surrounding configurations,
        //       but only when holding the item. Would be gated by client config.
        //       A carousel like "X X X X X [X] X X X X X" the size of the hotbar
        //       Getting the visuals right will be a nightmare...
    }

    private static EnumHand getHeldHandbagHand(EntityPlayer player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() == ItemRegistry.HANDBAG) return EnumHand.MAIN_HAND;

        ItemStack offHand = player.getHeldItemOffhand();
        return offHand.getItem() == ItemRegistry.HANDBAG ? EnumHand.OFF_HAND : null;
    }
}
