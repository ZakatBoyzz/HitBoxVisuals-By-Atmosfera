package com.wwoqkx.hitboxvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class HitboxRenderer {
    private HitboxRenderer() {}

    public static void render(WorldRenderContext context) {
        HitboxConfig.Settings settings = HitboxConfig.SETTINGS;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!settings.enabled || client.player == null || context.world() == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cameraPos = context.camera().getPos();
        PlayerEntity hovered = null;
        HitResult crosshair = client.crosshairTarget;
        if (settings.hoverEnabled && crosshair instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof PlayerEntity playerHit) {
            hovered = playerHit;
        }

        matrices.push();
        try {
            for (PlayerEntity target : context.world().getPlayers()) {
                if (target == client.player || !target.isAlive()) continue;

                boolean invisible = target.hasStatusEffect(StatusEffects.INVISIBILITY);
                boolean wearingArmor = hasAnyArmor(target);

                // Invisible players with no equipped armor are never rendered, under any condition.
                if (invisible && !wearingArmor) continue;

                // Explicit block ray checks are intentional: even if a renderer or shader changes
                // depth behavior, a fully occluded player must not receive a hitbox overlay.
                if (!hasClearViewToAnySample(client, target, cameraPos)) continue;

                boolean isHovered = target == hovered;
                int outline = isHovered ? settings.hoverOutlineColor : settings.outlineColor;
                int fill = withAlpha(isHovered ? settings.hoverFillColor : settings.fillColor, settings.fillAlpha);

                Box box = target.getBoundingBox().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                if (settings.fillEnabled) {
                    VertexConsumer fillConsumer = consumers.getBuffer(RenderLayer.getDebugFilledBox());
                    float[] rgba = rgba(fill);
                    VertexRendering.drawFilledBox(matrices, fillConsumer,
                            box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            rgba[0], rgba[1], rgba[2], rgba[3]);
                }

                VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getLines());
                float[] line = rgba(outline);
                VertexRendering.drawBox(matrices, lineConsumer, box,
                        line[0], line[1], line[2], line[3]);
            }
        } finally {
            matrices.pop();
        }
    }

    private static boolean hasAnyArmor(PlayerEntity player) {
        return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private static boolean hasClearViewToAnySample(MinecraftClient client, PlayerEntity target, Vec3d camera) {
        // Check multiple points so an exposed part of a player can still be seen around a corner.
        Box b = target.getBoundingBox();
        double midX = (b.minX + b.maxX) * 0.5;
        double midY = (b.minY + b.maxY) * 0.5;
        double midZ = (b.minZ + b.maxZ) * 0.5;
        Vec3d[] points = new Vec3d[] {
                new Vec3d(midX, b.minY + 0.12, midZ),
                new Vec3d(midX, midY, midZ),
                new Vec3d(midX, b.maxY - 0.12, midZ),
                new Vec3d(b.minX + 0.03, midY, b.minZ + 0.03),
                new Vec3d(b.maxX - 0.03, midY, b.minZ + 0.03),
                new Vec3d(b.minX + 0.03, midY, b.maxZ - 0.03),
                new Vec3d(b.maxX - 0.03, midY, b.maxZ - 0.03),
                new Vec3d(midX, b.maxY - 0.05, midZ)
        };

        for (Vec3d point : points) {
            HitResult result = client.world.raycast(new RaycastContext(
                    camera, point,
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    client.player
            ));
            if (result.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    private static float[] rgba(int argb) {
        return new float[] {
                ((argb >> 16) & 255) / 255.0f,
                ((argb >> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f
        };
    }
}
