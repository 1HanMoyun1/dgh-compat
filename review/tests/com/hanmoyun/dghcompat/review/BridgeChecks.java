package com.hanmoyun.dghcompat.review;

import com.hanmoyun.dghcompat.BookPages;
import com.hanmoyun.dghcompat.CaseBookText;
import com.hanmoyun.dghcompat.CompatRules;
import com.hanmoyun.dghcompat.CyberSnapshot;
import com.hanmoyun.dghcompat.StasisGeometry;
import com.hanmoyun.dghcompat.TransportOrientation;
import com.hanmoyun.dghcompat.client.PendingEntityStates;
import com.hanmoyun.dghcompat.mixin.SilenceChecks;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Deterministic regression checks; run with gradlew verifyBridge. */
public final class BridgeChecks {
    public static void main(String[] args) throws Exception {
        pendingStates();
        geometry();
        existingRules();
        SilenceChecks.run();
        targets();
        System.out.println(
                "All bridge checks passed. In-game integration remains a separate validation"
                        + " step.");
    }

    private static void pendingStates() {
        var queue = new PendingEntityStates<String, Integer>(2, 3);
        var seen = new ArrayList<Integer>();
        queue.put("patient", 1);
        queue.put("patient", 2);
        require(queue.size() == 1, "Latest state must replace earlier state");
        queue.tick(
                value -> {
                    seen.add(value);
                    return true;
                });
        require(seen.equals(List.of(2)) && queue.size() == 0, "Applied stale packet");
        queue.put("dead-player", 7);
        queue.remove("dead-player");
        queue.tick(value -> { throw new AssertionError("Stale player state survived respawn"); });
        queue.put("first", 1);
        queue.put("second", 2);
        queue.put("third", 3);
        seen.clear();
        queue.tick(
                value -> {
                    seen.add(value);
                    return false;
                });
        require(seen.equals(List.of(2, 3)), "Queue must evict the oldest pending entity");
        queue.tick(value -> false);
        require(queue.size() == 2, "Unspawned entity must have a grace period");
        queue.tick(
                value -> {
                    throw new AssertionError("Expired packet must not apply");
                });
        require(queue.size() == 0, "Unresolved state must expire");
        queue.put("patient", 4);
        queue.tick(value -> false);
        queue.put("patient", 5);
        queue.tick(value -> false);
        queue.tick(value -> false);
        require(queue.size() == 1, "Latest update must renew expiry");
        queue.clear();
        queue.tick(
                value -> {
                    throw new AssertionError("State crossed world boundary");
                });
        require(queue.size() == 0, "World cleanup failed");
        System.out.println(
                "PASS pending states: replacement, capacity, grace period, expiry, renewal and"
                        + " world cleanup");
    }

    private static void geometry() {
        for (float yaw : new float[] {0, 90, 180, 270, 45, -90}) {
            var ground = StasisGeometry.bounds(10, 64, 20, yaw, false);
            near(ground.getCenter().x, 10);
            near(ground.getCenter().z, 20);
            near(ground.minY, 64);
            near(ground.getYsize(), 0.5);
            var mounted = StasisGeometry.bounds(10, 64, 20, yaw, true);
            near(mounted.getCenter().x, 10 + Math.sin(Math.toRadians(yaw)));
            near(mounted.getCenter().z, 20 + Math.cos(Math.toRadians(yaw)));
            near(mounted.minY, 63.75);
            near(mounted.getXsize(), ground.getXsize());
            near(mounted.getZsize(), ground.getZsize());
        }
        near(StasisGeometry.bounds(0, 0, 0, 0, false).getXsize(), 1);
        near(StasisGeometry.bounds(0, 0, 0, 0, false).getZsize(), 2);
        near(StasisGeometry.bounds(0, 0, 0, 90, false).getXsize(), 2);
        near(StasisGeometry.bounds(0, 0, 0, 90, false).getZsize(), 1);
        for (float yaw : new float[] {0, 90, 180, 270}) {
            near(TransportOrientation.modelYaw(yaw, true), -yaw);
            near(TransportOrientation.modelYaw(yaw, false), 180 - yaw);
        }
        System.out.println(
                "PASS geometry: four bed directions, diagonal/negative angles, mounted offset and"
                        + " ground centering");
    }

    private static void existingRules() {
        require(
                BookPages.addition(99, "a".repeat(180)).size() == 1,
                "Last page must remain usable");
        require(BookPages.addition(99, "a".repeat(181)) == null, "Overflow must fail atomically");
        String unicode = "a".repeat(179) + "\uD83D\uDE00" + "b";
        var pages = BookPages.addition(0, unicode);
        require(String.join("", pages).equals(unicode), "Unicode content changed");
        require(
                !Character.isHighSurrogate(pages.getFirst().charAt(pages.getFirst().length() - 1)),
                "Split surrogate pair");
        require(CaseBookText.time(0).equals("[06:00] "), "World-time offset changed");
        require(CaseBookText.time(18000).equals("[00:00] "), "Midnight mapping changed");
        require(CaseBookText.part("LEFT_ARM").equals("左臂"), "Body label changed");
        require(
                CompatRules.organ("ORGANS", "createcybernetics:liver_items").equals("liver"),
                "Liver mapping changed");
        require(
                CompatRules.organ("ORGANS", "createcybernetics:intestines_items").equals("stomach"),
                "Digestion mapping changed");
        var working = new CyberSnapshot.Implant("LARM", 0, "example:prosthesis", true, true, "");
        var broken = new CyberSnapshot.Implant("RARM", 0, "example:prosthesis", true, false, "");
        var snapshot = new CyberSnapshot(List.of(working, broken));
        require(
                snapshot.hasLimb("LEFT_ARM") && snapshot.workingLimb("LEFT_ARM"),
                "Working prosthesis lost");
        require(
                snapshot.hasLimb("RIGHT_ARM") && !snapshot.workingLimb("RIGHT_ARM"),
                "Broken prosthesis treated as working");
        System.out.println(
                "PASS existing rules: page limits/Unicode, time labels and implant mappings (not"
                        + " physiological integration)");
    }

    private static void targets() throws Exception {
        require(calls("net/minecraft/client/renderer/entity/LivingEntityRenderer", "render",
                        "net/minecraft/client/model/EntityModel", "renderToBuffer") == 1,
                "Combat arm mask must cover the actual model draw");
        require(calls("com/lastimp/dgh/common/event/eventHandler/PlayerEventHandler", "lambda$onPlayerRespawn$0",
                        "com/lastimp/dgh/common/capability/HealthCapability", "deserialize") == 1,
                "DGH respawn record reader changed");
        require(calls("com/raiiiden/ragdollified/client/ClientRagdollRenderer", "renderPlayerRagdoll",
                        "com/raiiiden/ragdollified/client/ClientRagdollRenderer", "findPlayerEntity") == 1,
                "Ragdoll player model lookup changed");
        require(calls("com/raiiiden/ragdollified/client/ClientRagdollRenderer", "renderPlayerRagdoll",
                        "com/raiiiden/ragdollified/client/ClientPlayerSkinCache", "drawable") == 1,
                "Ragdoll skin texture lookup changed");
        System.out.println("PASS player model draw, corpse skin and DGH respawn injection sites");
        require(
                calls(
                                "net/minecraft/world/entity/Entity",
                                "saveWithoutId",
                                "net/minecraft/world/entity/Entity",
                                "isSilent")
                        == 2,
                "Vanilla save contract changed: expected two isSilent queries");
        require(
                calls(
                                "com/hanmoyun/dghcompat/client/StasisBagRenderer",
                                "render",
                                null,
                                "bakeLayer")
                        == 0,
                "Renderer must not bake a model per frame");
        require(
                calls(
                                "com/hanmoyun/dghcompat/client/StasisBagRenderer",
                                "reloadModels",
                                null,
                                "bakeLayer")
                        == 1,
                "Resource reload must rebuild the cached model");
        require(
                calls("com/hanmoyun/dghcompat/StasisPatients", "tick", null, "setPos") == 0,
                "Rotation-only bounds update must not move the entity");
        System.out.println(
                "PASS bytecode: real Minecraft save query sites, reload-only model baking and"
                        + " position-free bounds refresh");
    }

    private static int calls(String owner, String method, String targetOwner, String targetMethod)
            throws Exception {
        int[] count = {0};
        try (InputStream input =
                BridgeChecks.class.getClassLoader().getResourceAsStream(owner + ".class")) {
            if (input == null) throw new AssertionError("Missing real class: " + owner);
            new ClassReader(input)
                    .accept(
                            new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public MethodVisitor visitMethod(
                                        int access,
                                        String name,
                                        String descriptor,
                                        String signature,
                                        String[] exceptions) {
                                    if (!name.equals(method)) return null;
                                    return new MethodVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visitMethodInsn(
                                                int opcode,
                                                String calledOwner,
                                                String calledName,
                                                String desc,
                                                boolean isInterface) {
                                            if (calledName.equals(targetMethod)
                                                    && (targetOwner == null
                                                            || calledOwner.equals(targetOwner)))
                                                count[0]++;
                                        }
                                    };
                                }
                            },
                            ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return count[0];
    }

    private static void near(double actual, double expected) {
        require(Math.abs(actual - expected) < 1.0e-8, "Expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
