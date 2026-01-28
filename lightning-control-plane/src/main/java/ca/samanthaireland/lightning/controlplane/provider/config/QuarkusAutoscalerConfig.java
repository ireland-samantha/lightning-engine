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

package ca.samanthaireland.lightning.controlplane.provider.config;

import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Quarkus configuration mapping for the autoscaler.
 *
 * <p>This interface extends the core {@link AutoscalerConfiguration} to provide
 * framework-specific configuration binding via Quarkus/SmallRye Config.
 */
@ConfigMapping(prefix = "autoscaler")
public interface QuarkusAutoscalerConfig extends AutoscalerConfiguration {

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("enabled")
    @WithDefault("true")
    boolean enabled();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("scale-up-threshold")
    @WithDefault("0.8")
    double scaleUpThreshold();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("scale-down-threshold")
    @WithDefault("0.3")
    double scaleDownThreshold();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("min-nodes")
    @WithDefault("1")
    int minNodes();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("max-nodes")
    @WithDefault("100")
    int maxNodes();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("cooldown-seconds")
    @WithDefault("300")
    int cooldownSeconds();

    /**
     * {@inheritDoc}
     */
    @Override
    @WithName("target-saturation")
    @WithDefault("0.6")
    double targetSaturation();
}
