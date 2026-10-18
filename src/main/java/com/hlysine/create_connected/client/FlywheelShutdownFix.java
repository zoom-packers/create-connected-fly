package com.hlysine.create_connected.client;

import com.zurrtum.create.client.flywheel.impl.task.FlwTaskExecutor;
import com.zurrtum.create.client.flywheel.impl.task.ParallelTaskExecutor;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Works around a Create Fly bug, not one of ours: closing the game always ended in a
 * "Watchdog (Client shutdown from post-main)" crash report.
 * <p>
 * Create Fly's Flywheel starts its worker threads ("Flywheel Task Executor #N") as non-daemon
 * threads and never calls {@code ParallelTaskExecutor.stopWorkers()}. After {@code main} returns
 * they are the only non-daemon threads left, so the JVM cannot exit and 26.2's shutdown watchdog
 * writes a crash report. Reported upstream as ZurrTum/Create-Fly#357; this is the fix suggested
 * there, moved from a mixin on {@code Minecraft.close} to a lifecycle event (NeoForge: ClientStoppingEvent).
 * <p>
 * Safe to keep once Create Fly fixes it: {@code stopWorkers()} does nothing if already stopped.
 */
public final class FlywheelShutdownFix {
    private FlywheelShutdownFix() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientStoppingEvent event) -> {
            if (FlwTaskExecutor.get() instanceof ParallelTaskExecutor executor)
                executor.stopWorkers();
        });
    }
}
