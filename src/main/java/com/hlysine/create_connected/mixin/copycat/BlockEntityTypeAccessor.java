package com.hlysine.create_connected.mixin.copycat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * NeoForge: lets Connected's copycat blocks join Create's {@code create:copycat} block entity type, as Fabric's
 * {@code addValidBlock} did. NeoForge's own {@code BlockEntityTypeAddBlocksEvent#modify} refuses them, because they
 * extend {@code CopycatBlock} but not {@code WaterloggedCopycatBlock}, the common superclass of Create's copycats.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {
    @Accessor("validBlocks")
    @Mutable
    void create_connected$setValidBlocks(Set<Block> validBlocks);
}
