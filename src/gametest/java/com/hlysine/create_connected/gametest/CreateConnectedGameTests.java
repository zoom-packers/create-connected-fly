package com.hlysine.create_connected.gametest;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCCreativeTabs;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Dev-only GameTests for the NeoForge 26.3 port (mod {@code create_connected_gametest}, never in the jar). They run on
 * {@code ./gradlew runGameTestServer}, each in an empty 9x9x9 structure, building their own setup.
 * <p>
 * The first four are new with the NeoForge port and check what the port itself rewired: bootstrap registration,
 * NeoForge resource conditions, capabilities and Create's block-tag driven fan catalysts. The rest are the Fabric
 * port's tests ({@link ArmBasinTests}, {@link BatteryPlacementOrderTests}), unchanged apart from registration.
 * {@link KineticSourceTests} is a long survey rather than a check, so it only runs with
 * {@code -Dcreate_connected.gametest.kineticSources=true}.
 */
@Mod(CreateConnectedGameTests.MODID)
public final class CreateConnectedGameTests {
    public static final String MODID = "create_connected_gametest";
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(MODID, "empty_9x9x9");
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    /** Test name to {function, max ticks}. */
    private static final Map<String, Object[]> TESTS = new LinkedHashMap<>();

    static {
        test("content_registered", CreateConnectedGameTests::contentRegistered, 20);
        test("creative_tab_builds", CreateConnectedGameTests::creativeTabBuilds, 20);
        test("block_state_caches_built", CreateConnectedGameTests::blockStateCachesBuilt, 20);
        test("recipe_conditions", CreateConnectedGameTests::recipeConditions, 20);
        test("item_silo_item_capability", CreateConnectedGameTests::itemSiloItemCapability, 40);
        test("fan_catalyst_blasting", CreateConnectedGameTests::fanCatalystBlasting, 20);
        test("parallel_gearbox_propagates", CreateConnectedGameTests::parallelGearboxPropagates, 100);
        test("deployer_placed_against_running_battery", BatteryPlacementOrderTests::deployerPlacedAgainstRunningBattery, 100);
        test("deployer_moved_into_the_shafts_place", BatteryPlacementOrderTests::deployerMovedIntoTheShaftsPlace, 100);
        test("arm_takes_granite_behind_leftover_quartz", ArmBasinTests::armTakesGraniteBehindLeftoverQuartz, 400);
        test("arm_keeps_uppercase_targets_from_create", ArmBasinTests::armKeepsUppercaseTargetsFromCreate, 400);
        if (Boolean.getBoolean("create_connected.gametest.kineticSources")) {
            test("battery_drives_every_shaft_machine", KineticSourceTests::batteryDrivesEveryShaftMachine, 200_000);
        }
    }

    public CreateConnectedGameTests(IEventBus modBus, ModContainer container) {
        FUNCTIONS.register(modBus);
        modBus.addListener(CreateConnectedGameTests::registerTests);
    }

    private static void test(String name, Consumer<GameTestHelper> function, int maxTicks) {
        FUNCTIONS.register(name, () -> function);
        TESTS.put(name, new Object[]{function, maxTicks});
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "default"));
        TESTS.forEach((name, test) -> {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, STRUCTURE, (int) test[1], 0, true)));
        });
    }

    /**
     * Connected's content went in during bootstrap (CreateRegisterMixin): blocks, items, block entity types and the
     * creative tab exist, Create's freeze-time arm point list includes ours, and the copycats joined Create's copycat
     * block entity type through BlockEntityTypeAddBlocksEvent.
     */
    private static void contentRegistered(GameTestHelper helper) {
        for (String id : List.of("kinetic_battery", "brass_gearbox", "parallel_gearbox", "six_way_gearbox", "item_silo",
                "fluid_vessel", "inventory_bridge", "inventory_access_port", "fan_blasting_catalyst", "overstress_clutch",
                "copycat_block", "sequenced_pulse_generator", "dashboard", "kinetic_bridge")) {
            Identifier key = CreateConnected.asResource(id);
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), "missing block " + key);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key), "missing item " + key);
        }
        long blocks = BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals(CreateConnected.MODID)).count();
        long blockEntities = BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().filter(k -> k.getNamespace().equals(CreateConnected.MODID)).count();
        helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(CCCreativeTabs.MAIN), "missing creative tab");
        long armPoints = CreateRegistries.ARM_INTERACTION_POINT_TYPE.keySet().stream().filter(k -> k.getNamespace().equals(CreateConnected.MODID)).count();
        helper.assertTrue(armPoints > 0, "no create_connected arm interaction point types");
        helper.assertTrue(AllBlockEntityTypes.COPYCAT.isValid(CCBlocks.COPYCAT_BLOCK.get().defaultBlockState()),
                "copycat_block is not valid for create:copycat");
        CreateConnected.LOGGER.info("content_registered: {} blocks, {} block entity types, {} arm point types", blocks, blockEntities, armPoints);
        helper.succeed();
    }

    /**
     * Every creative tab entry resolves to a real item, and the tab builds through NeoForge's
     * BuildCreativeModeTabContentsEvent hook, which throws on any stack whose count isn't 1 (an air stack counts 0).
     * The kinetic battery entry is listed with its full charge.
     */
    private static void creativeTabBuilds(GameTestHelper helper) {
        List<Identifier> air = CCCreativeTabs.ITEMS.stream()
                .filter(item -> item.asItem() == Items.AIR)
                .map(item -> item.getId())
                .toList();
        helper.assertTrue(air.isEmpty(), "creative tab entries resolve to air: " + air);

        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CCCreativeTabs.MAIN);
        var level = helper.getLevel();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().noneMatch(ItemStack::isEmpty), "creative tab holds an empty stack");
        ItemStack battery = tab.getDisplayItems().stream()
                .filter(stack -> stack.is(CCBlocks.KINETIC_BATTERY.asItem()))
                .findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(battery.isEmpty(), "creative tab is missing the kinetic battery");
        CreateConnected.LOGGER.info("creative_tab_builds: {} items", tab.getDisplayItems().size());
        helper.succeed();
    }

    /**
     * CreateConnected.initBlockStateCaches ran: every Connected block state has its cached occlusion shape and fluid
     * state (null when initCache never ran on it).
     */
    private static void blockStateCachesBuilt(GameTestHelper helper) {
        List<String> uncached = new java.util.ArrayList<>();
        int states = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(CreateConnected.MODID))
                continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                states++;
                if (state.getOcclusionShape() == null || state.getFluidState() == null)
                    uncached.add(state.toString());
            }
        }
        helper.assertTrue(uncached.isEmpty(), uncached.size() + " block states without caches, e.g. "
                + uncached.stream().limit(5).toList());
        CreateConnected.LOGGER.info("block_state_caches_built: {} states", states);
        helper.succeed();
    }

    /** neoforge:conditions converted from fabric:load_conditions: feature toggles load, absent-mod recipes don't. */
    private static void recipeConditions(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        ResourceKey<Recipe<?>> silo = ResourceKey.create(Registries.RECIPE, CreateConnected.asResource("crafting/kinetics/item_silo"));
        ResourceKey<Recipe<?>> freezing = ResourceKey.create(Registries.RECIPE, CreateConnected.asResource("crafting/kinetics/empty_fan_catalyst_from_freezing"));
        helper.assertTrue(recipes.byKey(silo).isPresent(), "feature-gated recipe " + silo.identifier() + " did not load");
        helper.assertFalse(recipes.byKey(freezing).isPresent(), "recipe " + freezing.identifier() + " loaded without its mods");
        long count = recipes.getRecipes().stream().filter(r -> r.id().identifier().getNamespace().equals(CreateConnected.MODID)).count();
        CreateConnected.LOGGER.info("recipe_conditions: {} create_connected recipes loaded", count);
        helper.succeed();
    }

    /** CCTransfer: the item silo is visible to other mods as a NeoForge item handler, and takes items through it. */
    private static void itemSiloItemCapability(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, CCBlocks.ITEM_SILO.get());
        helper.runAfterDelay(2, () -> {
            ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.UP);
            if (handler == null)
                throw helper.assertionException(pos, "item silo exposes no Capabilities.Item.BLOCK handler");
            int inserted;
            try (Transaction tx = Transaction.openRoot()) {
                inserted = handler.insert(ItemResource.of(Items.COBBLESTONE), 32, tx);
                tx.commit();
            }
            helper.assertTrue(inserted == 32, "inserted " + inserted + " of 32 cobblestone into the silo");
            long stored = 0;
            for (int i = 0; i < handler.size(); i++) {
                if (handler.getResource(i).is(Items.COBBLESTONE))
                    stored += handler.getAmountAsLong(i);
            }
            helper.assertTrue(stored == 32, "silo reports " + stored + " cobblestone after inserting 32");
            helper.succeed();
        });
    }

    /** A blasting fan catalyst is a blasting source for Create's air currents (create:fan_processing_catalysts/blasting). */
    private static void fanCatalystBlasting(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, CCBlocks.FAN_BLASTING_CATALYST.get());
        FanProcessingType type = FanProcessingType.getAt(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(type == AllFanProcessingTypes.BLASTING, "fan processing type at the catalyst is " + type);
        helper.succeed();
    }

    /** A creative motor drives a parallel gearbox, which passes rotation on to a shaft on its far side. */
    private static void parallelGearboxPropagates(GameTestHelper helper) {
        BlockPos motor = new BlockPos(2, 1, 4);
        BlockPos gearbox = new BlockPos(3, 1, 4);
        BlockPos shaft = new BlockPos(4, 1, 4);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(gearbox, CCBlocks.PARALLEL_GEARBOX.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        helper.setBlock(shaft, AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        helper.succeedWhen(() -> {
            KineticBlockEntity box = helper.getBlockEntity(gearbox, KineticBlockEntity.class);
            KineticBlockEntity out = helper.getBlockEntity(shaft, KineticBlockEntity.class);
            helper.assertTrue(box.getType() == CCBlockEntityTypes.PARALLEL_GEARBOX.get(), "gearbox block entity is " + box.getType());
            helper.assertTrue(box.getSpeed() != 0 && out.getSpeed() != 0,
                    "gearbox speed " + box.getSpeed() + ", output shaft speed " + out.getSpeed());
            CreateConnected.LOGGER.info("parallel_gearbox_propagates passed at tick {}: gearbox {} rpm, shaft {} rpm",
                    helper.getTick(), box.getSpeed(), out.getSpeed());
        });
    }
}
