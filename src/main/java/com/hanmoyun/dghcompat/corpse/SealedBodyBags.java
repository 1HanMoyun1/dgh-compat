package com.hanmoyun.dghcompat.corpse;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.container.IItemHandler;
import com.lastimp.dgh.common.entry.register.ModItems;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Bind the actual equipment handler, not a slot number shared with unrelated inventories. */
public final class SealedBodyBags {
    private static final Map<IItemHandler, Boolean> CORPSE_HANDLERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SealedBodyBags() {}

    public static void bind(HealthCapability health) {
        CORPSE_HANDLERS.put(health.getAutoPulse(), true);
    }

    public static boolean locked(IItemHandler handler) {
        return handler != null
                && CORPSE_HANDLERS.containsKey(handler)
                && handler.getStackInSlot(0).is(ModItems.STASIS_BAG.get());
    }
}
