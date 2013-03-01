var wm_tooltip = null ;
var wm_requestedId = null ;

var wm_tooltipXmlCache = new Array() ;


var wm_host = "" ;

function wm_setHost(hostName) {
	wm_host = hostName ;
}

function wm_addDefinitionTooltipsToAllLinks(container, className) {
        
        if (container == null) 
		container = $("body") ;
        
        var links ;

	if (className == null)
		links = container.find("a") ;
	else
		links = container.find("a." + className) ;
        
        for (var i = 0; i < links.length; i++) {
                
                var link = $(links[i]);
                var dest = link.attr("href");
				var pageId = link.attr("pageId");
                var linkProb = link.attr("linkProb") ;
                var relatedness = link.attr("relatedness") ;
                
                
                if (pageId != null) {
                        link.bind("mouseover", {
                                id: pageId,
                                object: link,
                                linkProb: linkProb,
                                relatedness: relatedness
                        }, function(event){
                                wm_showDefinitionTooltip(event.data.id, event.data.object, event.data.linkProb, event.data.relatedness);
                        });
                        
                        link.bind("mouseout", function(event){
                                wm_hideTooltip() ;
                        });
                }  
         
        }       
}

function wm_showMessageTooltip(message, object) {
	
	if(wm_tooltip == null) {
		wm_tooltip = $("<div id='wm_tooltip' class='ui-corner-all'></div>") ;
		$("body").append(wm_tooltip) ;
	}
	
    var top = object.offset().top + object.height() ;
    var left = object.offset().left ;
    
    // if this is too far right to fit on page, move it to be against right side of page
    if (left + wm_tooltip.width() > $(window).width())
            left = $(window).width() - wm_tooltip.width() - 30 ;
                    
    wm_tooltip.css("top", top + "px") ;
    wm_tooltip.css("left", left + "px") ;
    
    wm_tooltip.html(message) ;
    wm_tooltip.show() ;
	
}


function wm_showDefinitionTooltip(topicId, object, linkProb, relatedness) {
                
    if(wm_tooltip == null) {
		wm_tooltip = $("<div id='wm_tooltip' class='ui-corner-all'></div>") ;
		$("body").append(wm_tooltip) ;
	}

	wm_requestedId = topicId ;

        var top = object.offset().top + object.height() ;
        var left = object.offset().left ;
        
        // if this is too far right to fit on page, move it to be against right side of page
        if (left + wm_tooltip.width() > $(window).width())
                left = $(window).width() - wm_tooltip.width() - 30 ;
                        
        wm_tooltip.css("top", top + "px") ;
        wm_tooltip.css("left", left + "px") ;
        
        wm_tooltip.html("<div class='loading'></div>") ;
        wm_tooltip.show() ;

	var xml = wm_tooltipXmlCache[topicId] ;
        if (xml != null) {
                wm_setTooltipContent(topicId, xml, linkProb, relatedness) ;
	} else {

		$.get(wm_host+"services/exploreArticle", {
			id: topicId,
			definition: true,
			emphasisFormat: "HTML",
			linkFormat: "PLAIN",
			images: true,
			maxImageWidth: 100,
			maxImageHeight: 100
       		}, function(response){

			wm_tooltipXmlCache[topicId] = response ;

			if (wm_requestedId == null || wm_requestedId == topicId)
				wm_setTooltipContent(topicId, response, linkProb, relatedness);
		});
	}
}





function wm_setTooltipContent(topicId, xml, linkProb, relatedness) {
                
	
	wm_requestedId = null ;

        wm_tooltip.empty() ;

        var image = $(xml).find("Image") ;
        if (image.length > 0) 
                wm_tooltip.append("<img src='" + image.attr("url") + "'></img>") ;
        
        
        var definition = $(xml).find("Definition") ;
        if (definition.length > 0) 
                wm_tooltip.append(definition.text()) ;
        else 
                wm_tooltip.append("no definition avaliable") ;
        
        if (linkProb != null) 
              wm_tooltip.append("<p><b>" + Math.round(linkProb*100) + "%</b> probability of being a link") ;
                
        if (relatedness != null) 
              wm_tooltip.append("<p><b>" + Math.round(relatedness*100) + "%</b> related") ;
        
        $(wm_tooltip).append("<div class='break'/>") ;
}

function wm_hideTooltip() {
	wm_tooltip.hide() ;

}

