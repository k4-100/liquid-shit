package com.example.cuboidcheck.commands;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.example.cuboidcheck.network.BlockDataTcpClient;
import com.example.cuboidcheck.utl.BlockData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CuboidCheckRestoreCommand {

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

    dispatcher.register(
        Commands.literal("cuboidrestore")
            .then(Commands.argument("start", BlockPosArgument.blockPos())
                .then(Commands.argument("end", BlockPosArgument.blockPos())
                    .requires(source -> source.hasPermission(2)) // OP level 2
                    .executes(context -> {
                      MinecraftServer server = context.getSource().getServer();
                      ServerLevel level = server.getLevel(Level.OVERWORLD);
                      BlockPos posStart = BlockPosArgument.getBlockPos(context, "start");
                      BlockPos posEnd = BlockPosArgument.getBlockPos(context, "end");

                      Map<BlockPos, BlockState> blockMap = new HashMap<>();
                      for (BlockPos pos : BlockPos.betweenClosed(posStart, posEnd))
                        blockMap.put(pos.immutable(), level.getBlockState(pos));

                      final int blockCount = blockMap.size();
                      // NOTE: check if it is withing boundaries
                      // so you cannot do it for entire 10kx10k map it would crash
                      // basically 0 0 0 x 50 50 50
                      if (blockCount > 132651) {
                        context.getSource().sendSuccess(
                            () -> Component.literal("WARNING: TOO MANY BLOCKS: " + blockCount),
                            false);

                        return 1;
                      }
                      // List<BlockData> blocks = new ArrayList<>();

                      context.getSource().sendSuccess(
                          () -> Component.literal(
                              "DATA: " + blockCount),
                          false);

                      context.getSource().sendSuccess(
                          () -> Component.literal("Sending coordinates via TCP..."),
                          false);

                      // CompletableFuture.runAsync(() -> {
                      //
                      // });

                      // --- TCP SOCKET SENDING LOGIC ---
                      // We use CompletableFuture to run this on a separate thread so the server
                      // doesn't lag/freeze
                      CompletableFuture.runAsync(() -> {
                        try (Socket socket = new Socket("127.0.0.1", 8082);
                        // PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                        ) {

                          BlockDataTcpClient.requestCuboidData(server,
                              posStart.getX(), posStart.getY(), posStart.getZ(),
                              posEnd.getX(), posEnd.getY(), posEnd.getZ());

                          // Format: "startX,startY,startZ;endX,endY,endZ"
                          // String data = String.format("%d,%d,%d;%d,%d,%d",
                          // posStart.getX(), posStart.getY(), posStart.getZ(),
                          // posEnd.getX(), posEnd.getY(), posEnd.getZ());
                          //
                          // out.println(data);
                          // context.getSource().sendSuccess(
                          // () -> Component.literal(
                          // "MANAGED TO SEND"),
                          // false);

                        } catch (Exception e) {
                          e.printStackTrace();
                          // Log the error if the socket connection fails
                          server.execute(() -> context.getSource().sendFailure(
                              Component.literal("Failed to send coords... data: " + e.getMessage())));
                        }
                      });
                      // ---------------------------------

                      return 1;
                    }))));
  }
}
