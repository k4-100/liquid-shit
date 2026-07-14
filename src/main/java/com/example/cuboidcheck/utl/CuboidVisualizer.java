package com.example.cuboidcheck.utl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class CuboidVisualizer {

  public static final Logger LOGGER = LogUtils.getLogger();

  @Nullable
  private static AABB currentSelection = null;

  public static void setSelection(int x1, int y1, int z1, int x2, int y2, int z2) {
    LOGGER.info("CUBOIDCHECK: Setting preview box coordinates selection...");
    double minX = Math.min(x1, x2);
    double minY = Math.min(y1, y2);
    double minZ = Math.min(z1, z2);

    // Include +1.0 offset so full physical block cubes are highlighted within range
    // selection
    double maxX = Math.max(x1, x2) + 1.0;
    double maxY = Math.max(y1, y2) + 1.0;
    double maxZ = Math.max(z1, z2) + 1.0;

    currentSelection = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
  }

  public static void clearSelection() {
    currentSelection = null;
  }

  public static void renderCuboid(PoseStack poseStack) {
    // Exit quickly if no coordinates are selected to avoid cluttering performance
    // paths
    if (currentSelection == null) {

      LOGGER.info("CUBOIDCHECK: EMPTY SELECTION");
      return;
    }
    LOGGER.info("CUBOIDCHECK: HAS SELECTION");

    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null)
      return;

    Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

    poseStack.pushPose();
    // Translate relative to camera vector space position layouts
    poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

    // Dark Blue with 60% Transparency
    float r = 0.0f;
    float g = 0.0f;
    float b = 0.5f;
    float a = 0.6f;

    // 1. FIXED: Grab standard global buffer source instead of breaking/crumbling
    // overlays
    // VertexConsumer fillBuffer =
    // mc.renderBuffers().bufferSource().getBuffer(RenderType.translucent());
    // renderSolidFilledBox(poseStack, fillBuffer, currentSelection, r, g, b, a);
    // 1. Change the RenderType to a Debug type that matches your format
    VertexConsumer fillBuffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.debugFilledBox());
    renderSolidFilledBox(poseStack, fillBuffer, currentSelection, r, g, b, a);

    // 2. Render a sharp outer outline border wireframe
    VertexConsumer lineBuffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
    LevelRenderer.renderLineBox(
        poseStack,
        lineBuffer,
        currentSelection,
        r, g, b, 1.0f // Fully opaque wireframe boundaries look best with semi-transparent fills
    );

    // FIXED: Crucial to prevent matrix stack corruption crash termination!
    poseStack.popPose();
  }

  private static void renderSolidFilledBox(PoseStack matrixStack, VertexConsumer buffer, AABB box, float r, float g,
      float b, float alpha) {
    PoseStack.Pose entry = matrixStack.last();

    float minX = (float) box.minX;
    float minY = (float) box.minY;
    float minZ = (float) box.minZ;
    float maxX = (float) box.maxX;
    float maxY = (float) box.maxY;
    float maxZ = (float) box.maxZ;

    // Bottom face
    buffer.addVertex(entry, minX, minY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, minY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, minY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, minY, maxZ).setColor(r, g, b, alpha);

    // Top face
    buffer.addVertex(entry, minX, maxY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, maxY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, minZ).setColor(r, g, b, alpha);

    // North face
    buffer.addVertex(entry, minX, minY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, maxY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, minY, minZ).setColor(r, g, b, alpha);

    // South face
    buffer.addVertex(entry, minX, minY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, minY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, maxY, maxZ).setColor(r, g, b, alpha);

    // West face
    buffer.addVertex(entry, minX, minY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, minY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, maxY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, minX, maxY, minZ).setColor(r, g, b, alpha);

    // East face
    buffer.addVertex(entry, maxX, minY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, minZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, maxY, maxZ).setColor(r, g, b, alpha);
    buffer.addVertex(entry, maxX, minY, maxZ).setColor(r, g, b, alpha);
  }
}
