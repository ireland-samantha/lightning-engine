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


package ca.samanthaireland.lightning.engine.rendering.testing;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Wait utility for polling until conditions are met.
 * Mirrors Selenium's WebDriverWait.
 *
 * <p>Example usage:
 * <pre>{@code
 * driver.wait(Duration.ofSeconds(5))
 *     .until(ExpectedConditions.visibilityOf(By.id("result")));
 * }</pre>
 */
public class Wait {

    private final GuiDriver driver;
    private Duration timeout = Duration.ofSeconds(10);
    private Duration pollingInterval = Duration.ofMillis(100);
    private String message = null;

    /**
     * Create a new Wait for the given driver.
     * @param driver the driver to wait on
     */
    public Wait(GuiDriver driver) {
        this.driver = driver;
    }

    /**
     * Set the timeout duration.
     * @param timeout the maximum time to wait
     * @return this wait for chaining
     */
    public Wait timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Set the polling interval.
     * @param interval the interval between condition checks
     * @return this wait for chaining
     */
    public Wait pollingEvery(Duration interval) {
        this.pollingInterval = interval;
        return this;
    }

    /**
     * Set a custom timeout message.
     * @param message the message to include in timeout exceptions
     * @return this wait for chaining
     */
    public Wait withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Wait until the condition returns a non-null, non-false result.
     * @param condition the condition to wait for
     * @param <T> the return type
     * @return the result of the condition
     * @throws TimeoutException if the timeout is reached
     */
    public <T> T until(ExpectedCondition<T> condition) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        Exception lastException = null;

        while (System.currentTimeMillis() < endTime) {
            try {
                T result = condition.apply(driver);
                if (result != null && (!(result instanceof Boolean) || (Boolean) result)) {
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
            }

            try {
                Thread.sleep(pollingInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted", e);
            }
        }

        String timeoutMessage = message != null
            ? message
            : "Timeout waiting for condition: " + condition.describe();

        if (lastException != null) {
            throw new TimeoutException(timeoutMessage, lastException);
        } else {
            throw new TimeoutException(timeoutMessage);
        }
    }

    /**
     * Wait until the boolean supplier returns true.
     * @param condition the condition to wait for
     * @throws TimeoutException if the timeout is reached
     */
    public void untilTrue(BooleanSupplier condition) {
        until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                return condition.getAsBoolean();
            }

            @Override
            public String describe() {
                return "custom boolean condition";
            }
        });
    }

    /**
     * Wait until the function returns a non-null result.
     * @param function the function to evaluate
     * @param <T> the return type
     * @return the result
     * @throws TimeoutException if the timeout is reached
     */
    public <T> T until(Function<GuiDriver, T> function) {
        return until(new ExpectedCondition<T>() {
            @Override
            public T apply(GuiDriver driver) {
                return function.apply(driver);
            }

            @Override
            public String describe() {
                return "custom function";
            }
        });
    }
}
