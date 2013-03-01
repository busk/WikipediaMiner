package org.wikipedia.miner.service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Category;
import org.wikipedia.miner.model.Page;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.service.param.BooleanParameter;
import org.wikipedia.miner.service.param.IntParameter;
import org.wikipedia.miner.service.param.ParameterGroup;
import org.wikipedia.miner.service.param.StringParameter;

public class ExploreCategoryService extends Service{

	private enum GroupName{id,title} ; 
	
	private ParameterGroup grpId ;
	private IntParameter prmId ;
	
	private ParameterGroup grpTitle ;
	private StringParameter prmTitle ;
	
	private BooleanParameter prmParentCategories ;
	
	private BooleanParameter prmChildCategories ;
	private IntParameter prmChildCategoryMax ;
	private IntParameter prmChildCategoryStart ;
	
	private BooleanParameter prmChildArticles ;
	private IntParameter prmChildArticleMax ;
	private IntParameter prmChildArticleStart ;
	
	public ExploreCategoryService() {
		super("core","Provides details of individual categories",
			
			"<p></p>",
			true, false
		);
	}
	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		
		grpId = new ParameterGroup(GroupName.id.name(), "To retrieve category by  id") ;
		prmId = new IntParameter("id", "The unique identifier of the category to explore", null) ;
		grpId.addParameter(prmId) ;
		addParameterGroup(grpId) ;
		
		grpTitle = new ParameterGroup(GroupName.title.name(), "To retrieve category by title") ;
		prmTitle = new StringParameter("title", "The (case sensitive) title of the category to explore", null) ;
		grpTitle.addParameter(prmTitle) ;
		addParameterGroup(grpTitle) ;
		
		prmParentCategories = new BooleanParameter("parentCategories", "<b>true</b> if parent categories of this category should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmParentCategories) ;
		
		prmChildCategories = new BooleanParameter("childCategories", "<b>true</b> if child categories of this category should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmChildCategories) ;
		
		prmChildCategoryMax = new IntParameter("childCategoryMax", "the maximum number of child categories that should be returned. A max of <b>0</b> will result in all child categories being returned", 250) ;
		addGlobalParameter(prmChildCategoryMax) ;
		
		prmChildCategoryStart = new IntParameter("childCategoryStart", "the index of the first child category to return. Combined with <b>childCategoryMax</b>, this parameter allows the user to page through large lists of child categories", 0) ;
		addGlobalParameter(prmChildCategoryStart) ;
		
		prmChildArticles = new BooleanParameter("childArticles", "<b>true</b> if child articles of this category should be returned, otherwise <b>false</b>", false) ;
		addGlobalParameter(prmChildArticles) ;
		
		prmChildArticleMax = new IntParameter("childArticleMax", "the maximum number of child articles that should be returned. A max of <b>0</b> will result in all child articles being returned", 250) ;
		addGlobalParameter(prmChildArticleMax) ;
		
		prmChildArticleStart = new IntParameter("childArticleStart", "the index of the first child article to return. Combined with <b>childArticleMax</b>, this parameter allows the user to page through large lists of child articles", 0) ;
		addGlobalParameter(prmChildArticleStart) ;
		
	}
	

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element xmlResponse) throws Exception {
		
		Wikipedia wikipedia = getWikipedia(request) ;
		
		ParameterGroup grp = getSpecifiedParameterGroup(request) ;
		
		if (grp == null) {
			xmlResponse.setAttribute("unspecifiedParameters", "true") ;
			return xmlResponse ;
		}
		
		Category cat = null ;
		
		switch(GroupName.valueOf(grp.getName())) {
		
		case id :
			Integer id = prmId.getValue(request) ;
			
			Page page = wikipedia.getPageById(id) ;
			if (page==null) 
				return buildErrorResponse("'" + id + "' is an unknown id", xmlResponse) ;
			
			switch(page.getType()) {
				case category:
					cat = (Category)page ;
					break ;
				default:
					return buildErrorResponse("'" + id + "' is not a category id", xmlResponse) ;
			}
			break ;
		case title :
			String title = prmTitle.getValue(request) ;
			cat = wikipedia.getCategoryByTitle(title) ;
			
			if (cat == null)
				return buildErrorResponse("'" + title + "' is an unknown category title", xmlResponse) ;
			break ;
		}
		
		xmlResponse.setAttribute("id", String.valueOf(cat.getId()));
		xmlResponse.setAttribute("title", String.valueOf(cat.getTitle()));
		
		if (prmParentCategories.getValue(request)) {
			
			Category[] parents = cat.getParentCategories() ;
			
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
		
		if (prmChildCategories.getValue(request)) {
			
			int start = prmChildCategoryStart.getValue(request) ;
			int max = prmChildCategoryMax.getValue(request) ;
			if (max <= 0) 
				max = Integer.MAX_VALUE ;
			else
				max = max + start ;
		
			Category[] children = cat.getChildCategories() ;
			
			Element xmlChildren = getHub().createElement("ChildCategories") ;
			xmlChildren.setAttribute("total", String.valueOf(children.length)) ;

			for (int i=start ; i < max && i < children.length ; i++) {

				Element xmlChild = getHub().createElement("ChildCategory") ;
				xmlChild.setAttribute("id", String.valueOf(children[i].getId())) ;
				xmlChild.setAttribute("title", children[i].getTitle()) ;
				
				xmlChildren.appendChild(xmlChild) ;
			}
			xmlResponse.appendChild(xmlChildren) ;
		}
		
		if (prmChildArticles.getValue(request)) {
			
			int start = prmChildArticleStart.getValue(request) ;
			int max = prmChildArticleMax.getValue(request) ;
			if (max <= 0) 
				max = Integer.MAX_VALUE ;
			else
				max = max + start ;
			
			Article[] children = cat.getChildArticles() ;
			
			Element xmlChildren = getHub().createElement("ChildArticles") ;
			xmlChildren.setAttribute("total", String.valueOf(children.length)) ;

			for (int i=start ; i < max && i < children.length ; i++) {

				Element xmlChild = getHub().createElement("ChildArticle") ;
				xmlChild.setAttribute("id", String.valueOf(children[i].getId())) ;
				xmlChild.setAttribute("title", children[i].getTitle()) ;
				
				xmlChildren.appendChild(xmlChild) ;
			}
			xmlResponse.appendChild(xmlChildren) ;
		}

		return xmlResponse ;
	}

}
