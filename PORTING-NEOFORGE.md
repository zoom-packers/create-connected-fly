# Porting notes: Create: Connected (Fly port, Fabric 26.2) → NeoForge 26.3

Written 2026-10-18. Checked against Minecraft 26.3, NeoForge 26.3.0.89, Java 25.0.1, and Zoompack 5's NeoForge port of
Create Fly (`create-fly-neoforge-6.0.9+neoforge-26.3.jar`, built from ZP5 `mods/create-fly-neoforge` commit `8da5b8c6`,
sha512 `68c1d7c8…41d8`).

This branch, `zp5/26.3`, is a Zoompack 5 fork of [GravisLudio/create-connected-fly](https://github.com/GravisLudio/create-connected-fly)
(Fabric 26.2 port of [hlysine/create_connected](https://github.com/hlysine/create_connected) onto ZurrTum's Create Fly).
It starts from `gravis/main` `95c3a7fd`. Remotes: `gravis` (the Fabric port), `upstream` (hlysine, NeoForge 1.21.1 on
Creators-of-Create's Create). `PORTING.md` is GravisLudio's Fabric document and is still accurate for everything that is
not about the loader.

## Why this base and this route

| | chaevsfe/create_connected (`create-fly-connected-unofficial-port`) | GravisLudio/create-connected-fly (`create-connected-fly-port`) |
|---|---|---|
| Licence | AGPL-3.0 (+ upstream's additional terms) | AGPL-3.0 (+ upstream's additional terms) |
| Version | 1.3.3, 2026-10-08 (26.1 and 26.2 builds) | 1.3.3, 2026-10-08; last commit 2026-10-11 |
| Code | 290 Java files, 13 renderers | 360 Java files, 27 renderers + 20 Flywheel visuals |
| Extras | EMI/REI viewer plugin | Ponder scenes, datagen, JEI plugin, GameTests, a long PORTING.md |

Gravis's port is the more complete one (every renderer and visual, Ponder, GameTests) and documents what it verified,
so it is the base.

**Native NeoForge, not Sinytra Connector.** The Fabric jar is built for 26.2 and Fabric Create Fly. Under Connector it
would need:
- the 26.2 → 26.3 vanilla changes below, which no Connector layer handles;
- Create Fly's Fabric-side API (Fabric transfer `Storage`, `CachedInventoryBehaviour` in `foundation`, Fabric
  `ResourceCondition`s), which ZP5's NeoForge Create port replaced with NeoForge equivalents;
- data in the 26.2 formats, which 26.3 refuses (loot conditions, advancements).

Create itself is ported natively for the same reasons (see ZP5's `create-fly-native-vs-connector-26.3.md`), and the Fabric
API surface here was small: 35 files, all lifecycle, networking, attachments, events and two transfer lookups.

## How the port is organised

- **Edits in place, marked.** Loader-specific edits say `NeoForge:` or `NeoForge 26.3:` in a comment.
- **Excluded, not deleted.** Integrations with mods that have no NeoForge 26.3 build stay in the tree and are excluded in
  `build.gradle`, as the Fabric port did (Copycats+, Additional Placements, Dye Depot, Simulated, the linked throttle
  lever).
- **Tools** (copied from the Create port, Python 3, run with `-I`):

| Script | Does |
|---|---|
| `tools/classtweaker2at.py src/main/resources/create_connected.classtweaker <patched MC merged jar>` | Writes `META-INF/accesstransformer.cfg` from the Fabric classtweaker, plus the override entries NeoForge's recompile needs |
| `tools/fabric_conditions_to_neoforge.py src/generated/resources src/main/resources` | `fabric:load_conditions` → `neoforge:conditions` (new for this port) |
| `tools/data_26_3.py src/generated/resources/data src/main/resources/data src/generated/resources/assets src/main/resources/assets` | 26.2 → 26.3 data formats (loot tables, advancements, model `shade`) |
| `tools/check_mixins.py <patched MC merged jar> <NeoForge universal jar> <Create port jar>` | Static mixin check (patched here to cover mixins in sub-packages) |
| `tools/class-moves-26.3.txt` | Classes 26.3 moved (`com.mojang.blaze3d` → `com.mojang.renderpearl`, …), applied to the sources once |

## Build

- ModDevGradle 2.0.148, NeoForge 26.3.0.89, Gradle 9.2.1, JDK 25. `./gradlew build` →
  `build/libs/create_connected-1.3.3-zp5.3+neoforge-26.3.jar` and a sources jar.
- **The Create port is a file dependency.** It has no published build yet, so `libs/` (gitignored) holds
  `create-fly-neoforge-6.0.9+neoforge-26.3.jar` and `create-interfaces.json` (its `interfaces.json`). Copy both from
  ZP5 `mods/create-fly-neoforge` (`build/libs/` and the repo root) before building. Switch to a maven coordinate once the
  Create port is released.
- **Create's access transformers** are extracted from that jar (`extractCreateAt`) and applied to the recompiled
  Minecraft, because the addon compiles against members Create widens (Fabric did the same through Create Fly's
  classtweaker). At runtime NeoForge applies both mods' ATs.
- **Source sets:** `main` (the mod) and `gametest` (dev-only mod `create_connected_gametest`).
  `./gradlew gametestJar` packages the tests as their own jar for booting the released jar elsewhere.
- `-PwithJei` adds JEI to the dev runs; `-PmixinDebug` turns on verbose mixin logging.

## Loader and lifecycle

**Registration happens during vanilla bootstrap.** NeoForge freezes the vanilla registries at the end of bootstrap,
before mods are constructed. The Create port registers its own content there (a HEAD injection into
`BuiltInRegistries.freeze()` that calls `Create.register()`), and Create's freeze-time hooks then sort arm interaction
point and fan processing types. The client entrypoint also binds renderers and models to registry objects at
construction. So Connected registers in the same window: `mixin/BuiltInRegistriesMixin` calls
`CreateConnected.registerContent()` at the first `Registry.freeze()` call inside `BuiltInRegistries.freeze()`, which is
after Create's HEAD hook and before anything is frozen. NeoForge's block callbacks build the block-state caches for these
blocks at bake time.

**Block items fill `Item.BY_BLOCK` themselves.** NeoForge adds the ITEM registry callback that maps blocks to their
items only at `ModifyRegistriesEvent`, after bootstrap. Vanilla's `Items.registerItem` calls
`BlockItem#registerBlocks(Item.BY_BLOCK, item)` itself, so `BlockBuilder` and `ItemBuilder` do the same before
`Registry.register`. Up to zp5.2 they didn't, so every Connected block's `asItem()` was air: the creative tab threw
"The stack count must be 1" (NeoForge's `EventHooks.onCreativeModeTabBuildContents`), and pick-block and
`BlockEntry#asStack()` returned empty stacks. The item-only block items (vertical gearboxes, copycat box and catwalk,
charged battery) override `registerBlocks` to stay out of the map, as upstream.

A mixin on `Create.register()` itself does not work: Create's mixin plugin loads `com.zurrtum.create.Create` before
mixins are applied, so the class is never transformed.

| Fabric | NeoForge |
|---|---|
| `main` entrypoint `CreateConnected#onInitialize` | `@Mod CreateConnected`: content in `registerContent()` (bootstrap), everything else in the constructor |
| `client` entrypoint | `@Mod(dist = CLIENT) CreateConnectedClient` |
| `jei_mod_plugin` entrypoint | JEI's `@JeiPlugin` on `CreateConnectedJEI` (already there) |
| `FabricLoader#isModLoaded` | Create port's `Platform.isModLoaded`; `LoadingModList` in the mixin plugin |
| `FabricLoader#getEnvironmentType` | `FMLEnvironment.getDist()` |
| `@Environment(CLIENT)` | removed (NeoForge has no equivalent; client classes are only reached from client code) |
| catnip config (`Builder.create`) | unchanged; built in the mod constructor, as Create builds its own |

## Fabric API replacements

| Fabric API | NeoForge 26.3 |
|---|---|
| `PayloadTypeRegistry` + `ServerPlayNetworking` / `ClientPlayNetworking` | `RegisterPayloadHandlersEvent` (`CCPackets`), `PacketDistributor`, `ClientPacketDistributor` |
| `ResourceCondition` / `ResourceConditionType` | `ICondition` codecs in `NeoForgeRegistries.Keys.CONDITION_CODECS` (`CCCraftingConditions`, as upstream) |
| `ItemStorage.SIDED` / `FluidStorage.SIDED` | `Capabilities.Item.BLOCK` / `Capabilities.Fluid.BLOCK` through the Create port's `NeoForgeTransfer` and its `Cached*InventoryBehaviour` (`CCTransfer`) |
| `FabricBlockEntityType#addValidBlock` (copycats join `create:copycat`) | Mod-bus `BlockEntityTypeAddBlocksEvent`, setting the set through an accessor (`BlockEntityTypeAccessor`): the event's own `modify` refuses Connected's copycats because they don't extend `WaterloggedCopycatBlock` |
| `CreativeModeTabEvents.modifyOutputEvent` | `BuildCreativeModeTabContentsEvent` with `PARENT_AND_SEARCH_TABS` |
| `AttachmentRegistry` (goggle slot) | NeoForge `AttachmentType` (`serialize` + `sync` to all players), `DeferredRegister` |
| `UseItemCallback`, `ServerLivingEntityEvents.AFTER_DEATH`, `ServerPlayerEvents.COPY_FROM` | `PlayerInteractEvent.RightClickItem`, `LivingDeathEvent` (LOWEST), `PlayerEvent.Clone` |
| `ScreenEvents.afterExtract`, `ScreenMouseEvents.allowMouseClick` | `ScreenEvent.Render.Post`, `ScreenEvent.MouseButtonPressed.Pre` |
| `LivingEntityRenderLayerRegistrationCallback` | `EntityRenderersEvent.AddLayers` (player skins) |
| `ServerLevelEvents.LOAD/UNLOAD` | `LevelEvent.Load/Unload`, server levels only |
| `ClientLifecycleEvents.CLIENT_STOPPING` | `ClientStoppingEvent` |
| `fabric:load_conditions` in data (210 files) | `neoforge:conditions` (`fabric:all_mods_loaded` → `neoforge:mod_loaded`, `fabric:or/not` → `neoforge:or/not`) |

## Minecraft 26.2 → 26.3 changes handled

- Class moves (`tools/class-moves-26.3.txt`) and renames: `RedStoneWireBlock` → `RedstoneWireBlock`, `DirtPathBlock` →
  `PathBlock`, `PushReaction.PUSH_ONLY` → `PUSH`.
- Block codecs are gone: `simpleCodec` / `codec()` removed from the dashboard and the sequenced pulse generator.
- `BlockItem#updateCustomBlockEntityTag` is static. Post-placement fix-ups (vertical gearboxes, copycat box/catwalk,
  charged battery) moved to NeoForge's `BlockItem#onBlockPlacedBy`; stack fix-ups before the data is applied (fluid
  vessel, item silo) moved to `placeBlock`, as in the Create port.
- `Prediction` argument on `Player#drop` and `Inventory#placeItemBackInInventory`.
- Sign text: no `setMessage`/`getMessage`; `SignText.Mutable#setLine`, `getMessages`, `withGlowingText`,
  `SignTextSlot`, `SignText.CODEC`.
- `PoseStack#mulPose(Quaternionf)` → `rotate`.
- Advancement triggers: player predicates are `Optional<Holder<LootItemCondition>>` (`LootItemCondition.CODEC`).
- Sound events used as holders need `.value()`.
- Create's ATs make `BlockBehaviour#getShape` public, so overrides must be public.
- Create Fly renamed `EncasedCogVisual::small` to `EncasedSmallCogVisual::new`.
- Data: 85 advancements and 72 loot tables converted by `tools/data_26_3.py`; 0 legacy constructs left.

## Not ported, approximated, or unverified

- **Client not run.** Everything client-side compiles and every client mixin passes the static check, but no client
  has been launched (renderers, Flywheel visuals, Ponder, screens, the goggle slot UI). Ask before running `runClient`.
- **Integrations excluded** as on Fabric: Copycats+, Additional Placements, Dye Depot, Simulated (linked throttle lever),
  Diagonal Fences/Radium compat mixins only apply when those mods are present (`require = 0` / `@Pseudo`).
- **Goggle slot** stands down only when Trinkets is installed, which never happens on NeoForge. With Curios in the pack
  it still shows its own slot; the Create port has no Curios goggles yet.
- **Server → client config sync** is still the Fabric port's open TODO (`config/CCommon.java`).
- **`KineticSourceTests`** (a 200k-tick survey of every shaft machine) only runs with
  `-Dcreate_connected.gametest.kineticSources=true`.
- One upstream data-map file (`create_dragons_plus:fan_processing_catalysts/coloring`) logs a warning when that mod is
  absent. Harmless.

## Verification (2026-10-18)

1. `./gradlew build`: passes; no compile errors, only upstream deprecation warnings.
2. `tools/check_mixins.py` against the patched MC, NeoForge universal and the Create port: 0 real problems. The three
   reports are `COMPATFenceBlockMixin` (`require = 0`, Diagonal Fences only), `COMPATPathNodeDefaultsMixin` (`@Pseudo`,
   Radium only) and an accessor-name quirk in the checker (`getCACHED_PARAMETERS`; the runtime log shows it applied).
3. `./gradlew runGameTestServer`: all 12 required tests pass (11 before zp5.3):
   - `create:water_wheel_drives_press` (Create's own baseline);
   - `content_registered`: 94 blocks, 27 block entity types, Connected's arm point type, creative tab, copycats valid
     for `create:copycat`;
   - `recipe_conditions`: feature-gated recipes load (70), recipes needing absent mods don't;
   - `item_silo_item_capability`: 32 cobblestone in through `Capabilities.Item.BLOCK`;
   - `fan_catalyst_blasting`: Create's `FanProcessingType.getAt` sees the catalyst as blasting;
   - `parallel_gearbox_propagates`: creative motor → parallel gearbox → shaft (16 rpm);
   - the Fabric port's battery/deployer and arm/basin tests (4).
   - `creative_tab_builds` (zp5.3): every tab entry resolves to an item, and the tab builds through NeoForge's hook.
4. The released jar on its own: a scratch MDG project with only the Create port jar, this jar and the gametest jar
   (`runtimeOnly`, no sources, so the jar's own ATs/mixins/metadata are what runs) passes the same 11 tests.

## Tracking upstream

1. `git fetch gravis` and rebase `zp5/26.3` on `gravis/main`. Conflicts should be limited to lines marked `NeoForge`,
   `build.gradle`, the entry points and `CCPackets`.
2. Re-run `tools/fabric_conditions_to_neoforge.py` and `tools/data_26_3.py` on the data (both idempotent).
3. Refresh `libs/` from the Create port, `./gradlew build`, `tools/check_mixins.py`, `./gradlew runGameTestServer`.

## Licence

AGPL-3.0 with upstream's additional terms (end of `LICENSE`): keep the link to the original, don't claim sole
authorship, and use a distinct name and icon. This build is "Create: Connected (ZP5 NeoForge Port)" and its icon's banner
reads "ZP5 NEOFORGE". AGPL also means every distributed jar must match published source: push this branch (for example
to a zoom-packers fork) before releasing a jar.
