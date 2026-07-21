package dev.thirdpersonmod;

import dev.thirdpersonmod.camera.ShoulderCameraController;
import dev.thirdpersonmod.config.ConfigManager;
import dev.thirdpersonmod.input.CameraKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShoulderCameraClient implements ClientModInitializer {
    public static final String MOD_ID = "thirdpersonmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ShoulderCameraController controller;

    @Override
    public void onInitializeClient() {
        ConfigManager configManager = new ConfigManager();
        configManager.load();
        controller = new ShoulderCameraController(configManager);
        CameraKeyBindings.register(controller, configManager);
        LOGGER.info("Cinematic Shoulder Camera initialized");
    }

    public static ShoulderCameraController controller() {
        return controller;
    }
}
