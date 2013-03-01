package org.wikipedia.miner.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

public class ListServicesService extends Service{

	private static final long serialVersionUID = 2955853789508099004L;
	
	private Comparator<String> groupNameComparator = new Comparator<String>() {

		@Override
		public int compare(String s1, String s2) {

			if (s1.equals(s2))
				return 0 ;
			
			//always put core first and meta last
			if (s2.equals("core") || s1.equals("meta"))
				return 1 ;

			if (s1.equals("core") || s2.equals("meta"))
				return -1 ;

			return s1.compareTo(s2) ;
		}
	} ;



	public ListServicesService() {
		super("meta","Lists available services", 
				"<p>This service lists the different services that are available.</p>",
				false,false
		);
	}

	@Override
	public Element buildWrappedResponse(HttpServletRequest request,
			Element response) throws Exception {

		TreeMap<String, ServiceGroup> serviceGroups = new TreeMap<String, ServiceGroup>(groupNameComparator) ;

		for (String serviceName:getHub().getServiceNames()) {

			Service service = getHub().getService(serviceName) ;

			String groupName = service.getGroupName() ;

			ServiceGroup sg = serviceGroups.get(groupName) ;

			if (sg == null)
				sg = new ServiceGroup(groupName) ;

			sg.put(serviceName,service) ;
			serviceGroups.put(groupName, sg) ;
		}

		for (ServiceGroup sg:serviceGroups.values()) 
			response.appendChild(sg.getXml()) ;

		return response ;
	}



	private class ServiceGroup extends TreeMap<String, Service>{

		private static final long serialVersionUID = -2117255966208147633L;
		
		private String name ;

		public ServiceGroup(String name)  {
			super() ;
			this.name = name ;
		}

		public Element getXml() {
			Element xmlGroup = getHub().createElement("ServiceGroup") ;
			xmlGroup.setAttribute("name", name) ;

			for (Map.Entry<String,Service>e:entrySet()) {
				Element xmlService = getHub().createElement("Service") ;
				xmlService.setAttribute("name", e.getKey()) ;
				xmlService.appendChild(getHub().createCDATAElement("Details", e.getValue().getShortDescription())) ;
				xmlGroup.appendChild(xmlService) ;
			}

			return xmlGroup ;
		}
	}

}
