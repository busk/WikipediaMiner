source = "" ;
minProbability = 0.5 ;
sourceMode = "AUTO"
repeatMode = "FIRST_IN_REGION" ;

function doEventBindings() {
	$("form").submit(function() {
		var ok = true;
		
		if ($('#txtSource').val() == "") {
			$('#txtSource').addClass("ui-state-error") ;
			ok = false ;
		}
		
		return ok ;
    });
	
	$('#cmdAnnotate').button() ;
	
	$('#tabs').tabs();
	
	$('#slider').slider(
		{
			step: 10, 
			change: function(event, ui){
            	$("#minProbability").val((100-ui.value)/100) ;
            }
         }
    );
}

function setOptionValues() {
	
	if (urlParams["source"] != undefined)
		source = urlParams["source"] ;

	var customOptions = false ; 
	
	if (urlParams["minProbability"] != undefined && urlParams["minProbability"] != minProbability) {
		minProbability = Number(urlParams["minProbability"]) ;
		customOptions = true ;
	}
		
	if (urlParams["sourceMode"] != undefined && urlParams["sourceMode"] != sourceMode) {
		sourceMode = urlParams["sourceMode"] ;
		customOptions = true ;
	}
		
	if (urlParams["repeatMode"] != undefined && urlParams["repeatMode"] != repeatMode) {
		repeatMode = urlParams["repeatMode"] ;	
		customOptions = true ;
	}
		
	$("#txtSource").val(source) ;
	
	$("input[name|=sourceMode][value|=" + sourceMode + "]").attr("checked",true) ;
	
	$("input[name|=repeatMode][value|=" + repeatMode + "]").attr("checked",true) ;
	
	$('#slider').slider(
		{value: Math.round((1-minProbability)*100)}
	) ;
	
	if (customOptions)
		showOptions() ;
	else
		hideOptions() ;
}

function doTooltipBindings() {

}


$(document).ready(function() {	
	
	wm_setHost("../../") ;
	

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
	
	setOptionValues() ;
	
	
	if (source != undefined && source != "") {
		
		$('#instructions').hide() ;
		$("#results").show() ;
		
		
		$.get(
			"../../services/wikify", 
			{
				source: source,
				sourceMode: sourceMode,
				repeatMode: repeatMode,
				minProbability: minProbability
			},
			function(data){
				processAnnotationResponse($(data).find("Response")) ;
			}
		);
	} 
}

function showOptions() {
    $('#options').show() ;
    $('#hideOptions').show() ;
    $('#showOptions').hide() ;
}

function hideOptions() {
    $('#options').hide() ;
    $('#hideOptions').hide() ;
    $('#showOptions').show() ;
}


function processAnnotationResponse(response) {
	
	var wikifiedDoc = response.find("WikifiedDocument") ;
	
	var sourceMode = wikifiedDoc.attr('sourceMode') ;
	
	var topicsByTitle = new Array() ;  
	
	var sortedTopics = $(response).find("DetectedTopic").get().sort(function(a,b) {
		var valA = $(a).attr('title') ;
		var valB = $(b).attr('title') ;
		
		return valA < valB ? -1 : valA == valB? 0 : 1 ;
	}) ;
	

	if (sortedTopics.length > 0) {
		
		var maxWeight = undefined ;
		var minWeight = undefined ;
		
		for (var i=0 ; i<sortedTopics.length ; i++) {
			var weight = Number($(sortedTopics[i]).attr('weight')) ;
			
			if (minWeight == undefined || weight < minWeight) minWeight = weight ;
			if (maxWeight == undefined || weight > maxWeight) maxWeight = weight ;
			
			topicsByTitle[$(sortedTopics[i]).attr('title').toLowerCase()] = {id:$(sortedTopics[i]).attr("id"), weight:$(sortedTopics[i]).attr("weight")}  ;
		}
		
		$.each(sortedTopics, function() {
			var xmlLink = $(this) ;
			
			var link = $("<a pageId='" + xmlLink.attr('id') + "' linkProb='" + xmlLink.attr('weight') + "' href='../search/?artId=" + xmlLink.attr('id') + "'>" + xmlLink.attr('title') + "</a>") ;
			var weight = Number(xmlLink.attr('weight')) ;
			weight = normalize(weight, minWeight, maxWeight) ;
			link.css('font-size', getFontSize(weight) + "px") ;
			link.css('color', getFontColor(weight)) ;
			
			var li = $("<li></li>") ;
			li.append(link) ;

			$('#topics').append(li) ;
		}) ;
		
		wm_addDefinitionTooltipsToAllLinks($('#topics')) ;
	}
	
	
	if (sourceMode == 'WIKI') {
		$('#tabs').tabs("remove", 1) ;
		$('#tabs').tabs("remove", 1) ;

		var origMarkup = wikifiedDoc.text() ;
		var newMarkup = "" ;

		var lastIndex = 0 ;
		
		var pattern=/\[\[(.*?)(|.*?)\]\]/g ;
		var result;
		while ((result = pattern.exec(origMarkup)) != null) {			
			newMarkup = newMarkup + origMarkup.substring(lastIndex, result.index) ;
			
			var dest = getDestination(result[0]) ;
			var topic = topicsByTitle[dest.toLowerCase()] ;
			
			newMarkup = newMarkup + "<a href=\"../search/?artId=" + topic.id + "\" pageId='" + topic.id + "' linkProb='" + topic.weight + "' \">" + result[0] + "</a>" ;
			
			lastIndex = pattern.lastIndex ;
		}
		newMarkup = newMarkup + origMarkup.substring(lastIndex) ;
		
		$('#wikiMarkup').html(newMarkup) ;
		
		wm_addDefinitionTooltipsToAllLinks($('#wikiMarkup')) ;
		
	} else {
		$('#tabs').tabs("remove", 0) ;
		
		$('#renderedHtml').html(wikifiedDoc.text()) ;
	
		wm_addDefinitionTooltipsToAllLinks($('#renderedHtml')) ;
		
		//alert("blah!") ;
		
		var escapedHTML= wikifiedDoc.text() ;
	    escapedHTML = escapedHTML.replace(/</g, "&lt;");
	    escapedHTML = escapedHTML.replace(/>/g, "&gt;");
	    //remove additional attributes
	    escapedHTML = escapedHTML.replace(/class=\"wm_wikifiedLink\" /g, "") ;
	    escapedHTML = escapedHTML.replace(/pageId=\".*?\" /g, "") ;
	    escapedHTML = escapedHTML.replace(/\s*linkProb=\".*?\"\s*/g, "") ;
	        
	    $('#rawHtml').html(escapedHTML) ;
		
	}
	
	$("#loading").hide() ;
	$('#tabs').show() ;
}

function getDestination(wikiLink) {
	
	var pos = wikiLink.indexOf("|") ;
	
	if (pos > 0)
		return wikiLink.substring(2, pos) ;
	else
		return wikiLink.substring(2, wikiLink.length - 2) ;
	
}
