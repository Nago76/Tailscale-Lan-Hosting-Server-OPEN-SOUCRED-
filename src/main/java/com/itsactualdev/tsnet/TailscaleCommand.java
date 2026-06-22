package com.itsactualdev.tsnet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.ChatFormatting;

public class TailscaleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tslan")
                .requires(source -> source.hasPermission(2)) // OP level 2
                .then(Commands.literal("cracked")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "value");
                            
                            // Save to configuration spec
                            Config.CRACKED.set(value);
                            
                            // Dynamically apply if server is running
                            MinecraftServer server = context.getSource().getServer();
                            if (server != null) {
                                server.setUsesAuthentication(!value);
                            }
                            
                            String status = value ? "ENABLED (offline/cracked mode)" : "DISABLED (online/mojang authentication mode)";
                            context.getSource().sendSuccess(() -> Component.literal("[Tailscale LAN] ")
                                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                                    .append(Component.literal("Cracked/offline-mode support is now ")
                                            .withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal(status)
                                            .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD)), true);
                            
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("mtu")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "value");
                            
                            // Save to configuration spec
                            Config.MTU_OPTIMIZATION.set(value);
                            
                            String status = value ? "ON" : "OFF";
                            context.getSource().sendSuccess(() -> Component.literal("§a[Tailscale LAN] MTU Optimization has been turned " + status + ". (Recommended for WireGuard connections)."), true);
                            
                            return 1;
                        })
                    )
                )
        );
    }
}
