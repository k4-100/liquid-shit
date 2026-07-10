package com.example.cuboidcheck.network;

import com.example.cuboidcheck.utl.BlockData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockDataTcpServer {

  public static final Logger LOGGER = LogUtils.getLogger();
  private static boolean isServerRunning = false;

  public static synchronized void start(MinecraftServer server, int port) {
    // If the server is already active or spinning up, bail out immediately!
    if (isServerRunning) {
      LOGGER.warn("CUBOIDCHECK: Server start requested, but TCP Server is already running on port " + port);
      return;
    }

    isServerRunning = true;

    Thread serverThread = new Thread(() -> {
      try (ServerSocket serverSocket = new ServerSocket(port)) {
        LOGGER.info("CUBOIDCHECK: TCP BlockData Server listening on port " + port);

        while (!server.isStopped()) {
          Socket clientSocket = serverSocket.accept();
          Thread.ofVirtual().start(() -> handleClient(server, clientSocket));
        }
      } catch (Exception e) {
        LOGGER.error("CUBOIDCHECK: Server socket crashed on port " + port);
        e.printStackTrace();
      } finally {
        // If it crashes or stops, reset the flag so it can be restarted later if needed
        synchronized (BlockDataTcpServer.class) {
          isServerRunning = false;
        }
      }
    }, "BlockData-TCP-Server");

    serverThread.setDaemon(true);
    serverThread.start();
  }

  private static void handleClient(MinecraftServer server, Socket socket) {
    try (socket; // Auto-closes socket when finishing execution block
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

      while (socket.isConnected() && !socket.isClosed()) {
        // 1. Read cuboid boundary parameters from Client
        int x1 = in.readInt();
        int y1 = in.readInt();
        int z1 = in.readInt();
        int x2 = in.readInt();
        int y2 = in.readInt();
        int z2 = in.readInt();

        // Establish correct normalized boundaries (min coordinates up to max
        // coordinates)
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        // 2. Fetch the whole cuboid volume payload safely on Minecraft's main thread
        CompletableFuture<List<BlockData>> futureData = CompletableFuture.supplyAsync(() -> {
          ServerLevel level = server.overworld();
          List<BlockData> cuboidBlocks = new ArrayList<>();

          for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
              for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);

                // com.google.gson.JsonElement stateJson = new com.google.gson.JsonPrimitive(
                // state.toString());

                // Replace it with this:
                String stateString = net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(state);
                com.google.gson.JsonElement stateJson = new com.google.gson.JsonPrimitive(stateString);

                net.minecraft.nbt.CompoundTag nbt = blockEntity != null
                    ? blockEntity.saveWithFullMetadata(level.registryAccess())
                    : null;

                cuboidBlocks.add(new BlockData(x, y, z, stateJson, nbt));
              }
            }
          }
          return cuboidBlocks;
        }, server);

        // 3. Serialize packed dataset and send it back down the pipeline
        List<BlockData> blockDataList = futureData.join();

        JsonObject responseJson = new JsonObject();
        JsonArray blocksArray = new JsonArray();
        for (BlockData bd : blockDataList) {
          blocksArray.add(bd.toJson());
        }
        responseJson.add("blocks", blocksArray);

        String jsonResponse = responseJson.toString();
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        // Write length prefix followed by complete JSON structural data
        out.writeInt(responseBytes.length);
        out.write(responseBytes);
        out.flush();
      }
    } catch (java.io.EOFException ignored) {
      // Client cleanly closed its side of the stream connection pipeline
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
