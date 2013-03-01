package org.wikipedia.miner.service.param;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.service.ServiceHub;




/**
 * A parameter or argument for a service. Parameters should know how to extract their value from an {@link HttpServletRequest}
 * and document themselves as an xml description.
 * 
 * @param <T> the type of value to expect.
 */
public abstract class Parameter<T> {

	private String name ;
	private String description ;
	private T defaultValue ;
	
	private String dataTypeName ;
	
	/**
	 * Returns the name of the parameter
	 * 
	 * @return the name of the parameter
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a short textual description of what this parameter does
	 * 
	 * @return a short textual description of what this parameter does
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the value to be used if no value is manually specified in the service request. This may be null, in which 
	 * case the parameter is considered mandatory: all requests to the service must specify a value 
	 * for this parameter
	 * 
	 * @return the value to be used if none is manually specified in the service request. 
	 */
	public T getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Returns an XML description of this parameter
	 * 
	 * @param hub a hub with utility functions for creating XML elements
	 * @return an XML description of this parameter
	 */
	public Element getXmlDescription(ServiceHub hub) {
		Element xmlParam = hub.createElement("Parameter") ;
		xmlParam.setAttribute("name", name) ;
		xmlParam.setAttribute("dataType", dataTypeName) ;
		xmlParam.appendChild(hub.createCDATAElement("Description", description)) ;
		
		if (defaultValue != null) {
			xmlParam.setAttribute("optional", "true") ;
			xmlParam.setAttribute("default", getValueForDescription(defaultValue)) ; 
		} else {
			xmlParam.setAttribute("optional", "false") ;
		}
		
		return xmlParam ;
	}
	
	public String getValueForDescription(T val) {
		return val.toString() ;
	}
	
	/**
	 * Initialises a new parameter
	 * 
	 * @param name the name of the parameter
	 * @param description a short description of what this parameter does
	 * @param defaultValue the value to use when requests do not specify a value for this parameter (may be null)
	 */
	public Parameter(String name, String description, T defaultValue, String dataTypeName) {
		this.name = name ;
		this.description = description ;
		this.defaultValue = defaultValue ;
		this.dataTypeName = dataTypeName ;
	}
	
	/**
	 * Returns the value of this parameter, as specified in the given request. 
	 * If the request specifies an invalid value, or none at all, then the default value will be returned.
	 * 
	 * @param request the request made to the service
	 * @return the value of this parameter, as specified in the given request.
	 * @throws IllegalArgumentException if the value specified in this request cannot be parsed
	 */
	public abstract T getValue(HttpServletRequest request) throws IllegalArgumentException ;
	
}
