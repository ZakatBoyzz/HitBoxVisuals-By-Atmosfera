package com.wwoqkx.hitboxvisuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
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

    /*
     * Custom layer:
     * - enables alpha blending;
     * - keeps depth testing enabled;
     * - disables depth writes;
     * - uses the position-color shader.
     *
     * The fill must be rendered BEFORE player models.
     */
    private static final RenderLayer FILL_LAYER = new RenderLayer(
            "hitboxvisuals:fill",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false,
            true,
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.setShader(
                        GameRenderer::getPositionColorProgram
                );
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
            }
    ) {};

    // Called BEFORE player entities are rendered.
    public static void renderFill(WorldRenderContext context) {
        render(context, true, false);
    }

    // Called AFTER player entities are rendered.
    public static void renderOutline(WorldRenderContext context) {
        render(context, false, true);
    }

    private static void render(
            WorldRenderContext context,
            boolean drawFill,
            boolean drawOutline
    ) {
        HitboxConfig.Settings settings = HitboxConfig.SETTINGS;
        MinecraftClient client = MinecraftClient.getInstance();

        if (!settings.enabled
                || client.player == null
                || context.world() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();

        if (matrices == null || consumers == null) {
            return;
        }

        Vec3d cameraPos = context.camera().getPos();
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

                // Invisible players without armor stay hidden.
                boolean invisible = target.isInvisible();

                if (invisible && !hasAnyArmor(target)) {
                    continue;
                }

                // Keep the stable interpolated hitbox position.
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

                // Do not render through obstructing blocks.
                if (!hasClearViewToAnySample(client, box, cameraPos)) {
                    continue;
                }

                boolean isHovered = target == hovered;

                if (drawFill && settings.fillEnabled) {
                    int fillColor = withAlpha(
                            isHovered
                                    ? settings.hoverFillColor
                                    : settings.fillColor,
                            settings.fillAlpha
                    );

                    float[] color = rgba(fillColor);

                    VertexConsumer consumer =
                            consumers.getBuffer(FILL_LAYER);

                    VertexRendering.drawFilledBox(
                            matrices,
                            consumer,
                            box.minX,
                            box.minY,
                            box.minZ,
                            box.maxX,
                            box.maxY,
                            box.maxZ,
                            color[0],
                            color[1],
                            color[2],
                            color[3]
                    );
                }

                if (drawOutline) {
                    int outlineColor = isHovered
                            ? settings.hoverOutlineColor
                            : settings.outlineColor;

                    float[] color = rgba(outlineColor);

                    VertexConsumer consumer =
                            consumers.getBuffer(RenderLayer.getLines());

                    VertexRendering.drawBox(
                            matrices,
                            consumer,
                            box,
                            color[0],
                            color[1],
                            color[2],
                            color[3]
                    );
                }
            }
        } finally {
            matrices.pop();
        }

        /*
         * Flush the fill immediately.
         * This ensures it is drawn before entity models.
         */
        if (drawFill && settings.fillEnabled
                && consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(FILL_LAYER);
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
        Box worldBox = box.offset(camera.x, camera.y, camera.z);

        double midX = (worldBox.minX + worldBox.maxX) * 0.5;
        double midY = (worldBox.minY + worldBox.maxY) * 0.5;
        double midZ = (worldBox.minZ + worldBox.maxZ) * 0.5;

        Vec3d[] points = {
                new Vec3d(midX, worldBox.minY + 0.12, midZ),
                new Vec3d(midX, midY, midZ),
                new Vec3d(midX, worldBox.maxY - 0.12, midZ),
                new Vec3d(
                        worldBox.minX + 0.03,
                        midY,
                        worldBox.minZ + 0.03
                ),
                new Vec3d(
                        worldBox.maxX - 0.03,
                        midY,
                        worldBox.minZ + 0.03
                ),
                new Vec3d(
                        worldBox.minX + 0.03,
                        midY,
                        worldBox.maxZ - 0.03
                ),
                new Vec3d(
                        worldBox.maxX - 0.03,
                        midY,
                        worldBox.maxZ - 0.03
                ),
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
