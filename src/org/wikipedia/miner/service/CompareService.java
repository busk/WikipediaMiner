package org.wikipedia.miner.service;

import gnu.trove.TLongHashSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.comparison.ConnectionSnippet;
import org.wikipedia.miner.comparison.ConnectionSnippetWeighter;
import org.wikipedia.miner.comparison.LabelComparer;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.model.Page.PageType;
import org.wikipedia.miner.service.Service.ExampleBuilder;
import org.wikipedia.miner.service.param.BooleanParameter;
import org.wikipedia.miner.service.param.FloatParameter;
import org.wikipedia.miner.service.param.IntListParameter;
import org.wikipedia.miner.service.param.IntParameter;
import org.wikipedia.miner.service.param.ParameterGroup;
import org.wikipedia.miner.service.param.StringParameter;
import org.wikipedia.miner.util.RelatednessCache;
import org.wikipedia.miner.util.text.TextProcessor;


/**
 * 
 * This web service measures relatedness between:
 * <ul>
 * 	<li>pairs of terms (or phrases)</li>
 *  <li>pairs of article ids</li>
 *  <li>lists of article ids</li>
 * </ul>
 * 
 * Pairs of terms will be automatically disambiguated against each other, to be
 * resolved to pairs of articles.  
 * If comparing pairs of terms or pairs of article ids, then users can also 
 * obtain connective topics (articles that link to both things being compared)
 * and explanatory sentences (which mention both things being compared). 
 * 
 * @author David Milne
 */
public class CompareService extends Service{

	private static final long serialVersionUID = 537957588352023198L;
	
	private ParameterGroup grpTerms ;
	private StringParameter prmTerm1 ;
	private StringParameter prmTerm2 ;
	
	private ParameterGroup grpIds ;
	private IntParameter prmId1 ;
	private IntParameter prmId2 ;	
	
	private ParameterGroup grpIdLists ;
	private IntListParameter prmIdList1 ;
	private IntListParameter prmIdList2 ;
	
	private BooleanParameter prmInterpretations ;
	private BooleanParameter prmConnections ;
	private BooleanParameter prmSnippets ;
	private BooleanParameter prmTitles ;
	
	private IntParameter prmMaxConsConsidered ;
	private IntParameter prmMaxConsReturned ;
	private IntParameter prmMaxSnippetsConsidered ;
	private IntParameter prmMaxSnippetsReturned ;
	
	//private BooleanParameter prmEscape ;
	
	private FloatParameter prmMinRelatedness ;	
	
	
	private enum GroupName{termPair,idPair,idLists, none} ; 
	
	/**
	 * Initialises a new CompareService
	 */
	public CompareService() {
		
		super("core","Measures and explains the connections between Wikipedia articles ",
				"<p>This service measures the semantic relatedness between pairs of terms, pairs of article ids, or sets of article ids.</p>" + 
				"<p>The relatedness measures are calculated from the links going into and out of each page. Links that are common to both pages are used as evidence that they are related, while links that are unique to one or the other indicate the opposite.</p>",
				true, true 
		);
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		grpTerms = new ParameterGroup(GroupName.termPair.name(), "To compare two (potentially ambiguous) terms") ;
		prmTerm1 = new StringParameter("term1", "The first of two terms (or phrases) to compare", null) ;
		grpTerms.addParameter(prmTerm1) ;
		prmTerm2 = new StringParameter("term2", "The second of two terms (or phrases) to compare", null) ;
		grpTerms.addParameter(prmTerm2) ;		
		prmInterpretations = new BooleanParameter("interpretations", "if <b>true</b>, then the service will list different interpretations (combinations of senses for ambiguous terms) that were considered.", false) ;
		grpTerms.addParameter(prmInterpretations) ;
		addParameterGroup(grpTerms) ;
		
		grpIds = new ParameterGroup(GroupName.idPair.name(), "To compare two (unambiguous) article ids") ;
		prmId1 = new IntParameter("id1", "The first of two article ids to compare", null) ;
		grpIds.addParameter(prmId1) ;
		prmId2 = new IntParameter("id2", "The second of two article ids to compare", null) ;
		grpIds.addParameter(prmId2) ;
		addParameterGroup(grpIds) ;
		
		grpIdLists = new ParameterGroup(GroupName.idLists.name(), "To compare multiple article ids") ;
		prmIdList1 = new IntListParameter("ids1", "A list of page ids to compare", null) ;
		grpIdLists.addParameter(prmIdList1) ;
		prmIdList2 = new IntListParameter("ids2", "A second list of page ids to compare. If this is specified, then each article in <em>ids1</em> will be compared against every article in <em>ids2</em>. Otherwise, every article in <em>ids1</em> will be compared against every other article in <em>ids1</em>", new Integer[0]) ; 
		grpIdLists.addParameter(prmIdList2) ;
		prmMinRelatedness = new FloatParameter("minRelatedness", "The minimum relatedness a term pair must have before it will be returned.", 0F) ;
		grpIdLists.addParameter(prmMinRelatedness) ;
		addParameterGroup(grpIdLists) ;
		
		prmConnections = new BooleanParameter("connections", "if <b>true</b>, then the service will list articles that refer to both topics being compared. This parameter is ignored if comparing lists of ids.", false) ;
		addGlobalParameter(prmConnections) ;
		
		prmMaxConsConsidered = new IntParameter("maxConnectionsConsidered", "The maximum number of connections that will be gathered and weighted by thier relatedness to the articles being compared. This parameter is ignored if comparing lists of ids.", 1000) ;
		addGlobalParameter(prmMaxConsConsidered) ;
		
		prmMaxConsReturned = new IntParameter("maxConnectionsReturned", "The maximum number of connections that will be returned. These will be the highest weighted connections. This parameter is ignored if comparing lists of ids.", 250) ;
		addGlobalParameter(prmMaxConsReturned) ;
		
		prmSnippets = new BooleanParameter("snippets", "if <b>true</b>, then the service will list sentences that either mention both of the articles being compared, or come from one of the articles and mention the other. This parameter is ignored if comparing lists of ids.", false) ;
		addGlobalParameter(prmSnippets) ;
		
		prmMaxSnippetsConsidered = new IntParameter("maxSnippetsConsidered", "The maximum number of snippets that will be gathered and weighted. This parameter is ignored if comparing lists of ids.", 30) ;
		addGlobalParameter(prmMaxSnippetsConsidered) ;
		
		prmMaxSnippetsReturned = new IntParameter("maxSnippets", "The maximum number of snippets that will be returned. These will be the highest weighted snippets. This parameter is ignored if comparing lists of ids.", 10) ;
		addGlobalParameter(prmMaxSnippetsReturned) ;
		
		prmTitles = new BooleanParameter("titles", "if <b>true</b>, then the corresponding titles for article ids will be returned. This parameter is ignored if comparing terms", false) ;
		addGlobalParameter(prmTitles) ;
		
		addGlobalParameter(getHub().getFormatter().getEmphasisFormatParam()) ;
		addGlobalParameter(getHub().getFormatter().getLinkFormatParam()) ;
		
		addExample(
				new ExampleBuilder("To measure the relatedness between <i>kiwi</i> and <i>takahe</i>").
				addParam(prmTerm1, "kiwi").
				addParam(prmTerm2, "takahe").
				build()
		) ;
		
		addExample(
				new ExampleBuilder("To see full details of the same comparison").
				addParam(prmTerm1, "kiwi").
				addParam(prmTerm2, "takahe").
				addParam(prmInterpretations, true).
				addParam(prmConnections, true).
				addParam(prmSnippets, true).
				build()
		) ;
		
		Integer[] kiwi = {17362} ;
		Integer[] otherBirds = {711147,89074,89073} ;
		
		addExample(
				new ExampleBuilder("To compare <i>kiwi</i> to <i>takahe</i>, <i>kakapo</i> and <i>kea</i>").
				addParam(prmIdList1, kiwi).
				addParam(prmIdList2, otherBirds).
				addParam(prmTitles, true).
				build()
		) ;
		
		
		
		//prmEscape = new BooleanParameter("escapeDefinition", "<true> if sentence snippets should be escaped, <em>false</em> if they are to be encoded directly", false) ;
		//addGlobalParameter(prmEscape) ;
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request, Element xmlResponse) throws Exception {
		
		Wikipedia wikipedia = getWikipedia(request) ;
		TextProcessor tp = wikipedia.getEnvironment().getConfiguration().getDefaultTextProcessor() ;
		
		ParameterGroup grp = getSpecifiedParameterGroup(request) ;
		
		if (grp == null) {
			xmlResponse.setAttribute("unspecifiedParameters", "true") ;
			return xmlResponse ;
		}
			
		switch(GroupName.valueOf(grp.getName())) {
		
		case termPair :
			
			LabelComparer lblComparer = getHub().getLabelComparer(getWikipediaName(request)) ;
			if (lblComparer == null) {
				this.buildErrorResponse("term comparisons are not available with this wikipedia instance", xmlResponse) ;
				return xmlResponse ;
			}
				
			
			String term1 = prmTerm1.getValue(request).trim() ;
			String term2 = prmTerm2.getValue(request).trim() ;
			
			Label label1 = new Label(wikipedia.getEnvironment(), term1, tp) ;
			Label.Sense[] senses1 = label1.getSenses() ; 

			if (senses1.length == 0) {
				xmlResponse.setAttribute("unknownTerm", term1) ; 
				return xmlResponse ;
			}
			
			Label label2 = new Label(wikipedia.getEnvironment(), term2, tp) ;
			Label.Sense[] senses2 = label2.getSenses() ; 

			if (senses2.length == 0) {
				xmlResponse.setAttribute("unknownTerm", term2) ; 
				return xmlResponse ;
			}
			
			LabelComparer.ComparisonDetails details = lblComparer.compare(label1, label2) ;
			
			xmlResponse.setAttribute("relatedness", getHub().format(details.getLabelRelatedness())) ;
		
			addInterpretations(xmlResponse, senses1, senses2, details, request) ;
			
			LabelComparer.SensePair bestInterpretation = details.getBestInterpretation() ;
			if (bestInterpretation != null) 
				addMutualLinksOrSnippets(xmlResponse, bestInterpretation.getSenseA(), bestInterpretation.getSenseB(), request, wikipedia) ;
			else 
				this.buildWarningResponse("Could not identify plausable senses for the given terms", xmlResponse) ;
			
			break ;
			
		case idPair:
			
			ArticleComparer artComparer = getHub().getArticleComparer(getWikipediaName(request)) ;
			if (artComparer == null) {
				this.buildErrorResponse("article comparisons are not available with this wikipedia instance", xmlResponse) ;
				return xmlResponse ;
			}
			
			Article art1 = new Article(wikipedia.getEnvironment(), prmId1.getValue(request)) ;
			if (!(art1.getType() == PageType.article || art1.getType() == PageType.disambiguation)) {
				xmlResponse.setAttribute("unknownId", String.valueOf(prmId1.getValue(request))) ; 
				return xmlResponse ;
			}

			Article art2 = new Article(wikipedia.getEnvironment(), prmId2.getValue(request)) ;
			if (!(art2.getType() == PageType.article || art2.getType() == PageType.disambiguation)) {
				xmlResponse.setAttribute("unknownId", String.valueOf(prmId2.getValue(request))) ; 
				return xmlResponse ;
			}
			
			xmlResponse.setAttribute("relatedness", getHub().format(artComparer.getRelatedness(art1, art2))) ;
			
			if (prmTitles.getValue(request)) {
				xmlResponse.setAttribute("title1", art1.getTitle()) ;
				xmlResponse.setAttribute("title2", art2.getTitle()) ;
			}
			
			addMutualLinksOrSnippets(xmlResponse, art1, art2, request, wikipedia) ;
			break ;
		case idLists :
			
			artComparer = getHub().getArticleComparer(getWikipediaName(request)) ;
			if (artComparer == null) {
				this.buildErrorResponse("article comparisons are not available with this wikipedia instance", xmlResponse) ;
				return xmlResponse ;
			}
			
			TreeSet<Integer> invalidIds = new TreeSet<Integer>() ;
			
			//gather articles from ids1 ;
			TreeSet<Article> articles1 = new TreeSet<Article>() ;
			for (Integer id:prmIdList1.getValue(request)) {
				try {
					Article art = (Article)wikipedia.getPageById(id) ;
					articles1.add(art) ;
				} catch (Exception e) {
					invalidIds.add(id) ;
				}
			}
			
			//gather articles from ids2 ;
			TreeSet<Article> articles2 = new TreeSet<Article>() ;
			for (Integer id:prmIdList2.getValue(request)) {
				try {
					Article art = (Article)wikipedia.getPageById(id) ;
					articles2.add(art) ;
				} catch (Exception e) {
					invalidIds.add(id) ;
				}
			}
			
			if (!invalidIds.isEmpty()) {
				StringBuffer sb = new StringBuffer("Invalid ids: ") ;
				
				for (int id:invalidIds) {
					sb.append(id) ;
					sb.append(", ") ;
				}
				
				xmlResponse = this.buildWarningResponse(sb.substring(0,sb.length()-2) + ".", xmlResponse) ;
			}
			
			//if ids2 is not specified, then we want to compare each item in ids1 with every other one
			if (articles2.isEmpty())
				articles2 = articles1 ;
			
			TLongHashSet doneKeys = new TLongHashSet() ;
			
			Element xmlMeasures = getHub().createElement("Measures") ;
			
			float minRelatedness = prmMinRelatedness.getValue(request) ;
			boolean showTitles = prmTitles.getValue(request) ;
			
			for (Article a1:articles1) {
				for (Article a2:articles2) {
					
					if (a1.equals(a2))
						continue ;
							
					
					//relatedness is symmetric, so create a unique key for this pair of ids were order doesnt matter 
					Article min, max ;
					
					if (a1.getId() < a2.getId()) {
						min = a1 ;
						max = a2 ;
					} else {
						min = a2 ;
						max = a1 ;
					}
					
					//long min = Math.min(a1.getId(), a2.getId()) ;
					//long max = Math.max(a1.getId(), a2.getId()) ;
					long key = ((long)min.getId()) + (((long)max.getId()) << 30) ;
					
					if(doneKeys.contains(key))
						continue ;
					
					double relatedness = artComparer.getRelatedness(a1, a2)  ;
					
					if (relatedness >= minRelatedness) {
					
						Element xmlMeasure = getHub().createElement("Measure") ;
						xmlMeasure.setAttribute("lowId", String.valueOf(min.getId())) ;
						xmlMeasure.setAttribute("highId", String.valueOf(max.getId())) ;
						
						if (showTitles) {
							xmlMeasure.setAttribute("lowTitle", min.getTitle()) ;
							xmlMeasure.setAttribute("highTitle", max.getTitle()) ;
						}
						
						xmlMeasure.appendChild(getHub().createTextNode(getHub().format(relatedness))) ;
						
						xmlMeasures.appendChild(xmlMeasure) ;
					}
					
					doneKeys.add(key) ;
				}
			}
			
			xmlResponse.appendChild(xmlMeasures) ;
			
			break ;
		}

		return xmlResponse ;
	}
	
	
	private Element addInterpretations(Element response, Label.Sense[] senses1, Label.Sense[] senses2, LabelComparer.ComparisonDetails details, HttpServletRequest request) {
		
		if (!prmInterpretations.getValue(request)) 
			return response ;
		
		Element xmlInterpretations = getHub().createElement("Interpretations") ;
		xmlInterpretations.setAttribute("term1Candidates", String.valueOf(senses1.length)) ;
		xmlInterpretations.setAttribute("term2Candidates", String.valueOf(senses2.length)) ;
		
		
		for (LabelComparer.SensePair interpretation:details.getCandidateInterpretations()) {
			
			Element xmlInterpretation = getHub().createElement("Interpretation") ;
			
			xmlInterpretation.setAttribute("id1", String.valueOf(interpretation.getSenseA().getId())) ;
			xmlInterpretation.setAttribute("title1", interpretation.getSenseA().getTitle()) ;
			
			xmlInterpretation.setAttribute("id2", String.valueOf(interpretation.getSenseB().getId())) ;
			xmlInterpretation.setAttribute("title2", interpretation.getSenseB().getTitle()) ;
			
			xmlInterpretation.setAttribute("relatedness", getHub().format(interpretation.getSenseRelatedness())) ;
			xmlInterpretation.setAttribute("disambiguationConfidence", getHub().format(interpretation.getDisambiguationConfidence())) ;
			
			xmlInterpretations.appendChild(xmlInterpretation) ;
		}
		
		response.appendChild(xmlInterpretations) ;
		return response ;
	}
	
	
	
	private Element addMutualLinksOrSnippets(Element response, Article art1, Article art2, HttpServletRequest request, Wikipedia wikipedia) throws Exception {

		boolean getConnections = prmConnections.getValue(request) ;
		boolean getSnippets = prmSnippets.getValue(request) ;
		
		if (!getConnections && !getSnippets)
			return response;
		
		
		//Build a list of pages that link to both art1 and art2, ordered by average relatedness to them
		TreeSet<Article> connections = new TreeSet<Article>() ;
		RelatednessCache rc = new RelatednessCache(getHub().getArticleComparer(getWikipediaName(request))) ;

		Article[] links1 = art1.getLinksIn() ;
		Article[] links2 = art2.getLinksIn() ;

		int index1 = 0 ;
		int index2 = 0 ;
		
		int maxConsConsidered = prmMaxConsConsidered.getValue(request) ;

		while (index1 < links1.length && index2 < links2.length) {

			Article link1 = links1[index1] ;
			Article link2 = links2[index2] ;

			int compare = link1.compareTo(link2) ;

			if (compare == 0) {
				if (link1.compareTo(art1)!= 0 && link2.compareTo(art2)!= 0) {

					double weight = (rc.getRelatedness(link1, art1) + rc.getRelatedness(link1, art2))/2 ;
					link1.setWeight(weight) ;
					connections.add(link1) ;

					
					if (connections.size() > maxConsConsidered) break  ;
				}

				index1 ++ ;
				index2 ++ ;
			} else {
				if (compare < 0)
					index1 ++ ;
				else 
					index2 ++ ;
			}
		}

		int maxConsReturned = prmMaxConsReturned.getValue(request) ;
		
		if (getConnections) {

			Element xmlConnections = getHub().createElement("Connections");
			response.appendChild(xmlConnections) ;

			int c = 0 ;

			for (Article connection:connections) {
				if (c++ >= maxConsReturned) break ;

				Element xmlCon = getHub().createElement("Connection");
				xmlCon.setAttribute("title", connection.getTitle()) ;
				xmlCon.setAttribute("id", String.valueOf(connection.getId())) ;
				xmlCon.setAttribute("relatedness1", getHub().format(rc.getRelatedness(connection, art1))) ;
				xmlCon.setAttribute("relatedness2", getHub().format(rc.getRelatedness(connection, art2))) ;
				xmlConnections.appendChild(xmlCon) ;
			}
			
			response.appendChild(xmlConnections) ;
		}

		if (getSnippets) {

			Element xmlSnippets = getHub().createElement("Snippets");
			response.appendChild(xmlSnippets) ;

			int snippetsCollected=0 ;
			int maxSnippetsConsidered = prmMaxSnippetsConsidered.getValue(request) ;
			
			
			//gather and weight snippets
			
			ConnectionSnippetWeighter snippetWeighter = getHub().getConnectionSnippetWeighter(getWikipediaName(request)) ;
			
			TreeSet<ConnectionSnippet> snippets = new TreeSet<ConnectionSnippet>() ;

			//look for snippets in art1 which mention art2
			for (int sentenceIndex:art1.getSentenceIndexesMentioning(art2)) {
				ConnectionSnippet s = new ConnectionSnippet(sentenceIndex, art1, art1, art2) ;
				s.setWeight(snippetWeighter.getWeight(s)) ;
				snippets.add(s) ;
			}
			
			//look for snippets in art2 which mention art1
			for (int sentenceIndex:art2.getSentenceIndexesMentioning(art1)) {
				ConnectionSnippet s = new ConnectionSnippet(sentenceIndex, art2, art1, art2) ;
				s.setWeight(snippetWeighter.getWeight(s)) ;
				snippets.add(s) ;
			}

			ArrayList<Article> articlesOfInterest = new ArrayList<Article>() ;
			articlesOfInterest.add(art1) ;
			articlesOfInterest.add(art2) ;
			
			for (Article connection:connections) {
				
				if (snippets.size() >= maxSnippetsConsidered)
					break ;
				
				for (int sentenceIndex:connection.getSentenceIndexesMentioning(articlesOfInterest)) {
					if (snippets.size() >= maxSnippetsConsidered)
						break ;
					
					ConnectionSnippet s = new ConnectionSnippet(sentenceIndex, connection, art1, art2) ;
					s.setWeight(snippetWeighter.getWeight(s)) ;
					snippets.add(s) ;
				}
			}
			
			Pattern labelPattern = getLabelMatchingPattern(art1, art2) ;
			
			int maxSnippetsReturned = prmMaxSnippetsReturned.getValue(request) ;
			
			int snippetsReturned = 0 ;
			for (ConnectionSnippet snippet:snippets) {
				snippetsReturned ++ ;
				if (snippetsReturned > maxSnippetsReturned)
					break ;
				
				String sentence = snippet.getPlainText() ;
				sentence = emphasizePatternMatches(sentence, labelPattern) ;
				sentence = getHub().getFormatter().format(sentence, request, wikipedia) ;

				Element xmlSnippet = getHub().createCDATAElement("Snippet", sentence);
				xmlSnippet.setAttribute("sourceId", String.valueOf(snippet.getSource().getId())) ;
				xmlSnippet.setAttribute("sourceTitle", snippet.getSource().getTitle()) ;
				xmlSnippet.setAttribute("sentenceIndex", String.valueOf(snippet.getSentenceIndex())) ;
				xmlSnippet.setAttribute("weight", getHub().format(snippet.getWeight())) ;
				
				xmlSnippets.appendChild(xmlSnippet) ;
				snippetsCollected ++ ;
			}

			response.appendChild(xmlSnippets) ;
		}

		return response ;
	}
	
	private String emphasizePatternMatches(String sentence, Pattern pattern) {
		
		String sentence2 = " " + sentence + " " ;
		
		Matcher m = pattern.matcher(sentence2) ;
		
		StringBuffer sb = new StringBuffer() ;
		int lastCopyIndex = 0 ;
		
		while (m.find()) {
			sb.append(sentence2.substring(lastCopyIndex, m.start())) ;
			sb.append("'''") ;
			sb.append(m.group()) ;
			sb.append("'''") ;
			lastCopyIndex = m.end() ;
		}
		sb.append(sentence2.substring(lastCopyIndex)) ;
		
		return sb.toString().trim();
	}
	
	private Pattern getLabelMatchingPattern(Article art1, Article art2) {
		StringBuffer labelRegex = new StringBuffer("(?<=\\W)(") ;
		
		for (Article.Label lbl:art1.getLabels()) {
			
			if (lbl.getLinkOccCount() > 3) {
				labelRegex.append(lbl.getText()) ;
				labelRegex.append("|") ;
			}
		}
		
		for (Article.Label lbl:art2.getLabels()) {
			
			if (lbl.getLinkOccCount() > 3) {
				labelRegex.append(lbl.getText()) ;
				labelRegex.append("|") ;
			}
		}
		
		labelRegex.deleteCharAt(labelRegex.length()-1) ;
		labelRegex.append(")(?=\\W)") ;
		
		return Pattern.compile(labelRegex.toString(), Pattern.CASE_INSENSITIVE) ;
	}
}
