package org.wikipedia.miner.service.param;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.service.ServiceHub;

/**
 * A Parameter that expects enum values. The values must match the enum
 * @param <T> the expected enum type
 */
public class EnumParameter<T extends Enum<T>> extends Parameter<T> {

	
	private T[] values ;
	private String[] valueDescriptions ;
	
	private HashMap<String, T> valuesByName ;
	
	/**
	 * Initialises a new EnumParameter
	 * 
	 * @param name the name of the parameter
	 * @param description a short description of what this parameter does
	 * @param defaultValue the value to use when requests do not specify a value for this parameter (may be null)
	 * @param allValues a list of all values for this enum
	 * @param valueDescriptions an array of short descriptions for each possible value 
	 */
	public EnumParameter(String name, String description, T defaultValue, T[] allValues, String[] valueDescriptions) {
		super(name, description, defaultValue, "enum");
		
		if (allValues.length != valueDescriptions.length) 
			throw new IllegalArgumentException("the number of values and valueDescriptions does not match!") ;
		
		valuesByName = new HashMap<String, T>() ;
		for (T val:allValues) {
			valuesByName.put(val.name().toLowerCase(), val) ;
		}
		
		this.values = allValues ;
		this.valueDescriptions = valueDescriptions ;
	}
	
	@Override
	public Element getXmlDescription(ServiceHub hub) {
		Element xml = super.getXmlDescription(hub) ;
		
		for (int i=0 ; i<values.length ; i++) {
			Element xmlVal = hub.createElement("PossibleValue") ;
			xmlVal.setAttribute("name", values[i].name()) ;
			xmlVal.setAttribute("description", valueDescriptions[i]) ;
			
			xml.appendChild(xmlVal) ;
		}
		
		return xml ;
	}

	@Override
	public T getValue(HttpServletRequest request) throws IllegalArgumentException{
		
		String s = request.getParameter(getName()) ;
		
		if (s == null)
			return getDefaultValue() ;
		
		T val = valuesByName.get(s.trim().toLowerCase()) ;
		
		if (val == null)
			throw new IllegalArgumentException("Invalid value for " + getName() + " parameter") ;
		
		return val ;
		
		
		
	}

}
