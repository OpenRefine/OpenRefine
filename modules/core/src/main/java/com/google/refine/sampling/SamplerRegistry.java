
package com.google.refine.sampling;

import java.util.HashMap;
import java.util.Map;

public class SamplerRegistry {

    static final private Map<String, Sampler> nameToSampler = new HashMap<String, Sampler>();

    static public void registerSampler(String name, Sampler sampler) {
        nameToSampler.put(name.toLowerCase(), sampler);
    }

    static public Sampler getSampler(String name) {
        Sampler sampler = nameToSampler.get(name.toLowerCase());
        if (sampler == null) {
            throw new IllegalArgumentException("Unknown sampling method: " + name);
        }

        return sampler;
    }
}
