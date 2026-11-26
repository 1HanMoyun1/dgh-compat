package com.hanmoyun.dghcompat.integration;

import com.raiiiden.ragdollified.RagdollTransform;
import com.raiiiden.ragdollified.server.ServerRagdollSyncManager;

import io.netty.buffer.Unpooled;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DeathPose {
    private DeathPose() {}

    public static RagdollTransform[] current(LivingEntity source) {
        var body =
                ServerRagdollSyncManager.retainedBody(source.getId(), source.level().dimension());
        if (body == null
                || !body.live()
                || RagdollGeometry.shape(body.pose(), RagdollBodies.scale(source)) == null)
            return null;
        return translate(body.pose(), source.position().scale(-1));
    }

    private static RagdollTransform[] translate(RagdollTransform[] parts, Vec3 offset) {
        var result = new RagdollTransform[parts.length];
        for (int i = 0; i < parts.length; i++) {
            var t = parts[i];
            if (t == null) continue;
            result[i] =
                    new RagdollTransform(
                            t.partId,
                            (float) (t.position.x + offset.x),
                            (float) (t.position.y + offset.y),
                            (float) (t.position.z + offset.z),
                            t.rotation.x,
                            t.rotation.y,
                            t.rotation.z,
                            t.rotation.w);
        }
        return result;
    }

    public static void capture(LivingEntity entity, CompoundTag tag) {
        var parts = current(entity);
        if (parts == null) return;
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(parts.length);
            for (var t : parts) {
                buf.writeBoolean(t != null);
                if (t != null) t.writeTo(buf);
            }
            var bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            tag.putByteArray("death_pose", bytes);
            tag.putLong("pose_time", entity.level().getGameTime());
        } finally {
            buf.release();
        }
    }

    public static RagdollTransform[] read(CompoundTag tag, Vec3 origin) {
        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("death_pose")));
        try {
            int count = buf.readVarInt();
            if (count < 1 || count > RagdollTransform.MAX_PARTS) return null;
            var parts = new RagdollTransform[count];
            for (int i = 0; i < count; i++)
                if (buf.readBoolean()) parts[i] = RagdollTransform.readFrom(buf);
            return translate(parts, origin);
        } catch (RuntimeException malformed) {
            return null;
        } finally {
            buf.release();
        }
    }
}
