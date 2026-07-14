package com.example.cuboidcheck.utl;

import com.example.cuboidcheck.CuboidCheck;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// This annotation tells NeoForge to automatically register this class to the NeoForge event bus on the client side only
// @EventBusSubscriber(modid = "cuboidcheck", value = Dist.CLIENT)
@EventBusSubscriber(modid = CuboidCheck.MODID, value = Dist.CLIENT)
// @Mod(CuboidCheck.MODID)
public class ClientRenderHandler {

  @SubscribeEvent
  public static void onRenderLevelStage(RenderLevelStageEvent event) {
    // We render immediately after translucent blocks finish drawing so our clear
    // blue box layers on top nicely
    if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
      CuboidVisualizer.renderCuboid(event.getPoseStack());
    }
  }
}
