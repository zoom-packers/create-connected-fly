package com.hlysine.create_connected.registries;

import net.minecraft.client.Minecraft;

/**
 * Split out of {@link CCPackets} so that class stays loadable on a dedicated server: touching
 * client classes from there would resolve client-only types during verification, even
 * inside a branch that never runs.
 */
public class CCClientPackets {
    /** NeoForge: called from the payload handler CCPackets registers, already on the client thread. */
    public static void playContraptionJukebox(com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacket payload) {
        payload.handle(Minecraft.getInstance().player);
    }
}
