package org.wikipedia.miner.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.db.WEnvironment.StatisticName;
import org.wikipedia.miner.model.Wikipedia;

public class StatsService extends Service{
	
	DateFormat df ;

	public StatsService() {
		super("meta","Provides statistics of a specific wikipedia version",
				"<p>Retrieves statistics (article counts, last edit date, etc.) for a wikipedia dump.</p>",
				true, false
				);
		
		TimeZone tz = TimeZone.getTimeZone("GMT:00");

		df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z") ;
	    df.setTimeZone( tz );
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element response) throws Exception {
		
		Element xmlStats = getHub().createElement("Statistics") ;
		xmlStats.setAttribute("forWikipedia", getWikipediaName(request)) ;
		
		Wikipedia w = getWikipedia(request) ;
		
		for (StatisticName statName:StatisticName.values()) {
			
			Element xmlStat = getHub().createElement(statName.name()) ;
			long stat = w.getEnvironment().retrieveStatistic(statName) ;
			
			
			switch(statName) {
			
			case lastEdit: 
				String date = df.format(new Date(stat)) ;
				xmlStat.appendChild(getHub().createTextNode(date)) ;
				break ;
			default: 
				xmlStat.appendChild(getHub().createTextNode(String.valueOf(stat))) ;
			}
			
			xmlStats.appendChild(xmlStat) ;
		}
		response.appendChild(xmlStats) ;
		
		return response;
	}

}
