package com.hanmoyun.dghcompat;

import com.lastimp.dgh.common.capability.HealthCapability;
import com.lastimp.dgh.common.capability.bodyPart.ConditionAccessor;
import com.lastimp.dgh.common.capability.bodyPart.base.AbstractBody;
import com.lastimp.dgh.common.capability.bodyPart.base.BodyCondition;
import com.lastimp.dgh.common.enums.BodyComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LimbBridge {
    public static final ResourceLocation CYBER =
            ResourceLocation.fromNamespaceAndPath(CompatMod.ID, "cybernetic_limb");

    public static boolean isCybernetic(AbstractBody body) {
        return CyberApi.available()
                && body.getBodyConditions().contains(CYBER)
                && body.getConditionValue(CYBER) > 0;
    }

    public static void registerCondition() {
        com.lastimp
                .dgh
                .api
                .DGH_API
                .registerCondition(
                        CYBER,
                        ResourceLocation.fromNamespaceAndPath(
                                "dgh", "container/condition_icons/bone_iron"),
                        com.lastimp.dgh.api.ComponentLevel.EXTREMITIES)
                .isComfort()
                .eyeVisible()
                .setHealing(0);
    }

    public static boolean prosthesis(CyberSnapshot.Implant implant) {
        return implant.replacement()
                && CompatRules.limb(implant.slot()) != null
                && !implant.item().startsWith("createcybernetics:bodypart_");
    }

    public static void clear(AbstractBody body, ResourceLocation key) {
        body.setConditionValue(key, ConditionAccessor.get(key).defaultValue());
        body.setConditionHidden(key, 0);
    }

    public static CyberSnapshot update(
            HealthCapability health, ServerPlayer player, CyberSnapshot snapshot) {

        for (BodyComponents part : BodyComponents.EXTREMITIES) {
            AbstractBody body = health.getComponent(part);
            boolean present =
                    snapshot.implants().stream()
                            .anyMatch(
                                    i ->
                                            prosthesis(i)
                                                    && part.name()
                                                            .equals(CompatRules.limb(i.slot())));
            present &=
                    !body.abnormal(BodyCondition.SURGICAL_AMPUTATION)
                            && !body.abnormal(BodyCondition.TRAUMATIC_AMPUTATION);
            if (body.getConditionValue(CYBER) != (present ? 1 : 0)) {
                body.setConditionValue(CYBER, present ? 1 : 0);
                health.updateIfDirty(Boolean.TRUE, Boolean.FALSE);
            }
        }
        return snapshot;
    }
}
