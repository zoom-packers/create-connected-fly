package com.hlysine.create_connected.registries;


/**
 * Split out of {@link CCPackets} so that class stays loadable on a dedicated server: touching
 * client classes from there would resolve client-only types during verification, even
 * inside a branch that never runs.
 */
public class CCClientPackets {
    /** NeoForge: called from the payload handler CCPackets registers, already on the client thread. */
    public static void playContraptionJukebox(com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacket payload) {
        com.hlysine.create_connected.client.CCClientHooks.playContraptionJukebox(payload);
    }
}
