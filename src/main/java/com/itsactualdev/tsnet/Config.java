package com.itsactualdev.tsnet;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue CRACKED = BUILDER
            .comment("Whether cracked/offline-mode account support is enabled. When true, online-mode/authentication is disabled.")
            .define("cracked", false);

    public static final ForgeConfigSpec.BooleanValue MTU_OPTIMIZATION = BUILDER
            .comment("Whether MTU optimization for Tailscale connections is enabled (recommended for WireGuard connections).")
            .define("mtuOptimization", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}

