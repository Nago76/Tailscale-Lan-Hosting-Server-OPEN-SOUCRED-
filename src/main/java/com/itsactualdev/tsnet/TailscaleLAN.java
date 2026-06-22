package com.itsactualdev.tsnet;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.Connection;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Mod(TailscaleLAN.MODID)
public class TailscaleLAN {
    public static final String MODID = "tslan";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static String tailscaleIp = null;


    public TailscaleLAN() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("[Tailscale LAN] Mod initialized.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            tailscaleIp = null; // Reset on startup
            boolean cracked = Config.CRACKED.get();
            LOGGER.info("[Tailscale LAN] Integrated server starting. Applying cracked account policy (cracked = {}).", cracked);
            server.setUsesAuthentication(!cracked);
            
            // Start separate thread for Tailscale detection and reporting
            startTailscaleLanThread(server);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (TailscaleLAN.tailscaleIp != null && Config.MTU_OPTIMIZATION.get()) {
            try {
                if (player.connection != null && player.connection.connection != null) {
                    Connection connection = player.connection.connection;
                    if (!connection.isMemoryConnection()) {
                        LOGGER.info("[Tailscale LAN] Optimizing Netty outbound channels for remote player: {}", player.getScoreboardName());
                        io.netty.channel.Channel channel = getChannel(connection);
                        if (channel != null) {
                            // Tweak Netty outbound/socket settings for MTU alignment
                            channel.config().setOption(io.netty.channel.ChannelOption.SO_SNDBUF, 1280);
                            // Set write buffer watermarks to flush smaller segments and prevent sudden bursts
                            channel.config().setWriteBufferWaterMark(new io.netty.channel.WriteBufferWaterMark(1024, 2048));
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("[Tailscale LAN] Failed to optimize player outbound connection settings", t);
            }
        }
    }


    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[Tailscale LAN] Registering commands.");
        TailscaleCommand.register(event.getDispatcher());
    }

    private void startTailscaleLanThread(MinecraftServer server) {
        Thread thread = new Thread(() -> {
            LOGGER.info("[Tailscale LAN] Background thread started. Waiting for server to be published to LAN...");
            try {
                int attempts = 0;
                // Poll every 500ms, up to 10 minutes (1200 attempts)
                while (server.isRunning() && !server.isPublished() && attempts < 1200) {
                    Thread.sleep(500);
                    attempts++;
                }

                if (!server.isRunning()) {
                    LOGGER.info("[Tailscale LAN] Server stopped before being published. Exiting thread.");
                    return;
                }

                if (!server.isPublished()) {
                    LOGGER.info("[Tailscale LAN] Timeout waiting for server to be published to LAN. Exiting thread.");
                    return;
                }

                int port = server.getPort();
                if (port <= 0) {
                    LOGGER.warn("[Tailscale LAN] Server published but port is invalid ({}). Exiting thread.", port);
                    return;
                }

                LOGGER.info("[Tailscale LAN] Integrated server published on LAN port: {}. Fetching Tailscale IP...", port);
                String tailscaleIp = getTailscaleIp();

                // Wait 1.5 seconds to ensure player join handling and chat listeners are ready
                Thread.sleep(1500);

                if (tailscaleIp != null && !tailscaleIp.isEmpty()) {
                    TailscaleLAN.tailscaleIp = tailscaleIp;
                    int voicePort = getSimpleVoiceChatPort();
                    String fullAddress = tailscaleIp + ":" + port;
                    LOGGER.info("[Tailscale LAN] Tailscale IP detected: {}. Shareable address: {}", tailscaleIp, fullAddress);
                    
                    if (Config.MTU_OPTIMIZATION.get()) {
                        setServerCompressionThreshold(server, 64);
                    }
                    
                    sendAddressToHost(server, fullAddress, tailscaleIp, voicePort);

                } else {
                    LOGGER.warn("[Tailscale LAN] Tailscale status query failed or returned no IP. Sending offline alert.");
                    sendTailscaleOfflineAlert(server);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("[Tailscale LAN] Background thread interrupted.");
            } catch (Exception e) {
                LOGGER.error("[Tailscale LAN] Error in background thread", e);
            }
        }, "TailscaleLAN-PortScanner");
        
        thread.setDaemon(true);
        thread.start();
    }

    private String getTailscaleIp() {
        List<String> candidates = new java.util.ArrayList<>();
        candidates.add("tailscale"); // Try default command first

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            candidates.add("C:\\Program Files\\Tailscale\\tailscale.exe");
            candidates.add("C:\\Value\\Tailscale\\tailscale.exe");
        } else if (os.contains("mac") || os.contains("darwin")) {
            candidates.add("/Applications/Tailscale.app/Contents/MacOS/Tailscale");
            candidates.add("/Applications/Tailscale.app/Contents/PlugIns/IPNExtension.appex/Contents/MacOS/Tailscale");
        }

        // Try CLI candidates
        for (String command : candidates) {
            // If it's an absolute path, only run if the file exists
            if (!command.equals("tailscale")) {
                java.io.File file = new java.io.File(command);
                if (!file.exists()) {
                    continue;
                }
            }

            try {
                LOGGER.info("[Tailscale LAN] Attempting to retrieve IP using tailscale command: {}", command);
                ProcessBuilder pb = new ProcessBuilder(command, "status", "--json");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder jsonBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    String ip = extractIpFromJson(jsonBuilder.toString());
                    if (ip != null) {
                        return ip;
                    }
                } else {
                    LOGGER.warn("[Tailscale LAN] Command '{} status --json' exited with code {} or timed out.", command, finished ? process.exitValue() : "timeout");
                }
            } catch (java.io.IOException e) {
                LOGGER.debug("[Tailscale LAN] Failed to run tailscale command due to IOException: " + command, e);
            } catch (Exception e) {
                LOGGER.error("[Tailscale LAN] Unexpected error running tailscale command: " + command, e);
            }
        }

        // Fallback to network interfaces if CLI failed
        LOGGER.info("[Tailscale LAN] All tailscale CLI attempts failed. Falling back to query network interfaces...");
        String ip = getTailscaleIpFromNetworkInterfaces();
        if (ip != null) {
            return ip;
        }

        return null;
    }

    private String extractIpFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        // Check if BackendState is Running
        java.util.regex.Pattern backendStatePattern = java.util.regex.Pattern.compile("\"BackendState\"\\s*:\\s*\"Running\"");
        if (!backendStatePattern.matcher(json).find()) {
            LOGGER.warn("[Tailscale LAN] Tailscale backend state is not 'Running' in JSON status.");
            return null;
        }

        int selfStart = json.indexOf("\"Self\"");
        if (selfStart == -1) {
            return null;
        }

        // Extract Self block. We take from "Self" to the end of the JSON or up to the next top-level key "Peer" or "User"
        String selfPart = json.substring(selfStart);
        int peerIdx = selfPart.indexOf("\"Peer\"");
        if (peerIdx != -1) {
            selfPart = selfPart.substring(0, peerIdx);
        }

        // Approach 1: Parse TailscaleIPs array inside Self block
        int ipsStart = selfPart.indexOf("\"TailscaleIPs\"");
        if (ipsStart != -1) {
            int bracketStart = selfPart.indexOf("[", ipsStart);
            if (bracketStart != -1) {
                int bracketEnd = selfPart.indexOf("]", bracketStart);
                if (bracketEnd != -1) {
                    String ipsArrayStr = selfPart.substring(bracketStart + 1, bracketEnd);
                    String[] tokens = ipsArrayStr.split(",");
                    for (String token : tokens) {
                        String ip = token.replace("\"", "").trim();
                        if (isValidIPv4(ip)) {
                            return ip;
                        }
                    }
                }
            }
        }

        // Approach 2: Regex search for any 100.x.y.z IP in the Self block
        java.util.regex.Pattern tailscaleIpPattern = java.util.regex.Pattern.compile("\\b(100\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
        java.util.regex.Matcher m = tailscaleIpPattern.matcher(selfPart);
        if (m.find()) {
            String ip = m.group(1);
            if (isValidIPv4(ip)) {
                return ip;
            }
        }

        // Approach 3: Regex search for any valid IPv4 in the Self block
        java.util.regex.Pattern generalIpPattern = java.util.regex.Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
        java.util.regex.Matcher m2 = generalIpPattern.matcher(selfPart);
        while (m2.find()) {
            String ip = m2.group(1);
            if (isValidIPv4(ip)) {
                return ip;
            }
        }

        return null;
    }

    private String getTailscaleIpFromNetworkInterfaces() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface netInterface = interfaces.nextElement();
                    String name = netInterface.getName().toLowerCase();
                    String displayName = netInterface.getDisplayName().toLowerCase();
                    boolean isTailscaleInterface = name.contains("tailscale") || displayName.contains("tailscale");

                    java.util.Enumeration<java.net.InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress()) {
                            String hostAddress = addr.getHostAddress();
                            if (isValidIPv4(hostAddress)) {
                                if (isTailscaleInterface || hostAddress.startsWith("100.")) {
                                    LOGGER.info("[Tailscale LAN] Found Tailscale IP {} on interface: {} ({})", hostAddress, netInterface.getName(), netInterface.getDisplayName());
                                    return hostAddress;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Tailscale LAN] Failed to query network interfaces", e);
        }
        return null;
    }

    private boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private void sendAddressToHost(MinecraftServer server, String address, String tailscaleIp, int voicePort) {
        MutableComponent prefix = Component.literal("[Tailscale LAN] ")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true));
        
        MutableComponent body;
        if (voicePort > 0) {
            body = Component.literal("World shared over Tailscale! Game is live with Proximity Voice Support.\n")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        } else {
            body = Component.literal("World shared over Tailscale!\n")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        }

        MutableComponent copyPrompt = Component.literal("👉 Click here to copy shareable address: ")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));

        MutableComponent addressComp = Component.literal(address)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, address))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy address to clipboard"))));

        MutableComponent fullMessage = Component.literal("")
                .append(prefix)
                .append(body)
                .append(copyPrompt)
                .append(addressComp);

        if (voicePort > 0) {
            MutableComponent voiceMsg = Component.literal("\n🎙️ Simple Voice Chat Data: " + tailscaleIp + " (Voice UDP Port: " + voicePort + ")")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE));
            fullMessage.append(voiceMsg);
        }

        sendMessageToHost(server, fullMessage);
    }

    private int getSimpleVoiceChatPort() {
        java.io.File file = new java.io.File("config/voicechat/voicechat-server.properties");
        if (!file.exists()) {
            return -1;
        }
        java.util.Properties properties = new java.util.Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            properties.load(in);
            String portStr = properties.getProperty("port");
            if (portStr != null) {
                try {
                    return Integer.parseInt(portStr.trim());
                } catch (NumberFormatException e) {
                    LOGGER.warn("[Tailscale LAN] Failed to parse Simple Voice Chat port: {}", portStr, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Tailscale LAN] Failed to read Simple Voice Chat configuration file.", e);
        }
        return -1;
    }

    private void sendTailscaleOfflineAlert(MinecraftServer server) {
        MutableComponent prefix = Component.literal("[Tailscale LAN] ")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));

        MutableComponent body = Component.literal("Could not retrieve Tailscale IP. Please make sure the Tailscale desktop client is running and online.")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));

        MutableComponent fullMessage = Component.literal("")
                .append(prefix)
                .append(body);

        sendMessageToHost(server, fullMessage);
    }

    private void sendMessageToHost(MinecraftServer server, Component message) {
        boolean sent = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                player.sendSystemMessage(message);
                sent = true;
                break;
            }
        }
        if (!sent) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void setServerCompressionThreshold(MinecraftServer server, int threshold) {
        try {
            java.lang.reflect.Field field = null;
            try {
                field = MinecraftServer.class.getDeclaredField("compressionThreshold");
            } catch (NoSuchFieldException e) {
                // Try SRG fallback: f_129753_
                field = MinecraftServer.class.getDeclaredField("f_129753_");
            }
            if (field != null) {
                field.setAccessible(true);
                field.setInt(server, threshold);
                LOGGER.info("[Tailscale LAN] Optimizing server network compression threshold to: {}", threshold);
            } else {
                LOGGER.warn("[Tailscale LAN] Could not find compressionThreshold field on MinecraftServer.");
            }
        } catch (Throwable t) {
            LOGGER.error("[Tailscale LAN] Failed to set server compression threshold via reflection", t);
        }
    }

    private io.netty.channel.Channel getChannel(Connection connection) {
        try {
            java.lang.reflect.Field field = null;
            try {
                field = Connection.class.getDeclaredField("channel");
            } catch (NoSuchFieldException e) {
                for (java.lang.reflect.Field f : Connection.class.getDeclaredFields()) {
                    if (f.getType() == io.netty.channel.Channel.class) {
                        field = f;
                        break;
                    }
                }
            }
            if (field != null) {
                field.setAccessible(true);
                return (io.netty.channel.Channel) field.get(connection);
            }
        } catch (Throwable t) {
            LOGGER.error("[Tailscale LAN] Failed to get Channel from Connection via reflection", t);
        }
        return null;
    }
}


