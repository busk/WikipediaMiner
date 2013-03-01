package org.wikipedia.miner.service;

import gnu.trove.TIntFloatHashMap;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.service.Service.ExampleBuilder;
import org.wikipedia.miner.service.param.BooleanParameter;
import org.wikipedia.miner.service.param.FloatParameter;
import org.wikipedia.miner.service.param.StringParameter;
import org.wikipedia.miner.util.Position;
import org.wikipedia.miner.util.RelatednessCache;

/**
 * 
 * 
 * 
 * NOTE: this does not support {@link Service.ResponseFormat#DIRECT} 
 */
public class SearchService extends Service {


	private static final long serialVersionUID = 5011451347638265017L;



	//Pattern topicPattern = Pattern.compile("\\[\\[(\\d+)\\|(.*?)\\]\\]") ;
	Pattern quotePattern = Pattern.compile("\".*?\"");



	private StringParameter prmQuery ;
	private BooleanParameter prmComplex ;
	private FloatParameter prmMinPriorProb ;
	


	public SearchService() {
		super("core","Lists the senses (wikipedia articles) of terms and phrases",
				"<p>This service takes a term or phrase, and returns the different Wikipedia articles that these could refer to.</p>" +
				"<p>By default, it will treat the entire query as one term, but it can be made to break it down into its components " +
				"(to recognize, for example, that <i>hiking new zealand</i> contains two terms: <i>hiking</i> and <i>new zealand</i>)</p>" +
				"<p>For each component term, the service will list the different articles (or concepts) that it could refer to, in order of prior probability " +
				"so that the most obvious senses are listed first.</p>" +
				"<p>For queries that contain multiple terms, the senses of each term will be compared against each other to disambiguate them. This " +
				"provides the weight attribute, which is larger for senses that are likely to be the correct interpretation of the query.</p>",
				true, false);
		
		// TODO Auto-generated constructor stub

		prmQuery = new StringParameter("query", "Your query", null) ;
		addGlobalParameter(prmQuery) ;

		prmComplex = new BooleanParameter("complex", "<b>true</b> if your query might reference multiple topics, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmComplex) ;
		
		prmMinPriorProb = new FloatParameter("minPriorProbability", "the minimum prior probability that a sense must have for it to be returned", 0.01F) ;
		addGlobalParameter(prmMinPriorProb) ;
		
		addExample(
				new ExampleBuilder("List senses of an ambiguous term").
				addParam(prmQuery, "kiwi").
				build()
		) ;
		
		addExample(
				new ExampleBuilder("Break a complex multi-topic query into its component terms, and list thier senses").
				addParam(prmQuery, "hiking new zealand").
				addParam(prmComplex, true).
				build()
		) ;
	}



	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element xmlResponse) throws Exception {

		String query = prmQuery.getValue(request) ;
		if (query == null) {
			xmlResponse.setAttribute("unspecifiedParameters", "true") ;
			return xmlResponse ;
		}

		if (prmComplex.getValue(request))
			return resolveComplexQuery(query, request, xmlResponse) ;
		else
			return resolveSimpleQuery(query, request, xmlResponse) ;

	}




	public Element resolveSimpleQuery(String query, HttpServletRequest request, Element xmlResponse) {

		Wikipedia wikipedia = getWikipedia(request) ;
		
		query = query.replaceAll("^[\\W]*", "") ;
		query = query.replaceAll("[\\W]*$", "") ;

		Label label = new Label(wikipedia.getEnvironment(), query, wikipedia.getConfig().getDefaultTextProcessor()) ;
		Element xmlLabel = createXML(label) ;

		float minPriorProb = prmMinPriorProb.getValue(request) ;
		for (Label.Sense sense:label.getSenses()) {
			
			if (sense.getPriorProbability() < minPriorProb) 
				break ;

			Element xmlSense = createXML(sense) ;
			xmlLabel.appendChild(xmlSense) ;

		}

		xmlResponse.appendChild(xmlLabel) ;

		return xmlResponse ;
	}


	public Element resolveComplexQuery(String query, HttpServletRequest request, Element xmlResponse) throws Exception {

		Wikipedia wikipedia = getWikipedia(request) ;
		
		ArticleComparer artComparer = getHub().getArticleComparer(getWikipediaName(request)) ;
		if (artComparer == null) {
			this.buildErrorResponse("article comparisons are not available with this wikipedia instance", xmlResponse) ;
			return xmlResponse ;
		}
		
		ExhaustiveDisambiguator disambiguator = new ExhaustiveDisambiguator(artComparer) ;

		float minPriorProb = prmMinPriorProb.getValue(request) ;
		
		//resolve query
		ArrayList<QueryLabel> queryLabels = getReferences(query, wikipedia) ;	
		queryLabels = resolveCollisions(queryLabels) ;
		queryLabels = disambiguator.disambiguate(queryLabels, minPriorProb) ;

		for (QueryLabel queryLabel:queryLabels) {

			Element xmlLabel = createXML(queryLabel);
			xmlLabel.setAttribute("isStopword", String.valueOf(queryLabel.isStopword)) ;
			xmlLabel.setAttribute("start", String.valueOf(queryLabel.getPosition().getStart())) ;
			xmlLabel.setAttribute("end", String.valueOf(queryLabel.getPosition().getEnd())) ;

			for (Label.Sense sense:queryLabel.getSenses()) {

				if (sense.getPriorProbability() < minPriorProb) 
					break ;
				
				Element xmlSense = createXML(sense);
				xmlSense.setAttribute("weight", getHub().format(disambiguator.getSenseWeight(sense.getId()))) ;
				xmlLabel.appendChild(xmlSense) ;
			}
			
			xmlResponse.appendChild(xmlLabel) ;
		}

		return xmlResponse;
	}














	private Element createXML(Label label) {

		Element xmlLabel = getHub().createElement("Label") ;
		xmlLabel.setAttribute("text", label.getText()) ;

		xmlLabel.setAttribute("linkDocCount", String.valueOf(label.getLinkDocCount())) ;
		xmlLabel.setAttribute("linkOccCount", String.valueOf(label.getLinkOccCount())) ;
		xmlLabel.setAttribute("docCount", String.valueOf(label.getDocCount())) ;
		xmlLabel.setAttribute("occCount", String.valueOf(label.getOccCount())) ;
		xmlLabel.setAttribute("linkProbability", String.valueOf(label.getLinkProbability())) ;

		return xmlLabel ;
	}

	private Element createXML(Label.Sense sense) {

		Element xmlSense = getHub().createElement("Sense") ;
		xmlSense.setAttribute("id", String.valueOf(sense.getId())) ;
		xmlSense.setAttribute("title", sense.getTitle()) ;
		xmlSense.setAttribute("linkDocCount", String.valueOf(sense.getLinkDocCount())) ;
		xmlSense.setAttribute("linkOccCount", String.valueOf(sense.getLinkOccCount())) ;
		xmlSense.setAttribute("priorProbability", getHub().format(sense.getPriorProbability())) ;
		xmlSense.setAttribute("fromTitle", String.valueOf(sense.isFromTitle())) ;
		xmlSense.setAttribute("fromRedirect", String.valueOf(sense.isFromTitle())) ;

		return xmlSense ;
	}



	private String getMaskedQuery(String query) {

		//mask out topics that have been detected already
		StringBuffer sb = new StringBuffer() ;
		int lastIndex = 0 ;

		Matcher m = quotePattern.matcher(query) ;
		while (m.find()) {
			sb.append(query.substring(lastIndex, m.start())) ;

			for (int i=0 ; i<m.group().length() ;i++)
				sb.append("A") ;

			lastIndex = m.end() ;
		}
		sb.append(query.substring(lastIndex)) ;
		return sb.toString() ;

	}


	private ArrayList<QueryLabel> getReferences(String query, Wikipedia wikipedia) {

		ArrayList<QueryLabel> queryLabels = new ArrayList<QueryLabel>() ;

		String text = "$ " + getMaskedQuery(query) + " $" ;

		Pattern p = Pattern.compile("[\\s\\{\\}\\(\\)\'\\.\\,\\;\\:\\-\\_]") ;  //would just match all non-word chars, but we don't want to match utf chars
		Matcher m = p.matcher(text) ;

		Vector<Integer> matchPositions = new Vector<Integer>() ;

		while (m.find()) 
			matchPositions.add(m.start()) ;

		for (int i=0 ; i<matchPositions.size() ; i++) {

			int startPos = matchPositions.elementAt(i) + 1 ;

			if (Character.isWhitespace(text.charAt(startPos))) 
				continue ;

			for (int j=Math.min(i + 15, matchPositions.size()-1) ; j > i ; j--) {
				int currPos = matchPositions.elementAt(j) ;	
				
				String origNgram = query.substring(startPos-2, currPos-2) ;
				
				String trimmedNgram = origNgram.replaceAll("^[\\W]*", "") ;
				trimmedNgram = trimmedNgram.replaceAll("[\\W]*$", "") ;
				
				if (trimmedNgram.indexOf('\"') > 0)
					continue ;

				if (! (trimmedNgram.length()==1 && trimmedNgram.substring(startPos-1, startPos).equals("'"))&& !trimmedNgram.trim().equals("")) {

					Position pos = new Position(startPos-2, currPos-2) ;
					QueryLabel ql = new QueryLabel(trimmedNgram, origNgram, pos, wikipedia) ;

					queryLabels.add(ql) ;

					//System.out.println(qt.getAnchor().getText() + "," + qt.getAnchor().getLinkProbability()) ;
				}
			}
		}
		return queryLabels ;		
	}

	private ArrayList<QueryLabel> resolveCollisions(ArrayList<QueryLabel> queryLabels) {

		for (int i=0 ; i<queryLabels.size(); i++) {
			QueryLabel lbl1 = queryLabels.get(i) ;

			Vector<QueryLabel> overlappingTopics = new Vector<QueryLabel>() ;

			double qtWeight ;
			if (lbl1.isStopword)
				qtWeight = 0 ;
			else
				qtWeight = lbl1.getLinkProbability() ;

			double overlapWeight = 0 ;

			for (int j=i+1 ; j<queryLabels.size(); j++){
				QueryLabel lbl2 = queryLabels.get(j) ;

				if (lbl1.overlaps(lbl2)) {
					overlappingTopics.add(lbl2) ;	

					if (!lbl2.isStopword)
						overlapWeight = overlapWeight + lbl2.getLinkProbability() ;
				} else {
					break ;
				}
			}

			if (overlappingTopics.size() > 0)
				overlapWeight = overlapWeight / overlappingTopics.size() ;

			if (overlapWeight > qtWeight) {
				// want to keep the overlapped items
				queryLabels.remove(i) ;
				i = i-1 ;				
			} else {
				//want to keep the overlapping item
				for (int j=0 ; j<overlappingTopics.size() ; j++) {
					queryLabels.remove(i+1) ;
				}
			}
		}

		return queryLabels ;
	}


	
	
	

	private class ExhaustiveDisambiguator {

		//TODO: make this use disambiguator in labelComparer instead.
		
		ArrayList<QueryLabel> queryTerms ;
		RelatednessCache rc ;

		Label.Sense currCombo[] ;
		Label.Sense bestCombo[] ;
		float bestComboWeight ;

		private TIntFloatHashMap bestSenseWeights ;
		
		public ExhaustiveDisambiguator(ArticleComparer comparer) {
			
			rc = new RelatednessCache(comparer) ;
			
		}

		public ArrayList<QueryLabel> disambiguate(ArrayList<QueryLabel> queryTerms, float minPriorProb) throws Exception {

			this.queryTerms = queryTerms ;

			this.currCombo = new Label.Sense[queryTerms.size()] ;

			this.bestCombo = null ;
			this.bestComboWeight = 0 ;

			bestSenseWeights = new TIntFloatHashMap() ;

			//recursively check and weight every possible combination of senses 
			checkSenses(0, minPriorProb) ;

			return queryTerms ;
		}

		public float getSenseWeight(int id) {
			return bestSenseWeights.get(id) ;
		}

		private void checkSenses(int termIndex, float minPriorProb) throws Exception {

			if (termIndex == queryTerms.size()) {

				//this is a complete (and unique) combination of senses, so lets weight it
				weightCombo() ;				
			} else {
				// this is not a complete combination of senses, so continue recursion 
				QueryLabel qt = queryTerms.get(termIndex) ;

				if (qt.isStopword || qt.getSenses().length == 0) {
					checkSenses(termIndex + 1, minPriorProb) ;
				} else {

					for(Label.Sense s:qt.getSenses()) {
						
						if (s.getPriorProbability() < minPriorProb)
							break ;
						
						currCombo[termIndex] = s ;
						checkSenses(termIndex + 1, minPriorProb) ;
					}
				}
			}
		}

		private void weightCombo() throws Exception {

			float commoness = 0 ;
			float relatedness = 0 ;
			int comparisons = 0 ;

			for (Label.Sense s1:currCombo) {

				if (s1 != null) {
					commoness += s1.getPriorProbability() ;

					//weight = weight + s1.getProbability() ;

					for (Label.Sense s2:currCombo) {
						if (s2 != null && s1.getId() != s2.getId()) { 
							relatedness += rc.getRelatedness(s1, s2) ;
							comparisons++ ;
						}
					}
				}
			}

			//average commonness and relatedness
			commoness = commoness / currCombo.length ;
			if (comparisons==0)
				relatedness = (float)0.5 ;
			else
				relatedness = relatedness/comparisons ;

			//relatedness is three times as important as commonness (hmmm, ad-hoc)
			float weight = (commoness + (3*relatedness))/4 ;

			//check if this is best overall combination			
			if (weight > bestComboWeight) {
				bestComboWeight = weight ;
				bestCombo = currCombo.clone() ;
			}

			//check if this is best weight for each individual sense
			for (Label.Sense s:currCombo) {
				if (s != null) {
					double sWeight = bestSenseWeights.get(s.getId()) ;
					if (sWeight < weight)
						bestSenseWeights.put(s.getId(),weight) ;
				}
			}
		}
	}


	private class QueryLabel extends Label {

		private Position position ;
		private boolean isStopword ;

		public QueryLabel(String trimmedNgram, String origNgram, Position pos, Wikipedia wikipedia) {

			super(wikipedia.getEnvironment(), trimmedNgram, wikipedia.getConfig().getDefaultTextProcessor()) ;

			isStopword = wikipedia.getConfig().isStopword(trimmedNgram) ;
			position = pos ; 
		}

		/**
		 * @param tr the topic reference to check for overlap
		 * @return true if this overlaps the given reference, otherwise false.
		 */
		public boolean overlaps(QueryLabel qt) {
			return position.overlaps(qt.getPosition()) ;
		}

		/**
		 * @return the position (start and end character locations) in the document where this reference was found.
		 */
		public Position getPosition() {
			return position ;
		}


		public int compareTo(QueryLabel qt) {

			//starts first, then goes first
			int c = new Integer(position.getStart()).compareTo(qt.getPosition().getStart()) ;
			if (c != 0) return c ;

			//starts at same time, so longest one goes first
			c = new Integer(qt.getPosition().getEnd()).compareTo(position.getEnd()) ;
			return c ;
		}
	}


















}
