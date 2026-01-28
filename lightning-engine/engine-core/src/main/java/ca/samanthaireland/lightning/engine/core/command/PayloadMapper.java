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


package ca.samanthaireland.lightning.engine.core.command;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Utility for converting command payloads to typed DTOs using Jackson.
 *
 * <p>This class provides a shared ObjectMapper instance configured for command
 * payload conversion, avoiding manual field extraction with type coercion.
 *
 * <p>Usage:
 * <pre>{@code
 * CreateGridMapPayload dto = PayloadMapper.convert(payload, CreateGridMapPayload.class);
 * }</pre>
 */
public final class PayloadMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

    private PayloadMapper() {
        // Utility class
    }

    /**
     * Converts a CommandPayload to a typed DTO.
     *
     * @param payload the command payload containing the raw data
     * @param type    the target DTO class
     * @param <T>     the DTO type
     * @return the converted DTO instance
     * @throws PayloadConversionException if conversion fails
     */
    public static <T> T convert(CommandPayload payload, Class<T> type) {
        return convert(payload.getPayload(), type);
    }

    /**
     * Converts a Map to a typed DTO.
     *
     * @param data the raw payload map
     * @param type the target DTO class
     * @param <T>  the DTO type
     * @return the converted DTO instance
     * @throws PayloadConversionException if conversion fails
     */
    public static <T> T convert(Map<String, Object> data, Class<T> type) {
        try {
            return MAPPER.convertValue(data, type);
        } catch (IllegalArgumentException e) {
            throw new PayloadConversionException(
                    "Failed to convert payload to " + type.getSimpleName(), e);
        }
    }

    /**
     * Gets the shared ObjectMapper instance for custom operations.
     *
     * @return the configured ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Exception thrown when payload conversion fails.
     */
    public static class PayloadConversionException extends RuntimeException {
        public PayloadConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
