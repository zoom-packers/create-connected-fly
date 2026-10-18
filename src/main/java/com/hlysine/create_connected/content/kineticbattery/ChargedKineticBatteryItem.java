package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.registries.CCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Deprecated(forRemoval = true, since = "1.3.0")
public class ChargedKineticBatteryItem extends BlockItem {

    public ChargedKineticBatteryItem(Properties builder) {
        // getDescriptionId is final in 26.2; a custom lang key is declared on the properties instead.
        super(CCBlocks.KINETIC_BATTERY.get(), builder.overrideDescription("item.create_connected.charged_kinetic_battery"));
    }

    
    @Override
    public void registerBlocks(@NotNull Map<Block, Item> map, @NotNull Item self) {
    }

    // NeoForge 26.3: BlockItem#updateCustomBlockEntityTag is static now; this runs at the same point (after placement)
    @Override
    public void onBlockPlacedBy(net.minecraft.world.item.context.BlockPlaceContext context, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @org.jetbrains.annotations.Nullable Player player, @NotNull ItemStack stack) {
        if (world.isClientSide())
            return;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof KineticBatteryBlockEntity batteryBE))
            return;
        batteryBE.setBatteryLevel(KineticBatteryBlockEntity.getMaxBatteryLevel());
    }

}

