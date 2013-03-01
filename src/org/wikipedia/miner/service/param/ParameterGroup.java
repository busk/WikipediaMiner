package org.wikipedia.miner.service.param;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.service.ServiceHub;

/**
 * A group of parameters that are somehow related to each other
 */
@SuppressWarnings("unchecked")
public class ParameterGroup {
	
	String name ;
	String descriptionMarkup ;
	
	Vector<Parameter> parameters ;
	
	/**
	 * Initialises a parameter group
	 * 
	 * @param name the name of this parameter group
	 */
	public ParameterGroup(String name, String descriptionMarkup) {
		this.name = name ;
		this.descriptionMarkup = descriptionMarkup ;
		this.parameters = new Vector<Parameter>() ;
	}
	
	/**
	 * Adds a parameter to this group
	 * 
	 * @param param the parameter to add
	 */
	public void addParameter(Parameter param) {
		parameters.add(param) ;
	}
	
	
	
	/**
	 * Returns the name of this parameter group
	 * 
	 * @return the name of this parameter group
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a vector of parameters within this group
	 * 
	 * @return a vector of parameters within this group
	 */
	public Vector<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Returns an XML description of this parameter group
	 * 
	 * @param hub a hub with utility functions for creating XML elements
	 * @return an XML description of this parameter group
	 */
	public Element getXmlDescription(ServiceHub hub) {
		
		Element xml = hub.createElement("ParameterGroup") ;
		xml.setAttribute("name", name) ;
		
		Element xmlDescription = hub.createCDATAElement("Description", descriptionMarkup) ;
		xml.appendChild(xmlDescription) ;
			
		for (Parameter param:parameters) 
			xml.appendChild(param.getXmlDescription(hub)) ;
			
		return xml ;
	}
	
	/**
	 * Returns true if all mandatory parameters within this group have been specified in the given request, otherwise false
	 * 
	 * @param request the request to check
	 * @return true if all mandatory parameters within this group have been specified in the given request
	 */
	public boolean isSpecified(HttpServletRequest request) {
				
		for (Parameter param:parameters) {
			
			if (param.getValue(request) == null) {
				return false ;
			}
		}
		
		return true ;
	}
	
}
