package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.integration.RagdollBodies;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Optional boundary. The server creates bodies; clients follow native poses. */
public final class RagdollAdapter {
    private RagdollAdapter() {}

    public static Entity pick(Vec3 start, Vec3 end) {
        return RagdollBodies.available() ? NativeRagdollClient.pick(start, end) : null;
    }

    public static void update(LivingEntity entity) {
        if (RagdollBodies.available()) NativeRagdollClient.update(entity);
    }

    public static boolean supported(LivingEntity entity) {
        return RagdollBodies.available() && NativeRagdollClient.supported(entity);
    }

    public static void replacePlayer(Player previous) {
        if (RagdollBodies.available()) NativeRagdollClient.replacePlayer(previous);
    }

    public static boolean rendersCorpse(com.hanmoyun.dghcompat.corpse.CorpseEntity corpse) {
        return RagdollBodies.available() && NativeRagdollClient.renders(corpse);
    }

    public static void clear() {
        if (RagdollBodies.available()) NativeRagdollClient.clear();
        CorpseVisuals.clear();
    }

    public static void forget(Entity entity) {
        if (RagdollBodies.available()) NativeRagdollClient.forget(entity);
    }
}
