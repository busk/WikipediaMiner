package org.wikipedia.miner.service;

import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.model.Wikipedia;

public class ProgressService extends Service {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1217650265475115103L;
	
	private DecimalFormat df = new DecimalFormat("#.00") ;
	
	
	public ProgressService() {
		super("meta","Monitors progress of service initialization",
				"<p>Wikipedia Miner can take a while to get started. This service allows polling to see how much progress has been made loading up a particular edition of Wikipedia</p>",
				true, false
		);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Element buildWrappedResponse(HttpServletRequest request, Element xmlResponse) {
		
		Wikipedia wikipedia = getWikipedia(request) ;
		
		double progress = wikipedia.getEnvironment().getProgress() ;
	
		xmlResponse.setAttribute("progress", df.format(progress)) ;

		return xmlResponse ;
	}
	
	@Override
	public boolean requiresWikipedia() {
		return false ;
	}

}
