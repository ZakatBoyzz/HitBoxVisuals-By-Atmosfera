package com.wwoqkx.hitboxvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HitboxVisualsClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        HitboxConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitboxvisuals.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.hitboxvisuals"
        ));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitboxvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.hitboxvisuals"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                HitboxConfig.SETTINGS.enabled =
                        !HitboxConfig.SETTINGS.enabled;

                HitboxConfig.save();
            }

            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HitboxSettingsScreen());
                }
            }
        });

        // Fill first: player models render afterward.
        WorldRenderEvents.BEFORE_ENTITIES.register(
                HitboxRenderer::renderFill
        );

        // Outlines remain on top of the player models.
        WorldRenderEvents.AFTER_ENTITIES.register(
                HitboxRenderer::renderOutline
        );
    }

    static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    public static String toggleKeyLabel() {
        return toggleKey == null
                ? "H"
                : toggleKey.getBoundKeyLocalizedText().getString();
    }

    public static String menuKeyLabel() {
        return menuKey == null
                ? "K"
                : menuKey.getBoundKeyLocalizedText().getString();
    }
}
