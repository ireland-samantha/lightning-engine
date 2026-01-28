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


package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.internal.ext.jar.ModuleFactoryClassLoader;
import ca.samanthaireland.game.backend.installation.AIFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper class to load JAR files and find AIFactory implementations.
 *
 * <p>This class scans JAR files for classes implementing {@link AIFactory}
 * and provides methods to instantiate them. It delegates to the generic
 * {@link ModuleFactoryClassLoader} for the actual JAR loading logic.
 */
@Slf4j
public class AIFactoryFileLoader {

    private final ModuleFactoryClassLoader<AIFactory> jarLoader;

    public AIFactoryFileLoader() {
        this.jarLoader = new ModuleFactoryClassLoader<>(AIFactory.class, "AIFactory");
    }

    /**
     * Load a JAR file and find all AIFactory implementations.
     *
     * @param jarFile the JAR file to load
     * @return list of AIFactory instances found in the JAR
     * @throws IOException if the JAR file cannot be read
     */
    public List<AIFactory> loadAIFactories(File jarFile) throws IOException {
        return jarLoader.loadFactoriesFromJar(jarFile);
    }

    /**
     * Get the name of an AI from its factory.
     *
     * @param factory the AI factory
     * @return the AI name from the factory's getName() method
     */
    public String getAIName(AIFactory factory) {
        return factory.getName();
    }
}
