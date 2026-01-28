/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package ca.samanthaireland.lightning.engine.rendering.gui;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

/**
 * JUnit 5 condition that checks LWJGL native library availability.
 *
 * <p>This condition checks if LWJGL can be initialized by attempting to
 * load a native library. If the load fails with UnsatisfiedLinkError
 * or NoClassDefFoundError, the test is disabled.
 *
 * <p>The check uses reflection to avoid loading LWJGL classes directly
 * in this condition class, which would defeat the purpose of the check.
 *
 * @see DisabledIfLwjglUnavailable
 */
public class LwjglAvailabilityCondition implements ExecutionCondition {

    private static final Boolean LWJGL_AVAILABLE = checkLwjglAvailability();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (LWJGL_AVAILABLE) {
            return ConditionEvaluationResult.enabled("LWJGL native libraries are available");
        } else {
            return ConditionEvaluationResult.disabled(
                    "LWJGL native libraries are unavailable (headless/containerized environment)");
        }
    }

    /**
     * Check if LWJGL native libraries are available using reflection.
     *
     * <p>This is done once at class loading time to avoid repeated
     * expensive checks. We use reflection to avoid importing LWJGL
     * classes directly, which would cause class loading failures.
     */
    private static boolean checkLwjglAvailability() {
        try {
            // Load LWJGL Library class using reflection to trigger native library loading
            Class<?> libraryClass = Class.forName("org.lwjgl.system.Library");

            // Call Library.initialize() to trigger native library loading
            Method initMethod = libraryClass.getMethod("initialize");
            initMethod.invoke(null);

            // Also verify GLFW can load (required for our rendering tests)
            Class.forName("org.lwjgl.glfw.GLFW");

            return true;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | ExceptionInInitializerError e) {
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            // Any other exception (including InvocationTargetException wrapping
            // UnsatisfiedLinkError) means LWJGL isn't properly available
            return false;
        }
    }
}
