package com.example.cuboidcheck.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CuboidCheckConfig {
  private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

  public static final ModConfigSpec.EnumValue<ServerMode> SERVER_MODE;
  public static final ModConfigSpec.IntValue SERVER_B_PORT;
  public static final ModConfigSpec.ConfigValue<String> SERVER_B_IP;

  static {
    BUILDER.comment("CuboidCheck Server-to-Server Configuration").push("general");

    SERVER_MODE = BUILDER
        .comment("What role does this server play?\n" +
            "SERVER_A: Requests block data from Server B\n" +
            "SERVER_B: Listens and sends block data to Server A\n" +
            "STANDALONE: Does nothing (normal server)")
        .defineEnum("serverMode", ServerMode.STANDALONE);

    SERVER_B_PORT = BUILDER
        .comment("The TCP port used for communication.")
        // .defineInRange("serverBPort", 8082, 1024, 65535);
        // .defineInRange("serverBPort", 8083, 1024, 65535);
        .defineInRange("serverBPort", 8083, 1024, 65536);

    SERVER_B_IP = BUILDER
        .comment("The IP address of Server B (Only used if this is Server A).")
        .define("serverBIp", "127.0.0.1");

    BUILDER.pop();
  }

  public static final ModConfigSpec SPEC = BUILDER.build();

  public enum ServerMode {
    SERVER_A,
    SERVER_B,
    STANDALONE
  }
}
