package com.example;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private boolean enabled = false;
    private int step = 0;
    private int delayTicks = 0;

    @Override
    public void onInitializeClient() {
        // 1. Hotkey: "+" Taste (Numpad)
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.autosell.toggle", 
            InputUtil.Type.KEYSYM, 
            GLFW.GLFW_KEY_KP_ADD, 
            "category.autosell"
        ));

        // 2. Chat-Befehl /autosell
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autosell")
                .executes(context -> {
                    toggleAutoSell(MinecraftClient.getInstance());
                    return 1;
                })
            );
        });

        // 3. Tick Loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Hotkey prüfen
            while (toggleKey.wasPressed()) {
                toggleAutoSell(client);
            }

            if (!enabled) {
                step = 0;
                return;
            }

            // In Version 26.2 wird die generische HandledScreen-Klasse als Basis genutzt
            if (client.currentScreen instanceof HandledScreen<?> screen) {
                int syncId = screen.getScreenHandler().syncId;

                if (delayTicks > 0) {
                    delayTicks--;
                    return;
                }

                int playerFirstSlot = 36; // Erster Inventar-Slot
                int greenButtonSlot = 35; // Grüner Bestätigungsknopf

                switch (step) {
                    case 0:
                        var slotStack = screen.getScreenHandler().getSlot(playerFirstSlot).getStack();
                        
                        if (!slotStack.isEmpty()) {
                            // Shift-Klick ins Menü
                            client.interactionManager.clickSlot(
                                syncId, 
                                playerFirstSlot, 
                                0, 
                                SlotActionType.QUICK_MOVE, 
                                client.player
                            );
                            
                            delayTicks = 2; // Pause (~100ms)
                            step = 1;
                        }
                        break;

                    case 1:
                        // Klick auf den grünen Button zum Bestätigen
                        client.interactionManager.clickSlot(
                            syncId, 
                            greenButtonSlot, 
                            0, 
                            SlotActionType.PICKUP, 
                            client.player
                        );

                        delayTicks = 3; // Pause (~150ms)
                        step = 0;
                        break;
                }
            } else {
                step = 0;
            }
        });
    }

    private void toggleAutoSell(MinecraftClient client) {
        if (client.player == null) return;
        enabled = !enabled;
        String status = enabled ? "§aAKTIVIERT" : "§cDEAKTIVIERT";
        client.player.sendMessage(Text.literal("§7[AutoSell] Status: " + status), false);
    }
}
