package com.example.cuboidcheck.network;

import com.example.cuboidcheck.utl.BlockData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

  // private static boolean isServerRunning = false;

  public static void connect(String ip, int port) {
    if (socket != null && !socket.isClosed()) {
      LOGGER.warn("CUBOIDCHECK: Socket connection requested, but TCP Server is already running on port " + port);
      return;
    }

    try {
      LOGGER.info("CUBOIDCHECK: loading SOCKET");
      socket = new Socket(ip, port);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
      LOGGER.info("CUBOIDCHECK: Successfully connected to Server B via TCP!");
    } catch (Exception e) {
      // LOGGER.warn("CUBOIDCHECK: Failed to establish TCP connection to Server B at
      // {}:{}", ip, port);
      LOGGER.error("CUBOIDCHECK: Failed to establish TCP connection to Server B at {}:{}", ip, port);
      e.printStackTrace();
    }
  }
  // public static synchronized void connect(String ip, int port) {
  //
  // if (!socket.isClosed() && socket.isBound()) {
  // LOGGER.warn("CUBOIDCHECK: Socket connection requested, but TCP Server is
  // already running on port " + port);
  // return;
  // }
  //
  // try {
  // LOGGER.info("CUBOIDCHECK: loading SOCKET");
  // socket = new Socket(ip, port);
  // out = new DataOutputStream(socket.getOutputStream());
  // in = new DataInputStream(socket.getInputStream());
  // LOGGER.info("CUBOIDCHECK: Successfully connected to Server B via TCP!");
  // } catch (Exception e) {
  // LOGGER.warn("CUBOIDCHECK: Failed to establish TCP connection to Server B at
  // {}:{}", ip, port);
  // e.printStackTrace();
  // }
  // // finally {
  // //
  // // synchronized (BlockDataTcpClient.class) {
  // // isServerRunning = false;
  // // }
  // // }
  // }

  /**
   * Requests data for all blocks within the specified cuboid boundaries.
   */
  public static void requestCuboidData(MinecraftServer server, int x1, int y1, int z1, int x2, int y2, int z2) {
    if (socket == null || socket.isClosed() || out == null) {
      LOGGER.warn("CUBOIDCHECK: Cannot request cuboid data; TCP socket is disconnected.");
      return;
    }

    networkExecutor.submit(() -> {

      LOGGER.info("CUBOIDCHECK: networkExecutor.submit(() ...");
      try {
        // 1. Write the bounding box coordinates to the stream
        out.writeInt(x1);
        out.writeInt(y1);
        out.writeInt(z1);
        out.writeInt(x2);
        out.writeInt(y2);
        out.writeInt(z2);
        out.flush();

        LOGGER.info("CUBOIDCHECK: requestCuboidData out.flush()...");
        // 2. Read the response payload length prefix
        int length = in.readInt();
        if (length > 0) {
          byte[] messageBytes = new byte[length];
          in.readFully(messageBytes);

          String jsonStr = new String(messageBytes, StandardCharsets.UTF_8);
          JsonObject responseJson = JsonParser.parseString(jsonStr).getAsJsonObject();

          JsonArray blocksArray = responseJson.getAsJsonArray("blocks");
          List<BlockData> receivedBlocks = new ArrayList<>();

          LOGGER.info("CUBOIDCHECK: requestCuboidData List<BlockData> receivedBlocks = new ArrayList<>();...");
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
    LOGGER.info("CUBOIDCHECK: Pasting batch cuboid containing {} blocks into Server A.", blocks.size());

    ServerLevel level = server.overworld();

    for (BlockData data : blocks) {
      BlockPos pos = new BlockPos(data.x(), data.y(), data.z());

      try {
        // 1. Get the raw serialized state string from JSON
        String stateString = data.blockState().getAsString();

        // 2. Parse it back into a valid BlockState using Server A's registry lookup
        // context
        BlockStateParser.BlockResult parsedResult = BlockStateParser.parseForBlock(
            level.holderLookup(BuiltInRegistries.BLOCK.key()),
            stateString,
            false);
        BlockState targetState = parsedResult.blockState();

        // 3. Set the block state down into Server A's world map layout
        level.setBlock(pos, targetState, 2);

        // 4. Update Block Entities safely (Inventories, signs, chests, containers)
        CompoundTag nbt = data.blockEntityTag();
        if (nbt != null) {
          BlockEntity blockEntity = level.getBlockEntity(pos);
          if (blockEntity != null) {
            // Update target structural vector coordinates inside NBT block mapping
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());

            // For modern engine environments, load standard block data configurations
            blockEntity.loadWithComponents(nbt, level.registryAccess());
            blockEntity.setChanged();
          }
        }

      } catch (Exception e) {
        LOGGER.error("CUBOIDCHECK: Failed to paste block at XYZ: {}, {}, {}", data.x(), data.y(), data.z());
        e.printStackTrace();
      }
    }
  }

  // private static void processReceivedCuboid(MinecraftServer server,
  // List<BlockData> blocks) {
  // LOGGER.info("CUBOIDCHECK: Pasting batch cuboid containing {} blocks into
  // Server A.", blocks.size());
  //
  // ServerLevel level = server.overworld();
  //
  // for (BlockData data : blocks) {
  // BlockPos pos = new BlockPos(data.x(), data.y(), data.z());
  //
  // try {
  // // 1. Convert the JSON string back into a valid Minecraft BlockState
  // String stateString = data.blockState().getAsString();
  //
  // // This parses vanilla state strings like "minecraft:chest[facing=north]"
  // safely
  // BlockStateParser.BlockResult parsedResult = BlockStateParser.parseForBlock(
  // level.holderLookup(BuiltInRegistries.BLOCK.key()),
  // stateString,
  // false);
  // BlockState targetState = parsedResult.blockState();
  //
  // // 2. Set the block state in Server A's world
  // // Flags: 2 = Send to clients, 16 = Prevent neighbor reactions if you want it
  // // exact
  // level.setBlock(pos, targetState, 2);
  //
  // // 3. If the block has NBT data (like a Chest, Furnace, or Sign), paste it in
  // CompoundTag nbt = data.blockEntityTag();
  // if (nbt != null) {
  // BlockEntity blockEntity = level.getBlockEntity(pos);
  // if (blockEntity != null) {
  // // Update the block entity's internal coordinates to match the new position
  // nbt.putInt("x", pos.getX());
  // nbt.putInt("y", pos.getY());
  // nbt.putInt("z", pos.getZ());
  //
  // // Load the metadata payload directly into the tile entity
  // blockEntity.loadWithComponents(nbt, level.registryAccess());
  // blockEntity.setChanged(); // Mark chunk dirty so it saves to disk
  // }
  // }
  //
  // } catch (Exception e) {
  // LOGGER.error("CUBOIDCHECK: Failed to paste block at XYZ: {}, {}, {}",
  // data.x(), data.y(), data.z());
  // e.printStackTrace();
  // }
  // }
  // }

  // private static void processReceivedCuboid(MinecraftServer server,
  // List<BlockData> blocks) {
  // LOGGER.info("CUBOIDCHECK: Processing batch cuboid containing {} blocks.",
  // blocks.size());
  // for (BlockData data : blocks) {
  // LOGGER.info("CUBOIDCHECK: Processing: {}", data.toJson().toString());
  // // Your custom logic per block goes here
  // // e.g., level.setBlock(...) or tracking layout differentials
  // }
  // }

  public static void close() {
    try {
      if (socket != null)
        socket.close();
      networkExecutor.shutdown();
    } catch (Exception ignored) {
    }
  }
}
