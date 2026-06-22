# 🛠️ Installation & Usage Guide

Follow these steps to set up and use **Tailscale LAN (TSLAN)** to host low-latency P2P Minecraft sessions.

---

## 📋 Requirements
Both the **Host** (the person opening the world) and the **Clients** (the friends joining) must complete the Tailscale setup.

* Minecraft 1.20.1 running **Forge**.
* The **Tailscale** desktop application installed on your PC.

---

## 🔌 Step 1: Set Up Tailscale (Do this once)

### 1. Download and Sign Up
1. Go to [Tailscale's Official Website](https://tailscale.com/) and download the installer for your Operating System (Windows, macOS, or Linux).
2. Install the application and sign in using a Google, GitHub, or Microsoft account.

### 2. Connect Together (The Tailnet)
To play together, your computers need to be in the same private network mesh (called a Tailnet). 
* **If you are friends:** The host needs to invite the clients to their Tailnet using the **Tailscale Share** feature in the web dashboard, or everyone can join a single shared Tailscale network.
* Ensure your Tailscale desktop client toggle switch is turned **ON** (Connected).

---

## 📦 Step 2: Mod Installation

1. Make sure you have **Forge 1.20.1** installed on your Minecraft launcher.
2. Download the `tslan-beta.jar` file from our repository's releases page.
3. Drop the `.jar` file directly into your Minecraft instance's `mods` folder:
   * **Windows:** `%appdata%\.minecraft\mods\`
   * **macOS:** `~/Library/Application Support/minecraft/mods/`
4. *(Optional)* If you want proximity voice chat, ensure the **Simple Voice Chat** mod is also placed in your `mods` folder.

---

## 🎮 Step 3: How to Host (For the Host)

1. Launch Minecraft 1.20.1 with the mod installed.
2. Open your single-player world.
3. Press `Esc` and click **Open to LAN**. Configure your game settings, then hit **Start LAN World**.
4. Look at your in-game chat window. TSLAN will automatically print out a layout like this:
   > **[Tailscale LAN] World shared over Tailscale!** > 👉 **Game Address (Click to Copy):** `100.x.y.z:54321`  
   > 🎙️ **Simple Voice Chat Data:** `100.x.y.z` (Voice UDP Port: `24454`)
5. Click on the green address string in chat to automatically copy it to your clipboard, and send it to your friends!

### Toggling Cracked Accounts
If your friends are using offline/cracked clients, type this command in your singleplayer chat *before* opening the world to LAN:
```text
/tslan cracked true
