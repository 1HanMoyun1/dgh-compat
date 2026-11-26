package com.hanmoyun.dghcompat;

import java.util.List;
import java.util.Map;

/** Pure mapping rules, deliberately independent of registries and class loading. */
public final class CompatRules {
    private CompatRules() {}

    public static final List<String> VISIBLE =
            List.of("HEAD", "TORSO", "LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG");
    private static final Map<String, String> LIMBS =
            Map.of(
                    "LARM",
                    "LEFT_ARM",
                    "RARM",
                    "RIGHT_ARM",
                    "LLEG",
                    "LEFT_LEG",
                    "RLEG",
                    "RIGHT_LEG");

    public static String limb(String slot) {
        return LIMBS.get(slot);
    }

    public static List<String> parts(String slot) {
        String limb = limb(slot);
        if (limb != null) return List.of(limb);
        return switch (slot) {
            case "BRAIN", "EYES" -> List.of("HEAD");
            case "HEART", "LUNGS", "ORGANS" -> List.of("TORSO");
            case "BONE", "MUSCLE", "SKIN" -> VISIBLE;
            default -> List.of();
        };
    }

    public static String organ(String slot, String replacementTag) {
        return switch (slot) {
            case "BRAIN" -> "brain";
            case "EYES" -> "eye";
            case "HEART" -> "heart";
            case "LUNGS" -> "lungs";
            case "MUSCLE" -> "muscle";
            case "SKIN" -> "skin";
            // Intestines provide digestive function, but never kidney or liver function.
            case "ORGANS" ->
                    switch (replacementTag) {
                        case "createcybernetics:liver_items" -> "liver";
                        case "createcybernetics:intestines_items",
                                        "createcybernetics:stomach_items" ->
                                "stomach";
                        case "createcybernetics:kidney_items" -> "kidney";
                        default -> null;
                    };
            default -> null;
        };
    }

    public static int capacity(String organ) {
        return organ.equals("eye") || organ.equals("kidney") ? 2 : 1;
    }

    /** Replacements fill a deficit; they never multiply the natural organ bonus. */
    public static int effective(int natural, int functionalReplacements, int capacity) {
        return Math.max(natural, Math.min(functionalReplacements, capacity));
    }

    public static boolean holdingArm(
            boolean hasLoadout, boolean installed, boolean amputated, boolean nativeVisible) {
        return !amputated && (hasLoadout ? installed : nativeVisible);
    }
}
