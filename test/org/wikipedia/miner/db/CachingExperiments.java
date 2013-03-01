package org.wikipedia.miner.db;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.xml.sax.SAXException;

public class CachingExperiments {

	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		WikipediaConfiguration conf = new WikipediaConfiguration(new File("configs/en.xml")) ;
		
		Wikipedia wikipedia = new Wikipedia(conf, true) ;
		
		System.out.println(wikipedia.getMostLikelyArticle("Kiwi", null)) ;
		
	}
}
