package com.hanmoyun.dghcompat.mixin;

import com.lastimp.dgh.common.entity.StretcherEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;

/** Feet-based patient origin shared by both client and server. */
@Mixin(value = StretcherEntity.class, remap = false)
abstract class StretcherGeometryMixin {
    /**
     * Keep vanilla base ticking but skip DGH's manual gravity/drive for a bed anchor. DGH's tick
     * adds gravity regardless of noGravity; server repositioning cannot prevent the client's
     * intervening fall. Invisible + noGravity is synchronized to both sides.
     */
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "tick",
            at =
                    @org.spongepowered.asm.mixin.injection.At(
                            value = "INVOKE",
                            target = "Lcom/lastimp/dgh/common/entity/StretcherEntity;drive()V"),
            cancellable = true)
    private void fixedBedAnchor(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var carrier = (StretcherEntity) (Object) this;
        if (carrier.isInvisible() && carrier.isNoGravity()) {
            carrier.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "getDropItem",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            cancellable = true)
    private void noAnchorDrop(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<
                            net.minecraft.world.item.Item>
                    ci) {
        var carrier = (StretcherEntity) (Object) this;
        if (carrier.getPersistentData().getBoolean("dghcompat:stretcher_returned"))
            ci.setReturnValue(net.minecraft.world.item.Items.AIR);
    }

    public boolean shouldRiderSit() {
        return false;
    }

    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        var carrier = (StretcherEntity) (Object) this;
        if (!carrier.hasPassenger(passenger)) return;
        // An invisible bed anchor already stores the feet position; the stretcher mattress is 0.25
        // high.
        Vec3 feet =
                carrier.isInvisible()
                        ? carrier.position()
                        : carrier.position()
                                .add(Vec3.directionFromRotation(0, carrier.getYRot()).scale(0.35))
                                .add(0, 0.52, 0);
        move.accept(passenger, feet.x, feet.y, feet.z);
        passenger.setYRot(carrier.getYRot());
        if (passenger instanceof net.minecraft.world.entity.LivingEntity living) {
            living.setYBodyRot(carrier.getYRot());
            living.setYHeadRot(carrier.getYRot());
        }
    }
}
