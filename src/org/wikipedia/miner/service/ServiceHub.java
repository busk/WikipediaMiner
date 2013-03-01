package org.wikipedia.miner.service;

//import info.bliki.wiki.model.WikiModel;

import java.io.File;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.comparison.ConnectionSnippetWeighter;
import org.wikipedia.miner.comparison.LabelComparer;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

public class ServiceHub {
	
	private static ServiceHub instance ;
	
	private HubConfiguration config ;
	private HashMap<String, Wikipedia> wikipediasByName ;
	
	private HashMap<String, ArticleComparer> articleComparersByWikiName ;
	private HashMap<String, LabelComparer> labelComparersByWikiName ;
	private HashMap<String, ConnectionSnippetWeighter> snippetWeightersByWikiName ;
	
	private HashMap<String, Client> clientsByName ;
	
	private HashMap<String,Service> registeredServices ;
	
	
	private MarkupFormatter formatter = new MarkupFormatter() ;
	private WebContentRetriever retriever ;
	private Document doc = new DocumentImpl();
	private DOMParser parser = new DOMParser() ;
	
	private DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Locale.US);
	
	// Protect the constructor, so no other class can call it
	private ServiceHub(ServletContext context) throws ServletException {

		wikipediasByName = new HashMap<String, Wikipedia>() ;
		articleComparersByWikiName = new HashMap<String, ArticleComparer>()  ;
		labelComparersByWikiName = new HashMap<String, LabelComparer>()  ;
		snippetWeightersByWikiName = new HashMap<String, ConnectionSnippetWeighter>() ;
		
		registeredServices = new HashMap<String,Service>() ;
				
		try {
			String hubConfigFile = context.getInitParameter("hubConfigFile") ;
			config = new HubConfiguration(new File(hubConfigFile)) ; 
			
			for (String wikiName:config.getWikipediaNames()) {
				File wikiConfigFile = new File(config.getWikipediaConfig(wikiName)) ;
				WikipediaConfiguration wikiConfig = new WikipediaConfiguration(wikiConfigFile);
				
				
				
				Wikipedia wikipedia = new Wikipedia(wikiConfig, true) ;
				wikipediasByName.put(wikiName, wikipedia) ;
				
				ArticleComparer artCmp = new ArticleComparer(wikipedia) ;
				articleComparersByWikiName.put(wikiName, artCmp) ;
				
				if (artCmp != null && wikiConfig.getLabelDisambiguationModel() != null && wikiConfig.getLabelComparisonModel() != null) {
					LabelComparer lblCmp = new LabelComparer(wikipedia, artCmp) ;
					labelComparersByWikiName.put(wikiName, lblCmp) ;
				}
				
				ConnectionSnippetWeighter sw = new ConnectionSnippetWeighter(wikipedia, artCmp) ;
				snippetWeightersByWikiName.put(wikiName, sw) ;
			}
		
			clientsByName = config.getClientsByName() ;
		
			retriever = new WebContentRetriever(config) ;
		} catch (Exception e) {
			throw new ServletException(e) ;
		}
	} 
	  
	public static ServiceHub getInstance(ServletContext context) throws ServletException {
		
		if (instance != null) 
			return instance ;
		
		instance = new ServiceHub(context) ;
		return instance ;
		
	}
	
	public void registerService(Service service) {
		registeredServices.put(service.getServletName(), service) ;
	}
	
	public void dropService(Service service) {
		registeredServices.remove(service.getServletName()) ;
		
		
		if (registeredServices.isEmpty()) {
			
			for (Wikipedia w:wikipediasByName.values()) 
				w.close() ;
		}
	}
	
	public Set<String> getServiceNames() {
		return registeredServices.keySet() ;
	}
	
	public Service getService(String serviceName) {
		return registeredServices.get(serviceName) ;
	}
	
	public String getDefaultWikipediaName() {
		return config.getDefaultWikipediaName() ;
	}
	
	public Wikipedia getWikipedia(String wikiName) {
		return wikipediasByName.get(wikiName) ;
	}
	
	public String getWikipediaDescription(String wikiName) {
		return config.getWikipediaDescription(wikiName) ;
	}
	
	public String[] getWikipediaNames() {
		
		Set<String> wikipediaNames = wikipediasByName.keySet() ;
		return wikipediaNames.toArray(new String[wikipediaNames.size()]) ;
	}
	
	public ArticleComparer getArticleComparer(String wikiName) {
		return articleComparersByWikiName.get(wikiName) ;
	}
	
	public LabelComparer getLabelComparer(String wikiName) {
		return labelComparersByWikiName.get(wikiName) ;
	}
	
	public ConnectionSnippetWeighter getConnectionSnippetWeighter(String wikiName) {
		return snippetWeightersByWikiName.get(wikiName) ;
	}
	
	public MarkupFormatter getFormatter() {
		return formatter ;
	}
	
	public WebContentRetriever getRetriever() {
		return retriever ;
	}
	
	public DOMParser getParser() {
		return parser ;
	}
	
	
	public Element createElement(String tagName) {
		
		return doc.createElement(tagName) ;
	}
	
	public Text createTextNode(String data) {
		return doc.createTextNode(data) ;
	}
	
	public Element createCDATAElement(String tagName, String data) {
		Element e = doc.createElement(tagName) ;
		e.appendChild(doc.createCDATASection(data)) ;
		return e ;			
	}
	
	public String format(double number) {
		return decimalFormat.format(number) ;
	}
	
	public Client identifyClient(HttpServletRequest request) {
		
		String username = null ;
		String password = null ;
		
		//first, look for the cookie name ;
		if (config.getCookieForUsername() != null) {

			for (Cookie cookie:request.getCookies()) {
				
				if (cookie.getName().equals(config.getCookieForUsername()))
					username = cookie.getValue() ;
				
				if (cookie.getName().equals(config.getCookieForPassword()))
					password = cookie.getValue() ;
			}
		}
		if (username != null) {
			Client client = clientsByName.get(username) ;
			if (client == null)
				return null ;
			
			if (client.passwordMatches(password))
				return null ;
			
			return client ;
		}
		
		//failing that, use the remote host ;
		username = request.getRemoteHost() ;
		
		Client client = clientsByName.get(username) ;
		if (client != null)
			return client ;
		
		//if there is no client with that name, create a new one with no password, and same limits as default.
		client = new Client(username, null, config.getDefaultClient()) ;
		
		clientsByName.put(username, client) ;
 		
		return client ;
		
	}
	
	
}
