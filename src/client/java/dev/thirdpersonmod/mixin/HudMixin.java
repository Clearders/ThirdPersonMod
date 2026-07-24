package dev.thirdpersonmod.mixin;

import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.CameraPresentationState;
import dev.thirdpersonmod.camera.ShoulderCameraController;
import dev.thirdpersonmod.hud.CrosshairProjection;
import java.util.Optional;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    private static final Identifier THIRDPERSONMOD_CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Identifier THIRDPERSONMOD_ATTACK_FULL = Identifier.withDefaultNamespace(
        "hud/crosshair_attack_indicator_full"
    );
    private static final Identifier THIRDPERSONMOD_ATTACK_BACKGROUND = Identifier.withDefaultNamespace(
        "hud/crosshair_attack_indicator_background"
    );
    private static final Identifier THIRDPERSONMOD_ATTACK_PROGRESS = Identifier.withDefaultNamespace(
        "hud/crosshair_attack_indicator_progress"
    );
    private static final int CROSSHAIR_SIZE = 15;
    private static final int CROSSHAIR_HALF_SIZE = 8;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER
        )
    )
    private void thirdpersonmod$extractCorrectedCrosshair(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        CallbackInfo callbackInfo
    ) {
        ShoulderCameraController controller = ShoulderCameraClient.controller();
        if (controller == null || this.minecraft.player == null || this.minecraft.player.isSpectator()) {
            return;
        }

        Camera camera = this.minecraft.gameRenderer.mainCamera();
        CameraPresentationState presentation = controller.presentationState(camera);
        if (!presentation.active() || !presentation.correctedCrosshairEnabled() || this.minecraft.hitResult == null) {
            return;
        }

        double framebufferAspect = (double) this.minecraft.getWindow().getWidth()
            / this.minecraft.getWindow().getHeight();
        Optional<CrosshairProjection.ScreenPoint> projected = CrosshairProjection.project(
            camera.position(),
            this.minecraft.hitResult.getLocation(),
            new Vec3(camera.forwardVector()),
            new Vec3(camera.leftVector()).scale(-1.0),
            new Vec3(camera.upVector()),
            camera.getFov(),
            framebufferAspect,
            graphics.guiWidth(),
            graphics.guiHeight()
        );
        if (projected.isEmpty()) {
            return;
        }

        CrosshairProjection.ScreenPoint visiblePoint = CrosshairProjection.clampToViewport(
            projected.get(),
            graphics.guiWidth(),
            graphics.guiHeight(),
            CROSSHAIR_HALF_SIZE
        );
        int centerX = (int) Math.round(visiblePoint.x());
        int centerY = (int) Math.round(visiblePoint.y());

        graphics.nextStratum();
        graphics.blitSprite(
            RenderPipelines.CROSSHAIR,
            THIRDPERSONMOD_CROSSHAIR,
            centerX - CROSSHAIR_SIZE / 2,
            centerY - CROSSHAIR_SIZE / 2,
            CROSSHAIR_SIZE,
            CROSSHAIR_SIZE
        );
        extractAttackIndicator(graphics, centerX, centerY);
    }

    private void extractAttackIndicator(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        if (this.minecraft.options.attackIndicator().get() != AttackIndicatorStatus.CROSSHAIR) {
            return;
        }

        float attackStrength = this.minecraft.player.getAttackStrengthScale(0.0F);
        boolean renderFull = this.minecraft.crosshairPickEntity instanceof LivingEntity
            && attackStrength >= 1.0F
            && this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F
            && this.minecraft.crosshairPickEntity.isAlive();
        if (renderFull) {
            AttackRange attackRange = this.minecraft.player.getActiveItem().get(DataComponents.ATTACK_RANGE);
            renderFull = attackRange == null
                || attackRange.isInRange(this.minecraft.player, this.minecraft.hitResult.getLocation());
        }

        int x = centerX - 8;
        int y = centerY - 7 + 16;
        if (renderFull) {
            graphics.blitSprite(RenderPipelines.CROSSHAIR, THIRDPERSONMOD_ATTACK_FULL, x, y, 16, 16);
        } else if (attackStrength < 1.0F) {
            int progress = (int) (attackStrength * 17.0F);
            graphics.blitSprite(RenderPipelines.CROSSHAIR, THIRDPERSONMOD_ATTACK_BACKGROUND, x, y, 16, 4);
            graphics.blitSprite(
                RenderPipelines.CROSSHAIR,
                THIRDPERSONMOD_ATTACK_PROGRESS,
                16,
                4,
                0,
                0,
                x,
                y,
                progress,
                4
            );
        }
    }
}
