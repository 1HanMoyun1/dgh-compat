package com.hanmoyun.dghcompat;

import net.minecraft.world.phys.Vec3;

/** Bed-relative positions keep the shoulders attached while the patient looks down. */
public final class SelfSurgeryMotion {
    private SelfSurgeryMotion() {}

    public static Vec3 home(Vec3 feet, Vec3 along) {
        return feet.add(along.scale(1.58)).add(0, 0.22, 0);
    }

    public static Vec3 destination(Vec3 feet, Vec3 along, Vec3 across, String part) {
        return switch (part) {
            case "LEFT_ARM", "RIGHT_ARM" ->
                    feet.add(along.scale(1.43))
                            .add(across.scale(part.equals("RIGHT_ARM") ? 0.10 : -0.10))
                            .add(0, 0.38, 0);
            case "TORSO" -> feet.add(along.scale(1.44)).add(0, 0.40, 0);
            case "LEFT_LEG", "RIGHT_LEG" ->
                    feet.add(along.scale(0.96))
                            .add(across.scale(part.equals("RIGHT_LEG") ? 0.08 : -0.08))
                            .add(0, 0.40, 0);
            default -> home(feet, along);
        };
    }

    public static Vec3 shoulder(Vec3 feet, Vec3 along, Vec3 across, boolean right) {
        return feet.add(along.scale(1.28)).add(across.scale(right ? 0.24 : -0.24)).add(0, 0.12, 0);
    }

    public static Vec3 rest(Vec3 feet, Vec3 along, Vec3 across, boolean right) {
        return feet.add(along.scale(0.91)).add(across.scale(right ? 0.31 : -0.31)).add(0, 0.17, 0);
    }
}
