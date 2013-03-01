package org.wikipedia.miner.service.param;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;
import org.wikipedia.miner.service.ServiceHub;

public class StringArrayParameter extends Parameter<String> {

	
	private String[] values ;
	private String[] valueDescriptions ;
	private HashMap<String, String> valuesByNormalizedValue ;
	
	public StringArrayParameter(String name, String description, String defaultValue, String[] allValues, String[] valueDescriptions) {
		super(name, description, defaultValue, "enum");
		
		valuesByNormalizedValue = new HashMap<String, String>() ;
		for (String val:allValues) {
			valuesByNormalizedValue.put(normalizeValue(val), val) ;
		}
		
		this.values = allValues ;
		this.valueDescriptions = valueDescriptions ;
	}
	
	@Override
	public Element getXmlDescription(ServiceHub hub) {
		Element xml = super.getXmlDescription(hub) ;
		
		for (int i=0 ; i<values.length ; i++) {
			Element xmlVal = hub.createElement("PossibleValue") ;
			xmlVal.setAttribute("name", values[i]) ;
			xmlVal.setAttribute("description", valueDescriptions[i]) ;
			
			xml.appendChild(xmlVal) ;
		}
		
		return xml ;
	}

	@Override
	public String getValue(HttpServletRequest request) throws IllegalArgumentException {
		
		String s = request.getParameter(getName()) ;
		
		if (s == null)
			return getDefaultValue() ;
		
		String val = valuesByNormalizedValue.get(normalizeValue(s)) ;
		
		if (val == null)
			return getDefaultValue() ;
		
		return val ;
		
	}
	
	private String normalizeValue(String val) {
		return val.trim().toLowerCase() ;
	}

}
