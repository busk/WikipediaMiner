function doEventBindings() {
	$("form").submit(function() {
		var ok = true;
		
		if ($('#query').val() == "") {
			$('#query').addClass("ui-state-error") ;
			ok = false ;
		}
		
		return ok ;
    });
	
	$('#cmdSearch').button() ;
}

function doTooltipBindings() {
	
	
	$('#sensesHelp').qtip({
	      content: "It looks like your query is ambiguous, because there are multiple articles that it could refer to. So, which one do you want?",
		  style: { name: 'wmstyle' }
    }) ;
	
	$('#labelHelp').qtip({
	      content: "Terms that are used to refer to this article. These are usually synonyms or alternative spellings.",
		  style: { name: 'wmstyle' }
    }) ;
	
	$('#translationHelp').qtip({
	      content: "Links to articles which describe the same concept in another language.",
		  style: { name: 'wmstyle' }
    }) ;
	
	$('#categoryHelp').qtip({
	      content: "Categories to which this article belongs. These represent broader topics or ways of organizing this one.",
		  style: { name: 'wmstyle' }
    }) ;
    
    $('#linksOutHelp').qtip({
	      content: "<p>Pages that this article links to. Some of these represent related topics, others are fairly random.</p> <p>The toolkit provides relatedness measures&mdash;indicated here by the brightness of each link&mdash;to help separate them.</p>",
		  style: { name: 'wmstyle' }
    }) ;
    
    $('#linksInHelp').qtip({
	      content: "<p>Pages that link to this article. Some of these represent related topics, others are fairly random.</p> <p>The toolkit provides relatedness measures&mdash;indicated here by the brightness of each link&mdash;to help separate them.</p>",
		  style: { name: 'wmstyle' }
    }) ;
}



$(document).ready(function() {
	
	wm_setHost("../../")
	
	doEventBindings() ;
	doTooltipBindings() ;
	
	

	checkProgress() ;
}) ;

function checkProgress() {
	
	$.get(
		"../../services/getProgress",
		function(data) {
			
			var xmlResponse = $(data).find("Response") ;
			var progress = Number(xmlResponse.attr("progress")) ;

			if (progress >= 1) {
				ready() ;
			} else {
		
				$('#progress').progressbar(
					{value: Math.floor(progress*100)}
				) ;
		
				setTimeout(
					checkProgress,
					500
				) ;
			}
		}
	) ;	
}


function ready() {
	
	$("#initializing").hide() ;
	$("#ready").show() ;
	
	var query = urlParams["query"];
	var artId = urlParams["artId"];
	var catId = urlParams["catId"];
	
	
	if (artId != undefined || catId != undefined || query != undefined) {
		$('#searchbox').removeClass('majorSearch') ;
		$('#searchbox').addClass('minorSearch') ;
	}
		
	if (artId != undefined) {
		
		$('#instructions').hide() ;
		$('#loading').show() ;
		
		
		$.get(
			"../../services/exploreArticle", 
			{
				id: artId,
				definition: true,
				definitionLength:'LONG',
				linkFormat: 'WIKI_ID',
				labels:true,
				translations:true,
				images:true,
				maxImageWidth:'300',
				maxImageHeight:'300',
				parentCategories:true,
				inLinks:true,
				inLinkMax: 100,
				outLinks:true,
				outLinkMax: 100,
				linkRelatedness:true 
			},
			function(data){
				processArticleResponse($(data).find("Response")) ;
			}
		);
		
		return ;
	}
	
	
	if (query != undefined) {
		
		$('#query').val(query) ;
		
		$('#instructions').hide() ;
		$('#loading').show() ;
		
		$.get(
			"../../services/search", 
			{
				query: query, 
			},
			function(data){
				processSearchResponse($(data).find("Response")) ;
			}
		);
		
		
	}
	
}


function processSearchResponse(response) {
	
	$('#loading').hide() ;
	
	
	var senses = response.find("Sense") ;
	
	if (senses.length > 1) {
		$('#senseDetails').show() ;
		
		$.each(senses, function() {
			var xmlSense = $(this) ;
			
			var pp = Number(xmlSense.attr("priorProbability")) ;
			
			var senseBox = $("<div class='senseBox' id='senseBox_" + xmlSense.attr('id') + "'></div>") ;
			
			var bar = $("<div class='bar'></div>") ;
			bar.append("<div style='width:" + Math.floor(pp*50) + "px'></div>") ;
			
			bar.qtip({
				content: "<em>" + Math.floor(pp*100) + "%</em> of <i>" + urlParams["query"] + "</i> occurrences refer to this.",
				style: { name: 'wmstyle' }
			}) ;
			
			senseBox.append(bar) ;
			
			senseBox.append("<a class='title' href='?artId=" + xmlSense.attr('id') + "'>" + xmlSense.attr('title') + "</a>") ;
			
			
			senseBox.append("<p class='definition'><img src='../images/loading.gif'></img></p>") ;
			
			$.get(
				"../../services/exploreArticle", 
				{
					id: xmlSense.attr('id'), 
					definition:true,
					linkFormat:'PLAIN'
				},
				function(data){
					
					var id = Number($(data).find("Request").attr("id")) ;
					var definition = $(data).find("Definition") ;
					
					var senseBox = $('#senseBox_' + id)
					
					var definitionText = definition.text() ;
					
					if (definitionText == undefined || definitionText == "")
						senseBox.find('.definition').html("No definition could be found") ;
					else
						senseBox.find('.definition').html(definition.text()) ;
					
					
				}
			);
			
			$('#senses').append(senseBox) ;
		}) ;

	} else if (senses.length == 1){
		
		$('#loading').show() ;
		
		$.get(
			"../../services/exploreArticle", 
			{
				id: $(senses[0]).attr('id'),
				definition: true,
				definitionLength:'LONG',
				linkFormat: 'WIKI_ID',
				labels:true,
				translations:true,
				images:true,
				maxImageWidth:'300',
				maxImageHeight:'300',
				parentCategories:true,
				inLinks:true,
				inLinkMax: 100,
				outLinks:true,
				outLinkMax: 100,
				linkRelatedness:true 
			},
			function(data){
				processArticleResponse($(data).find("Response")) ;
			}
		);
		
	} else {
		
		$('#unkownTerm').html(urlParams["query"]) ;
		
		$('#senseDetails').hide() ;
		$('#error').show() ;
		
		
	}
}


function processArticleResponse(response) {
	
	$('#loading').hide() ;
	$('#articleDetails').show() ;
	

	
	$('#artTitle').html(response.attr("title")) ;
	
	var origDefinition = response.find("Definition").text() ;
	
	if (origDefinition == undefined || origDefinition.length == 0)
		origDefinition = "No definition avaialble" ;
	
	var newDefinition = "" ;
	
	var lastIndex = 0 ;
		
	var pattern=/\[\[(\d+)\|(.*?)\]\]/g ;
	var result;
	while ((result = pattern.exec(origDefinition)) != null) {			
		newDefinition = newDefinition + origDefinition.substring(lastIndex, result.index) ;
		
		var id = result[1] ;
		var anchor = result[2] ;
		
		newDefinition = newDefinition + "<a href=\"./?artId=" + id + "\" pageId='" + id + "'>" + anchor + "</a>" ;
		
		lastIndex = pattern.lastIndex ;
	}
	newDefinition = newDefinition + origDefinition.substring(lastIndex) ;

	$('#artDefinition').html(newDefinition) ;
	
	wm_addDefinitionTooltipsToAllLinks($('#artDefinition')) ;
	
	
	var sortedLabels = $(response).find("Label").get().sort(function(a,b) {
		var valA = $(a).text() ;
		var valB = $(b).text() ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	
	if (sortedLabels.length > 0) {
		
		var maxWeight, minWeight ;
		$.each(sortedLabels, function() {
			var weight = Number($(this).attr('proportion')) ;
			
			if (minWeight == undefined || weight < minWeight) minWeight = weight ;
			if (maxWeight == undefined || weight > maxWeight) maxWeight = weight ;
		}) ;
		
		$.each(sortedLabels, function() {
			var xmlLabel = $(this) ;
			
			var label = $("<li>" + xmlLabel.text() + "</li>") ;
			var weight = Number(xmlLabel.attr('proportion')) ;
			weight = normalize(weight, minWeight, maxWeight) ;
						
			if (weight > 0.01) {
				label.css('font-size', getFontSize(weight) + "px") ;
				label.css('color', getFontColor(weight)) ;
				
				label.qtip({
					content: "used <em>" +xmlLabel.attr('occurances') + "</em> times",
					style: { name: 'wmstyle' }
				}) ;
				
	
				$('#labels').append(label) ;
			}
		}) ;
	} else {
		$('#noLabels').show() ;
	}
	
	var sortedTranslations = $(response).find("Translation").get().sort(function(a,b) {
		var valA = $(a).attr('lang') ;
		var valB = $(b).attr('lang') ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	
	if (sortedTranslations.length > 0) {
		$.each(sortedTranslations, function() {
			var xmlTranslation = $(this) ;
			
			var translation = $("<li><em>" + xmlTranslation.attr('lang') + "</em>: " + xmlTranslation.text() + "</li>") ;
			$('#translations').append(translation) ;
		}) ;
	} else {
		$('#noTranslations').show() ;
	}
	
	var sortedCategories = $(response).find("ParentCategory").get().sort(function(a,b) {
		var valA = $(a).attr('title') ;
		var valB = $(b).attr('title') ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	
	if (sortedCategories.length > 0) {
		$.each(sortedCategories, function() {
			var xmlCategory = $(this) ;
			
			var category = $("<a href='?catId=" + xmlCategory.attr('id') + "'>" + xmlCategory.attr('title') + "</a>") ;
			
			var li = $("<li></li>") ;
			li.append(category) ;

			$('#categories').append(li) ;
		}) ;

	} else {
		$('#categories').hide() ;
		$('#noCategories').show() ;
	}
	
	var sortedOutLinks = $(response).find("OutLink").get().sort(function(a,b) {
		var valA = $(a).attr('title') ;
		var valB = $(b).attr('title') ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	

	if (sortedOutLinks.length > 0) {
		
		var maxWeight = undefined ;
		var minWeight = undefined ;
		
		for (var i=0 ; i<sortedOutLinks.length ; i++) {
			var weight = Number($(sortedOutLinks[i]).attr('relatedness')) ;
			
			if (minWeight == undefined || weight < minWeight) minWeight = weight ;
			if (maxWeight == undefined || weight > maxWeight) maxWeight = weight ;
			
		}
		
		$.each(sortedOutLinks, function() {
			var xmlLink = $(this) ;
			
			var link = $("<a pageId='" + xmlLink.attr('id') + "' href='./?artId=" + xmlLink.attr('id') + "' relatedness='" + xmlLink.attr('relatedness') + "'>" + xmlLink.attr('title') + "</a>") ;
			var weight = Number(xmlLink.attr('relatedness')) ;
			weight = normalize(weight, minWeight, maxWeight) ;
			link.css('font-size', getFontSize(weight) + "px") ;
			link.css('color', getFontColor(weight)) ;
			
			var li = $("<li></li>") ;
			li.append(link) ;

			$('#linksOut').append(li) ;
		}) ;
		wm_addDefinitionTooltipsToAllLinks($('#linksOut')) ;
	} else {
		$('#linksOut').hide() ;
		$('#noLinksOut').show() ;
	}
	
	
	var sortedInLinks = $(response).find("InLink").get().sort(function(a,b) {
		var valA = $(a).attr('title') ;
		var valB = $(b).attr('title') ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	
	if (sortedInLinks.length > 0) {
		
		var maxWeight, minWeight ;
		$.each(sortedInLinks, function() {
			var weight = Number($(this).attr('relatedness')) ;
			
			if (minWeight == undefined || weight < minWeight) minWeight = weight ;
			if (maxWeight == undefined || weight > maxWeight) maxWeight = weight ;
		}) ;
		
		$.each(sortedInLinks, function() {
			var xmlLink = $(this) ;
			
			var link = $("<a pageId='" + xmlLink.attr('id') + "' href='./?artId=" + xmlLink.attr('id') + "' relatedness='" + xmlLink.attr('relatedness') + "'>" + xmlLink.attr('title') + "</a>") ;
			var weight = Number(xmlLink.attr('relatedness')) ;
			weight = normalize(weight, minWeight, maxWeight) ;
			link.css('font-size', getFontSize(weight) + "px") ;
			link.css('color', getFontColor(weight)) ;
			
			var li = $("<li></li>") ;
			li.append(link) ;

			$('#linksIn').append(li) ;
		}) ;
		wm_addDefinitionTooltipsToAllLinks($('#linksIn')) ;
	} else {
		$('#linksIn').hide() ;
		$('#noLinksIn').show() ;
	}
	
	
}



