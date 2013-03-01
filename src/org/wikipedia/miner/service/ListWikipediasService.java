package org.wikipedia.miner.service;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

public class ListWikipediasService extends Service{

	public ListWikipediasService() {
		super("meta","Lists available editions of Wikipedia", 
				"<p>This service lists the different editions of Wikipedia that are available</p>",
				false,false
				);
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element response) throws Exception {
		
		for (String wikiName: getHub().getWikipediaNames()) {
			
			Element xmlWikipedia = getHub().createElement("Wikipedia") ;
			xmlWikipedia.setAttribute("name", wikiName) ;
			xmlWikipedia.setAttribute("description", getHub().getWikipediaDescription(wikiName)) ;
			xmlWikipedia.setAttribute("isDefault", String.valueOf(wikiName.equals(getHub().getDefaultWikipediaName()))) ;
			response.appendChild(xmlWikipedia) ;
		}
		
		return response ;
	}

	
	
}
