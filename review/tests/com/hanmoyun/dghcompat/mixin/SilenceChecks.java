package com.hanmoyun.dghcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Exercises the actual mixin handler; does not claim to replace in-game Mixin transformation. */
public final class SilenceChecks extends StasisSoundMixin {
    public static void run() throws Exception {
        var instance = new SilenceChecks();
        Field saving = StasisSoundMixin.class.getDeclaredField("dghcompat$readingSavedSilence");
        saving.setAccessible(true);
        Method save =
                StasisSoundMixin.class.getDeclaredMethod(
                        "dghcompat$saveOriginalSilence", Entity.class, Operation.class);
        save.setAccessible(true);
        Method query =
                StasisSoundMixin.class.getDeclaredMethod(
                        "wrappedSilent", CallbackInfoReturnable.class);
        query.setAccessible(true);

        for (boolean original : new boolean[] {false, true}) {
            Operation<Boolean> operation =
                    args -> {
                        try {
                            require(
                                    saving.getBoolean(instance),
                                    "Save must suppress the temporary overlay");
                            var callback =
                                    new CallbackInfoReturnable<Boolean>("isSilent", true, original);
                            query.invoke(instance, callback);
                            require(
                                    !callback.isCancelled(),
                                    "Save query must retain the original result");
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        return original;
                    };
            require(
                    (boolean) save.invoke(instance, null, operation) == original,
                    "Original silence lost");
            require(!saving.getBoolean(instance), "Save suppression leaked after success");
        }

        var failure = new IllegalStateException("Expected operation failure");
        try {
            save.invoke(
                    instance,
                    null,
                    (Operation<Boolean>)
                            args -> {
                                throw failure;
                            });
            throw new AssertionError("Expected operation failure to propagate");
        } catch (InvocationTargetException e) {
            require(e.getCause() == failure, "Unexpected exception");
        }
        require(!saving.getBoolean(instance), "Save suppression leaked after exception");

        saving.setBoolean(instance, true);
        save.invoke(instance, null, (Operation<Boolean>) args -> false);
        require(saving.getBoolean(instance), "Nested operation must restore previous suppression");
        saving.setBoolean(instance, false);
        System.out.println(
                "PASS silence: original false/true, query bypass, exception and nested-state"
                        + " restoration");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
