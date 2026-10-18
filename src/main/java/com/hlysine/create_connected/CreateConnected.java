package com.hlysine.create_connected;

import com.hlysine.create_connected.content.goggleslot.GoggleSlot;
import com.hlysine.create_connected.content.redstonelinkwildcard.LinkWildcardNetworkHandler;
import com.hlysine.create_connected.config.CCConfigs;
import com.hlysine.create_connected.datagen.advancements.CCAdvancements;
import com.hlysine.create_connected.datagen.advancements.CCTriggers;
import com.hlysine.create_connected.foundation.registrate.CCRegistrate;
import com.hlysine.create_connected.compat.SchematicRequirementFixes;
import com.hlysine.create_connected.registries.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * NeoForge entrypoint (the Fabric port's {@code ModInitializer}).
 * <p>
 * Registration is split in two, following the Create port this builds on:
 * <ul>
 *   <li>{@link #registerContent()} adds everything that goes into a registry. NeoForge freezes the
 *   vanilla registries at the end of bootstrap, before any mod is constructed, and Create registers its
 *   own content just before that freeze ({@code Create.register()}). {@code mixin.BuiltInRegistriesMixin}
 *   calls this method right after it, inside {@code BuiltInRegistries.freeze()}, so Connected's blocks exist before
 *   Create's freeze-time hooks sort arm interaction point and fan processing types, and before the
 *   client entrypoint binds renderers and models to them.</li>
 *   <li>The constructor wires up what Fabric API did: payloads, NeoForge-registry entries (condition
 *   codecs, attachment types), events and the config.</li>
 * </ul>
 * Order inside {@link #registerContent()} is the Fabric port's: blocks before block entities, because
 * {@code validBlocks} resolves actual Block instances.
 */
@Mod(CreateConnected.MODID)
public class CreateConnected {
    public static final String MODID = "create_connected";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final CCRegistrate REGISTRATE = CCRegistrate.create(MODID);

    private static boolean contentRegistered;

    public CreateConnected(IEventBus modBus, ModContainer container) {
        if (!contentRegistered)
            throw new IllegalStateException("Create: Connected content was not registered during bootstrap; "
                    + "is mixin BuiltInRegistriesMixin applied?");

        // Must come after every registry, as it did upstream. Two configs build their entries from
        // lists the block builders fill as they register: CStress from the defaults that
        // .transform(CStress.setImpact/setCapacity) records, CFeatures from the keys
        // FeatureToggle.register() collects. Create builds its own configs here too (Create#onInitialize).
        CCConfigs.register();

        CCCraftingConditions.register(modBus);
        GoggleSlot.registerEvents(modBus);
        modBus.addListener(CCPackets::register);
        modBus.addListener(CCCreativeTabs::hideDisabled);
        modBus.addListener(CCBlockEntityTypes::addCopycatBlocks);

        // Was an @EventBusSubscriber class upstream.
        LinkWildcardNetworkHandler.register();
    }

    /**
     * Registers every registry entry. Called once, during vanilla bootstrap, from
     * {@code mixin.BuiltInRegistriesMixin}.
     */
    public static void registerContent() {
        if (contentRegistered)
            throw new IllegalStateException("Create: Connected content registered twice");
        contentRegistered = true;

        CCSoundEvents.register();
        CCDataComponents.register();
        CCBlocks.register();
        CCItems.register();
        CCBlockEntityTypes.register();
        CCCreativeTabs.register();
        CCArmInteractionPointTypes.register();
        // Original to the Fabric port: a goggles-only equipment slot. Registers a feature toggle, so it
        // has to run before CCConfigs is built.
        GoggleSlot.register();

        // Previously ran from FMLCommonSetupEvent upstream.
        CCInteractionBehaviours.register();
        CCMovementBehaviours.register();
        CCMountedStorageTypes.register();
        // Mounted storage is the contraption side; CCTransfer is the world side, and publishes the
        // same inventories as NeoForge capabilities so that Jade and other mods can read them.
        CCTransfer.register();
        CCDisplaySources.register();
        CCDisplayTargets.register();
        CCUnpackingHandlers.register();
        CCInventoryIdentifiers.register();
        SchematicRequirementFixes.register();

        // Were keyed off RegisterEvent for a specific registry upstream; now called directly.
        CCItemAttributes.register();
        CCAdvancements.register();
        CCTriggers.register();

        // Copycats+ and Additional Placements integrations are excluded from the build until
        // those mods exist on 26.3 -- see the sourceSets excludes in build.gradle.

        initBlockStateCaches();
    }

    /**
     * Builds the block-state caches (occlusion shapes, fluid state, solid render, ...) for Connected's blocks. Vanilla
     * does this for every block at the end of {@code Blocks.<clinit>}, where Create also registers its own, and NeoForge
     * does it at registry bake for blocks its callback saw added. Connected registers later in bootstrap than the first
     * and before NeoForge attaches that callback, so neither covers it: every state kept null caches (MoreCulling's
     * reload hit a null occlusion shape on the copycat fences). Registry order puts the wrapped_* blocks that copycats
     * delegate to before the copycats.
     */
    private static void initBlockStateCaches() {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(MODID))
                continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                state.initCache();
            }
        }
    }

    public static CCRegistrate getRegistrate() {
        return REGISTRATE;
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
