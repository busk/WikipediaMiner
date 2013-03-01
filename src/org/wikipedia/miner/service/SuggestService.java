package org.wikipedia.miner.service;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.db.struct.DbLinkLocation;
import org.wikipedia.miner.db.struct.DbLinkLocationList;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Category;
import org.wikipedia.miner.model.Page;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.service.param.FloatParameter;
import org.wikipedia.miner.service.param.IntListParameter;
import org.wikipedia.miner.service.param.IntParameter;
import org.wikipedia.miner.util.RelatednessCache;

public class SuggestService extends Service {

	private static final long serialVersionUID = 2890788121538938947L;

	private IntListParameter prmQueryTopics ;
	
	private IntParameter prmSuggestionLimit ;
	private IntParameter prmCategoryLimit ;
	private IntParameter prmSearchSpace ;
	
	private FloatParameter prmMinIndividualRelatedness ;
	private FloatParameter prmMinAverageRelatedness ;
	
	public SuggestService() {
		super("query","Suggests alternative topics that are related to a set of seed topics",
				"<p>This service takes a set of seed topics, and suggests articles that relate to them. These suggestions are weighted by thier relatedness to the query, and organized by the categories they belong to.</p>" +
				"<p>It is designed to be used in conjunction with the <a href='services.html?search'>search</a> service</p>",
				true, false);
		
		prmQueryTopics = new IntListParameter("queryTopics", "A set of topic ids that suggestons should relate to", null) ;
		addGlobalParameter(prmQueryTopics) ;
		
		prmSuggestionLimit = new IntParameter("maxSuggestions", "Maximum number of suggested topics to return", 250) ;
		addGlobalParameter(prmSuggestionLimit) ;
		
		prmCategoryLimit = new IntParameter("maxCategories", "Maximum number of categories to organize suggestions under", 25) ;
		addGlobalParameter(prmCategoryLimit) ;
		
		prmSearchSpace = new IntParameter("searchSpace", "Maximum number of rough suggestions to search. Increasing this will likely provide better suggestions, but slower responses", 100000) ;
		addGlobalParameter(prmSearchSpace) ;
		
		prmMinIndividualRelatedness = new FloatParameter("minIndividualRelatedness", "Minimum relatedness a suggestion must have to each query topic", 0.2F) ;
		addGlobalParameter(prmMinIndividualRelatedness) ;
		
		prmMinAverageRelatedness = new FloatParameter("minAverageRelatedness", "Minimum average relatedness a suggestion must have to all query topics", 0.3F) ;
		addGlobalParameter(prmMinAverageRelatedness) ;
		
		Integer[] topics = {147313, 4913064} ;
		
		addExample(
			new ExampleBuilder("To see suggestions for <i>hiking new zealand</i>")
			.addParam(prmQueryTopics, topics)
			.build() 
		) ;
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element xmlResponse) throws Exception {
		
		Integer[] queryTopicIds = prmQueryTopics.getValue(request) ;
		
		if (queryTopicIds == null || queryTopicIds.length == 0) {
			xmlResponse.setAttribute("unspecifiedParameters", "true") ;
			return xmlResponse ;
		}
		
		Wikipedia wikipedia = this.getWikipedia(request) ;
		
		//identify query topics
		HashMap<Integer,Article> queryTopics = new HashMap<Integer,Article>() ;
		for (int id:queryTopicIds) {
			Page page = wikipedia.getPageById(id) ;
			if (page.exists() && page.getType() == PageType.article) 
				queryTopics.put(id, (Article)page) ;
		}
		
		if (queryTopics.isEmpty()) {
			buildErrorResponse("no valid query topic ids specified", xmlResponse) ;
			return xmlResponse ;
		}
			
		//gather roughly weighted suggestions
		TreeSet<Article> roughSuggestions = getRoughSuggestions(queryTopics, wikipedia) ;

		//refine suggestions 
		List<Article> refinedSuggestions = getRefinedSuggestions(roughSuggestions, queryTopics, wikipedia, request) ;

		// gather categories
		TIntObjectHashMap<SuggestionCategory> categoriesById = getCategoriesById(refinedSuggestions) ;
		
		// refine and sort categories, identify categorized topics
		TIntHashSet categorizedIds = new TIntHashSet() ;
		ArrayList<SuggestionCategory> refinedCategories = getSortedCategories(categoriesById, categorizedIds, request) ;
		
		//build xml response
		for (SuggestionCategory cat: refinedCategories) 
			xmlResponse.appendChild(getCategoryXml(cat)) ;
		
		xmlResponse.appendChild(getUncategorizedXml(refinedSuggestions, categorizedIds)) ;
		return xmlResponse;
	}
	
	
	private TreeSet<Article> getRoughSuggestions(HashMap<Integer,Article> queryTopics, Wikipedia wikipedia) {

		// get a rough ranking of suggestions just from overlaps of ids
		TIntIntHashMap roughWeights = new TIntIntHashMap() ;

		for (Article topic:queryTopics.values()){

			//gather rough suggestions from all out links
			DbLinkLocationList outLinks = wikipedia.getEnvironment().getDbPageLinkOut().retrieve(topic.getId()) ;
			if (outLinks != null && outLinks.getLinkLocations() != null) {
				for (DbLinkLocation outLink:outLinks.getLinkLocations()) {
					
					
					Integer weight = roughWeights.get(outLink.getLinkId()) ;
					if (weight == null) weight = 0 ;
					roughWeights.put(outLink.getLinkId(), weight+outLink.getSentenceIndexes().size()) ;
				}
			}

			//gather rough suggestions from all in links
			DbLinkLocationList inLinks = wikipedia.getEnvironment().getDbPageLinkIn().retrieve(topic.getId()) ;
			if (inLinks != null && inLinks.getLinkLocations() != null) {
				for (DbLinkLocation inLink:inLinks.getLinkLocations()) {
					Integer weight = roughWeights.get(inLink.getLinkId()) ;
					if (weight == null) weight = 0 ;
					roughWeights.put(inLink.getLinkId(), weight+inLink.getSentenceIndexes().size()) ;
				}
			}
		}
		
		//sort rough suggestions
		TreeSet<Article> roughSuggestions = new TreeSet<Article>() ;
		TIntIntIterator iter = roughWeights.iterator() ;
		while (iter.hasNext()) {
			iter.advance() ;
			

			if (!queryTopics.containsKey(iter.key())) {
				Article rs = new Article(wikipedia.getEnvironment(), iter.key()) ;
				
				//System.out.println("rs:" + rs + ", " + iter.value()) ;
				
				rs.setWeight((double)iter.value()) ;
				roughSuggestions.add(rs) ;
			}
		}
		
		//for (Article art:roughSuggestions) {
		//	System.out.println("gatheredRs: " + art + ", " + getHub().format(art.getWeight())) ;
		//}

		System.out.println(roughSuggestions.size() + " rough suggestions") ;
		return roughSuggestions ;
	}

	private List<Article> getRefinedSuggestions(TreeSet<Article> roughSuggestions, HashMap<Integer, Article> queryTopics, Wikipedia wikipedia, HttpServletRequest request) throws Exception {

		ArrayList<Article> refinedSuggestions = new ArrayList<Article>() ;
		
		int searchSpace = prmSearchSpace.getValue(request) ;
		
		float minIndividualRelatedness = prmMinIndividualRelatedness.getValue(request) ;
		float minAvgRelatedness = prmMinAverageRelatedness.getValue(request) ;
		
		RelatednessCache rc = new RelatednessCache(new ArticleComparer(wikipedia)) ;
		
		int c=0 ;
		for (Article suggestion:roughSuggestions) {

			if (c++ > searchSpace) break ;

			try {
				if (suggestion.getType() != PageType.article)
					continue ;

				double relatedness = 0 ;
				for (Article topic:queryTopics.values()) {
					double r = rc.getRelatedness(topic, suggestion) ;

					if (r < minIndividualRelatedness) {
						suggestion = null ;
						break ;
					} else {
						relatedness = relatedness + r ;
					}
				}

				if (suggestion == null)
					continue ;

				relatedness = relatedness/queryTopics.size() ;

				if (relatedness < minAvgRelatedness)
					continue ;

				suggestion.setWeight(relatedness) ;
				refinedSuggestions.add(suggestion) ;

			} catch (Exception e) { 
				System.out.println(e.getMessage()) ;
				e.printStackTrace() ;
				
			};
		}

		Collections.sort(refinedSuggestions) ; 

		return refinedSuggestions.subList(0, Math.min(refinedSuggestions.size(), prmSuggestionLimit.getValue(request))) ;	
	}
	
	private TIntObjectHashMap<SuggestionCategory> getCategoriesById(List<Article> suggestions)  {

		TIntObjectHashMap<SuggestionCategory> categoriesById = new TIntObjectHashMap<SuggestionCategory>() ;

		for (Article suggestion:suggestions) {
			for (Category cat : suggestion.getParentCategories()) {

				SuggestionCategory category = categoriesById.get(cat.getId()) ;

				if (category == null)
					category = new SuggestionCategory(cat) ;

				category.addSuggestion(suggestion) ;

				categoriesById.put(cat.getId(), category) ;
			}
		}

		return categoriesById ;
	}

	private ArrayList<SuggestionCategory> getSortedCategories(TIntObjectHashMap<SuggestionCategory> categoriesById, TIntHashSet categorizedIds, HttpServletRequest request) {

		//sort categories according to the weights of the articles they contain, discarding those that are too small
		ArrayList<SuggestionCategory> weightedCategories = new ArrayList<SuggestionCategory>() ;
		
		TIntObjectIterator<SuggestionCategory> iter = categoriesById.iterator() ; 
		while (iter.hasNext()) {
			iter.advance() ;
			SuggestionCategory cat = iter.value() ;
			cat.recalculateWeight() ;

			//TODO: make configurable?
			if (cat.getSuggestions().size() > 3 && cat.getWeight() > 1) {
				weightedCategories.add(cat) ;
			}
		}
		Collections.sort(weightedCategories) ;


		//reweight categories to ignore weights of duplicate articles
		TIntHashSet seenIds = new TIntHashSet() ;

		Iterator<SuggestionCategory> iter2 = weightedCategories.iterator() ;
		while (iter2.hasNext()) {
			SuggestionCategory cat = iter2.next() ;

			for(Article art:cat.getSuggestions()) {

				if (seenIds.contains(art.getId())) {
					//this was seen in a higher-ranked category, so don't count it as part of the category weight.
					cat.ignore(art.getId()) ;
				} else {				
					seenIds.add(art.getId()) ;
				}
			}
			cat.recalculateWeight() ;
		}
		
		//re-sort refined categories
		Collections.sort(weightedCategories) ;
		
		
		int maxCategories = prmCategoryLimit.getValue(request) ;
		
		ArrayList<SuggestionCategory> refinedCategories = new ArrayList<SuggestionCategory>() ;
		for (SuggestionCategory cat: weightedCategories){
			
			if (refinedCategories.size() >= maxCategories) break ;
			
			if (cat.getWeight() > 1.5 && cat.getNonIgnoredSize() > 2) {
				// keep this category			
				refinedCategories.add(cat) ;
				for (Article art:cat.getSuggestions())
					categorizedIds.add(art.getId()) ;
			}
		}

		return refinedCategories ;		
	}
	
	private Element getCategoryXml(SuggestionCategory cat) {
		
		Element xmlCat = getHub().createElement("SuggestionCategory") ;
		xmlCat.setAttribute("id", String.valueOf(cat.getId())) ;
		xmlCat.setAttribute("title", cat.getTitle()) ;
		xmlCat.setAttribute("weight", getHub().format(cat.getWeight())) ;
		
		xmlCat.setAttribute("totalSuggestions", String.valueOf(cat.getSuggestions().size())) ;
		
		for (Article suggestion:cat.getSuggestions()) 
			xmlCat.appendChild(getSuggestionXml(suggestion)) ;
		
		return xmlCat ;
	}
	
	private Element getSuggestionXml(Article suggestion) {
		
		Element xmlSuggestion = getHub().createElement("Suggestion") ;
		xmlSuggestion.setAttribute("id", String.valueOf(suggestion.getId())) ;
		xmlSuggestion.setAttribute("title", suggestion.getTitle()) ;
		xmlSuggestion.setAttribute("weight", getHub().format(suggestion.getWeight())) ;

		return xmlSuggestion ;
	}
	
	private Element getUncategorizedXml(List<Article>allSuggestions, TIntHashSet categorizedIds) {
		
		int uncategorizedCount = 0 ;
		
		Element xmlUncategorized = getHub().createElement("UncategorizedSuggestions") ;
		for (Article art:allSuggestions) {
			if (!categorizedIds.contains(art.getId())) {
				uncategorizedCount ++ ;
				xmlUncategorized.appendChild(getSuggestionXml(art)) ;
			}
		}	
		xmlUncategorized.setAttribute("totalSuggestions", String.valueOf(uncategorizedCount)) ;
		
		return xmlUncategorized ;
	}
	
	private class SuggestionCategory extends Category {

		private ArrayList<Article> suggestions ;
		private TIntHashSet idsToIgnore ;

		public SuggestionCategory(Category cat) {
			super(cat.getEnvironment(), cat.getId()) ;

			suggestions = new ArrayList<Article>() ;
			idsToIgnore = new TIntHashSet() ;
			setWeight(0.0) ;
		}

		public void addSuggestion(Article article) {
			suggestions.add(article) ;
			//weight = weight + article.getWeight() ;
		}

		public ArrayList<Article> getSuggestions() {
			return suggestions ;
		}

		public void ignore(int id) {
			idsToIgnore.add(id) ;			
		}

		public boolean isIgnored(int id) {
			return idsToIgnore.contains(id) ;			
		}

		public int getNonIgnoredSize() {
			return suggestions.size() - idsToIgnore.size() ;			
		}

		public double recalculateWeight() {

			setWeight(0.0) ;
			int c = 0 ;

			for (Article a: suggestions) {

				if (!idsToIgnore.contains(id)) {
					setWeight(getWeight() + a.getWeight()) ;

					//only consider weight of top 3 articles?
					if (c++ >= 3) break ;
				}
			}

			return weight ;
		}


	}
}
