package com.example.cuboidcheck.network;

import com.example.cuboidcheck.utl.BlockData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

public class BlockDataTcpClient {

  private static Socket socket;
  private static DataOutputStream out;
  private static DataInputStream in;
  private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
  public static final Logger LOGGER = LogUtils.getLogger();

  public static void connect(String ip, int port) {
    // networkExecutor.submit(() -> {
      try {
        LOGGER.info("CUBOIDCHECK: loading SOCKET");
        socket = new Socket(ip, port);
        LOGGER.info("CUBOIDCHECK: loading SOCKET IN");
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        // System.out.println("Successfully connected to Server B via TCP!");
        LOGGER.info("CUBOIDCHECK: Successfully connected to Server B via TCP!");
      } catch (Exception e) {
        LOGGER.warn("CUBOIDCHECK: Failed to establish TCP connection to Server B.");
        LOGGER.warn("CUBOIDCHECK: " + ip + ":" + port );
        e.printStackTrace();
      }
    // });
  }

  public static void requestBlockData(MinecraftServer server, int x, int y, int z) {
    if (socket == null || socket.isClosed() || out == null) {
      LOGGER.warn("CUBOIDCHECK:  Cannot request block data; TCP socket is disconnected.");
      return;
    }

    networkExecutor.submit(() -> {
      try {
        // 1. Write request payload
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.flush();

        // 2. Read length prefix of the response payload
        int length = in.readInt();
        if (length > 0) {
          byte[] messageBytes = new byte[length];
          in.readFully(messageBytes);

          String jsonStr = new String(messageBytes, StandardCharsets.UTF_8);
          JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
          BlockData receivedData = BlockData.fromJson(json);

          // 3. Synchronize data execution safely on Server A's main world thread
          server.execute(() -> {
            processReceivedBlock(server, receivedData);
          });
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private static void processReceivedBlock(MinecraftServer server, BlockData data) {
    // Handle your logic on Server A here!
    LOGGER.info("CUBOIDCHECK:  Processing fast TCP block at XYZ: " + data.x() + ", " + data.y() + ", " + data.z());
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
