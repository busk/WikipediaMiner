package org.wikipedia.miner.comparison;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.RelatednessCache;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.Cleaner;
import org.wikipedia.miner.util.text.TextProcessor;
import org.xml.sax.SAXException;

public class ComparisonSnippetExperiment {

	
	Wikipedia _wikipedia ;
	
	ArticleComparer _artCmp ;
	
	BufferedReader _input = new BufferedReader(new InputStreamReader(System.in)) ;
	
	
	public ComparisonSnippetExperiment(Wikipedia wikipedia) throws Exception {
		_wikipedia = wikipedia ;
		_artCmp = new ArticleComparer(wikipedia) ;
		
	}
	
	public Vector<ConnectionSnippet> addToDataset(Vector<ConnectionSnippet> dataset, Article topic1, Article topic2) throws Exception {
		
		
		TreeSet<Article> connections = new TreeSet<Article>() ;

		Article[] links1 = topic1.getLinksIn() ;
		Article[] links2 = topic2.getLinksIn() ;

		int index1 = 0 ;
		int index2 = 0 ;
		
		int maxConsConsidered = 1000 ;

		while (index1 < links1.length && index2 < links2.length) {

			Article link1 = links1[index1] ;
			Article link2 = links2[index2] ;

			int compare = link1.compareTo(link2) ;

			if (compare == 0) {
				if (link1.compareTo(topic1)!= 0 && link2.compareTo(topic2)!= 0) {

					double weight = (_artCmp.getRelatedness(link1, topic1) + _artCmp.getRelatedness(link1, topic2))/2 ;
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
		
		//look for snippets in topic1 which mention topic2
		for (int sentenceIndex:topic1.getSentenceIndexesMentioning(topic2)) {
			dataset.add(setWeightInteractively(new ConnectionSnippet(sentenceIndex, topic1, topic1, topic2))) ;
		}
		
		//look for snippets in topic2 which mention topic1
		for (int sentenceIndex:topic2.getSentenceIndexesMentioning(topic1)) {
			dataset.add(setWeightInteractively(new ConnectionSnippet(sentenceIndex, topic2, topic1, topic2))) ;
		}
		
		ArrayList<Article> articlesOfInterest = new ArrayList<Article>() ;
		articlesOfInterest.add(topic1) ;
		articlesOfInterest.add(topic2) ;
		
		for (Article connection:connections) {
			for (int sentenceIndex:connection.getSentenceIndexesMentioning(articlesOfInterest)) {
				dataset.add(setWeightInteractively(new ConnectionSnippet(sentenceIndex, connection, topic1, topic2))) ;
			}
		}
		
		return dataset ;
	}
	
	public ConnectionSnippet setWeightInteractively(ConnectionSnippet s) throws Exception {
		
		System.out.println("Snippet for " + s.getTopic1() + " v.s. " + s.getTopic2()) ;
		System.out.println(s.getPlainText()) ;
		
		
		s.setWeight(getDoubleInteractively("Snippet weight:")) ;
		
		
		return s ;
	}
	
	public double getDoubleInteractively(String prompt) throws Exception {
		
		System.out.println(prompt) ;
		
		Double dbl = null; 
	
		while (dbl == null) {
			
			try {
				dbl = Double.parseDouble(_input.readLine()) ;
			} catch (Exception e) {
				System.out.println("Not a number. Try again") ;
				continue ;
			}
			
			if (dbl < 0 || dbl > 1) {
				dbl = null ;
				System.out.println("Out of range. Try again") ;
				continue;
			}
		}
		
		return dbl ;
	}
	
	
	public static void main(String args[]) throws Exception {
		
		File confFile = new File("configs/en.xml") ;
		
		TextProcessor tp = new CaseFolder() ;
		

		WikipediaConfiguration conf = new WikipediaConfiguration(confFile) ;
		Wikipedia wikipedia = new Wikipedia(conf, false) ;
		
		ComparisonSnippetExperiment cse = new ComparisonSnippetExperiment(wikipedia) ;
		
		Vector<ConnectionSnippet> dataset = new Vector<ConnectionSnippet>() ;
		
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("Cat", tp), wikipedia.getMostLikelyArticle("Dog", tp)) ;
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("Kiwi", tp), wikipedia.getMostLikelyArticle("Kapiti Island", tp)) ;
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("Book", tp), wikipedia.getMostLikelyArticle("Library", tp)) ;
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("Jimmy Wales", tp), wikipedia.getMostLikelyArticle("Wikipedia", tp)) ;
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("NZ", tp), wikipedia.getMostLikelyArticle("Maori", tp)) ;
		cse.addToDataset(dataset, wikipedia.getMostLikelyArticle("Waikato", tp), wikipedia.getMostLikelyArticle("Maori land wars", tp)) ;
		
		ConnectionSnippetWeighter csw = new ConnectionSnippetWeighter(wikipedia, new ArticleComparer(wikipedia)) ;
		
		csw.train(dataset) ;
		csw.saveTrainingData(new File("data/compare/snippetWeighting_en.arff")) ;
		
		System.out.println("Blah") ;
	}
}
