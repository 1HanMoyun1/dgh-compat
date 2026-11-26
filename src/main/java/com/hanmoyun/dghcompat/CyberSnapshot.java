package com.hanmoyun.dghcompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CyberSnapshot {
    public record Implant(
            String slot,
            int index,
            String item,
            boolean replacement,
            boolean functional,
            String replacementTag) {}

    public static final CyberSnapshot EMPTY = new CyberSnapshot(List.of());
    private final List<Implant> implants;
    private final Map<String, Boolean> limbs = new HashMap<>();
    private final Map<String, Map<String, Integer>> organs = new HashMap<>();
    private final Map<String, Map<String, Integer>> installedOrgans = new HashMap<>();

    public CyberSnapshot(List<Implant> implants) {
        this.implants = List.copyOf(implants);
        for (Implant implant : implants) {
            if (!implant.replacement()) continue;
            String limb =
                    implant.item().startsWith("createcybernetics:bodypart_")
                            ? null
                            : CompatRules.limb(implant.slot());
            if (limb != null) limbs.merge(limb, implant.functional(), Boolean::logicalOr);
            String matched = CompatRules.organ(implant.slot(), implant.replacementTag());
            if (matched != null && !natural(implant.item())) {
                for (String part : CompatRules.parts(implant.slot()))
                    installedOrgans
                            .computeIfAbsent(part, ignored -> new HashMap<>())
                            .merge(matched, 1, Integer::sum);
            }
            if (!implant.functional() || natural(implant.item())) continue;
            String organ = CompatRules.organ(implant.slot(), implant.replacementTag());
            if (organ != null)
                for (String part : CompatRules.parts(implant.slot())) add(part, organ, 1);
        }
    }

    private void add(String part, String organ, int count) {
        organs.computeIfAbsent(part, ignored -> new HashMap<>()).merge(organ, count, Integer::sum);
    }

    public List<Implant> implants() {
        return implants;
    }

    public boolean hasLimb(String part) {
        return limbs.containsKey(part);
    }

    public boolean workingLimb(String part) {
        return limbs.getOrDefault(part, false);
    }

    public int count(String part, String organ) {
        return Math.min(
                organs.getOrDefault(part, Map.of()).getOrDefault(organ, 0),
                CompatRules.capacity(organ));
    }

    private static boolean natural(String id) {
        return switch (id) {
            case "createcybernetics:bodypart_brain",
                            "createcybernetics:bodypart_eyeballs",
                            "createcybernetics:bodypart_heart",
                            "createcybernetics:bodypart_lungs",
                            "createcybernetics:bodypart_liver",
                            "createcybernetics:bodypart_intestines",
                            "createcybernetics:bodypart_muscle",
                            "createcybernetics:bodypart_skin",
                            "createcybernetics:bodypart_kidney" ->
                    true;
            default -> false;
        };
    }

    public int replaced(String part, String organ) {
        return installedOrgans.getOrDefault(part, Map.of()).getOrDefault(organ, 0);
    }
}
