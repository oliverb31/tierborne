package com.ollie.tierborne.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ollie.tierborne.playerclass.SkillIcon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class RpgUi {
    static final int BACKDROP = 0xE0101218;
    static final int PANEL = 0xF0181C24;
    static final int PANEL_LIGHT = 0xFF242A34;
    public static final int GOLD = 0xFFD7AD55;
    public static final int GOLD_DARK = 0xFF71572D;
    public static final int TEXT = 0xFFE9E2D0;
    static final int MUTED = 0xFF9B968A;
    static final int LOCKED = 0xFF4A4D54;
    static final int AVAILABLE = 0xFFB98538;
    static final int UNLOCKED = 0xFF4EA56B;

    private RpgUi() {}

    public static void panel(PoseStack poseStack, int left, int top, int right, int bottom) {
        GuiComponent.fill(poseStack, left - 2, top - 2, right + 2, bottom + 2, 0xD0000000);
        GuiComponent.fill(poseStack, left, top, right, bottom, PANEL);
        GuiComponent.fill(poseStack, left, top, right, top + 1, GOLD_DARK);
        GuiComponent.fill(poseStack, left, bottom - 1, right, bottom, GOLD_DARK);
        GuiComponent.fill(poseStack, left, top, left + 1, bottom, GOLD_DARK);
        GuiComponent.fill(poseStack, right - 1, top, right, bottom, GOLD_DARK);
        corner(poseStack, left, top, 1, 1);
        corner(poseStack, right, top, -1, 1);
        corner(poseStack, left, bottom, 1, -1);
        corner(poseStack, right, bottom, -1, -1);
    }

    public static void border(PoseStack poseStack, int left, int top, int right, int bottom, int color) {
        GuiComponent.fill(poseStack, left, top, right, top + 1, color);
        GuiComponent.fill(poseStack, left, bottom - 1, right, bottom, color);
        GuiComponent.fill(poseStack, left, top, left + 1, bottom, color);
        GuiComponent.fill(poseStack, right - 1, top, right, bottom, color);
    }

    static void node(PoseStack poseStack, int centerX, int centerY, int radius, int color, boolean hovered) {
        int outer = hovered ? GOLD : color;
        GuiComponent.fill(poseStack, centerX - radius + 5, centerY - radius, centerX + radius - 5, centerY + radius, outer);
        GuiComponent.fill(poseStack, centerX - radius, centerY - radius + 5, centerX + radius, centerY + radius - 5, outer);
        GuiComponent.fill(poseStack, centerX - radius + 4, centerY - radius + 4, centerX + radius - 4, centerY + radius - 4, PANEL_LIGHT);
    }

    static void line(PoseStack poseStack, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            GuiComponent.fill(poseStack, x - thickness, y - thickness,
                    x + thickness + 1, y + thickness + 1, color);
        }
    }

    static int drawWrapped(PoseStack poseStack, Font font, String text, int x, int y, int width, int color) {
        return drawWrapped(poseStack, font, text, x, y, width, color, Integer.MAX_VALUE);
    }

    static int drawWrapped(PoseStack poseStack, Font font, String text, int x, int y,
                           int width, int color, int maxLines) {
        int safeWidth = Math.max(12, width);
        List<FormattedCharSequence> lines = font.split(Component.literal(text), safeWidth);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            FormattedCharSequence line = lines.get(i);
            font.draw(poseStack, line, x, y, color);
            y += font.lineHeight + 2;
        }
        return y;
    }

    static void drawFitted(PoseStack poseStack, Font font, Component text, int x, int y,
                           int availableWidth, int color) {
        float scale = textScale(font, text, availableWidth);
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0F);
        font.draw(poseStack, text, 0, 0, color);
        poseStack.popPose();
    }

    static void drawCenteredFitted(PoseStack poseStack, Font font, Component text, int centerX, int y,
                                   int availableWidth, int color) {
        float scale = textScale(font, text, availableWidth);
        poseStack.pushPose();
        poseStack.translate(centerX, y, 0);
        poseStack.scale(scale, scale, 1.0F);
        font.draw(poseStack, text, -font.width(text) / 2.0F, 0, color);
        poseStack.popPose();
    }

    private static float textScale(Font font, Component text, int availableWidth) {
        int textWidth = font.width(text);
        if (textWidth <= 0 || textWidth <= availableWidth) return 1.0F;
        return Math.max(0.72F, availableWidth / (float) textWidth);
    }

    static boolean inside(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    static void classIcon(Minecraft minecraft, net.minecraft.world.item.ItemStack icon, int centerX, int centerY) {
        minecraft.getItemRenderer().renderAndDecorateItem(icon, centerX - 8, centerY - 8);
    }

    static void skillIcon(PoseStack poseStack, Minecraft minecraft, SkillIcon icon, int centerX, int centerY,
                          float scale) {
        poseStack.pushPose();
        try {
            if (!icon.isEffect()) {
                ItemRenderer itemRenderer = minecraft.getItemRenderer();
                float previousBlitOffset = itemRenderer.blitOffset;
                PoseStack modelView = RenderSystem.getModelViewStack();
                modelView.pushPose();
                try {
                    modelView.translate(centerX, centerY, 0.0D);
                    modelView.scale(scale, scale, 1.0F);
                    modelView.translate(-centerX, -centerY, 0.0D);
                    RenderSystem.applyModelViewMatrix();
                    itemRenderer.blitOffset = 0.0F;
                    itemRenderer.renderAndDecorateItem(
                            new net.minecraft.world.item.ItemStack(icon.item()), centerX - 8, centerY - 8);
                    minecraft.renderBuffers().bufferSource().endBatch();
                } finally {
                    modelView.popPose();
                    RenderSystem.applyModelViewMatrix();
                    itemRenderer.blitOffset = previousBlitOffset;
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableDepthTest();
                }
                return;
            }
            TextureAtlasSprite sprite = minecraft.getMobEffectTextures().get(icon.effect());
            RenderSystem.setShaderTexture(0, sprite.atlas().location());
            poseStack.translate(centerX, centerY, 0.0D);
            poseStack.scale(scale, scale, 1.0F);
            GuiComponent.blit(poseStack, -9, -9, 0, 18, 18, sprite);
        } finally {
            poseStack.popPose();
        }
    }

    private static void corner(PoseStack poseStack, int x, int y, int xDirection, int yDirection) {
        GuiComponent.fill(poseStack, x, y, x + 7 * xDirection, y + yDirection, GOLD);
        GuiComponent.fill(poseStack, x, y, x + xDirection, y + 7 * yDirection, GOLD);
    }
}
