package com.hlysine.create_connected.content.kineticbridge;

import net.neoforged.fml.loading.FMLEnvironment;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class KineticBridgeBlockItem extends BlockItem {

    public KineticBridgeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        InteractionResult result = super.place(ctx);
        // Catnip's platform abstraction did not survive the Fabric rewrite; the loader's own
        // environment check is the physical-side guard now.
        if (result == InteractionResult.FAIL && ctx.getLevel().isClientSide()
                && FMLEnvironment.getDist().isClient())
            showBounds(ctx);
        return result;
    }

    public void showBounds(BlockPlaceContext context) {
        // ZP5 port: client-only work lives in CCClientHooks so a dedicated server never verifies LocalPlayer here
        com.hlysine.create_connected.client.CCClientHooks.showKineticBridgeBounds((KineticBridgeBlock) getBlock(), context);
    }

}

