package com.example.cuboidcheck.network;

import com.example.cuboidcheck.utl.BlockData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockDataTcpClient {

  private static Socket socket;
  private static DataOutputStream out;
  private static DataInputStream in;
  private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
  public static final Logger LOGGER = LogUtils.getLogger();

  public static void connect(String ip, int port) {
    try {
      LOGGER.info("CUBOIDCHECK: loading SOCKET");
      socket = new Socket(ip, port);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
      LOGGER.info("CUBOIDCHECK: Successfully connected to Server B via TCP!");
    } catch (Exception e) {
      LOGGER.warn("CUBOIDCHECK: Failed to establish TCP connection to Server B at {}:{}", ip, port);
      e.printStackTrace();
    }
  }

  /**
   * Requests data for all blocks within the specified cuboid boundaries.
   */
  public static void requestCuboidData(MinecraftServer server, int x1, int y1, int z1, int x2, int y2, int z2) {
    if (socket == null || socket.isClosed() || out == null) {
      LOGGER.warn("CUBOIDCHECK: Cannot request cuboid data; TCP socket is disconnected.");
      return;
    }

    networkExecutor.submit(() -> {
      try {
        // 1. Write the bounding box coordinates to the stream
        out.writeInt(x1);
        out.writeInt(y1);
        out.writeInt(z1);
        out.writeInt(x2);
        out.writeInt(y2);
        out.writeInt(z2);
        out.flush();

        // 2. Read the response payload length prefix
        int length = in.readInt();
        if (length > 0) {
          byte[] messageBytes = new byte[length];
          in.readFully(messageBytes);

          String jsonStr = new String(messageBytes, StandardCharsets.UTF_8);
          JsonObject responseJson = JsonParser.parseString(jsonStr).getAsJsonObject();

          JsonArray blocksArray = responseJson.getAsJsonArray("blocks");
          List<BlockData> receivedBlocks = new ArrayList<>();

          for (JsonElement element : blocksArray) {
            receivedBlocks.add(BlockData.fromJson(element.getAsJsonObject()));
          }

          // 3. Synchronize data execution back onto the Minecraft main thread safely
          server.execute(() -> {
            processReceivedCuboid(server, receivedBlocks);
          });
        }
      } catch (Exception e) {
        LOGGER.error("CUBOIDCHECK: Error during cuboid data request process.");
        e.printStackTrace();
      }
    });
  }

  private static void processReceivedCuboid(MinecraftServer server, List<BlockData> blocks) {
    LOGGER.info("CUBOIDCHECK: Processing batch cuboid containing {} blocks.", blocks.size());
    for (BlockData data : blocks) {
      // Your custom logic per block goes here
      // e.g., level.setBlock(...) or tracking layout differentials
    }
  }

  public static void close() {
    try {
      if (socket != null)
        socket.close();
      networkExecutor.shutdown();
    } catch (Exception ignored) {
    }
  }
}
