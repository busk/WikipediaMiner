package org.wikipedia.miner.service;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Category;
import org.wikipedia.miner.model.Page;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.service.param.BooleanParameter;
import org.wikipedia.miner.service.param.EnumParameter;
import org.wikipedia.miner.service.param.IntParameter;
import org.wikipedia.miner.service.param.ParameterGroup;
import org.wikipedia.miner.service.param.StringParameter;

public class ExploreArticleService extends Service{
	
	//TODO:modify freebase image request to use article titles rather than ids
	//TODO:if lang is not en, use languageLinks to translate article title to english.

	private enum GroupName{id,title} ; 
	public enum DefinitionLength{LONG, SHORT} ;

	private Pattern fb_imagePattern = Pattern.compile("\"image\"\\:\\[(.*?)\\]") ;
	private Pattern fb_idPattern = Pattern.compile("\"id\"\\:\"(.*?)\"") ;
	
	private ParameterGroup grpId ;
	private IntParameter prmId ;
	
	private ParameterGroup grpTitle ;
	private StringParameter prmTitle ;
	
	private BooleanParameter prmDefinition;
	private EnumParameter<DefinitionLength> prmDefinitionLength ;
	
	private BooleanParameter prmLabels ;
	
	private BooleanParameter prmTranslations ;
	
	private BooleanParameter prmImages ;
	private IntParameter prmImageWidth ;
	private IntParameter prmImageHeight ;
	
	private BooleanParameter prmParentCategories ;
	
	private BooleanParameter prmInLinks ;
	private IntParameter prmInLinkMax ;
	private IntParameter prmInLinkStart ;
	
	private BooleanParameter prmOutLinks ;
	private IntParameter prmOutLinkMax ;
	private IntParameter prmOutLinkStart ;
	
	private BooleanParameter prmLinkRelatedness ;
	
	
	public ExploreArticleService() {
		
		super("core","Provides details of individual articles",
				
				"<p></p>",
				true, false
			);
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		
		grpId = new ParameterGroup(GroupName.id.name(), "To retrieve article by  id") ;
		prmId = new IntParameter("id", "The unique identifier of the article to explore", null) ;
		grpId.addParameter(prmId) ;
		addParameterGroup(grpId) ;
		
		grpTitle = new ParameterGroup(GroupName.title.name(), "To retrieve article by title") ;
		prmTitle = new StringParameter("title", "The (case sensitive) title of the article to explore", null) ;
		grpTitle.addParameter(prmTitle) ;
		addParameterGroup(grpTitle) ;
		
		
		prmDefinition = new BooleanParameter("definition", "<b>true</b> if a snippet definition should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmDefinition) ;
		
		String[] descLength = {"first paragraph", "first sentence"} ;
		prmDefinitionLength = new EnumParameter<DefinitionLength>("definitionLength", "The required length of the definition", DefinitionLength.SHORT, DefinitionLength.values(), descLength) ;
		addGlobalParameter(prmDefinitionLength) ;

		addGlobalParameter(getHub().getFormatter().getEmphasisFormatParam()) ;
		addGlobalParameter(getHub().getFormatter().getLinkFormatParam()) ;
		
		prmLabels = new BooleanParameter("labels", "<b>true</b> if labels (synonyms, etc) for this topic are to be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmLabels) ;

		prmTranslations = new BooleanParameter("translations", "<b>true</b> if translations (language links) for this topic are to be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmTranslations) ;

		prmImages = new BooleanParameter("images", "Whether or not to retrieve relevant image urls from freebase", false) ;
		addGlobalParameter(prmImages) ;

		prmImageWidth = new IntParameter("maxImageWidth", "Images can be scaled. This defines their maximum width, in pixels", 150) ;
		addGlobalParameter(prmImageWidth) ;

		prmImageHeight = new IntParameter("maxImageHeight", "Images can be scaled. This defines their maximum height, in pixels", 150) ;
		addGlobalParameter(prmImageHeight) ;
		
		prmParentCategories = new BooleanParameter("parentCategories", "<b>true</b> if parent categories of this category should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmParentCategories) ;
		
		prmInLinks = new BooleanParameter("inLinks", "<b>true</b> if articles that link to this article should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmInLinks) ;

		prmInLinkMax = new IntParameter("inLinkMax", "the maximum number of in-links that should be returned. A max of <b>0</b> will result in all in-links being returned", 250) ;
		addGlobalParameter(prmInLinkMax) ;
		
		prmInLinkStart = new IntParameter("inLinkStart", "the index of the first in-link to return. Combined with <b>inLinkMax</b>, this parameter allows the user to page through large lists of in-links", 0) ;
		addGlobalParameter(prmInLinkStart) ;
		
		prmOutLinks = new BooleanParameter("outLinks", "<b>true</b> if articles that this article links to should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmOutLinks) ;
		
		prmOutLinkMax = new IntParameter("outLinkMax", "the maximum number of out-links that should be returned. A max of <b>0</b> will result in all out-links being returned", 250) ;
		addGlobalParameter(prmOutLinkMax) ;
		
		prmOutLinkStart = new IntParameter("outLinkStart", "the index of the first out-link to return. Combined with <b>outLinkMax</b>, this parameter allows the user to page through large lists of out-links", 0) ;
		addGlobalParameter(prmOutLinkStart) ;
		
		prmLinkRelatedness = new BooleanParameter("linkRelatedness", "<b>true</b> if the relatedness of in- and out-links should be measured, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmLinkRelatedness) ;
		
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element xmlResponse) throws Exception {
		
		Wikipedia wikipedia = getWikipedia(request) ;
		
		ArticleComparer artComparer = null ;
		if (prmLinkRelatedness.getValue(request)) {
			artComparer = getHub().getArticleComparer(this.getWikipediaName(request)) ;
			if (artComparer == null) 
				this.buildWarningResponse("Relatedness measures are unavalable for this instance of wikipedia", xmlResponse) ;
		}
		
		ParameterGroup grp = getSpecifiedParameterGroup(request) ;
		
		if (grp == null) {
			xmlResponse.setAttribute("unspecifiedParameters", "true") ;
			return xmlResponse ;
		}
		
		Article art = null ;
		
		switch(GroupName.valueOf(grp.getName())) {
		
		case id :
			Integer id = prmId.getValue(request) ;
			
			Page page = wikipedia.getPageById(id) ;
			if (page==null) 
				return buildErrorResponse("'" + id + "' is an unknown id", xmlResponse) ;
			
			switch(page.getType()) {
			case disambiguation:
				case article:
					art = (Article)page ;
					break ;
				default:
					return buildErrorResponse("'" + id + "' is not an article id", xmlResponse) ;
			}
			break ;
		case title :
			String title = prmTitle.getValue(request) ;
			art = wikipedia.getArticleByTitle(title) ;
			
			if (art == null)
				return buildErrorResponse("'" + title + "' is an unknown article title", xmlResponse) ;
			break ;
		}
		
		xmlResponse.setAttribute("id", String.valueOf(art.getId()));
		xmlResponse.setAttribute("title", String.valueOf(art.getTitle()));
		
		
		if (prmDefinition.getValue(request)) {
			String definition = null ;

			if (prmDefinitionLength.getValue(request)==DefinitionLength.SHORT) 
				definition = art.getSentenceMarkup(0) ; 
			else
				definition = art.getFirstParagraphMarkup() ; 
	
			definition = getHub().getFormatter().format(definition, request, wikipedia) ;
	
			xmlResponse.appendChild(getHub().createCDATAElement("Definition", definition)) ;
		}
		
		if (prmLabels.getValue(request)) {
			//get labels for this concept

			Article.Label[] labels = art.getLabels() ;
			if (labels.length > 0) {
				Element xmlLabels = getHub().createElement("Labels") ;

				int total = 0 ;
				for (Article.Label lbl:labels) 
					total += lbl.getLinkOccCount() ;

				for (Article.Label lbl:labels) {
					long occ = lbl.getLinkOccCount() ;

					if (occ > 0) {
						Element xmlLabel = getHub().createElement("Label") ;
						xmlLabel.appendChild(getHub().createTextNode(lbl.getText())) ;
						xmlLabel.setAttribute("occurances", String.valueOf(occ)) ;
						xmlLabel.setAttribute("proportion", getHub().format((double)occ/total)) ;
						xmlLabel.setAttribute("isPrimary", String.valueOf(lbl.isPrimary())) ;
						xmlLabel.setAttribute("fromRedirect", String.valueOf(lbl.isFromRedirect())) ;
						xmlLabel.setAttribute("fromTitle", String.valueOf(lbl.isFromTitle())) ;

						xmlLabels.appendChild(xmlLabel) ;
					}
				}
				xmlResponse.appendChild(xmlLabels) ;
			}
		}

		if (prmTranslations.getValue(request)) {

			TreeMap<String,String> translations = art.getTranslations() ;

			if (translations.size() > 0) {
				Element xmlTranslations = getHub().createElement("Translations") ;

				for (Map.Entry<String,String> entry:translations.entrySet()) {
					Element xmlTranslation = getHub().createElement("Translation") ;
					xmlTranslation.setAttribute("lang", entry.getKey()) ;
					xmlTranslation.appendChild(getHub().createTextNode(entry.getValue())) ;
					xmlTranslations.appendChild(xmlTranslation) ;
				}
				xmlResponse.appendChild(xmlTranslations) ;
			}

		}


		if (prmImages.getValue(request)) {

			URL freebaseRequest = new URL("http://www.freebase.com/api/service/mqlread?query={\"query\":{\"key\":[{\"namespace\":\"/wikipedia/en_id\",\"value\":\"" + art.getId() + "\"}], \"type\":\"/common/topic\", \"article\":[{\"id\":null}], \"image\":[{\"id\":null}]}}") ;

			String freebaseResponse = getHub().getRetriever().getWebContent(freebaseRequest) ;

			freebaseResponse = freebaseResponse.replaceAll("\\s", "") ;

			Matcher m = fb_imagePattern.matcher(freebaseResponse) ;

			if (m.find()) {
				Matcher n = fb_idPattern.matcher(m.group(1)) ;
				while (n.find()) {
					Element xmlImage = getHub().createElement("Image") ;
					xmlImage.setAttribute("url", "http://www.freebase.com/api/trans/image_thumb" + n.group(1).replace("\\/", "/") + "?maxwidth=" + prmImageWidth.getValue(request) + "&maxheight=" + prmImageHeight.getValue(request)) ;
					xmlResponse.appendChild(xmlImage) ;
				}
			}
		}
		
		if (prmParentCategories.getValue(request)) {
			
			Category[] parents = art.getParentCategories() ;
			
			Element xmlParents = getHub().createElement("ParentCategories") ;
			xmlParents.setAttribute("total", String.valueOf(parents.length)) ;

			for (Category parent:parents) {
				Element xmlParent = getHub().createElement("ParentCategory") ;
				xmlParent.setAttribute("id", String.valueOf(parent.getId())) ;
				xmlParent.setAttribute("title", parent.getTitle()) ;
				
				xmlParents.appendChild(xmlParent) ;
			}
			xmlResponse.appendChild(xmlParents) ;
		}
		
		if (prmOutLinks.getValue(request)) {

			int start = prmOutLinkStart.getValue(request) ;
			int max = prmOutLinkMax.getValue(request) ;
			if (max <= 0) 
				max = Integer.MAX_VALUE ;
			else
				max = max + start ;
			
			Article[] linksOut = art.getLinksOut() ;

			Element xmlLinks = getHub().createElement("OutLinks") ;
			xmlLinks.setAttribute("total", String.valueOf(linksOut.length)) ;

			for (int i=start ; i < max && i < linksOut.length ; i++) {

				Element xmlLink = getHub().createElement("OutLink") ;
				xmlLink.setAttribute("id", String.valueOf(linksOut[i].getId())) ;
				xmlLink.setAttribute("title", linksOut[i].getTitle()) ;
				if (artComparer != null)
					xmlLink.setAttribute("relatedness", getHub().format(artComparer.getRelatedness(art, linksOut[i]))) ;

				xmlLinks.appendChild(xmlLink) ;
			}
			xmlResponse.appendChild(xmlLinks) ;
		}

		if (prmInLinks.getValue(request)) {
			
			int start = prmInLinkStart.getValue(request) ;
			int max = prmInLinkMax.getValue(request) ;
			if (max <= 0) 
				max = Integer.MAX_VALUE ;
			else
				max = max + start ;

			Article[] linksIn = art.getLinksIn() ;

			Element xmlLinks = getHub().createElement("InLinks") ;
			xmlLinks.setAttribute("total", String.valueOf(linksIn.length)) ;

			for (int i=start ; i < max && i < linksIn.length ; i++) {

				Element xmlLink = getHub().createElement("InLink") ;
				xmlLink.setAttribute("id", String.valueOf(linksIn[i].getId())) ;
				xmlLink.setAttribute("title", linksIn[i].getTitle()) ;
				if (artComparer != null)
					xmlLink.setAttribute("relatedness", getHub().format(artComparer.getRelatedness(art, linksIn[i]))) ;

				xmlLinks.appendChild(xmlLink) ;
			}
			xmlResponse.appendChild(xmlLinks) ;
		}
		
		return xmlResponse ;
	}
	
	

}
