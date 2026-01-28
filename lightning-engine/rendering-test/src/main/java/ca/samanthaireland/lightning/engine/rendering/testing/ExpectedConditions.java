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

import java.util.List;

/**
 * Factory for common expected conditions.
 * Mirrors Selenium's ExpectedConditions.
 */
public final class ExpectedConditions {

    private ExpectedConditions() {}

    // ========== Element Presence ==========

    /**
     * Wait for an element to be present in the registry.
     * @param locator the locator to use
     * @return condition that returns the element when found
     */
    public static ExpectedCondition<GuiElement> presenceOf(By locator) {
        return new ExpectedCondition<>() {
            @Override
            public GuiElement apply(GuiDriver driver) {
                try {
                    return driver.findElement(locator);
                } catch (NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public String describe() {
                return "presence of element " + locator.describe();
            }
        };
    }

    /**
     * Wait for an element to be visible.
     * @param locator the locator to use
     * @return condition that returns the element when visible
     */
    public static ExpectedCondition<GuiElement> visibilityOf(By locator) {
        return new ExpectedCondition<>() {
            @Override
            public GuiElement apply(GuiDriver driver) {
                try {
                    GuiElement element = driver.findElement(locator);
                    return element.isVisible() ? element : null;
                } catch (NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public String describe() {
                return "visibility of element " + locator.describe();
            }
        };
    }

    /**
     * Wait for an element to become invisible or be removed.
     * @param locator the locator to use
     * @return condition that returns true when element is invisible or gone
     */
    public static ExpectedCondition<Boolean> invisibilityOf(By locator) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                try {
                    GuiElement element = driver.findElement(locator);
                    return !element.isVisible();
                } catch (NoSuchElementException e) {
                    return true;
                }
            }

            @Override
            public String describe() {
                return "invisibility of element " + locator.describe();
            }
        };
    }

    /**
     * Wait for an element to be clickable (visible and enabled).
     * @param locator the locator to use
     * @return condition that returns the element when clickable
     */
    public static ExpectedCondition<GuiElement> elementToBeClickable(By locator) {
        return new ExpectedCondition<>() {
            @Override
            public GuiElement apply(GuiDriver driver) {
                try {
                    GuiElement element = driver.findElement(locator);
                    return (element.isVisible() && element.isEnabled()) ? element : null;
                } catch (NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public String describe() {
                return "element to be clickable " + locator.describe();
            }
        };
    }

    // ========== Text Conditions ==========

    /**
     * Wait for an element's text to equal the expected value.
     * @param locator the locator to use
     * @param text the expected text
     * @return condition that returns true when text matches
     */
    public static ExpectedCondition<Boolean> textToBe(By locator, String text) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                try {
                    GuiElement element = driver.findElement(locator);
                    return text.equals(element.getText());
                } catch (NoSuchElementException e) {
                    return false;
                }
            }

            @Override
            public String describe() {
                return "text of " + locator.describe() + " to be \"" + text + "\"";
            }
        };
    }

    /**
     * Wait for an element's text to contain the expected substring.
     * @param locator the locator to use
     * @param text the expected substring
     * @return condition that returns true when text contains substring
     */
    public static ExpectedCondition<Boolean> textContains(By locator, String text) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                try {
                    GuiElement element = driver.findElement(locator);
                    String actualText = element.getText();
                    return actualText != null && actualText.contains(text);
                } catch (NoSuchElementException e) {
                    return false;
                }
            }

            @Override
            public String describe() {
                return "text of " + locator.describe() + " to contain \"" + text + "\"";
            }
        };
    }

    // ========== Element Count ==========

    /**
     * Wait for a specific number of elements to be present.
     * @param locator the locator to use
     * @param count the expected count
     * @return condition that returns true when count matches
     */
    public static ExpectedCondition<Boolean> elementCount(By locator, int count) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                List<GuiElement> elements = driver.findElements(locator);
                return elements.size() == count;
            }

            @Override
            public String describe() {
                return "element count of " + locator.describe() + " to be " + count;
            }
        };
    }

    /**
     * Wait for at least a minimum number of elements to be present.
     * @param locator the locator to use
     * @param minCount the minimum count
     * @return condition that returns true when count is at least minCount
     */
    public static ExpectedCondition<Boolean> elementCountAtLeast(By locator, int minCount) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                List<GuiElement> elements = driver.findElements(locator);
                return elements.size() >= minCount;
            }

            @Override
            public String describe() {
                return "element count of " + locator.describe() + " to be at least " + minCount;
            }
        };
    }

    // ========== Boolean Conditions ==========

    /**
     * Negate a condition.
     * @param condition the condition to negate
     * @return negated condition
     */
    public static ExpectedCondition<Boolean> not(ExpectedCondition<?> condition) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                Object result = condition.apply(driver);
                if (result == null) {
                    return true;
                }
                if (result instanceof Boolean) {
                    return !(Boolean) result;
                }
                return false;
            }

            @Override
            public String describe() {
                return "not(" + condition.describe() + ")";
            }
        };
    }

    /**
     * Combine conditions with AND logic.
     * @param conditions the conditions to combine
     * @return combined condition
     */
    @SafeVarargs
    public static ExpectedCondition<Boolean> and(ExpectedCondition<Boolean>... conditions) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                for (ExpectedCondition<Boolean> condition : conditions) {
                    Boolean result = condition.apply(driver);
                    if (result == null || !result) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String describe() {
                StringBuilder sb = new StringBuilder("and(");
                for (int i = 0; i < conditions.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(conditions[i].describe());
                }
                return sb.append(")").toString();
            }
        };
    }

    /**
     * Combine conditions with OR logic.
     * @param conditions the conditions to combine
     * @return combined condition
     */
    @SafeVarargs
    public static ExpectedCondition<Boolean> or(ExpectedCondition<Boolean>... conditions) {
        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(GuiDriver driver) {
                for (ExpectedCondition<Boolean> condition : conditions) {
                    Boolean result = condition.apply(driver);
                    if (result != null && result) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String describe() {
                StringBuilder sb = new StringBuilder("or(");
                for (int i = 0; i < conditions.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(conditions[i].describe());
                }
                return sb.append(")").toString();
            }
        };
    }
}
