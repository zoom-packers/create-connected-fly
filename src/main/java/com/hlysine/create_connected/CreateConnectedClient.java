package com.hlysine.create_connected;

import com.hlysine.create_connected.content.goggleslot.GoggleSlotLayer;
import com.hlysine.create_connected.content.goggleslot.GoggleSlotScreen;
import com.hlysine.create_connected.client.FlywheelShutdownFix;
import com.hlysine.create_connected.client.CCConnectedTextures;
import com.hlysine.create_connected.client.CCBlockEntityRenders;
import com.hlysine.create_connected.client.CCModels;
import com.hlysine.create_connected.client.display.CCDisplaySourceRenders;
import com.hlysine.create_connected.registries.CCBlockEntityBehaviours;
import com.hlysine.create_connected.registries.CCPartialModels;
import com.hlysine.create_connected.registries.CCPonderPlugin;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge client entrypoint (the Fabric port's {@code ClientModInitializer}).
 * <p>
 * It runs after Create's client entrypoint (the mod declares an AFTER ordering on {@code create}) and after
 * bootstrap, so the registry objects these calls bind renderers and models to already exist.
 * <p>
 * Order matters in one place: {@code CCPartialModels.register()} has to run before
 * {@code CCBlockEntityRenders.register()}, because the Flywheel visuals resolve partial models at
 * construction.
 */
@Mod(value = CreateConnected.MODID, dist = Dist.CLIENT)
public class CreateConnectedClient {
    public CreateConnectedClient(IEventBus modBus, ModContainer container) {
        CCPartialModels.register();
        CCModels.register();
        CCBlockEntityRenders.register();
        CCBlockEntityBehaviours.register();
        CCDisplaySourceRenders.register();
        CCConnectedTextures.register();
        GoggleSlotScreen.register();
        modBus.addListener(GoggleSlotLayer::register);
        FlywheelShutdownFix.register();
        PonderIndex.addPlugin(new CCPonderPlugin());
    }
}
