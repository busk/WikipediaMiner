package org.wikipedia.miner.service;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.service.param.BooleanParameter;
import org.wikipedia.miner.service.param.EnumParameter;
import org.wikipedia.miner.service.param.IntListParameter;
import org.wikipedia.miner.service.param.IntParameter;
import org.wikipedia.miner.service.param.Parameter;
import org.wikipedia.miner.service.param.ParameterGroup;
import org.wikipedia.miner.service.param.StringArrayParameter;
import org.wikipedia.miner.service.param.StringParameter;


public abstract class Service extends HttpServlet {

	public enum ResponseFormat {XML,DIRECT} ; 

	private ServiceHub hub ;
	
	private String groupName ;
	private String shortDescription ;
	private String detailsMarkup ;

	private Vector<ParameterGroup> parameterGroups ;
	@SuppressWarnings("unchecked")
	private Vector<Parameter> globalParameters ;
	@SuppressWarnings("unchecked")
	private Vector<Parameter> baseParameters ;
	private Vector<Example> examples = new Vector<Example>() ;


	boolean wikipediaSpecific ;
	boolean supportsDirectResponse ;
	
	protected EnumParameter<ResponseFormat> prmResponseFormat ;
	protected BooleanParameter prmHelp ;
	protected StringArrayParameter prmWikipedia ;

	private Transformer serializer ;


	private DecimalFormat progressFormat = new DecimalFormat("#0%") ;

	@SuppressWarnings("unchecked")
	public Service(String groupName, String shortDescription, String detailsMarkup, boolean wikipediaSpecific, boolean supportsDirectResponse) {

		//this.name = name ;
		this.groupName = groupName ;
		this.shortDescription = shortDescription ;
		this.detailsMarkup = detailsMarkup ;
		this.parameterGroups = new Vector<ParameterGroup>() ;
		this.globalParameters = new Vector<Parameter>() ;
		this.baseParameters = new Vector<Parameter>() ;

		this.wikipediaSpecific = wikipediaSpecific ;
		this.supportsDirectResponse = supportsDirectResponse ;


		try {
			serializer = TransformerFactory.newInstance().newTransformer();

			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			serializer.setOutputProperty(OutputKeys.METHOD,"xml");
			serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		hub = ServiceHub.getInstance(config.getServletContext()) ;

		if (supportsDirectResponse) {
			String[] descResponseFormat = {"in XML format", "directly, without any additional information such as request parameters. This format will not be valid for some services."} ;
			prmResponseFormat = new EnumParameter<ResponseFormat>("responseFormat", "the format in which the response should be returned", ResponseFormat.XML, ResponseFormat.values(), descResponseFormat) ;
			baseParameters.add(prmResponseFormat) ;
		}

		
		if (wikipediaSpecific) {
			String[] valsWikipedia = getHub().getWikipediaNames() ;
			String[] dscsWikipedia = new String[valsWikipedia.length] ;
			
			for (int i=0 ; i<valsWikipedia.length ; i++) {
				dscsWikipedia[i] = getHub().getWikipediaDescription(valsWikipedia[i]) ;
				
				if (dscsWikipedia[i] == null)
					dscsWikipedia[i] = "No description available" ;
			}
			prmWikipedia = new StringArrayParameter("wikipedia", "Which edition of Wikipedia to retrieve information from", getHub().getDefaultWikipediaName(), valsWikipedia, dscsWikipedia) ;
			baseParameters.add(prmWikipedia) ;
		}
		
		prmHelp = new BooleanParameter("help", "If <b>true</b>, this will return a description of the service and the parameters available", false) ;
		baseParameters.add(prmHelp) ;
		
		hub.registerService(this) ;
	}
	
	public void addExample(Example example) {
		this.examples.add(example) ;
	}

	public ServiceHub getHub() {
		return hub ;
	}	                                                            

	public Wikipedia getWikipedia(HttpServletRequest request) {

		String wikiName = prmWikipedia.getValue(request) ;

		Wikipedia wiki = hub.getWikipedia(wikiName) ;

		return wiki ;
	}

	public String getWikipediaName(HttpServletRequest request) {
		return prmWikipedia.getValue(request) ; 
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		doGet(request, response) ;

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		try {

			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF-8") ;

			if (prmHelp.getValue(request)) {
				response.setContentType("application/xml;charset=UTF-8");

				serializer.transform(new DOMSource(getXmlDescription()), new StreamResult(response.getWriter()));
				return ;
			}

			
			
			double loadProgress ;
			
			if (wikipediaSpecific) {
				Wikipedia wikipedia = getWikipedia(request) ;
				loadProgress= wikipedia.getEnvironment().getProgress() ;
			} else {
				loadProgress = 1 ;
			}
			
			boolean usageExceeded = isUsageLimitExceeded(request) ;
			
			if (supportsDirectResponse) {
				ResponseFormat responseFormat = prmResponseFormat.getValue(request) ;
	
				if (responseFormat == ResponseFormat.DIRECT) { 
	
					if (requiresWikipedia() && loadProgress < 1)
						throw new ServletException("Wikipedia is not yet ready. Current progress is " + progressFormat.format(loadProgress)) ;
	
					if (usageExceeded)
						throw new ServletException("You have exceeded your usage limits.") ;
					
					try {
						buildUnwrappedResponse(request, response) ;
					} catch (Exception e) {
						throw new ServletException(e) ;
					}
	
					return ;
				}
			}

			Element xmlRoot = getHub().createElement("WikipediaMiner") ;
			xmlRoot.setAttribute("service", request.getServletPath()) ;
			xmlRoot.appendChild(getXmlRequest(request)) ;

			Element xmlResponse = getHub().createElement("Response") ;

			if (requiresWikipedia() && loadProgress < 1) {
				xmlResponse = buildErrorResponse("Wikipedia is not yet ready. Current progress is " + progressFormat.format(loadProgress), xmlResponse) ;
			} else if (usageExceeded) {
				xmlResponse = buildErrorResponse("Usage limits exceeded", xmlResponse) ;
				xmlResponse = buildUsageResponse(request, xmlResponse) ;			
			} else {
				try {
					xmlResponse = buildWrappedResponse(request, xmlResponse) ;
				} catch (Exception e) {
					throw new ServletException(e) ;
				}
			}

			xmlRoot.appendChild(xmlResponse) ;

			response.setContentType("application/xml");
			response.setHeader("Cache-Control", "no-cache"); 
			response.setCharacterEncoding("UTF8") ;

			serializer.transform(new DOMSource(xmlRoot), new StreamResult(response.getWriter()));
		} catch (TransformerException e) {
			throw new ServletException(e) ;
		}
	}

	@Override
	public void destroy() {
		getHub().dropService(this) ;
	}


	public abstract Element buildWrappedResponse(HttpServletRequest request, Element response) throws Exception;

	public void buildUnwrappedResponse(HttpServletRequest request, HttpServletResponse response) throws Exception{
		throw new UnsupportedOperationException() ;
	}
	
	public int getUsageCost(HttpServletRequest request) {
		return 1 ;
	}
	
	private boolean isUsageLimitExceeded(HttpServletRequest request) {
		Client client = getHub().identifyClient(request) ;
		
		if (client == null)
			return false ;
		
		return client.update(getUsageCost(request)) ; 
	}
	
	protected Element buildUsageResponse(HttpServletRequest request, Element xmlResponse) {
		
		Client client = getHub().identifyClient(request) ;
		
		
		xmlResponse.appendChild(client.getXML(hub)) ;
		return xmlResponse ;
		
	}

	public boolean requiresWikipedia() {
		return wikipediaSpecific ;
	}

	public Element buildErrorResponse(String message, Element response) {

		response.setAttribute("error", message) ;
		return response ;
	}
	
	public Element buildWarningResponse(String message, Element response) {

		response.setAttribute("warning", message) ;
		return response ;
	}
	
	

	public String getBasePath(HttpServletRequest request) {

		StringBuffer path = new StringBuffer() ;
		path.append(request.getScheme()) ;
		path.append("://") ;
		path.append(request.getServerName()) ;
		path.append(":") ;
		path.append(request.getServerPort()) ;
		path.append(request.getContextPath()) ;

		return path.toString() ;
	}
	
	public String getGroupName() {
		if (groupName != null)
			return groupName ;
		else 
			return "ungrouped" ;
	}
	
	public String getShortDescription() {
		return shortDescription ;
	}

	@SuppressWarnings("unchecked")
	public Element getXmlDescription() {

		Element xmlResponse = hub.createElement("Response") ;
		
		Element xmlDescription = hub.createElement("ServiceDescription") ;
		xmlDescription.setAttribute("groupName", groupName) ;
		xmlDescription.setAttribute("serviceName", getServletName()) ;
		xmlDescription.setAttribute("description", shortDescription) ;
		
		
		xmlDescription.appendChild(hub.createCDATAElement("Details", detailsMarkup)) ;
		

		for(ParameterGroup paramGroup:parameterGroups) 
			xmlDescription.appendChild(paramGroup.getXmlDescription(hub)) ;

		for (Parameter param:globalParameters) 
			xmlDescription.appendChild(param.getXmlDescription(hub)) ;
		
		Element xmlBaseParams = hub.createElement("BaseParameters") ;
		for (Parameter param:baseParameters)
			xmlBaseParams.appendChild(param.getXmlDescription(hub)) ;
		xmlDescription.appendChild(xmlBaseParams) ;

		Element xmlExamples = hub.createElement("Examples") ;
		for (Example example:examples) {
			xmlExamples.appendChild(example.getXML()) ;
		}
		xmlDescription.appendChild(xmlExamples) ;
		
		
		xmlResponse.appendChild(xmlDescription) ;
		return xmlResponse ;
	}


	public void addParameterGroup(ParameterGroup paramGroup) {
		parameterGroups.add(paramGroup) ;
	}

	@SuppressWarnings("unchecked")
	public void addGlobalParameter(Parameter param) {
		globalParameters.add(param) ;
	}

	public ParameterGroup getSpecifiedParameterGroup(HttpServletRequest request) {


		for (ParameterGroup paramGroup:parameterGroups) {
			if (paramGroup.isSpecified(request))
				return paramGroup ;		
		}

		return null ;
	}

	private Element getXmlRequest(HttpServletRequest request) {

		Element xmlRequest = getHub().createElement("Request") ;

		for (Enumeration<String> e = request.getParameterNames() ; e.hasMoreElements() ;) {
			String paramName = e.nextElement() ;
			xmlRequest.setAttribute(paramName, request.getParameter(paramName)) ;
		}

		return xmlRequest ;
	}

	private class Example {
		
		private String description ;
		private LinkedHashMap params ;
		
		public Example(String description, LinkedHashMap<String,String>params) {
			this.description = description ;
			this.params = params ;
		}
		
		private String getUrl() {
			StringBuffer sb = new StringBuffer() ;
			sb.append(getServletName()) ;
			
			int index = 0 ;
			for (Object o:params.entrySet())  {
				Map.Entry<String, String> e = (Map.Entry<String, String>)o ;
				
				if (index == 0)
					sb.append("?") ;
				else
					sb.append("&") ;
				
				sb.append(e.getKey()) ;
				sb.append("=") ;
				sb.append(e.getValue()) ;
				
				index++ ;
			}
			return sb.toString() ;
		}
		
		private Element getXML() {
			Element xml = getHub().createCDATAElement("Example", description) ;
			xml.setAttribute("url", getUrl()) ;
			
			Element xmlParams = getHub().createElement("Parameters") ;
			for (Object o:params.entrySet())  {
				Map.Entry<String, String> e = (Map.Entry<String, String>)o ;
				Element xmlParam = getHub().createElement("Parameter") ;
				xmlParam.setAttribute("name", e.getKey()) ;
				xmlParam.setAttribute("value", e.getValue()) ;
				xmlParams.appendChild(xmlParam) ;
			}
			xml.appendChild(xmlParams) ;
			
			return xml ;
		}
	}
	
	public class ExampleBuilder {
		
		private String description ;
		private LinkedHashMap<String,String> params = new LinkedHashMap<String,String>() ;
		
		public ExampleBuilder(String description) {
			this.description = description ;
		}
		
		public <T> ExampleBuilder addParam(Parameter<T> param, T value) {
			
			params.put(param.getName(), param.getValueForDescription(value)) ;
			return this ;
		}
		
		public Example build() {
			return new Example(description, params) ;
		}
	
	}

}
