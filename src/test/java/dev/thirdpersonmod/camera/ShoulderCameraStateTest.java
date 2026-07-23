package dev.thirdpersonmod.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShoulderCameraStateTest {
    @Test
    void ownershipTransitionsClearSmoothingState() {
        ShoulderCameraState state = new ShoulderCameraState(ShoulderSide.RIGHT);

        state.enterPlayer(3.5);
        state.currentShoulderOffset(0.75);
        state.currentVerticalOffset(0.35);
        state.movementBlend(0.8);
        state.focusBlend(0.4);
        state.fovOffsetDegrees(2.0);
        assertTrue(state.initialized());
        assertEquals(CameraOwnerState.PLAYER, state.owner());

        state.leavePlayer(CameraOwnerState.EXTERNAL);
        assertFalse(state.initialized());
        assertEquals(0.0, state.currentDistance());
        assertEquals(0.0, state.currentShoulderOffset());
        assertEquals(0.0, state.currentVerticalOffset());
        assertEquals(0.0, state.movementBlend());
        assertEquals(0.0, state.focusBlend());
        assertEquals(0.0, state.fovOffsetDegrees());
        assertEquals(CameraOwnerState.PLAYER, state.previousOwner());
        assertEquals(CameraOwnerState.EXTERNAL, state.owner());

        state.resetForLifecycleChange();
        assertEquals(CameraOwnerState.EXTERNAL, state.previousOwner());
        assertEquals(CameraOwnerState.INACTIVE, state.owner());
    }

    @Test
    void shoulderCanToggleAndBeRestored() {
        ShoulderCameraState state = new ShoulderCameraState(ShoulderSide.RIGHT);

        state.toggleShoulder();
        assertEquals(ShoulderSide.LEFT, state.shoulder());

        state.shoulder(ShoulderSide.RIGHT);
        assertEquals(ShoulderSide.RIGHT, state.shoulder());
    }
}
