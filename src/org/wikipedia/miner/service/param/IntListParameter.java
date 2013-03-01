package org.wikipedia.miner.service.param;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

public class IntListParameter extends Parameter<Integer[]> {

	public IntListParameter(String name, String description, Integer[] defaultValue) {
		super(name, description, defaultValue, "integer list");
	}
	
	@Override 
	public String getValueForDescription(Integer[] val) {
		
		if (val.length == 0)
			return "empty list" ;
		
		StringBuffer sb = new StringBuffer() ;
		for (int v:val) {
			sb.append(v) ;
			sb.append(",") ;
		}
		sb.deleteCharAt(sb.length() -1) ;
		
		return sb.toString() ;
	}

	@Override
	public Integer[] getValue(HttpServletRequest request) throws IllegalArgumentException {
		
		String s = request.getParameter(getName()) ;
		
		if (s == null)		
			return getDefaultValue();
		
		ArrayList<Integer> values = new ArrayList<Integer>() ;
		for (String val:s.split("[,;:]")) {
			values.add(Integer.parseInt(val.trim())) ;	
		}

		return values.toArray(new Integer[values.size()]) ;
	}

}
