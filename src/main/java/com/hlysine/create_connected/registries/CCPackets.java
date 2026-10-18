package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacket;
import com.hlysine.create_connected.content.sequencedpulsegenerator.ConfigureSequencedPulseGeneratorPacket;
import com.hlysine.create_connected.content.goggleslot.GoggleSlotClickPacket;
import com.hlysine.create_connected.content.goggleslot.GoggleSlotQuickMovePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Catnip's {@code CatnipPacketRegistry} and its {@code BasePacketPayload} hierarchy went with the
 * platform layer. Create Fly moved to vanilla {@code Packet}s registered into the protocol by
 * mixin; Connected does not need that reach, so this uses the loader's payload API (NeoForge:
 * {@link RegisterPayloadHandlersEvent}), which takes the {@link CustomPacketPayload} and
 * {@code StreamCodec} shapes the packets already had. Handlers run on the main thread.
 * <p>
 * The direction a payload travels is declared at registration now, rather than implied by which
 * Catnip interface it implemented.
 */
public class CCPackets {
    public static final CustomPacketPayload.Type<ConfigureSequencedPulseGeneratorPacket> CONFIGURE_SEQUENCER =
            new CustomPacketPayload.Type<>(CreateConnected.asResource("configure_sequencer"));

    public static final CustomPacketPayload.Type<PlayContraptionJukeboxPacket> PLAY_CONTRAPTION_JUKEBOX =
            new CustomPacketPayload.Type<>(CreateConnected.asResource("play_contraption_jukebox"));

    public static final CustomPacketPayload.Type<GoggleSlotClickPacket> GOGGLE_SLOT_CLICK =
            new CustomPacketPayload.Type<>(CreateConnected.asResource("goggle_slot_click"));

    public static final CustomPacketPayload.Type<GoggleSlotQuickMovePacket> GOGGLE_SLOT_QUICK_MOVE =
            new CustomPacketPayload.Type<>(CreateConnected.asResource("goggle_slot_quick_move"));

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CONFIGURE_SEQUENCER, ConfigureSequencedPulseGeneratorPacket.STREAM_CODEC,
                (payload, context) -> payload.apply((ServerPlayer) context.player()));
        registrar.playToServer(GOGGLE_SLOT_CLICK, GoggleSlotClickPacket.STREAM_CODEC,
                (payload, context) -> payload.apply((ServerPlayer) context.player()));
        registrar.playToServer(GOGGLE_SLOT_QUICK_MOVE, GoggleSlotQuickMovePacket.STREAM_CODEC,
                (payload, context) -> payload.apply((ServerPlayer) context.player()));
        // The handler only touches CCClientPackets when a packet arrives, so a dedicated server never loads it.
        registrar.playToClient(PLAY_CONTRAPTION_JUKEBOX, PlayContraptionJukeboxPacket.STREAM_CODEC,
                (payload, context) -> CCClientPackets.playContraptionJukebox(payload));
    }
}
