package com.hlysine.create_connected.datagen.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * See {@link FeatureEnabledCondition} for the NeoForge-to-Fabric mapping.
 * <p>
 * This one asked Copycats+ whether a feature was on, and returned false when the mod was absent.
 * CopycatsManager is excluded from the build until Copycats+ ships for 26.2 (see build.gradle), so
 * only that false branch can exist -- which is what would run without the mod anyway. The condition
 * itself stays registered because recipe JSONs reference it by id, and an unregistered condition id
 * would fail the recipe load (NeoForge) or silently enable it (Fabric).
 */
public record FeatureEnabledInCopycatsCondition(Identifier feature) implements ICondition {
    public static final MapCodec<FeatureEnabledInCopycatsCondition> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(Identifier.CODEC.fieldOf("tag").forGetter(FeatureEnabledInCopycatsCondition::feature))
            .apply(builder, FeatureEnabledInCopycatsCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        return false;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }


    @Override
    public String toString() {
        return "feature_enabled_in_copycats(\"" + feature + "\")";
    }
}
