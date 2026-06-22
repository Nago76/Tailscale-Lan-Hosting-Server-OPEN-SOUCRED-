# 🌐 Tailscale LAN (TSLAN)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net)
[![Forge Version](https://img.shields.io/badge/Forge-47.4.10-orange.svg)](https://files.minecraftforge.net/)
[![Platform](https://img.shields.io/badge/Network-Tailscale%20%7C%20WireGuard-blue.svg)](https://tailscale.com)
[![Djscord](https://img.shields.io/badge/Discord_Server-purple.svg)](https://discord.gg/xBcyYfbXdz)

## Requirement 
- You need Minecraft 1.20.1 installed
- You and your friend need to have TailScale Client Installed
- Forge version is lock to 47.4.10 until further notice
> [!CAUTION]
> This mods still on bleeding edge stage, use it at your own risk!

## What is TSLAN ?

**TSLAN** (Tailscale LAN Server) is a lightweight Minecraft mods designed let you create or host minecraft server on your machines and play it with your friend.

**Tailscale Lan Hosting** is a Ultra-fast lan hosting, it create a direct, point-to-point connection between your device and your friend device `using WireGuard`. Data travel straight to its destination to minize latency without any lag
> [!IMPORTANT]
> Your connection speed is only limited by your own ISP's upload speeds and the physical distance between you and your friend. Ensure you have better upload speed

> [!WARNING]
> If your friend live at big distance that almost 15000km, your friend might be experience high pings, ghost block, interaction delay and sometime connection reset.

Normally, if you want to play a local world with friends who aren't on your home Wi-Fi, you either have to mess around with confusing router settings (Port Forwarding) or use public tunnel mods like `e4mc` or `Essential`. While those mods are great, they route all of your game data through public, crowded third-party proxy servers, which often causes frustrating lag spikes, high ping, and rubber-banding.

**TSLAN fixed this by utilizing Tailscale.**

Instead of routing your game traffic through a random server in the middle of nowhere, TSLAN securely detects your local **Tailscale WireGuard Mesh Network**. The moment you click "Open to LAN", the mod instantly bridges your single-player world[cite: 2] directly to your friends **peer-to-peer (P2P)**.

### Why it's better:
* **True LAN Speed:** Because the connection is direct between you and your friend, your ping is as low as your actual internet connection allows—no middleman slows you down.
* **Completely Private:** Your game is never exposed to the public internet; only devices you explicitly invite and authorize on your private Tailscale network can see or join your world.
* **Zero Router Configuration:** You don't need to touch your router, change firewall settings, or know what port-forwarding is. It just works.

## Key Features

* **Direct Peer-to-Peer Latency:** Connects you and your friends directly over your private Tailscale network, offering near-zero latency compared to standard middleman relays.

* **Offline / Cracked Account Support:** Includes a toggleable mode via commands or configuration files allowing offline/unauthenticated clients to join your network dynamically.

* **Simple Voice Chat Integrated:** Automatically detects, reads, and formats proximity voice chat data (`config/voicechat/voicechat-server.properties`), ensuring your UDP audio bridge works flawlessly out of the box.

* **Automated Port & IP Fetching:** Runs on a safe, non-blocking background daemon thread that automatically grabs your Tailscale IP and randomized LAN ports upon world hosting.

* **Click-to-Copy Address Link:** Prints a beautiful, styled, interactive chat component directly to the host when a LAN server goes live. Just click the link to copy the server string!

* **MTU Overload Protections:** Automatically optimizes outbound Netty packet chunk distribution sizes to prevent packet fragmentation over WireGuard's compact 1280-byte MTU limits.


