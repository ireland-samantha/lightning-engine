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

package ca.samanthaireland.game.orchestrator;

import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of AIOperations that delegates to ContainerScope.
 * Follows Dependency Inversion Principle - depends on abstraction (ContainerScope).
 */
public class ContainerAIOperations implements AIOperations {

    private final ContainerAdapter.ContainerScope scope;

    public ContainerAIOperations(ContainerAdapter.ContainerScope scope) {
        this.scope = scope;
    }

    @Override
    public boolean hasAI(String aiName) throws IOException {
        return scope.listAI().contains(aiName);
    }

    @Override
    public List<String> listAI() throws IOException {
        return scope.listAI();
    }

    @Override
    public void uploadAI(String fileName, byte[] data) throws IOException {
        // AI upload is not container-scoped in current API
        // This would need to be added to ContainerScope or handled differently
        throw new UnsupportedOperationException("AI upload not supported via container scope");
    }

    @Override
    public void uninstallAI(String aiName) throws IOException {
        // AI uninstall is not container-scoped in current API
        throw new UnsupportedOperationException("AI uninstall not supported via container scope");
    }
}
