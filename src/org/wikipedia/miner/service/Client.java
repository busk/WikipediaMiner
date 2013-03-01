package org.wikipedia.miner.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.time.DateUtils;
import org.w3c.dom.Element;

public class Client {

	
	private String _name ;
	private String _password ;
	
	private HashMap<Integer, Usage> _usageByGranularity ;	
	
	private static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss") ;
	
	public Client(String name, String password, int minLimit, int hourLimit, int dayLimit) {
		_name = name ;
		_password = password ;
		
		_usageByGranularity = new HashMap<Integer, Usage>() ;
		_usageByGranularity.put(Calendar.MINUTE, new Usage(Calendar.MINUTE, minLimit)) ;
		_usageByGranularity.put(Calendar.HOUR, new Usage(Calendar.HOUR, hourLimit)) ;
		_usageByGranularity.put(Calendar.DAY_OF_MONTH, new Usage(Calendar.DAY_OF_MONTH, dayLimit)) ;
	}
	
	public Client(String name, String password, Client client) {
		_name = name ;
		_password = password ;
		
		_usageByGranularity = new HashMap<Integer, Usage>() ;
		_usageByGranularity.put(Calendar.MINUTE, new Usage(Calendar.MINUTE, client.getMinuteUsage().getLimit())) ;
		_usageByGranularity.put(Calendar.HOUR, new Usage(Calendar.HOUR, client.getHourUsage().getLimit())) ;
		_usageByGranularity.put(Calendar.DAY_OF_MONTH, new Usage(Calendar.DAY_OF_MONTH, client.getDayUsage().getLimit())) ;
	}
	
	public String getName() {
		return _name ;
	}
	
	public boolean passwordMatches(String password) {
			
		if (_password == null)
			return true ;
		
		return _password.equals(password) ;
	}

	public boolean update(int usageCost) {
		
		boolean limitExceeded = false ;
		
		for (Usage u:_usageByGranularity.values()) {
			
			if (u.update(usageCost)) 
				limitExceeded = true ;
		}
		
		return limitExceeded ;
	}
	
	public Usage getMinuteUsage() {
		return _usageByGranularity.get(Calendar.MINUTE) ;
	}
	
	public Usage getHourUsage() {
		return _usageByGranularity.get(Calendar.HOUR) ;
	}
	
	public Usage getDayUsage() {
		return _usageByGranularity.get(Calendar.DAY_OF_MONTH) ;
	}
	
	public Element getXML(ServiceHub hub) {
		
		Element xml = hub.createElement("Client") ;
		
		xml.setAttribute("name", _name) ;
		
		for (Usage u:_usageByGranularity.values())
			xml.appendChild(u.getXML(hub)) ;
		
		return xml ;
	}
	
	public class Usage {
		
		private int _granularity ;
		private Date _start ;
		private Date _end ;
		int _count ;
		int _limit ;
		
		public Usage(int granularity, int limit) {
			
			_granularity = granularity ;
			_count = 0 ;
			setPeriod() ;
			
			_limit = limit ;
		}
		
		public Element getXML(ServiceHub hub) {
			
			Element xml = hub.createElement("Usage") ;
			
			switch(_granularity) {
			case Calendar.MINUTE :
				xml.setAttribute("granularity", "minute") ;
				break ;
			case Calendar.HOUR :
				xml.setAttribute("granularity", "hour") ;
				break ;
			case Calendar.DAY_OF_MONTH :
				xml.setAttribute("granularity", "day") ;
				break ;
			}
			
			xml.setAttribute("unitLimit", String.valueOf(_limit)) ;
			xml.setAttribute("unitsUsed", String.valueOf(_count)) ;
			xml.setAttribute("start", df.format(_start)) ;
			xml.setAttribute("end", df.format(_end)) ;
			
			return xml ;
		}
		
		public Date getPeriodStart() {
			return _start ;
		}
		
		public Date getPeriodEnd() {
			return _end ;
		}
		
		public int getCount() {
			return _count ;
		}
		
		public int getLimit() {
			return _limit ;
		}
		
		public boolean limitExceeded() {
			return (_limit > 0 && _count > _limit) ;
		}
		
		private void setPeriod() {
			
			_start = new Date() ;
			_end = new Date() ;
			
			DateUtils.truncate(_start, _granularity) ;
			DateUtils.truncate(_end, _granularity) ;
			
			switch(_granularity) {
			case Calendar.MINUTE:
				_end = DateUtils.addMinutes(_end, 1) ;
				break ;
			case Calendar.HOUR:
				_end = DateUtils.addHours(_end, 1) ;
				break ;
			case Calendar.DAY_OF_MONTH:
				_end = DateUtils.addDays(_end, 1) ;
				break ;
			}
			
		}
		
		protected boolean update(int usageCost) {
			
			Date now = new Date() ;
			
			if (now.after(_end)) {
				setPeriod() ;
				_count = usageCost ;
			} else {
				_count = _count + usageCost ;
			}
			
			return limitExceeded() ;
		}
	}
	
}
