package com.hanmoyun.dghcompat.client;

import com.hanmoyun.dghcompat.corpse.CorpseEntity;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Render-only original-species surrogate; never added to the world and never owns inventory/health.
 */
public final class CorpseVisuals {
    private static final Map<CorpseEntity, LivingEntity> CACHE = new WeakHashMap<>();

    private CorpseVisuals() {}

    public static LivingEntity get(CorpseEntity corpse) {
        LivingEntity result = CACHE.get(corpse);
        if (result == null) {
            var data = corpse.visual();
            if (data.isEmpty() || Minecraft.getInstance().level == null) return corpse;
            if (data.getString("type").equals("minecraft:player")) {
                result =
                        new RemotePlayer(
                                Minecraft.getInstance().level,
                                new GameProfile(data.getUUID("owner"), data.getString("name"))) {
                            @Override
                            protected net.minecraft.client.multiplayer.PlayerInfo getPlayerInfo() {
                                var connection = Minecraft.getInstance().getConnection();
                                return connection == null ? null : connection.getPlayerInfo(getGameProfile().getId());
                            }
                        };
                com.hanmoyun.dghcompat.CyberPatientData.applyCorpseSkin((RemotePlayer) result, data);
            } else {
                var key = ResourceLocation.tryParse(data.getString("type"));
                var type =
                        key == null
                                ? null
                                : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
                var entity = type == null ? null : type.create(corpse.level());
                if (!(entity instanceof LivingEntity living) || entity instanceof CorpseEntity)
                    return corpse;
                result = living;
                if (result instanceof AgeableMob ageable) ageable.setBaby(data.getBoolean("baby"));
            }
            result.setId(corpse.getId());
            result.setUUID(corpse.getUUID());
            CACHE.put(corpse, result);
        }
        result.moveTo(corpse.getX(), corpse.getY(), corpse.getZ(), corpse.getYRot(), 0);
        result.yRotO = corpse.yRotO;
        result.yBodyRot = corpse.yBodyRot;
        result.yBodyRotO = corpse.yBodyRotO;
        result.yHeadRot = corpse.yHeadRot;
        result.tickCount = corpse.tickCount;
        return result;
    }

    public static void clear() {
        CACHE.clear();
    }
}
