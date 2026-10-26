package com.buildershandbag.client;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketPickHandbagConfiguration;


/**
 * Client controls for picking configurations while holding the handbag.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class HandbagPicker {

    // Minecraft encodes mouse buttons as their index minus this offset
    private static final int MOUSE_KEY_OFFSET = 100;
    private static final int MIDDLE_MOUSE_BUTTON = 2;

    private static final KeyBinding PICK_BLOCK = new KeyBinding(
        "key.buildershandbag.pick_block", new PickerContext(),
        MIDDLE_MOUSE_BUTTON - MOUSE_KEY_OFFSET, "key.categories.buildershandbag");

    private HandbagPicker() {
    }

    public static void registerKeyBinding() {
        ClientRegistry.registerKeyBinding(PICK_BLOCK);
    }

    @SubscribeEvent
    public static void onMouseInput(MouseEvent event) {
        if (event.getButton() < 0
                || !PICK_BLOCK.isActiveAndMatches(event.getButton() - MOUSE_KEY_OFFSET)) return;

        if (event.isButtonstate()) pickBlock(Minecraft.getMinecraft());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // Minecraft places character-based key codes after the physical keyboard range
        int keyCode = Keyboard.getEventKey() == Keyboard.KEY_NONE
            ? Keyboard.getEventCharacter() + Keyboard.KEYBOARD_SIZE : Keyboard.getEventKey();
        if (!PICK_BLOCK.isActiveAndMatches(keyCode)) return;

        // Consume queued presses before Minecraft handles its pick-block binding
        while (PICK_BLOCK.isPressed()) {}

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.keyBindPickBlock.getKeyCode() == keyCode) {
            while (minecraft.gameSettings.keyBindPickBlock.isPressed()) {}
        }

        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) pickBlock(minecraft);
    }

    private static void pickBlock(Minecraft minecraft) {
        RayTraceResult target = minecraft.objectMouseOver;
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;

        HandbagNetwork.INSTANCE.sendToServer(new PacketPickHandbagConfiguration(target));
    }

    private static final class PickerContext implements IKeyConflictContext {

        private PickerContext() {
        }

        @Override
        public boolean isActive() {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft.currentScreen == null && minecraft.inGameHasFocus
                && minecraft.player != null && !minecraft.player.isSpectator()
                && minecraft.player.getHeldItemMainhand().getItem() == ItemRegistry.HANDBAG;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this || other == KeyConflictContext.IN_GAME;
        }
    }
}
