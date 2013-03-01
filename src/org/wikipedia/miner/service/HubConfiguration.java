package org.wikipedia.miner.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HubConfiguration {
	
	
	private enum ParamName{proxy, wikipedia, client, authentication, unknown} ;
	
	private String proxyHost ;
	private String proxyPort ;
	private String proxyUser ;
	private String proxyPassword ;
	
	private String defaultWikipedia = null;
	private HashMap<String,String> wikiDescriptions ;
	private HashMap<String,String> wikiConfigs ;
	
	private HashMap<String,Client> clients ;
	private Client defaultClient = null ;
	
	private String cookieUserName ;
	private String cookiePassword ;
	
	
	
	public String getProxyHost() {
		return proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public String getProxyUser() {
		return proxyUser;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public String getCookieForUsername() {
		return cookieUserName ;
	}
	
	public String getCookieForPassword() {
		return cookiePassword ;
	}
	
	public String getDefaultWikipediaName() {
		return defaultWikipedia;
	}
	
	public String[] getWikipediaNames() {
		Set<String> wikipediaNames = wikiConfigs.keySet() ;
		return wikipediaNames.toArray(new String[wikipediaNames.size()]) ;
	}
	
	public String getWikipediaConfig(String wikiName) {
		return wikiConfigs.get(wikiName) ;
	}
	
	public String getWikipediaDescription(String wikiName) {
		return wikiDescriptions.get(wikiName) ;
	}
	
	public String[] getClientNames() {
		Set<String> clientNames = clients.keySet() ;
		return clientNames.toArray(new String[clientNames.size()]) ;
	}
	
	public Client getClient(String name) {
		return clients.get(name) ;
	}
	
	public Client getDefaultClient() {
		return defaultClient ;
	}
	
	public HashMap<String, Client> getClientsByName() {
		return clients ;
	}

	public HubConfiguration(File configFile) throws ParserConfigurationException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SAXException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(configFile);
		doc.getDocumentElement().normalize();
		
		initFromXml(doc.getDocumentElement()) ;
		
	}
	
	private void initFromXml(Element xml) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		wikiDescriptions = new HashMap<String,String>() ;
		wikiConfigs = new HashMap<String,String>() ;
		clients = new HashMap<String, Client>() ;
		
		String firstWikipedia = null ;
		
		NodeList children = xml.getChildNodes() ;
		
		for (int i=0 ; i<children.getLength() ; i++) {
			
			Node xmlChild = children.item(i) ;
			
			if (xmlChild.getNodeType() == Node.ELEMENT_NODE) {
				
				Element xmlParam = (Element)xmlChild ;
				
				String paramName = xmlParam.getNodeName() ;
				String paramValue = getParamValue(xmlParam) ;
				
				switch(resolveParamName(xmlParam.getNodeName())) {
				
				case proxy:
					proxyHost = xmlParam.getAttribute("host") ;
					proxyPort = xmlParam.getAttribute("port") ;
					proxyUser = xmlParam.getAttribute("user") ;
					proxyPassword = xmlParam.getAttribute("password") ;
					break ;
				case wikipedia:
					
					String wikiName = xmlParam.getAttribute("name") ;
					String description = xmlParam.getAttribute("description") ;
					
					boolean isDefault = false ;
					if (xmlParam.hasAttribute("default"))
						isDefault = Boolean.parseBoolean(xmlParam.getAttribute("default")) ;
					
					if (firstWikipedia == null)
						firstWikipedia = wikiName ;
					
					if (isDefault)
						defaultWikipedia = wikiName ;
					
					wikiDescriptions.put(wikiName, description) ;
					wikiConfigs.put(wikiName, paramValue) ;
					break ;
				case client:

					String clientName = null ;
					if (xmlParam.hasAttribute("name")) 
						clientName = xmlParam.getAttribute("name") ;
					
					String password = null ;
					if (xmlParam.hasAttribute("password")) 
						password = xmlParam.getAttribute("password") ;
					
					int minLimit = -1 ;
					if (xmlParam.hasAttribute("minLimit")) 
						minLimit = Integer.parseInt(xmlParam.getAttribute("minLimit"));
					
					int hourLimit = -1 ;
					if (xmlParam.hasAttribute("hourLimit")) 
						hourLimit = Integer.parseInt(xmlParam.getAttribute("hourLimit"));
					
					int dayLimit = -1 ;
					if (xmlParam.hasAttribute("dayLimit")) 
						dayLimit = Integer.parseInt(xmlParam.getAttribute("dayLimit"));	
					
					if (clientName == null)
						defaultClient = new Client("anonymous", password, minLimit, hourLimit, dayLimit) ;
					else
						this.clients.put(clientName, new Client(clientName, password, minLimit, hourLimit, dayLimit)) ;	
					break ;
				case authentication:
					cookieUserName = xmlParam.getAttribute("nameCookie") ;
					cookiePassword = xmlParam.getAttribute("passwordCookie") ;
					break ;
				default:
					Logger.getLogger(HubConfiguration.class).warn("Ignoring unknown parameter: '" + paramName + "'") ;
				} ;
			}
			
			if (defaultWikipedia == null)
				defaultWikipedia = firstWikipedia ;
			
			//if there is no default client, make one with no access limits
			if (defaultClient == null)
				defaultClient = new Client("anonymous", null, 0,0,0) ;
		
			//TODO: throw fit if mandatory params (at least one wikipedia) are missing. 	
		}
	}
	
	private String getParamValue(Element xmlParam) {
		
		Node nodeContent = xmlParam.getChildNodes().item(0) ;
		
		if (nodeContent == null)
			return null ;
		
		if (nodeContent.getNodeType() != Node.TEXT_NODE)
			return null ;
		
		String content = nodeContent.getTextContent().trim() ;
		
		if (content.length() == 0)
			return null ;
		
		return content ;
	}
		
	private ParamName resolveParamName(String name) {
		try {
			return ParamName.valueOf(name.trim()) ;
		} catch (Exception e) {
			return ParamName.unknown ;
		}
	}
	
}
