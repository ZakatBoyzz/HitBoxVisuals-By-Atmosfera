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

        if (!settings.enabled || client.player == null || context.world() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();

        if (matrices == null || consumers == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();

        // Interpolate player positions between game ticks.
        float tickDelta = context.tickCounter().getTickDelta(false);

        PlayerEntity hovered = null;
        HitResult crosshair = client.crosshairTarget;

        if (settings.hoverEnabled
                && crosshair instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof PlayerEntity playerHit) {
            hovered = playerHit;
        }

        matrices.push();

        try {
            for (PlayerEntity target : context.world().getPlayers()) {
                if (target == client.player || !target.isAlive()) {
                    continue;
                }

                // Preserve the invisible-player fix.
                boolean invisible = target.isInvisible();
                boolean wearingArmor = hasAnyArmor(target);

                if (invisible && !wearingArmor) {
                    continue;
                }

                // Interpolated position prevents the hitbox from jumping
                // between discrete entity positions while the player runs.
                Vec3d interpolatedPos = target.getLerpedPos(tickDelta);

                double offsetX =
                        interpolatedPos.x - target.getX() - cameraPos.x;
                double offsetY =
                        interpolatedPos.y - target.getY() - cameraPos.y;
                double offsetZ =
                        interpolatedPos.z - target.getZ() - cameraPos.z;

                Box box = target.getBoundingBox().offset(
                        offsetX,
                        offsetY,
                        offsetZ
                );

                // Keep hitboxes from appearing through obstructing blocks.
                if (!hasClearViewToAnySample(client, box, cameraPos)) {
                    continue;
                }

                boolean isHovered = target == hovered;

                int outline = isHovered
                        ? settings.hoverOutlineColor
                        : settings.outlineColor;

                int fill = withAlpha(
                        isHovered ? settings.hoverFillColor : settings.fillColor,
                        settings.fillAlpha
                );

                // Filled hitbox.
                if (settings.fillEnabled) {
                    VertexConsumer fillConsumer =
                            consumers.getBuffer(RenderLayer.getDebugFilledBox());

                    float[] rgba = rgba(fill);

                    VertexRendering.drawFilledBox(
                            matrices,
                            fillConsumer,
                            box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ,
                            rgba[0], rgba[1], rgba[2], rgba[3]
                    );
                }

                // Hitbox outline.
                VertexConsumer lineConsumer =
                        consumers.getBuffer(RenderLayer.getLines());

                float[] line = rgba(outline);

                VertexRendering.drawBox(
                        matrices,
                        lineConsumer,
                        box,
                        line[0], line[1], line[2], line[3]
                );
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

    private static boolean hasClearViewToAnySample(
            MinecraftClient client,
            Box box,
            Vec3d camera
    ) {
        // Convert the camera-relative box back to world coordinates
        // for raycasting against blocks.
        Box worldBox = box.offset(camera.x, camera.y, camera.z);

        double midX = (worldBox.minX + worldBox.maxX) * 0.5;
        double midY = (worldBox.minY + worldBox.maxY) * 0.5;
        double midZ = (worldBox.minZ + worldBox.maxZ) * 0.5;

        Vec3d[] points = new Vec3d[] {
                new Vec3d(midX, worldBox.minY + 0.12, midZ),
                new Vec3d(midX, midY, midZ),
                new Vec3d(midX, worldBox.maxY - 0.12, midZ),
                new Vec3d(worldBox.minX + 0.03, midY, worldBox.minZ + 0.03),
                new Vec3d(worldBox.maxX - 0.03, midY, worldBox.minZ + 0.03),
                new Vec3d(worldBox.minX + 0.03, midY, worldBox.maxZ - 0.03),
                new Vec3d(worldBox.maxX - 0.03, midY, worldBox.maxZ - 0.03),
                new Vec3d(midX, worldBox.maxY - 0.05, midZ)
        };

        for (Vec3d point : points) {
            HitResult result = client.world.raycast(
                    new RaycastContext(
                            camera,
                            point,
                            RaycastContext.ShapeType.VISUAL,
                            RaycastContext.FluidHandling.NONE,
                            client.player
                    )
            );

            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24)
                | (argb & 0x00FFFFFF);
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
