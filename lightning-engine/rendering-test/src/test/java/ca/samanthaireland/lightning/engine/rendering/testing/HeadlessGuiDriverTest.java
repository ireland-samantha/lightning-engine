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

import ca.samanthaireland.lightning.engine.rendering.render2d.Button;
import ca.samanthaireland.lightning.engine.rendering.render2d.ComponentFactory;
import ca.samanthaireland.lightning.engine.rendering.render2d.Label;
import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeView;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import ca.samanthaireland.lightning.engine.rendering.testing.headless.HeadlessComponentFactory;
import ca.samanthaireland.lightning.engine.rendering.testing.headless.HeadlessWindow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Headless unit tests for the GUI test framework.
 *
 * <p>These tests demonstrate the framework works without OpenGL or any
 * rendering context. They run fast and can be executed in CI/CD pipelines
 * without a display.
 */
@DisplayName("Headless GUI Driver Tests")
class HeadlessGuiDriverTest {

    private HeadlessWindow window;
    private ComponentFactory factory;
    private GuiDriver driver;

    @BeforeEach
    void setUp() {
        window = new HeadlessWindow(800, 600, "Test Window");
        factory = HeadlessComponentFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }

    @Nested
    @DisplayName("Button Interaction Tests")
    class ButtonTests {

        @Test
        @DisplayName("Click button triggers onClick handler")
        void clickButton_triggersOnClick() {
            // Arrange
            AtomicBoolean clicked = new AtomicBoolean(false);
            Button button = factory.createButton(100, 100, 150, 40, "Save");
            button.setOnClick(() -> clicked.set(true));
            window.addComponent((WindowComponent) button);

            driver = GuiDriver.connect(window);

            // Act
            driver.findElement(By.text("Save")).click();

            // Assert
            assertThat(clicked.get()).isTrue();
        }

        @Test
        @DisplayName("Click button by ID")
        void clickButton_byId() {
            // Arrange
            AtomicBoolean clicked = new AtomicBoolean(false);
            Button button = factory.createButton(100, 100, 150, 40, "Submit");
            ((WindowComponent) button).setId("submitBtn");
            button.setOnClick(() -> clicked.set(true));
            window.addComponent((WindowComponent) button);

            driver = GuiDriver.connect(window);

            // Act
            driver.findElement(By.id("submitBtn")).click();

            // Assert
            assertThat(clicked.get()).isTrue();
        }

        @Test
        @DisplayName("Find multiple buttons by type")
        void findButtons_byType() {
            // Arrange
            window.addComponent((WindowComponent) factory.createButton(10, 10, 80, 30, "Btn1"));
            window.addComponent((WindowComponent) factory.createButton(100, 10, 80, 30, "Btn2"));
            window.addComponent((WindowComponent) factory.createButton(190, 10, 80, 30, "Btn3"));

            driver = GuiDriver.connect(window);

            // Act
            List<GuiElement> buttons = driver.findElements(By.type(Button.class));

            // Assert
            assertThat(buttons).hasSize(3);
        }

        @Test
        @DisplayName("Get button text")
        void getButtonText() {
            // Arrange
            Button button = factory.createButton(100, 100, 150, 40, "Click Me");
            window.addComponent((WindowComponent) button);

            driver = GuiDriver.connect(window);

            // Act
            String text = driver.findElement(By.type(Button.class)).getText();

            // Assert
            assertThat(text).isEqualTo("Click Me");
        }
    }

    @Nested
    @DisplayName("TextField Interaction Tests")
    class TextFieldTests {

        @Test
        @DisplayName("Type text into focused TextField")
        void typeText_intoTextField() {
            // Arrange
            TextField textField = factory.createTextField(100, 100, 200, 30);
            ((WindowComponent) textField).setId("nameField");
            window.addComponent((WindowComponent) textField);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement field = driver.findElement(By.id("nameField"));
            field.click(); // Focus
            field.type("Hello World");

            // Assert
            assertThat(textField.getText()).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Clear TextField")
        void clearTextField() {
            // Arrange
            TextField textField = factory.createTextField(100, 100, 200, 30);
            textField.setText("Initial text");
            window.addComponent((WindowComponent) textField);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement field = driver.findElement(By.type(TextField.class));
            field.click();
            field.clear();

            // Assert
            assertThat(textField.getText()).isEmpty();
        }

        @Test
        @DisplayName("TextField onChange handler is called")
        void textFieldOnChange_isCalled() {
            // Arrange
            AtomicInteger changeCount = new AtomicInteger(0);
            TextField textField = factory.createTextField(100, 100, 200, 30);
            textField.setOnChange(changeCount::incrementAndGet);
            window.addComponent((WindowComponent) textField);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement field = driver.findElement(By.type(TextField.class));
            field.click();
            field.type("ABC");

            // Assert
            assertThat(changeCount.get()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Label Tests")
    class LabelTests {

        @Test
        @DisplayName("Find label by text")
        void findLabel_byText() {
            // Arrange
            Label label = factory.createLabel(100, 100, "Welcome Message");
            window.addComponent((WindowComponent) label);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement element = driver.findElement(By.text("Welcome Message"));

            // Assert
            assertThat(element).isNotNull();
            assertThat(element.getText()).isEqualTo("Welcome Message");
        }

        @Test
        @DisplayName("Find label by partial text")
        void findLabel_byTextContaining() {
            // Arrange
            Label label = factory.createLabel(100, 100, "Status: Connected");
            window.addComponent((WindowComponent) label);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement element = driver.findElement(By.textContaining("Connected"));

            // Assert
            assertThat(element).isNotNull();
        }
    }

    @Nested
    @DisplayName("Panel and Hierarchy Tests")
    class PanelTests {

        @Test
        @DisplayName("Find components inside panel")
        void findComponentsInPanel() {
            // Arrange
            Panel panel = factory.createPanel(10, 10, 400, 300);
            Button button = factory.createButton(20, 30, 100, 30, "Panel Button");
            panel.addChild((WindowComponent) button);
            window.addComponent(panel);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement element = driver.findElement(By.text("Panel Button"));

            // Assert
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Find panel by title")
        void findPanel_byTitle() {
            // Arrange
            Panel panel = factory.createPanel(10, 10, 400, 300);
            panel.setTitle("Settings Panel");
            window.addComponent(panel);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement element = driver.findElement(By.title("Settings Panel"));

            // Assert
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Click button inside nested panel")
        void clickButton_inNestedPanel() {
            // Arrange
            AtomicBoolean clicked = new AtomicBoolean(false);

            Panel outerPanel = factory.createPanel(10, 10, 600, 400);
            Panel innerPanel = factory.createPanel(20, 50, 300, 200);
            Button button = factory.createButton(30, 60, 100, 30, "Nested Button");
            button.setOnClick(() -> clicked.set(true));

            innerPanel.addChild((WindowComponent) button);
            outerPanel.addChild(innerPanel);
            window.addComponent(outerPanel);

            driver = GuiDriver.connect(window);

            // Act
            driver.findElement(By.text("Nested Button")).click();

            // Assert
            assertThat(clicked.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("TreeView Tests")
    class TreeViewTests {

        @Test
        @DisplayName("Create and navigate tree structure")
        void treeView_navigation() {
            // Arrange
            TreeView tree = factory.createTreeView(10, 10, 300, 400);
            ((WindowComponent) tree).setId("fileTree");

            TreeNode root = factory.createTreeNode("Root");
            TreeNode child1 = factory.createTreeNode("Child 1");
            TreeNode child2 = factory.createTreeNode("Child 2");
            TreeNode grandchild = factory.createTreeNode("Grandchild");

            child1.addChild(grandchild);
            root.addChild(child1);
            root.addChild(child2);
            tree.addRootNode(root);

            window.addComponent((WindowComponent) tree);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement treeElement = driver.findElement(By.id("fileTree"));

            // Assert
            assertThat(treeElement).isNotNull();
            assertThat(tree.getRootNodes()).hasSize(1);
            assertThat(tree.getRootNodes().get(0).getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("TreeView selection handler")
        void treeView_selectionHandler() {
            // Arrange
            AtomicReference<TreeNode> selectedNode = new AtomicReference<>();
            TreeView tree = factory.createTreeView(10, 10, 300, 400);

            TreeNode node1 = factory.createTreeNode("Node 1");
            TreeNode node2 = factory.createTreeNode("Node 2");
            tree.addRootNode(node1);
            tree.addRootNode(node2);

            tree.setOnSelect(selectedNode::set);

            window.addComponent((WindowComponent) tree);

            driver = GuiDriver.connect(window);

            // Act - simulate click on second node (row 1)
            window.simulateMouseClick(20, 10 + 24, 0, 1); // Click on node2

            // Assert
            assertThat(selectedNode.get()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Wait and ExpectedConditions Tests")
    class WaitTests {

        @Test
        @DisplayName("Wait for element to be visible")
        void waitForVisibility() {
            // Arrange
            Button button = factory.createButton(100, 100, 150, 40, "Delayed Button");
            ((WindowComponent) button).setVisible(false);
            window.addComponent((WindowComponent) button);

            driver = GuiDriver.connect(window);

            // Schedule visibility change
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    ((WindowComponent) button).setVisible(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // Act & Assert
            GuiElement element = driver.waitFor(Duration.ofSeconds(2))
                .until(ExpectedConditions.visibilityOf(By.text("Delayed Button")));

            assertThat(element).isNotNull();
            assertThat(element.isVisible()).isTrue();
        }

        @Test
        @DisplayName("Wait for text to change")
        void waitForTextChange() {
            // Arrange
            Label label = factory.createLabel(100, 100, "Loading...");
            ((WindowComponent) label).setId("status");
            window.addComponent((WindowComponent) label);

            driver = GuiDriver.connect(window);

            // Schedule text change
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    label.setText("Complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // Act & Assert
            Boolean result = driver.waitFor(Duration.ofSeconds(2))
                .until(ExpectedConditions.textToBe(By.id("status"), "Complete"));

            assertThat(result).isTrue();
            assertThat(label.getText()).isEqualTo("Complete");
        }

        @Test
        @DisplayName("Timeout throws exception")
        void waitTimeout_throwsException() {
            // Arrange
            driver = GuiDriver.connect(window);

            // Act & Assert
            assertThatThrownBy(() ->
                driver.waitFor(Duration.ofMillis(200))
                    .pollingEvery(Duration.ofMillis(50))
                    .until(ExpectedConditions.presenceOf(By.id("nonexistent")))
            ).isInstanceOf(TimeoutException.class);
        }
    }

    @Nested
    @DisplayName("Locator Strategy Tests")
    class LocatorTests {

        @Test
        @DisplayName("Compound locator with AND")
        void compoundLocator_and() {
            // Arrange
            Button button1 = factory.createButton(10, 10, 100, 30, "Save");
            Button button2 = factory.createButton(120, 10, 100, 30, "Save");
            ((WindowComponent) button2).setId("saveBtn2");
            window.addComponent((WindowComponent) button1);
            window.addComponent((WindowComponent) button2);

            driver = GuiDriver.connect(window);

            // Act - find button with both "Save" text AND specific ID
            GuiElement element = driver.findElement(
                By.and(By.text("Save"), By.id("saveBtn2"))
            );

            // Assert
            assertThat(element).isNotNull();
            assertThat(element.getComponent()).isSameAs(button2);
        }

        @Test
        @DisplayName("Compound locator with OR")
        void compoundLocator_or() {
            // Arrange
            Button button1 = factory.createButton(10, 10, 100, 30, "Save");
            ((WindowComponent) button1).setId("saveBtn");
            Button button2 = factory.createButton(120, 10, 100, 30, "Cancel");
            ((WindowComponent) button2).setId("cancelBtn");
            window.addComponent((WindowComponent) button1);
            window.addComponent((WindowComponent) button2);

            driver = GuiDriver.connect(window);

            // Act - find either button
            List<GuiElement> elements = driver.findElements(
                By.or(By.id("saveBtn"), By.id("cancelBtn"))
            );

            // Assert
            assertThat(elements).hasSize(2);
        }

        @Test
        @DisplayName("NoSuchElementException for missing element")
        void noSuchElement_throwsException() {
            // Arrange
            driver = GuiDriver.connect(window);

            // Act & Assert
            assertThatThrownBy(() ->
                driver.findElement(By.id("nonexistent"))
            ).isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("Actions Builder Tests")
    class ActionsTests {

        @Test
        @DisplayName("Actions builder performs click")
        void actionsBuilder_click() {
            // Arrange
            AtomicBoolean clicked = new AtomicBoolean(false);
            Button button = factory.createButton(100, 100, 150, 40, "Action Button");
            button.setOnClick(() -> clicked.set(true));
            window.addComponent((WindowComponent) button);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement element = driver.findElement(By.text("Action Button"));
            driver.actions()
                .moveToElement(element)
                .click()
                .perform();

            // Assert
            assertThat(clicked.get()).isTrue();
        }

        @Test
        @DisplayName("Actions builder sends keys")
        void actionsBuilder_sendKeys() {
            // Arrange
            TextField textField = factory.createTextField(100, 100, 200, 30);
            window.addComponent((WindowComponent) textField);

            driver = GuiDriver.connect(window);

            // Act
            GuiElement field = driver.findElement(By.type(TextField.class));
            driver.actions()
                .click(field)
                .sendKeys("Test Input")
                .perform();

            // Assert
            assertThat(textField.getText()).isEqualTo("Test Input");
        }
    }

    @Nested
    @DisplayName("Component Tree Debugging")
    class DebuggingTests {

        @Test
        @DisplayName("Dump component tree")
        void dumpComponentTree() {
            // Arrange
            Panel panel = factory.createPanel(10, 10, 400, 300);
            panel.setTitle("Main Panel");
            Button button = factory.createButton(20, 50, 100, 30, "Click");
            ((WindowComponent) button).setId("clickBtn");
            panel.addChild((WindowComponent) button);
            window.addComponent(panel);

            driver = GuiDriver.connect(window);

            // Act
            String dump = driver.dumpComponentTree();

            // Assert
            assertThat(dump).contains("MockPanel");
            assertThat(dump).contains("MockButton");
            assertThat(dump).contains("#clickBtn");
            assertThat(dump).contains("\"Click\"");
        }
    }

    @Nested
    @DisplayName("Real-World Scenario Tests")
    class ScenarioTests {

        @Test
        @DisplayName("Login form scenario")
        void loginFormScenario() {
            // Arrange - Create a login form
            Panel loginPanel = factory.createPanel(200, 150, 400, 300);
            loginPanel.setTitle("Login");

            Label usernameLabel = factory.createLabel(220, 200, "Username:");
            TextField usernameField = factory.createTextField(220, 220, 200, 30);
            ((WindowComponent) usernameField).setId("username");

            Label passwordLabel = factory.createLabel(220, 260, "Password:");
            TextField passwordField = factory.createTextField(220, 280, 200, 30);
            ((WindowComponent) passwordField).setId("password");

            AtomicReference<String> submittedUser = new AtomicReference<>();
            AtomicReference<String> submittedPass = new AtomicReference<>();

            Button loginButton = factory.createButton(220, 330, 100, 35, "Login");
            ((WindowComponent) loginButton).setId("loginBtn");
            loginButton.setOnClick(() -> {
                submittedUser.set(usernameField.getText());
                submittedPass.set(passwordField.getText());
            });

            loginPanel.addChild((WindowComponent) usernameLabel);
            loginPanel.addChild((WindowComponent) usernameField);
            loginPanel.addChild((WindowComponent) passwordLabel);
            loginPanel.addChild((WindowComponent) passwordField);
            loginPanel.addChild((WindowComponent) loginButton);

            window.addComponent(loginPanel);

            driver = GuiDriver.connect(window);

            // Act - Fill and submit the form
            driver.findElement(By.id("username")).click();
            driver.findElement(By.id("username")).type("testuser");

            driver.findElement(By.id("password")).click();
            driver.findElement(By.id("password")).type("secret123");

            driver.findElement(By.id("loginBtn")).click();

            // Assert
            assertThat(submittedUser.get()).isEqualTo("testuser");
            assertThat(submittedPass.get()).isEqualTo("secret123");
        }

        @Test
        @DisplayName("Navigation panel scenario")
        void navigationScenario() {
            // Arrange - Create navigation with multiple panels
            AtomicReference<String> activePanel = new AtomicReference<>("home");

            Button homeBtn = factory.createButton(10, 10, 80, 30, "Home");
            homeBtn.setOnClick(() -> activePanel.set("home"));
            ((WindowComponent) homeBtn).setId("homeBtn");

            Button settingsBtn = factory.createButton(100, 10, 80, 30, "Settings");
            settingsBtn.setOnClick(() -> activePanel.set("settings"));
            ((WindowComponent) settingsBtn).setId("settingsBtn");

            Button aboutBtn = factory.createButton(190, 10, 80, 30, "About");
            aboutBtn.setOnClick(() -> activePanel.set("about"));
            ((WindowComponent) aboutBtn).setId("aboutBtn");

            window.addComponent((WindowComponent) homeBtn);
            window.addComponent((WindowComponent) settingsBtn);
            window.addComponent((WindowComponent) aboutBtn);

            driver = GuiDriver.connect(window);

            // Act & Assert
            assertThat(activePanel.get()).isEqualTo("home");

            driver.findElement(By.id("settingsBtn")).click();
            assertThat(activePanel.get()).isEqualTo("settings");

            driver.findElement(By.id("aboutBtn")).click();
            assertThat(activePanel.get()).isEqualTo("about");

            driver.findElement(By.id("homeBtn")).click();
            assertThat(activePanel.get()).isEqualTo("home");
        }

        @Test
        @DisplayName("Counter with increment/decrement")
        void counterScenario() {
            // Arrange
            AtomicInteger counter = new AtomicInteger(0);
            Label countLabel = factory.createLabel(150, 100, "Count: 0");
            ((WindowComponent) countLabel).setId("countLabel");

            Button incrementBtn = factory.createButton(100, 130, 40, 30, "+");
            incrementBtn.setOnClick(() -> {
                counter.incrementAndGet();
                countLabel.setText("Count: " + counter.get());
            });

            Button decrementBtn = factory.createButton(150, 130, 40, 30, "-");
            decrementBtn.setOnClick(() -> {
                counter.decrementAndGet();
                countLabel.setText("Count: " + counter.get());
            });

            window.addComponent((WindowComponent) countLabel);
            window.addComponent((WindowComponent) incrementBtn);
            window.addComponent((WindowComponent) decrementBtn);

            driver = GuiDriver.connect(window);

            // Act
            driver.findElement(By.text("+")).click();
            driver.findElement(By.text("+")).click();
            driver.findElement(By.text("+")).click();
            driver.findElement(By.text("-")).click();

            // Assert
            assertThat(counter.get()).isEqualTo(2);
            assertThat(countLabel.getText()).isEqualTo("Count: 2");
        }
    }
}
