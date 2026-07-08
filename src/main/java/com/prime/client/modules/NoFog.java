package com.prime.client.modules;

import com.prime.client.api.Module;

/**
 * NoFog module: removes distance and liquid fog via a Mixin on
 * BackgroundRenderer. The actual fog suppression happens in
 * {@link com.prime.client.mixins.BackgroundRendererMixin}.
 * This class just registers the module in the manager.
 */
public class NoFog implements Module {
    @Override
    public String getName() {
        return "NoFog";
    }

    @Override
    public void init() {
        // All logic is in BackgroundRendererMixin, which reads the config flag.
    }
}
