package com.hlysine.create_connected.client;

import java.util.Optional;

import com.hlysine.create_connected.ConnectedLang;
import com.hlysine.create_connected.content.contraption.jukebox.ContraptionMusicManager;
import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacket;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlock;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockEntity;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorScreen;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.catnip.gui.ScreenOpener;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;

/**
 * ZP5 port (NeoForge 26.3): the client-only halves of common classes. Common code calls these only behind a physical
 * client check, so a dedicated server never loads this class or the client types it uses (LocalPlayer, ClientLevel,
 * Screen). Before, those lived in KineticBridgeBlockItem, SequencedPulseGeneratorBlock and
 * PlayContraptionJukeboxPacket, and the server failed verifying them ("LocalPlayer ... not present on the dedicated
 * server").
 */
public final class CCClientHooks {
    private CCClientHooks() {}

    /** The kinetic bridge didn't fit: outline where it would go and say why. */
    public static void showKineticBridgeBounds(KineticBridgeBlock block, BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction facing = block.getDirectionForPlacement(context);
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer))
            return;
        Outliner.getInstance().showAABB(Pair.of("kinetic_bridge", pos), new AABB(pos).expandTowards(facing.getUnitVec3i().getX(),
                        facing.getUnitVec3i().getY(), facing.getUnitVec3i().getZ()))
                .colored(0xFF_ff5d6c);
        ConnectedLang.translate("kinetic_bridge.not_enough_space")
                .color(0xFF_ff5d6c)
                .sendStatus(localPlayer);
    }

    public static void openSequencedPulseGeneratorScreen(SequencedPulseGeneratorBlockEntity be, Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new SequencedPulseGeneratorScreen(be));
    }

    /** Client handling of {@link PlayContraptionJukeboxPacket}, on the client thread. */
    public static void playContraptionJukebox(PlayContraptionJukeboxPacket packet) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || !world.dimension().identifier().equals(packet.level()))
            return;
        if (!world.isLoaded(packet.worldPos()))
            return;
        Entity entity = world.getEntity(packet.contraptionId());
        if (!(entity instanceof AbstractContraptionEntity contraptionEntity))
            return;
        JukeboxSong song = null;
        if (packet.play()) {
            Optional<JukeboxSong> found = world.registryAccess()
                    .lookupOrThrow(Registries.JUKEBOX_SONG)
                    .get(packet.recordId())
                    .map(Holder.Reference::value);
            if (found.isEmpty())
                return;
            song = found.get();
        }
        ContraptionMusicManager.playContraptionMusic(song, contraptionEntity, packet.contraptionPos(), packet.worldPos(), packet.silent());
    }
}
