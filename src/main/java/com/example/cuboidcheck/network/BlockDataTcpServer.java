package com.example.cuboidcheck.network;

import com.example.cuboidcheck.utl.BlockData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class BlockDataTcpServer {

  public static void start(MinecraftServer server, int port) {
    Thread serverThread = new Thread(() -> {
      try (ServerSocket serverSocket = new ServerSocket(port)) {
        System.out.println("TCP BlockData Server listening on port " + port);

        while (!server.isStopped()) {
          Socket clientSocket = serverSocket.accept();

          // Handle each connection in a lightweight thread task to keep the listener open
          Thread.ofVirtual().start(() -> handleClient(server, clientSocket));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, "BlockData-TCP-Server");

    serverThread.setDaemon(true);
    serverThread.start();
  }

  private static void handleClient(MinecraftServer server, Socket socket) {
    try (DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

      while (socket.isConnected() && !socket.isClosed()) {
        // 1. Read request coordinates from Server A
        int targetX = in.readInt();
        int targetY = in.readInt();
        int targetZ = in.readInt();

        // 2. Fetch the block safely on Minecraft's main thread
        CompletableFuture<BlockData> futureData = CompletableFuture.supplyAsync(() -> {
          ServerLevel level = server.overworld();
          BlockPos pos = new BlockPos(targetX, targetY, targetZ);

          BlockState state = level.getBlockState(pos);
          BlockEntity blockEntity = level.getBlockEntity(pos);

          // Use a primitive string representation or codec JSON for blockstate
          com.google.gson.JsonElement stateJson = new com.google.gson.JsonPrimitive(state.toString());
          net.minecraft.nbt.CompoundTag nbt = blockEntity != null
              ? blockEntity.saveWithFullMetadata(level.registryAccess())
              : null;

          return new BlockData(targetX, targetY, targetZ, stateJson, nbt);
        }, server);

        // 3. Wait for the main thread, serialize, and push back down the socket
        // pipeline
        BlockData blockData = futureData.join();
        String jsonResponse = blockData.toJson().toString();
        byte[] responseBytes = jsonResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Send length prefix, then the actual payload data
        out.writeInt(responseBytes.length);
        out.write(responseBytes);
        out.flush();
      }
    } catch (java.io.EOFException ignored) {
      // Client disconnected naturally, safely close connection
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
