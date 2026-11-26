package com.hanmoyun.dghcompat.mixin;

import com.hanmoyun.dghcompat.BodyLink;
import com.hanmoyun.dghcompat.CyberSnapshot;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AbstractBody.class, remap = false)
public abstract class BodyMixin implements BodyLink {
    @Unique private CyberSnapshot dghcyber$snapshot = CyberSnapshot.EMPTY;

    @Override
    public CyberSnapshot dghcyber$snapshot() {
        return dghcyber$snapshot;
    }

    @Override
    public void dghcyber$bind(CyberSnapshot snapshot) {
        dghcyber$snapshot = snapshot;
    }
}
