package tw.com.aitc.sample;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.Registry;

import java.util.Map;

public class RegistryUtils {

	public static void addRegistry(CamelContext context, String key, Object value) {
		SimpleRegistry registry = getRegistry(context);
		registry.put(key, value);
		((DefaultCamelContext) context).setRegistry(registry);
	}

	public static void addAllRegistry(CamelContext context, Map<String, Object> map) {
		SimpleRegistry registry = getRegistry(context);
		registry.putAll(map);
		((DefaultCamelContext) context).setRegistry(registry);
	}

	private static SimpleRegistry getRegistry(CamelContext context) {
		Registry camelRegistry = context.getRegistry();
		if (camelRegistry instanceof SimpleRegistry) {
			return (SimpleRegistry) camelRegistry;
		}
		else {
			SimpleRegistry registry = new SimpleRegistry();
			registry.putAll(camelRegistry.findByTypeWithName(Object.class));
			((DefaultCamelContext) context).setRegistry(registry);
			return registry;
		}
	}
}