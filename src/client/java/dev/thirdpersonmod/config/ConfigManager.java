package dev.thirdpersonmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.ShoulderSide;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("thirdpersonmod.json");
    private CameraConfig config = new CameraConfig();

    public void load() {
        if (!Files.exists(this.configPath)) {
            this.config.validate();
            save();
            return;
        }

        try {
            CameraConfig loaded = GSON.fromJson(Files.readString(this.configPath, StandardCharsets.UTF_8), CameraConfig.class);
            if (loaded == null) {
                throw new JsonParseException("configuration root is null");
            }
            loaded.validate();
            this.config = loaded;
        } catch (IOException | RuntimeException exception) {
            ShoulderCameraClient.LOGGER.error(
                "Failed to load {}; using safe cinematic defaults. The game will continue.",
                this.configPath,
                exception
            );
            this.config = new CameraConfig();
            this.config.validate();
        }
    }

    public CameraConfig get() {
        return this.config;
    }

    public void apply(CameraConfig updated) {
        this.config.copyFrom(updated);
        this.config.validate();
        save();
    }

    public void setEnabled(boolean enabled) {
        this.config.enabled = enabled;
        save();
    }

    public void setDefaultShoulder(ShoulderSide shoulder) {
        this.config.defaultShoulder = shoulder;
        save();
    }

    public void save() {
        Path temporaryPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.configPath.getParent());
            Files.writeString(temporaryPath, GSON.toJson(this.config), StandardCharsets.UTF_8);
            try {
                Files.move(
                    temporaryPath,
                    this.configPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryPath, this.configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            ShoulderCameraClient.LOGGER.error("Failed to save camera configuration to {}", this.configPath, exception);
        }
    }
}
