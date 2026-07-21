package dev.thirdpersonmod.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.ShoulderCameraController;
import dev.thirdpersonmod.config.ConfigManager;
import dev.thirdpersonmod.screen.CameraConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CameraKeyBindings {
    private CameraKeyBindings() {
    }

    public static void register(ShoulderCameraController controller, ConfigManager configManager) {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ShoulderCameraClient.MOD_ID, "camera")
        );
        KeyMapping toggleCamera = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.thirdpersonmod.toggle_camera", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category)
        );
        KeyMapping toggleShoulder = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.thirdpersonmod.toggle_shoulder", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, category)
        );
        KeyMapping openConfig = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.thirdpersonmod.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, category)
        );

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            boolean cameraPressed = consumeAll(toggleCamera);
            boolean shoulderPressed = consumeAll(toggleShoulder);
            boolean configPressed = consumeAll(openConfig);
            if (minecraft.gui.screen() != null || minecraft.gui.overlay() != null) {
                return;
            }
            if (cameraPressed) {
                controller.toggleEnabled();
            }
            if (shoulderPressed) {
                controller.toggleShoulder();
            }
            if (configPressed) {
                minecraft.setScreenAndShow(new CameraConfigScreen(null, configManager, controller));
            }
        });
    }

    private static boolean consumeAll(KeyMapping keyMapping) {
        boolean pressed = false;
        while (keyMapping.consumeClick()) {
            pressed = true;
        }
        return pressed;
    }
}
