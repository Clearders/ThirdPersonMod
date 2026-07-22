package dev.thirdpersonmod.screen;

import dev.thirdpersonmod.camera.CompositionPreset;
import dev.thirdpersonmod.camera.ShoulderCameraController;
import dev.thirdpersonmod.camera.ShoulderSide;
import dev.thirdpersonmod.config.CameraConfig;
import dev.thirdpersonmod.config.ConfigManager;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CameraConfigScreen extends Screen {
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;
    private final ConfigManager configManager;
    private final ShoulderCameraController controller;
    private CameraConfig working;
    private Page page = Page.CAMERA;
    private CycleButton<CompositionPreset> presetButton;
    private boolean previewActive;
    private boolean finished;

    public CameraConfigScreen(
        Screen parent,
        ConfigManager configManager,
        ShoulderCameraController controller
    ) {
        super(Component.translatable("config.thirdpersonmod.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.controller = controller;
        this.working = configManager.get().copy();
    }

    @Override
    protected void init() {
        this.presetButton = null;
        this.controller.previewConfiguration(this.working);
        this.previewActive = true;

        int contentWidth = Math.min(410, this.width - 20);
        int left = (this.width - contentWidth) / 2;
        int tabWidth = contentWidth / Page.values().length;
        int tabY = 28;

        for (Page candidate : Page.values()) {
            int index = candidate.ordinal();
            Button tab = Button.builder(
                Component.translatable(candidate.translationKey),
                button -> {
                    this.page = candidate;
                    rebuildWidgets();
                }
            ).bounds(left + index * tabWidth, tabY, tabWidth - 2, WIDGET_HEIGHT).build();
            tab.active = candidate != this.page;
            addRenderableWidget(tab);
        }

        int columnGap = 6;
        int columnWidth = (contentWidth - columnGap) / 2;
        int right = left + columnWidth + columnGap;
        int contentY = 57;

        switch (this.page) {
            case CAMERA -> addCameraOptions(left, right, columnWidth, contentY);
            case COLLISION -> addCollisionOptions(left, right, columnWidth, contentY);
            case BEHAVIOR -> addBehaviorOptions(left, right, columnWidth, contentY);
        }

        int footerY = this.height - 27;
        int footerWidth = Math.min(310, contentWidth);
        int footerLeft = (this.width - footerWidth) / 2;
        int smallWidth = (footerWidth - 8) / 3;
        addRenderableWidget(Button.builder(
            Component.translatable("config.thirdpersonmod.reset"),
            button -> {
                this.working = new CameraConfig();
                previewWorking();
                rebuildWidgets();
            }
        ).bounds(footerLeft, footerY, smallWidth, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            button -> closeWithoutSaving()
        ).bounds(footerLeft + smallWidth + 4, footerY, smallWidth, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> saveAndClose()
        ).bounds(footerLeft + (smallWidth + 4) * 2, footerY, smallWidth, WIDGET_HEIGHT).build());
    }

    private void addCameraOptions(int left, int right, int width, int top) {
        addBoolean(left, top, width, "config.thirdpersonmod.enabled", this.working.enabled,
            value -> {
                this.working.enabled = value;
                previewWorking();
            });
        this.presetButton = addEnum(
            right, top, width, "config.thirdpersonmod.preset", this.working.compositionPreset,
            CompositionPreset.values(), this::applyPreset, "config.thirdpersonmod.preset.");

        addEnum(left, row(top, 1), width, "config.thirdpersonmod.shoulder", this.working.defaultShoulder,
            ShoulderSide.values(), value -> {
                this.working.defaultShoulder = value;
                previewWorking();
            }, "config.thirdpersonmod.shoulder.");
        addSlider(right, row(top, 1), width, "config.thirdpersonmod.distance",
            1.0, 12.0, this.working.distance, 2, value -> {
                this.working.distance = value;
                previewWorking();
            });

        addSlider(left, row(top, 2), width, "config.thirdpersonmod.shoulder_offset",
            0.0, 2.0, this.working.shoulderOffset, 2, value -> {
                this.working.shoulderOffset = value;
                previewWorking();
            });
        addSlider(right, row(top, 2), width, "config.thirdpersonmod.vertical_offset",
            -1.0, 3.0, this.working.verticalOffset, 2, value -> {
                this.working.verticalOffset = value;
                previewWorking();
            });

        addSlider(left, row(top, 3), width, "config.thirdpersonmod.vertical_smoothing",
            0.0, 60.0, this.working.verticalSmoothingSpeed, 1, value -> {
                this.working.verticalSmoothingSpeed = value;
                previewWorking();
            });
        addSlider(right, row(top, 3), width, "config.thirdpersonmod.shoulder_transition",
            0.0, 60.0, this.working.shoulderTransitionSpeed, 1, value -> {
                this.working.shoulderTransitionSpeed = value;
                previewWorking();
            });
    }

    private void addCollisionOptions(int left, int right, int width, int top) {
        addSlider(left, top, width, "config.thirdpersonmod.minimum_distance",
            0.1, 2.0, this.working.minimumDistance, 2, value -> {
                this.working.minimumDistance = value;
                previewWorking();
            });
        addSlider(right, top, width, "config.thirdpersonmod.collision_radius",
            0.01, 0.5, this.working.collisionRadius, 2, value -> {
                this.working.collisionRadius = value;
                previewWorking();
            });
        addSlider(left, row(top, 1), width, "config.thirdpersonmod.safety_margin",
            0.0, 0.5, this.working.collisionSafetyMargin, 2, value -> {
                this.working.collisionSafetyMargin = value;
                previewWorking();
            });
        addSlider(right, row(top, 1), width, "config.thirdpersonmod.collision_in",
            0.0, 60.0, this.working.collisionInSpeed, 1, value -> {
                this.working.collisionInSpeed = value;
                previewWorking();
            });
        addSlider(left, row(top, 2), width, "config.thirdpersonmod.collision_out",
            0.0, 60.0, this.working.collisionOutSpeed, 1, value -> {
                this.working.collisionOutSpeed = value;
                previewWorking();
            });
    }

    private void addBehaviorOptions(int left, int right, int width, int top) {
        addBoolean(left, top, width, "config.thirdpersonmod.disable_riding", this.working.disableWhileRiding,
            value -> {
                this.working.disableWhileRiding = value;
                previewWorking();
            });
        addBoolean(right, top, width, "config.thirdpersonmod.disable_sleeping", this.working.disableWhileSleeping,
            value -> {
                this.working.disableWhileSleeping = value;
                previewWorking();
            });
        addBoolean(left, row(top, 1), width, "config.thirdpersonmod.disable_swimming",
            this.working.disableWhileSwimming, value -> {
                this.working.disableWhileSwimming = value;
                previewWorking();
            });
        addBoolean(right, row(top, 1), width, "config.thirdpersonmod.disable_crawling",
            this.working.disableWhileCrawling, value -> {
                this.working.disableWhileCrawling = value;
                previewWorking();
            });
        addBoolean(left, row(top, 2), width, "config.thirdpersonmod.disable_fall_flying",
            this.working.disableWhileFallFlying, value -> {
                this.working.disableWhileFallFlying = value;
                previewWorking();
            });
        addBoolean(right, row(top, 2), width, "config.thirdpersonmod.debug",
            this.working.debugCameraOwnership, value -> {
                this.working.debugCameraOwnership = value;
                previewWorking();
            });
    }

    private void addBoolean(int x, int y, int width, String key, boolean value, Consumer<Boolean> setter) {
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value).create(
            x, y, width, WIDGET_HEIGHT, Component.translatable(key),
            (cycleButton, updated) -> setter.accept(updated)
        );
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        addRenderableWidget(button);
    }

    private <T> CycleButton<T> addEnum(
        int x,
        int y,
        int width,
        String key,
        T value,
        T[] values,
        Consumer<T> setter,
        String valueKeyPrefix
    ) {
        CycleButton<T> button = CycleButton.<T>builder(
            item -> Component.translatable(valueKeyPrefix + item.toString().toLowerCase(Locale.ROOT)),
            value
        ).withValues(Arrays.asList(values)).create(
            x, y, width, WIDGET_HEIGHT, Component.translatable(key),
            (cycleButton, updated) -> setter.accept(updated)
        );
        button.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        addRenderableWidget(button);
        return button;
    }

    private void addSlider(
        int x,
        int y,
        int width,
        String key,
        double minimum,
        double maximum,
        double value,
        int decimals,
        DoubleConsumer setter
    ) {
        NumericSlider slider = new NumericSlider(
            x, y, width, key, minimum, maximum, value, decimals, setter
        );
        slider.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        addRenderableWidget(slider);
    }

    private void applyPreset(CompositionPreset preset) {
        this.working.applyPreset(preset);
        previewWorking();
        rebuildWidgets();
    }

    private void previewWorking() {
        this.working.validate();
        this.controller.previewConfiguration(this.working);
        if (this.presetButton != null) {
            this.presetButton.setValue(this.working.compositionPreset);
        }
    }

    private void saveAndClose() {
        this.configManager.apply(this.working);
        this.controller.applyConfiguration();
        this.previewActive = false;
        this.finished = true;
        this.minecraft.setScreenAndShow(this.parent);
    }

    private void closeWithoutSaving() {
        if (!this.finished) {
            this.controller.cancelConfigurationPreview();
            this.previewActive = false;
            this.finished = true;
        }
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    @Override
    public void removed() {
        if (this.previewActive && !this.finished) {
            this.controller.cancelConfigurationPreview();
            this.previewActive = false;
            this.finished = true;
        }
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(
            this.font,
            Component.translatable("config.thirdpersonmod.preview_notice"),
            this.width / 2,
            this.height - 39,
            0xFFB0B0B0
        );
    }

    private static int row(int top, int row) {
        return top + row * ROW_SPACING;
    }

    private enum Page {
        CAMERA("config.thirdpersonmod.page.camera"),
        COLLISION("config.thirdpersonmod.page.collision"),
        BEHAVIOR("config.thirdpersonmod.page.behavior");

        private final String translationKey;

        Page(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private static final class NumericSlider extends AbstractSliderButton {
        private final String translationKey;
        private final double minimum;
        private final double maximum;
        private final int decimals;
        private final DoubleConsumer setter;

        private NumericSlider(
            int x,
            int y,
            int width,
            String translationKey,
            double minimum,
            double maximum,
            double current,
            int decimals,
            DoubleConsumer setter
        ) {
            super(
                x,
                y,
                width,
                WIDGET_HEIGHT,
                Component.empty(),
                (current - minimum) / (maximum - minimum)
            );
            this.translationKey = translationKey;
            this.minimum = minimum;
            this.maximum = maximum;
            this.decimals = decimals;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String format = "%." + this.decimals + "f";
            setMessage(Component.translatable(
                this.translationKey,
                String.format(Locale.ROOT, format, actualValue())
            ));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(actualValue());
        }

        private double actualValue() {
            double raw = this.minimum + this.value * (this.maximum - this.minimum);
            double scale = Math.pow(10.0, this.decimals);
            return Math.round(raw * scale) / scale;
        }
    }
}
