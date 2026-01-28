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

package ca.samanthaireland.lightning.controlplane.exception;

/**
 * Base exception for all Control Plane exceptions.
 * Provides error codes for consistent error handling and API responses.
 */
public abstract class ControlPlaneException extends RuntimeException {

    private final String code;

    /**
     * Creates a new Control Plane exception.
     *
     * @param code the error code for this exception
     * @param message the error message
     */
    protected ControlPlaneException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Creates a new Control Plane exception with a cause.
     *
     * @param code the error code for this exception
     * @param message the error message
     * @param cause the underlying cause
     */
    protected ControlPlaneException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Gets the error code for this exception.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }
}
