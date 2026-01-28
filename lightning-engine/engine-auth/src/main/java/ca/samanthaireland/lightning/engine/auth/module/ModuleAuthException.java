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


package ca.samanthaireland.lightning.engine.auth.module;

/**
 * Exception thrown when module authentication fails.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>A JWT token is invalid or malformed</li>
 *   <li>A JWT token has expired</li>
 *   <li>A JWT token's signature doesn't match</li>
 *   <li>Required claims are missing from the token</li>
 * </ul>
 */
public class ModuleAuthException extends RuntimeException {

    /**
     * Create a new ModuleAuthException with a message.
     *
     * @param message the error message
     */
    public ModuleAuthException(String message) {
        super(message);
    }

    /**
     * Create a new ModuleAuthException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ModuleAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
