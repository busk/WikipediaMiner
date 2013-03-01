

var datatypeDescriptions = {} ;
datatypeDescriptions["integer"] = "any whole number" ;
datatypeDescriptions["float"] = "any whole number or fraction" ;
datatypeDescriptions["string"] = "a URL-encoded string" ;
datatypeDescriptions["boolean"] = "<i>true</i> or <i>false</i>. If the name of a boolean parameter is given without a value, then the value is assumed to be true" ;
datatypeDescriptions["integer list"] = "a list of comma separated integers (e.g. <i>12,14,200</i>)" ;
datatypeDescriptions["enum"] = "one of a prespecified set of strings" ;
datatypeDescriptions["enum list"] = "a comma separated set of enums" ;


var currService ;

$(document).ready(function() {
	//requestServiceList() ;
	
	var location = document.location.toString() ;
	if (location.match('\\?')) 
		currService = location.split('?')[1] ;
		
	requestServiceList() ;
	
	if (currService != undefined) {
		requestServiceDetails(currService) ;
		
		
		$("#serviceIntro").hide() ;
		$("#serviceDetails").show() ;
	} else {
		wm_addDefinitionTooltipsToAllLinks($('#allServices')) ;
		
		$("#serviceIntro").show() ;
		$("#serviceDetails").hide() ;
	}
}) ;

function requestServiceList() {
	$.get("listServices", function(response){
		processServiceListResponse($(response));
	});
}

function processServiceListResponse(response) {
	
	var serviceList = $("#serviceList") ;
	
	if (currService == undefined) {
		serviceList.append("<li><em>introduction</em></li>") ;
	} else {
		serviceList.append("<li><a href='.'>introduction</a></li>") ;
	}

	$.each(response.find("ServiceGroup"), function() {
		
		var groupXml = $(this) ;
		
		serviceList.append("<li class='header'><em>" + groupXml.attr("name") + "</em> services</li>") ;
		
		$.each(groupXml.find("Service"), function() {
			var serviceXml = $(this) ;
		
			//alert(service.attr("name")) ;
			
			if (serviceXml.attr("name") == currService) {
				serviceList.append("<li><em>" + serviceXml.attr("name") + "</em></li>") ;
			} else {
					
				var service = $("<li><a href='?" + serviceXml.attr("name") + "'>" + serviceXml.attr("name") + "</a></li>") ;
				service.qtip(
				{
				      content: serviceXml.find("Details").text(),
					  style: { name: 'wmstyle' },
					  position: {
					  	corner: {
					  		target: 'rightMiddle',
					  		tooltip: 'leftMiddle'
					  	},
					  	adjust: {
					  		x:20
					  	}
					  }
				 }) ;
						
				serviceList.append(service) ;
			}
		}) ;
		
	}) ;
	
}

function requestServiceDetails(serviceName) {
	$.get(serviceName+"?help", function(xml){
		processServiceDetailsResponse($(xml).find("ServiceDescription"));
	});
}

function processServiceDetailsResponse(response) {
	
	$("#serviceDetails").append("<h2><em>" + response.attr("serviceName") + "</em> service</h2>") ;
	
	$("#serviceDetails").append($(response).children("Details").text()) ;
	
	$("#serviceDetails").append("<h3>Available parameters</h3>") ;
	
	if ($(response).find("Parameter[name!='help']").length == 0) {
		$("#serviceDetails").append("<p class='explanatory'>There are no parameters to specify</p>") ;
		return ;
	}
	
	var paramGroups = $(response).find("ParameterGroup") ;
	
	if (paramGroups.length > 0) {
		
		$("#serviceDetails").append("<p class='explanatory'>Only specify parameters from <em>one</em> of the following groups</p>") ;
		
		$.each($(response).find("ParameterGroup"), function(){
			$("#serviceDetails").append(constructParamGroupBox($(this))) ;
		}) ;
	}
	
	var mandatoryGlobalParams = $(response).children("Parameter[optional|='false']") ;
	
	if (mandatoryGlobalParams.length > 0) {
		
		if (mandatoryGlobalParams.length == 1)
			$("#serviceDetails").append("<p class='explanatory'>Specify the following</p>") ;
		else
			$("#serviceDetails").append("<p class='explanatory'>Specify <em>all</em> of the following</p>") ;
		
		$.each(mandatoryGlobalParams, function(){
			var xmlParam = $(this) ;
			var divParam = constructParamBox(xmlParam) ;
			$("#serviceDetails").append(divParam);
			intializeParamBox(divParam, xmlParam) ;
		}) ;
	}
	
	
	var optionalGlobalParams = $(response).children("Parameter[optional|='true']") ;
	var baseParams = $(response).children("BaseParameters").children("Parameter[name!='help']") ;
	
	if (optionalGlobalParams.length + baseParams.length > 0) {
		
		if (optionalGlobalParams.length + baseParams.length == 1)
			$("#serviceDetails").append("<p class='explanatory'>Optionally specify the following</p>") ;
		else 
			$("#serviceDetails").append("<p class='explanatory'>Optionally specify <em>any</em> of the following</p>") ;
		
		$.each(optionalGlobalParams, function(){
			var xmlParam = $(this) ;
			var divParam = constructParamBox(xmlParam) ;
			$("#serviceDetails").append(divParam);
			intializeParamBox(divParam, xmlParam) ;
		}) ;
		
		$.each(baseParams, function(){
			var xmlParam = $(this) ;
			var divParam = constructParamBox(xmlParam) ;
			$("#serviceDetails").append(divParam);
			intializeParamBox(divParam, xmlParam) ;
		}) ;
	}
	
	var examples = $(response).children("Examples").children("Example") ;
	
	if (examples.length > 0) {
		$("#serviceDetails").append("<h3>Examples</h3>") ;
		
		$.each(examples , function() {
			$("#serviceDetails").append(constructExampleBox($(this), response.attr("serviceName")));
		}) ;
	}
}

function constructParamGroupBox(xmlParamGroup) {
	var divParamGroup = $("<div class='paramGroup ui-widget-content ui-corner-all'></div>") ;
	
	var divParamGroupTitle = $("<div class='title'></div>") ;
	divParamGroup.append(divParamGroupTitle) ;
	
	divParamGroupTitle.append("<div style='float:left' class='ui-icon ui-icon-circle-triangle-s'></div>") ;
	divParamGroupTitle.append("<div style='float:left ; display:none ;' class='ui-icon ui-icon-circle-triangle-n'></div>") ;
	
	divParamGroupTitle.bind('click', function() {
		$(this).parent().children('.content').slideToggle('fast')  ;
		$(this).children('.ui-icon').toggle() ;
		return false ;
	}) ;
	
	divParamGroupTitle.bind('mouseover', function(event) {
		$(this).parent().addClass("ui-state-highlight") ;
	}) ;
	
	divParamGroupTitle.bind('mouseout', function(event) {
		$(this).parent().removeClass("ui-state-highlight") ;
	}) ;
	
	divParamGroupTitle.append("<span>" + xmlParamGroup.children("Description").text() + "</span>");
	
	var divParamGroupContent = $("<div class='content' style='display: none'></div>") ;
	divParamGroup.append(divParamGroupContent) ;
	
	var mandatoryParams = xmlParamGroup.children("Parameter[optional|='false']") ;
	
	if (mandatoryParams.length > 0) {
		if (mandatoryParams.length == 1)
			divParamGroupContent.append("<p class='explanatory'>Specify the following</p>");
		else
			divParamGroupContent.append("<p class='explanatory'>Specify <em>all</em> of the following</p>");
		
		$.each(mandatoryParams, function(){
			var xmlParam = $(this) ;
			var divParam = constructParamBox(xmlParam) ;
			divParamGroupContent.append(divParam);
			intializeParamBox(divParam, xmlParam) ;
		});
	}
	
	var optionalParams = xmlParamGroup.children("Parameter[optional|='true']") ;
	
	if (optionalParams.length > 0) {
		if (optionalParams.length == 1)
			divParamGroupContent.append("<p class='explanatory'>Optionally specify the following</p>");
		else
			divParamGroupContent.append("<p class='explanatory'>Optionally specify <em>any</em> of the following</p>");
		
		$.each(optionalParams, function(){
			var xmlParam = $(this) ;
			var divParam = constructParamBox(xmlParam) ;
			divParamGroupContent.append(divParam);
			intializeParamBox(divParam, xmlParam) ;
		});
	}
	
	return divParamGroup ;
}

function constructParamBox(xmlParam) {
	
	var divParam = $("<div class='param ui-widget-content ui-corner-all'></div>");
	
	var divParamTitle = $("<div class='title' ></div>") ;
	divParam.append(divParamTitle) ;

	
	divParamTitle.append("<div style='float:left' class='ui-icon ui-icon-circle-triangle-s'></div>") ;
	divParamTitle.append("<div style='float:left' class='ui-icon ui-icon-circle-triangle-n'></div>") ;
	
	divParamTitle.bind('click', function(event) {
		$(this).parent().children('.content').slideToggle('fast') ;
		$(this).children('.ui-icon').toggle() ;
		event.stopImmediatePropagation();
	}) ;
	
	
	divParamTitle.bind('mouseover', function(event) {
		$(this).parent().addClass("ui-state-highlight") ;
	}) ;
	
	divParamTitle.bind('mouseout', function(event) {
		$(this).parent().removeClass("ui-state-highlight") ;
	}) ;
	
	
	divParamTitle.append("<span>" + xmlParam.attr("name") + "</span>");
	
	var divParamContent = $("<div class='content'></div>") ;
	divParam.append(divParamContent) ;
	divParamContent.append("<p>" + xmlParam.children("Description").text() + "</p>") ;
	
	var tblType = divParamContent.append("<table></table>") ;
	
	var tr = tblType.append("<tr></tr>") ;
	tr.append("<td class='header'>type</td>") ;
	
	tr.append("<td><a class='datatype'>" + xmlParam.attr("dataType") + "</a></td>") ;
	tr.find(".datatype").qtip(
	   {
	      content: datatypeDescriptions[xmlParam.attr("dataType")],
		  style: {
              name: 'wmstyle' 
   		  }
	   }) ;
	
	
	var possibleValues = xmlParam.children("PossibleValue") ;
	
	if (possibleValues.length > 0) {
		
		tr = tblType.append("<tr/>") ;
		
		tr.append("<td class='header'>possible values</td>") ;
		
		var td = $("<td></td>") ;
	
		$.each(possibleValues, function() {
			var xmlPossibleVal = $(this) ;
			
			var val = $("<span class='value'>" + xmlPossibleVal.attr("name") + "</a></span>") ;
			val.qtip(
			   {
			      content: xmlPossibleVal.attr("description"),
				  style: {
	                  name: 'wmstyle' 
           		  }
			   }) ;
			td.append(val) ;
		}) ;
		
		tr.append(td) ;
	}
	
	
	if (typeof xmlParam.attr('default') != "undefined") {
		tr = tblType.append("<tr></tr>") ;
		
		tr.append("<td class='header'>default value</td>")
		tr.append("<td>" + xmlParam.attr('default') + "</td>");	
		
	}
	return divParam ;
}

//Decide wheither to initially expand or collapse param box initially
//Mandatory params are expanded initially, optional ones are collapsed
function intializeParamBox(divParam, xmlParam) {
	if (typeof xmlParam.attr('default') != "undefined") {
		divParam.find(".content").hide() ;
		divParam.find(".ui-icon-circle-triangle-s").show() ;
		divParam.find(".ui-icon-circle-triangle-n").hide() ;	
	} else {
		divParam.find(".content").show() ;
		divParam.find(".ui-icon-circle-triangle-s").hide() ;
		divParam.find(".ui-icon-circle-triangle-n").show() ;
	}
}



function constructExampleBox(xmlExample, serviceName) {
	
	var divExample = $("<div class='example ui-widget-content ui-corner-all'></div>");
	
	var divExampleTitle = $("<div class='title' ></div>") ;
	divExample.append(divExampleTitle) ;
	
	divExampleTitle.append("<div style='float:left' class='ui-icon ui-icon-circle-triangle-s'></div>") ;
	divExampleTitle.append("<div style='float:left; display:none' class='ui-icon ui-icon-circle-triangle-n'></div>") ;
	
	divExampleTitle.bind('click', function(event) {
		$(this).parent().children('.content').slideToggle('fast') ;
		$(this).children('.ui-icon').toggle() ;
	}) ;
	
	divExampleTitle.bind('mouseover', function(event) {
		$(this).parent().addClass("ui-state-highlight") ;
	}) ;
	
	divExampleTitle.bind('mouseout', function(event) {
		$(this).parent().removeClass("ui-state-highlight") ;
	}) ;
	
	divExampleTitle.append("<span class='title'>" + xmlExample.text() + "</span>") ;
	
	var divExampleContent = $("<div class='content'></div>") ;
	divExample.append(divExampleContent) ;
	
	
	
	var parParams = $("<p class='params'>services/" + serviceName + "</p>") ;
	
	divExampleContent.append(parParams) ;
	
	$.each(xmlExample.find("Parameter"), function(index, value) {
		
		var parParam = $("<p class='param'></p>") ;
		parParams.append(parParam) ;
		
		if (index == 0)
			parParam.append("?") ;
		else
			parParam.append("&") ;
			
		if ($(value).attr('value') == 'true')
			parParam.append("<b>" + $(value).attr("name") + "</b>") ;
		else
			parParam.append("<b>" + $(value).attr("name") + "</b>=" + $(value).attr("value")) ;
	});
	
	var cmdTry = $("<a class='try' target='_blank' href='" + xmlExample.attr("url") + "'>try it!</a>") ;
	divExampleContent.append(cmdTry) ;
	
	return divExample ;
}