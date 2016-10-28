package org.deri.grefine.reconcile.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author fadmaa
 * complex reconciliation requests will use additional properties to make reconciliation more precise... this class is used to represent these additional
 * properties aka context
 */
public class ReconciliationRequestContext {

	private final Set<PropertyContext> properties;
	
	public ReconciliationRequestContext(Set<PropertyContext> props){
		this.properties = props;
	}	
	
	public ReconciliationRequestContext(PropertyContext... prop){
		this.properties = new LinkedHashSet<ReconciliationRequestContext.PropertyContext>(Arrays.asList(prop));
	}
	
	public Set<PropertyContext> getProperties() {
		return properties;
	}

	/**
	 * @author fadmaa
	 * A property id and its value
	 */
	public static class PropertyContext{
		private final String pid;
		private final ValueContext v;

		public PropertyContext(String pid, ValueContext v) {
			this.pid = pid;
			this.v = v;
		}
		
		public PropertyContext(String pid, String v) {
			this.pid = pid;
			this.v = new TextualValueContext(v);
		}

		public String getPid() {
			return pid;
		}

		public ValueContext getV() {
			return v;
		}
		
	}
	
	public static interface ValueContext{
		public String asSparqlValue();
		public boolean isIRI();
	}
	
	public static class IdentifiedValueContext implements ValueContext{
		private final String id;

		public String getId() {
			return id;
		}
		
		public String toString(){
			return id;
		}
		
		public IdentifiedValueContext(String id){
			this.id = id;
		}
		
		public String asSparqlValue(){
			return "<" + id + ">";
		}
		
		public boolean isIRI(){
			return true;
		}
	}
	
	public static class TextualValueContext implements ValueContext{
		private final String text;

		public String getText() {
			return text;
		}
		
		public String asSparqlValue(){
			return "'" + text + "'";
		}
		
		public TextualValueContext(String text){
			this.text = text;
		}
		
		public boolean isIRI(){
			return false;
		}
		
		public String toString(){
			return text;
		}
	}
}
