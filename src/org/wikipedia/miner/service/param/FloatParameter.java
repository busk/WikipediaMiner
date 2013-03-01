package org.wikipedia.miner.service.param;

import javax.servlet.http.HttpServletRequest;

public class FloatParameter extends Parameter<Float> {

	public FloatParameter(String name, String description,
			Float defaultValue) {
		super(name, description, defaultValue, "float");
	}

	@Override
	public Float getValue(HttpServletRequest request) throws IllegalArgumentException {
		
		String s = request.getParameter(getName()) ;
		
		if (s == null)
			return getDefaultValue() ;
		else
			return Float.valueOf(s) ;
	}
	
}
