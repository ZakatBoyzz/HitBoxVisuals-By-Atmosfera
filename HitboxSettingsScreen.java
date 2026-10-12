package com.wwoqkx.hitboxvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class HitboxSettingsScreen extends Screen {
    private static final int[] COLORS = {
            0xFF35D06F, 0xFFFF4545, 0xFF43A5FF, 0xFFFFC928,
            0xFFFF55D8, 0xFFB77BFF, 0xFFFFFFFF, 0xFF25E0D0
    };
    private static final String[] COLOR_NAMES = {
            "обводка", "заливка", "обводка при наведении", "заливка при наведении"
    };
    private int selectedColor = 0;
    private TextFieldWidget hexField;

    public HitboxSettingsScreen() {
        super(Text.literal("Hitbox Visuals"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 105;
        int y = this.height / 4 + 10;
        addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            HitboxConfig.SETTINGS.enabled = !HitboxConfig.SETTINGS.enabled;
            HitboxConfig.save();
            button.setMessage(enabledText());
        }).dimensions(left, y, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(fillText(), button -> {
            HitboxConfig.SETTINGS.fillEnabled = !HitboxConfig.SETTINGS.fillEnabled;
            HitboxConfig.save();
            button.setMessage(fillText());
        }).dimensions(left, y + 25, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(hoverText(), button -> {
            HitboxConfig.SETTINGS.hoverEnabled = !HitboxConfig.SETTINGS.hoverEnabled;
            HitboxConfig.save();
            button.setMessage(hoverText());
        }).dimensions(left, y + 50, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(outlineText(), button -> {
            HitboxConfig.SETTINGS.outlineColor = nextColor(HitboxConfig.SETTINGS.outlineColor);
            HitboxConfig.save();
            button.setMessage(outlineText());
            refreshHexField();
        }).dimensions(left, y + 80, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(fillColorText(), button -> {
            HitboxConfig.SETTINGS.fillColor = withAlpha(nextColor(HitboxConfig.SETTINGS.fillColor), HitboxConfig.SETTINGS.fillAlpha);
            HitboxConfig.save();
            button.setMessage(fillColorText());
            refreshHexField();
        }).dimensions(left, y + 105, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(hoverOutlineText(), button -> {
            HitboxConfig.SETTINGS.hoverOutlineColor = nextColor(HitboxConfig.SETTINGS.hoverOutlineColor);
            HitboxConfig.save();
            button.setMessage(hoverOutlineText());
            refreshHexField();
        }).dimensions(left, y + 130, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(hoverFillText(), button -> {
            HitboxConfig.SETTINGS.hoverFillColor = withAlpha(nextColor(HitboxConfig.SETTINGS.hoverFillColor), HitboxConfig.SETTINGS.fillAlpha);
            HitboxConfig.save();
            button.setMessage(hoverFillText());
            refreshHexField();
        }).dimensions(left, y + 155, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(alphaText(), button -> {
            HitboxConfig.SETTINGS.fillAlpha = (HitboxConfig.SETTINGS.fillAlpha + 32) % 256;
            HitboxConfig.save();
            button.setMessage(alphaText());
        }).dimensions(left, y + 180, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(selectedColorText(), button -> {
            selectedColor = (selectedColor + 1) % COLOR_NAMES.length;
            button.setMessage(selectedColorText());
            refreshHexField();
        }).dimensions(left, y + 210, 210, 20).build());

        hexField = new TextFieldWidget(textRenderer, left, y + 235, 210, 20, Text.literal("HEX color"));
        hexField.setMaxLength(7);
        hexField.setText(hex(getSelectedColor()));
        hexField.setPlaceholder(Text.literal("#RRGGBB"));
        hexField.setTextPredicate(value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        addDrawableChild(hexField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Применить HEX"), button -> applyHex())
                .dimensions(left, y + 260, 210, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close())
                .dimensions(left, y + 285, 210, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Вкл/выкл: " + HitboxVisualsClient.toggleKeyLabel()
                        + " | Настройки: " + HitboxVisualsClient.menuKeyLabel()
                        + " | HEX: #RRGGBB"), width / 2, 30, 0xFFBBBBBB);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Клавиши меняются в Настройки → Управление → Назначение клавиш"),
                width / 2, 42, 0xFFBBBBBB);
        super.render(context, mouseX, mouseY, delta);
    }

    private void applyHex() {
        if (hexField == null) return;
        String value = hexField.getText().trim().replace("#", "");
        if (value.length() != 6) return;
        try {
            int color = 0xFF000000 | Integer.parseInt(value, 16);
            switch (selectedColor) {
                case 0 -> HitboxConfig.SETTINGS.outlineColor = color;
                case 1 -> HitboxConfig.SETTINGS.fillColor = withAlpha(color, HitboxConfig.SETTINGS.fillAlpha);
                case 2 -> HitboxConfig.SETTINGS.hoverOutlineColor = color;
                case 3 -> HitboxConfig.SETTINGS.hoverFillColor = withAlpha(color, HitboxConfig.SETTINGS.fillAlpha);
                default -> { return; }
            }
            HitboxConfig.save();
            refreshHexField();
        } catch (NumberFormatException ignored) {
            // Invalid HEX text is ignored; the field remains editable.
        }
    }

    private void refreshHexField() {
        if (hexField != null) hexField.setText(hex(getSelectedColor()));
    }

    private int getSelectedColor() {
        return switch (selectedColor) {
            case 0 -> HitboxConfig.SETTINGS.outlineColor;
            case 1 -> HitboxConfig.SETTINGS.fillColor;
            case 2 -> HitboxConfig.SETTINGS.hoverOutlineColor;
            default -> HitboxConfig.SETTINGS.hoverFillColor;
        };
    }

    private Text enabledText() { return Text.literal("Хитбоксы: " + onOff(HitboxConfig.SETTINGS.enabled)); }
    private Text fillText() { return Text.literal("Заливка: " + onOff(HitboxConfig.SETTINGS.fillEnabled)); }
    private Text hoverText() { return Text.literal("Цвет при наведении: " + onOff(HitboxConfig.SETTINGS.hoverEnabled)); }
    private Text outlineText() { return Text.literal("Цвет обводки: " + hex(HitboxConfig.SETTINGS.outlineColor)); }
    private Text fillColorText() { return Text.literal("Цвет заливки: " + hex(HitboxConfig.SETTINGS.fillColor)); }
    private Text hoverOutlineText() { return Text.literal("Обводка при наведении: " + hex(HitboxConfig.SETTINGS.hoverOutlineColor)); }
    private Text hoverFillText() { return Text.literal("Заливка при наведении: " + hex(HitboxConfig.SETTINGS.hoverFillColor)); }
    private Text alphaText() { return Text.literal("Прозрачность заливки: " + HitboxConfig.SETTINGS.fillAlpha + "/255"); }
    private Text selectedColorText() { return Text.literal("HEX редактирует: " + COLOR_NAMES[selectedColor]); }
    private String onOff(boolean value) { return value ? "ВКЛ" : "ВЫКЛ"; }
    private String hex(int color) { return String.format("#%06X", color & 0xFFFFFF); }

    private int nextColor(int current) {
        int rgb = current & 0xFFFFFF;
        for (int i = 0; i < COLORS.length; i++) {
            if ((COLORS[i] & 0xFFFFFF) == rgb) return COLORS[(i + 1) % COLORS.length];
        }
        return COLORS[0];
    }

    private int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
    }

    @Override
    public void close() {
        HitboxConfig.save();
        if (client != null) client.setScreen(null);
    }
}
