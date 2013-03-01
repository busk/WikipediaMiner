package org.wikipedia.miner.service.param;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.service.ServiceHub;

/**
 * @author dmilne
 *
 * @param <T>
 */
public class EnumSetParameter<T extends Enum<T>> extends Parameter<EnumSet<T>> {

	
	private T[] values ;
	private String[] valueDescriptions ;
	
	private HashMap<String, T> valuesByName ;
	
	public EnumSetParameter(
			String name, 
			String description,
			EnumSet<T> defaultValue,
			T[] allValues, 
			String[] valueDescriptions
	) {
		super(name, description, defaultValue, "enum list");
		
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
	public EnumSet<T> getValue(HttpServletRequest request) throws IllegalArgumentException {
		
		String allVals = request.getParameter(getName()) ;
		
		if (allVals == null)		
			return getDefaultValue();
		
		ArrayList<T> _enums = new ArrayList<T>() ;
		for (String val:allVals.split("[,;:]")) {
			
			T _enum = valuesByName.get(val.trim().toLowerCase()) ;
			
			if (_enum != null) {
				_enums.add(_enum) ;
			}
		}

		return EnumSet.copyOf(_enums) ;
	}




}
