package org.wikipedia.miner.service.param;

import javax.servlet.http.HttpServletRequest;


/**
 * A Parameter whose values are integers
 */
public class IntParameter extends Parameter<Integer> {

	/**
	 * Initialises a new int parameter
	 * 
	 * @param name the name of the parameter
	 * @param description a short description of what this parameter does
	 * @param defaultValue the value to use when requests do not specify a value for this parameter (may be null)
	 */
	public IntParameter(String name, String description, Integer defaultValue) {
		super(name, description, defaultValue, "integer");
	}

	@Override
	public Integer getValue(HttpServletRequest request) {
		
		String s = request.getParameter(getName()) ;
		
		if (s==null)
			return getDefaultValue() ;
		
		try {
			return Integer.parseInt(s) ;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid value for " + getName() + " parameter") ;
		}
	}
}
