
package com.example.cuboidcheck.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CuboidCheckRestoreCommand {

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

    dispatcher.register(
        Commands.literal("cuboidrestore")
            .requires(source -> source.hasPermission(2)) // OP level 2
            .executes(context -> {

              context.getSource().sendSuccess(
                  () -> Component.literal("RESTORING"),
                  false);

              return 1;
            }));
  }
}
