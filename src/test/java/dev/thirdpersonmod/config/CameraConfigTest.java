package dev.thirdpersonmod.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import dev.thirdpersonmod.camera.CompositionPreset;
import dev.thirdpersonmod.camera.ShoulderSide;
import org.junit.jupiter.api.Test;

class CameraConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void validationClampsInvalidValuesAndRestoresEnums() {
        CameraConfig config = new CameraConfig();
        config.distance = Double.NaN;
        config.shoulderOffset = Double.POSITIVE_INFINITY;
        config.verticalOffset = -100.0;
        config.minimumDistance = 20.0;
        config.collisionRadius = -2.0;
        config.collisionSafetyMargin = Double.NaN;
        config.verticalSmoothingSpeed = -1.0;
        config.collisionInSpeed = Double.POSITIVE_INFINITY;
        config.collisionOutSpeed = 100.0;
        config.shoulderTransitionSpeed = -100.0;
        config.compositionPreset = null;
        config.defaultShoulder = null;

        config.validate();

        assertEquals(3.6, config.distance);
        assertEquals(0.75, config.shoulderOffset);
        assertEquals(-1.0, config.verticalOffset);
        assertEquals(2.0, config.minimumDistance);
        assertEquals(0.01, config.collisionRadius);
        assertEquals(0.1, config.collisionSafetyMargin);
        assertEquals(0.0, config.verticalSmoothingSpeed);
        assertEquals(32.0, config.collisionInSpeed);
        assertEquals(60.0, config.collisionOutSpeed);
        assertEquals(0.0, config.shoulderTransitionSpeed);
        assertEquals(ShoulderSide.RIGHT, config.defaultShoulder);
        assertEquals(CompositionPreset.CUSTOM, config.compositionPreset);
    }

    @Test
    void legacySmoothingNameLoadsAndOnlyNewNameIsWritten() {
        CameraConfig config = GSON.fromJson("{\"positionSmoothingSpeed\":7.5}", CameraConfig.class);
        config.validate();

        String serialized = GSON.toJson(config);
        assertEquals(7.5, config.verticalSmoothingSpeed);
        assertTrue(serialized.contains("\"verticalSmoothingSpeed\""));
        assertFalse(serialized.contains("\"positionSmoothingSpeed\""));
    }

    @Test
    void presetStateTracksEffectiveComposition() {
        CameraConfig config = new CameraConfig();
        config.validate();
        assertEquals(CompositionPreset.CINEMATIC_RIGHT_SHOULDER, config.compositionPreset);

        config.distance = 5.0;
        assertEquals(CompositionPreset.CUSTOM, config.reconcilePreset());

        config.distance = 3.6;
        config.defaultShoulder = ShoulderSide.LEFT;
        assertEquals(CompositionPreset.CINEMATIC_LEFT_SHOULDER, config.reconcilePreset());

        config.applyPreset(CompositionPreset.COMPACT_RIGHT_SHOULDER);
        assertEquals(2.8, config.distance);
        assertEquals(0.55, config.shoulderOffset);
        assertEquals(0.25, config.verticalOffset);
        assertEquals(ShoulderSide.RIGHT, config.defaultShoulder);
        assertEquals(CompositionPreset.COMPACT_RIGHT_SHOULDER, config.compositionPreset);
    }

    @Test
    void customSelectionPreservesCompositionValues() {
        CameraConfig config = new CameraConfig();
        config.distance = 5.25;
        config.shoulderOffset = 1.1;
        config.verticalOffset = -0.2;

        config.applyPreset(CompositionPreset.CUSTOM);

        assertEquals(5.25, config.distance);
        assertEquals(1.1, config.shoulderOffset);
        assertEquals(-0.2, config.verticalOffset);
        assertEquals(CompositionPreset.CUSTOM, config.compositionPreset);
    }
}
