
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
import com.example.cuboidcheck.utl.CuboidVisualizer;
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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.cuboidcheck.network.ClientboundSyncSelectionPayload;

public class CuboidCheckPreviewCommand {
  // private CuboidVisualizer cuboidVisualizer;

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

    dispatcher.register(
        Commands.literal("cuboidpreview")
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
                          () -> Component.literal("Previewing Area..."),
                          false);

                      // com.example.cuboidcheck.utl.CuboidVisualizer.setSelection(posStart.getX(),
                      // posStart.getY(),
                      // posStart.getZ(), posEnd.getX(), posEnd.getY(), posEnd.getZ());
                      // Sets a temporary 3x3x3 box preview around 0, 90, 0

                      // CuboidVisualizer.setSelection(posStart.getX(), posStart.getY(),
                      // posStart.getZ(), posEnd.getX(), posEnd.getY(), posEnd.getZ());

                      // Inside your command execution logic:
                      ServerPlayer player = context.getSource().getPlayerOrException();

                      // Send packet directly to this player's client rendering engine instance
                      PacketDistributor.sendToPlayer(player,
                          new ClientboundSyncSelectionPayload(
                              posStart.getX(), posStart.getY(),
                              posStart.getZ(), posEnd.getX(), posEnd.getY(), posEnd.getZ()));

                      context.getSource().sendSuccess(
                          () -> Component.literal("Setting Area..."),
                          false);

                      return 1;
                    }))));
  }

}
