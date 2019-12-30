package com.google.refine.clustering;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.refine.clustering.binning.BinningClusterer.BinningClustererConfig;

/**
 * Registry where clusterers are registered,
 * to make this extensible.
 * 
 * If you want to implement a new clusterer, it is likely
 * that it is actually a binning or distance-based clusterer,
 * so you might be able to reuse the {@class kNNClusterer} or
 * {@class BinningClusterer} by implementing a {@class ClusteringDistance}
 * or {@class Keyer} instead.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ClustererConfigFactory extends TypeIdResolverBase {
	protected static Map<String, Class<? extends ClustererConfig>> registry =
			new HashMap<>(2);
	
	static {
		register("binning", BinningClustererConfig.class);
	}
	
	protected TypeFactory factory = TypeFactory.defaultInstance();
	
	public static void register(String type, Class<? extends ClustererConfig> configClass) {
		registry.put(type, configClass);
	}

	@Override
	public String idFromValue(Object value) {
		return ((ClustererConfig)value).getType();
	}

	@Override
	public String idFromValueAndType(Object value, Class<?> suggestedType) {
		return ((ClustererConfig)value).getType();
	}

	@Override
	public Id getMechanism() {
		return Id.NAME;
	}
	
    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
    	if (registry.containsKey(id)) {
    		return factory.constructSimpleType(registry.get(id), new JavaType[0]);
    	} else {
    		throw new IOException("Unknown clusterer type: '"+id+"'");
    	}
    }
}
