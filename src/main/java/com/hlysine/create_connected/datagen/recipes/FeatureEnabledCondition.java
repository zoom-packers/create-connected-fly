package com.hlysine.create_connected.datagen.recipes;

import com.hlysine.create_connected.config.FeatureToggle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A NeoForge {@code ICondition} again, as upstream (the Fabric port had made it a Fabric
 * {@code ResourceCondition}). The recipe JSONs carry {@code neoforge:conditions}, converted from the
 * Fabric port's {@code fabric:load_conditions} by {@code tools/fabric_conditions_to_neoforge.py}.
 */
public record FeatureEnabledCondition(Identifier feature) implements ICondition {
    public static final MapCodec<FeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(Identifier.CODEC.fieldOf("tag").forGetter(FeatureEnabledCondition::feature))
            .apply(builder, FeatureEnabledCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        return FeatureToggle.isEnabled(feature);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }


    @Override
    public String toString() {
        return "feature_enabled(\"" + feature + "\")";
    }
}
