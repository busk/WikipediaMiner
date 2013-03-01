package org.wikipedia.miner.service.param;

import javax.servlet.http.HttpServletRequest;

/**
 * A Parameter that expects String values
 */
public class StringParameter extends Parameter<String> {

	/**
	 * Initialises a new StringParameter
	 * 
	 * @param name the name of the parameter
	 * @param description a short description of what this parameter does
	 * @param defaultValue the value to use when requests do not specify a value for this parameter (may be null)
	 */
	public StringParameter(String name, String description, String defaultValue) {
		super(name, description, defaultValue, "string");
	}

	@Override
	public String getValue(HttpServletRequest request) throws IllegalArgumentException {
		return request.getParameter(getName()) ;
	}

}
