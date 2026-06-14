
package com.example.cuboidcheck.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
            .requires(source -> source.hasPermission(2)) // OP level 2
            .executes(context -> {
              MinecraftServer server = context.getSource().getServer();
              ServerLevel level = server.getLevel(Level.OVERWORLD);
              // BlockPos pos1 = new BlockPos(-50, 0, -50);
              // BlockPos pos2 = new BlockPos(50, 200, 50);
              BlockPos pos1 = new BlockPos(-5, 0, -5);
              BlockPos pos2 = new BlockPos(5, 5, 5);

              Map<BlockPos, BlockState> blockMap = new HashMap<>();
              for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2)) {
                blockMap.put(pos.immutable(), level.getBlockState(pos));

              }
              // String msg = "#####DATA######\n";
              // for (BlockPos bp : blockMap.keySet()) {
              // msg += bp.toString() + "\n";
              // }
              String msg = "#####DATA######\n"
                  + blockMap.keySet().stream().map(k -> k.toString()).collect(Collectors.joining())
                  + "\n#####DATA#####";

              final String finalmsg = msg;
              context.getSource().sendSuccess(
                  () -> Component.literal(finalmsg),
                  // () -> Component.literal("RESTORING"),
                  false);

              return 1;
            }));
  }
}
