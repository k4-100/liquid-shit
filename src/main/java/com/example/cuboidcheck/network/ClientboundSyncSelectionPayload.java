package com.example.cuboidcheck.network;

import com.example.cuboidcheck.CuboidCheck;
import com.example.cuboidcheck.utl.CuboidVisualizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundSyncSelectionPayload(int x1, int y1, int z1, int x2, int y2, int z2)
    implements CustomPacketPayload {

  public static final Type<ClientboundSyncSelectionPayload> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath(CuboidCheck.MODID, "sync_selection"));

  // Codec telling Minecraft how to serialize/deserialize the 6 integers across
  // the pipe
  public static final StreamCodec<FriendlyByteBuf, ClientboundSyncSelectionPayload> CODEC = StreamCodec.of(
      (buf, val) -> {
        buf.writeInt(val.x1);
        buf.writeInt(val.y1);
        buf.writeInt(val.z1);
        buf.writeInt(val.x2);
        buf.writeInt(val.y2);
        buf.writeInt(val.z2);
      },
      buf -> new ClientboundSyncSelectionPayload(
          buf.readInt(), buf.readInt(), buf.readInt(),
          buf.readInt(), buf.readInt(), buf.readInt()));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  // This executes explicitly on the CLIENT when the packet arrives
  public static void handleClient(final ClientboundSyncSelectionPayload payload, final IPayloadContext context) {
    context.enqueueWork(() -> {
      CuboidVisualizer.setSelection(
          payload.x1(), payload.y1(), payload.z1(),
          payload.x2(), payload.y2(), payload.z2());
    });
  }
}
