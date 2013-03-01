package org.wikipedia.miner.service.param;

import javax.servlet.http.HttpServletRequest;


/**
 * A Parameter whose values are booleans
 * <p>
 * If requests specify the parameter name but not a value (e.g "serviceName?getImages")
 * then we assume the value is true. Otherwise, this ignores case variations, and can parse 'true','false','t' or 'f' 
 */
public class BooleanParameter extends Parameter<Boolean> {

	
	/**
	 * Initialises a new boolean parameter
	 * 
	 * @param name the name of the parameter
	 * @param description a short description of what this parameter does
	 * @param defaultValue the value to use when requests do not specify a value for this parameter (may be null)
	 */
	public BooleanParameter(String name, String description,
			Boolean defaultValue) {
		super(name, description, defaultValue, "boolean");
	}

	@Override
	public Boolean getValue(HttpServletRequest request) {
		
		String s = request.getParameter(getName()) ;
		
		if (s == null)
			return getDefaultValue() ;
		
		if (s.trim().length() == 0)
			return true ;
		
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t"))
			return true ;
		
		if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("f"))
			return true ;
		
		throw new IllegalArgumentException() ;
	}

}
