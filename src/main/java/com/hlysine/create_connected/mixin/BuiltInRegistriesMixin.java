package com.hlysine.create_connected.mixin;

import com.hlysine.create_connected.CreateConnected;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge: registers Connected's content during vanilla bootstrap, right after Create's own (see {@link CreateConnected}).
 * <p>
 * The Create port registers its content from a HEAD injection into {@code BuiltInRegistries.freeze()}. This one sits
 * at the first {@code Registry.freeze()} call in that method, which is after every HEAD handler and before any registry
 * is frozen, so the order does not depend on mixin priorities. (A hook on {@code Create.register()} itself does not
 * work: Create's mixin plugin loads that class before mixins are applied.)
 */
@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {
    @Inject(method = "freeze()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;", ordinal = 0))
    private static void create_connected$register(CallbackInfo ci) {
        CreateConnected.registerContent();
    }
}
