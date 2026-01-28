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

import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLButton;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLPanel;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTreeView;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for button click handling.
 */
@Slf4j
class GLButtonClickTest {

    @Test
    void button_contains_worksCorrectly() {
        GLButton button = new GLButton(100, 200, 80, 30, "Test");

        // Inside bounds
        assertThat(button.contains(100, 200)).isTrue();  // top-left corner
        assertThat(button.contains(179, 229)).isTrue();  // bottom-right (exclusive boundary)
        assertThat(button.contains(140, 215)).isTrue();  // center

        // Outside bounds
        assertThat(button.contains(99, 200)).isFalse();   // left of button
        assertThat(button.contains(180, 200)).isFalse();  // right of button
        assertThat(button.contains(100, 199)).isFalse();  // above button
        assertThat(button.contains(100, 230)).isFalse();  // below button
    }

    @Test
    void button_directClick_triggersCallback() {
        GLButton button = new GLButton(50, 50, 100, 40, "Click Me");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Click at center of button
        int centerX = 50 + 50;  // 100
        int centerY = 50 + 20;  // 70

        // Press
        boolean pressHandled = button.onMouseClick(centerX, centerY, 0, 1);
        assertThat(pressHandled).isTrue();

        // Release
        boolean releaseHandled = button.onMouseClick(centerX, centerY, 0, 0);
        assertThat(releaseHandled).isTrue();

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void button_clickOutside_doesNotTrigger() {
        GLButton button = new GLButton(50, 50, 100, 40, "Click Me");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Click outside button
        button.onMouseClick(10, 10, 0, 1);  // Press outside
        button.onMouseClick(10, 10, 0, 0);  // Release outside

        assertThat(clickCount.get()).isZero();
    }

    @Test
    void button_pressInsideReleaseOutside_doesNotTrigger() {
        GLButton button = new GLButton(50, 50, 100, 40, "Click Me");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Press inside
        button.onMouseClick(100, 70, 0, 1);

        // Move outside (this resets pressed state)
        button.onMouseMove(200, 200);

        // Release outside
        button.onMouseClick(200, 200, 0, 0);

        assertThat(clickCount.get()).isZero();
    }

    @Test
    void panel_forwardsClickToChildButton() {
        GLPanel panel = new GLPanel(0, 0, 300, 200);
        GLButton button = new GLButton(50, 50, 100, 40, "Child Button");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);
        panel.addChild(button);

        // Click at button center (absolute coords)
        int centerX = 100;  // 50 + 50
        int centerY = 70;   // 50 + 20

        // Press
        panel.onMouseClick(centerX, centerY, 0, 1);
        // Release
        panel.onMouseClick(centerX, centerY, 0, 0);

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void panel_withTitle_forwardsClickToChildButton() {
        GLPanel panel = new GLPanel(10, 20, 300, 200);
        panel.setTitle("Test Panel");

        // Button at absolute position inside panel
        GLButton button = new GLButton(20, 70, 100, 40, "Child Button");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);
        panel.addChild(button);

        // Click at button center (absolute coords)
        int centerX = 70;   // 20 + 50
        int centerY = 90;   // 70 + 20

        // Press
        panel.onMouseClick(centerX, centerY, 0, 1);
        // Release
        panel.onMouseClick(centerX, centerY, 0, 0);

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void nestedPanels_forwardClickToButton() {
        GLPanel outerPanel = new GLPanel(0, 0, 400, 300);
        GLPanel innerPanel = new GLPanel(50, 50, 300, 200);
        GLButton button = new GLButton(100, 100, 100, 40, "Nested Button");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        innerPanel.addChild(button);
        outerPanel.addChild(innerPanel);

        // Click at button center
        int centerX = 150;  // 100 + 50
        int centerY = 120;  // 100 + 20

        // Press
        outerPanel.onMouseClick(centerX, centerY, 0, 1);
        // Release
        outerPanel.onMouseClick(centerX, centerY, 0, 0);

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void button_rightClick_doesNotTrigger() {
        GLButton button = new GLButton(50, 50, 100, 40, "Click Me");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Right click (button 1)
        button.onMouseClick(100, 70, 1, 1);
        button.onMouseClick(100, 70, 1, 0);

        assertThat(clickCount.get()).isZero();
    }

    @Test
    void button_invisibleButton_doesNotReceiveClicks() {
        GLButton button = new GLButton(50, 50, 100, 40, "Hidden");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);
        button.setVisible(false);

        button.onMouseClick(100, 70, 0, 1);
        button.onMouseClick(100, 70, 0, 0);

        assertThat(clickCount.get()).isZero();
    }

    @Test
    void multipleButtons_onlyClickedOneTriggered() {
        GLPanel panel = new GLPanel(0, 0, 300, 200);

        GLButton button1 = new GLButton(10, 10, 80, 30, "Button 1");
        GLButton button2 = new GLButton(100, 10, 80, 30, "Button 2");
        GLButton button3 = new GLButton(190, 10, 80, 30, "Button 3");

        AtomicInteger click1 = new AtomicInteger(0);
        AtomicInteger click2 = new AtomicInteger(0);
        AtomicInteger click3 = new AtomicInteger(0);

        button1.setOnClick(click1::incrementAndGet);
        button2.setOnClick(click2::incrementAndGet);
        button3.setOnClick(click3::incrementAndGet);

        panel.addChild(button1);
        panel.addChild(button2);
        panel.addChild(button3);

        // Click button 2
        panel.onMouseClick(140, 25, 0, 1);
        panel.onMouseClick(140, 25, 0, 0);

        assertThat(click1.get()).isZero();
        assertThat(click2.get()).isEqualTo(1);
        assertThat(click3.get()).isZero();
    }

    @Test
    void snapshotPanelLayout_buttonsClickable() {
        // Simulate SnapshotPanel button layout
        int panelX = 10;
        int panelY = 60;
        int panelWidth = 585;
        int panelHeight = 730;

        GLPanel panel = new GLPanel(panelX, panelY, panelWidth, panelHeight);
        panel.setTitle("Snapshot Viewer - Match 1");

        // Button positions as in SnapshotPanel
        GLButton connectButton = new GLButton(panelX + 10, panelY + 55, 100, 28, "Connect");
        GLButton refreshButton = new GLButton(panelX + 120, panelY + 55, 100, 28, "Refresh");

        AtomicInteger connectClicks = new AtomicInteger(0);
        AtomicInteger refreshClicks = new AtomicInteger(0);

        connectButton.setOnClick(connectClicks::incrementAndGet);
        refreshButton.setOnClick(refreshClicks::incrementAndGet);

        panel.addChild(connectButton);
        panel.addChild(refreshButton);

        // Click connect button center
        // Button at (20, 115) with size (100, 28)
        // Center at (70, 129)
        int connectCenterX = panelX + 10 + 50;  // 70
        int connectCenterY = panelY + 55 + 14;  // 129

        log.info("Connect button: x=" + connectButton.getX() + ", y=" + connectButton.getY());
        log.info("Click position: x=" + connectCenterX + ", y=" + connectCenterY);
        log.info("Contains check: " + connectButton.contains(connectCenterX, connectCenterY));

        panel.onMouseClick(connectCenterX, connectCenterY, 0, 1);
        panel.onMouseClick(connectCenterX, connectCenterY, 0, 0);

        assertThat(connectClicks.get()).as("Connect button should be clicked").isEqualTo(1);
        assertThat(refreshClicks.get()).as("Refresh button should not be clicked").isZero();

        // Click refresh button center
        int refreshCenterX = panelX + 120 + 50;  // 180
        int refreshCenterY = panelY + 55 + 14;   // 129

        panel.onMouseClick(refreshCenterX, refreshCenterY, 0, 1);
        panel.onMouseClick(refreshCenterX, refreshCenterY, 0, 0);

        assertThat(refreshClicks.get()).as("Refresh button should be clicked").isEqualTo(1);
    }

    @Test
    void resourcePanelLayout_buttonsClickable() {
        // Simulate ResourcePanel button layout
        int panelX = 605;
        int panelY = 60;
        int panelWidth = 585;
        int panelHeight = 730;

        GLPanel panel = new GLPanel(panelX, panelY, panelWidth, panelHeight);
        panel.setTitle("Resource Manager");

        int buttonY = panelY + 55;
        int buttonWidth = 80;
        int buttonSpacing = 10;

        GLButton refreshButton = new GLButton(panelX + 10, buttonY, buttonWidth, 28, "Refresh");
        GLButton uploadButton = new GLButton(panelX + 10 + buttonWidth + buttonSpacing, buttonY, buttonWidth, 28, "Upload");
        GLButton downloadButton = new GLButton(panelX + 10 + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, 28, "Download");
        GLButton deleteButton = new GLButton(panelX + 10 + (buttonWidth + buttonSpacing) * 3, buttonY, buttonWidth, 28, "Delete");

        AtomicInteger refreshClicks = new AtomicInteger(0);
        AtomicInteger uploadClicks = new AtomicInteger(0);
        AtomicInteger downloadClicks = new AtomicInteger(0);
        AtomicInteger deleteClicks = new AtomicInteger(0);

        refreshButton.setOnClick(refreshClicks::incrementAndGet);
        uploadButton.setOnClick(uploadClicks::incrementAndGet);
        downloadButton.setOnClick(downloadClicks::incrementAndGet);
        deleteButton.setOnClick(deleteClicks::incrementAndGet);

        panel.addChild(refreshButton);
        panel.addChild(uploadButton);
        panel.addChild(downloadButton);
        panel.addChild(deleteButton);

        // Click upload button
        int uploadCenterX = uploadButton.getX() + buttonWidth / 2;
        int uploadCenterY = uploadButton.getY() + 14;

        panel.onMouseClick(uploadCenterX, uploadCenterY, 0, 1);
        panel.onMouseClick(uploadCenterX, uploadCenterY, 0, 0);

        assertThat(uploadClicks.get()).as("Upload button should be clicked").isEqualTo(1);
        assertThat(refreshClicks.get()).isZero();
        assertThat(downloadClicks.get()).isZero();
        assertThat(deleteClicks.get()).isZero();
    }

    @Test
    void fullApplicationLayout_buttonsClickable() {
        // Simulate full EngineGuiApplication layout
        int windowWidth = 1200;
        int windowHeight = 800;
        int panelMargin = 10;
        int panelY = 60;
        int panelHeight = windowHeight - panelY - panelMargin;
        int leftPanelWidth = (windowWidth - 3 * panelMargin) / 2;
        int rightPanelWidth = windowWidth - leftPanelWidth - 3 * panelMargin;

        // Create snapshot panel (left side)
        GLPanel snapshotPanel = new GLPanel(panelMargin, panelY, leftPanelWidth, panelHeight);
        snapshotPanel.setTitle("Snapshot Viewer - Match 1");

        GLButton connectButton = new GLButton(panelMargin + 10, panelY + 55, 100, 28, "Connect");
        GLButton refreshButton = new GLButton(panelMargin + 120, panelY + 55, 100, 28, "Refresh");
        GLTreeView treeView = new GLTreeView(panelMargin + 10, panelY + 95, leftPanelWidth - 20, panelHeight - 105);

        AtomicInteger connectClicks = new AtomicInteger(0);
        AtomicInteger refreshClicks = new AtomicInteger(0);

        connectButton.setOnClick(connectClicks::incrementAndGet);
        refreshButton.setOnClick(refreshClicks::incrementAndGet);

        snapshotPanel.addChild(connectButton);
        snapshotPanel.addChild(refreshButton);
        snapshotPanel.addChild(treeView);

        // Create resource panel (right side)
        GLPanel resourcePanel = new GLPanel(leftPanelWidth + 2 * panelMargin, panelY, rightPanelWidth, panelHeight);
        resourcePanel.setTitle("Resource Manager");

        int buttonY = panelY + 55;
        int buttonWidth = 80;
        int buttonSpacing = 10;
        int rpX = leftPanelWidth + 2 * panelMargin;

        GLButton rpRefreshButton = new GLButton(rpX + 10, buttonY, buttonWidth, 28, "Refresh");
        GLButton uploadButton = new GLButton(rpX + 10 + buttonWidth + buttonSpacing, buttonY, buttonWidth, 28, "Upload");

        AtomicInteger rpRefreshClicks = new AtomicInteger(0);
        AtomicInteger uploadClicks = new AtomicInteger(0);

        rpRefreshButton.setOnClick(rpRefreshClicks::incrementAndGet);
        uploadButton.setOnClick(uploadClicks::incrementAndGet);

        resourcePanel.addChild(rpRefreshButton);
        resourcePanel.addChild(uploadButton);

        // Simulate component list as in GUIWindow
        List<WindowComponent> components = new ArrayList<>(List.of(snapshotPanel, resourcePanel));

        // Test clicking connect button on snapshot panel
        int clickX = panelMargin + 10 + 50;  // Center of connect button
        int clickY = panelY + 55 + 14;

        // Simulate GUIWindow's mouse handling (reverse order)
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseClick(clickX, clickY, 0, 1)) break;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseClick(clickX, clickY, 0, 0)) break;
        }

        assertThat(connectClicks.get()).as("Connect button on snapshot panel").isEqualTo(1);
        assertThat(refreshClicks.get()).isZero();
        assertThat(rpRefreshClicks.get()).isZero();
        assertThat(uploadClicks.get()).isZero();

        // Test clicking upload button on resource panel
        int uploadX = rpX + 10 + buttonWidth + buttonSpacing + buttonWidth / 2;
        int uploadY = buttonY + 14;

        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseClick(uploadX, uploadY, 0, 1)) break;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseClick(uploadX, uploadY, 0, 0)) break;
        }

        assertThat(uploadClicks.get()).as("Upload button on resource panel").isEqualTo(1);
    }

    @Test
    void buttonWithTreeView_clicksDoNotInterfere() {
        // Test that buttons and tree views in same panel don't interfere
        GLPanel panel = new GLPanel(0, 0, 400, 500);
        panel.setTitle("Test Panel");

        GLButton button = new GLButton(10, 55, 100, 28, "Button");
        GLTreeView treeView = new GLTreeView(10, 95, 380, 395);
        treeView.addRootNode(new GLTreeNode("Root Node"));

        AtomicInteger buttonClicks = new AtomicInteger(0);
        button.setOnClick(buttonClicks::incrementAndGet);

        panel.addChild(button);
        panel.addChild(treeView);

        // Click button (above tree view)
        int buttonCenterX = 60;
        int buttonCenterY = 69;

        // Verify button is at correct position and click is inside
        assertThat(button.contains(buttonCenterX, buttonCenterY)).isTrue();
        assertThat(treeView.contains(buttonCenterX, buttonCenterY)).isFalse();

        // Press and release
        panel.onMouseClick(buttonCenterX, buttonCenterY, 0, 1);
        panel.onMouseClick(buttonCenterX, buttonCenterY, 0, 0);

        assertThat(buttonClicks.get()).isEqualTo(1);

        // Click inside tree view (should not trigger button)
        int treeClickX = 100;
        int treeClickY = 150;

        assertThat(button.contains(treeClickX, treeClickY)).isFalse();
        assertThat(treeView.contains(treeClickX, treeClickY)).isTrue();

        panel.onMouseClick(treeClickX, treeClickY, 0, 1);
        panel.onMouseClick(treeClickX, treeClickY, 0, 0);

        assertThat(buttonClicks.get()).as("Button should not be clicked when clicking tree").isEqualTo(1);
    }

    @Test
    void button_rapidClicks_allRegister() {
        GLButton button = new GLButton(50, 50, 100, 40, "Rapid");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        int centerX = 100;
        int centerY = 70;

        // Simulate rapid clicking
        for (int i = 0; i < 5; i++) {
            button.onMouseClick(centerX, centerY, 0, 1);
            button.onMouseClick(centerX, centerY, 0, 0);
        }

        assertThat(clickCount.get()).isEqualTo(5);
    }

    @Test
    void button_mouseMoveDuringClick_stillWorks() {
        GLButton button = new GLButton(50, 50, 100, 40, "Move Test");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        int centerX = 100;
        int centerY = 70;

        // Press at center
        button.onMouseClick(centerX, centerY, 0, 1);

        // Move slightly but stay inside button
        button.onMouseMove(centerX + 5, centerY + 5);

        // Release still inside button
        button.onMouseClick(centerX + 5, centerY + 5, 0, 0);

        assertThat(clickCount.get()).isEqualTo(1);
    }
}
