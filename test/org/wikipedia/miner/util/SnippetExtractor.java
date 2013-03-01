package org.wikipedia.miner.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.service.MarkupFormatter;
import org.xml.sax.SAXException;

import com.sleepycat.je.EnvironmentLockedException;

public class SnippetExtractor {
	
	private Wikipedia _wikipedia ;
	private MarkupStripper _stripper ;
	private SentenceDetectorME _sentenceDetector ;
	
	private MarkupFormatter _formatter ;
	
	public SnippetExtractor(Wikipedia wikipedia, File sentenceModel) throws FileNotFoundException {
		
		_wikipedia = wikipedia ;
		_stripper = new MarkupStripper() ;
		
		
		InputStream sentenceModelStream = new FileInputStream(sentenceModel);
		SentenceModel model = null ;
		try {
		  model = new SentenceModel(sentenceModelStream);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (sentenceModelStream != null) {
		    try {
		    	sentenceModelStream.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}

		_sentenceDetector = new SentenceDetectorME(model) ;
		
		_formatter = new MarkupFormatter() ;
	}
	
	
	public Vector<Article> gatherArticles(File file) throws IOException {
		
		Vector<Article> arts = new Vector<Article>() ;
		
		BufferedReader reader = new BufferedReader(new FileReader(file)) ;
		
		String line ;
		while ((line = reader.readLine()) != null) {
			
			Article art = _wikipedia.getArticleByTitle(line.trim()) ;
			
			if (art != null)
				arts.add(art) ;
			
		}
		
		reader.close();
		
		return arts ;
	}
	
	public void testAllArticles(Vector<Article> arts) {
		
		for (Article art:arts) {
			
			System.out.println("\n\nTESTING " + art) ;
			testSentenceExtraction(art) ;
		}
	}
	
	public void testSentenceExtraction(Article art) {
		
		String fs = art.getSentenceMarkup(0) ;
		
		System.out.println(fs) ;
		
		
	}
	
	public void testParagraphExtraction(Article art) {
		
		String markup = art.getMarkup() ;

		markup = markup.replaceAll("={2,}(.+)={2,}", "\n") ; //clear section headings completely - not just formating, but content as well.			
		markup = _stripper.stripAllButInternalLinksAndEmphasis(markup, null) ;
		markup = _stripper.stripNonArticleInternalLinks(markup, null) ;
		markup = _stripper.stripExcessNewlines(markup) ;

		//System.out.println(markup) ;
		
		String fp = "" ;
		int pos = markup.indexOf("\n\n") ;

		while (pos>=0) {
			fp = markup.substring(0, pos) ;

			if (pos > 150) 
				break ;

			pos = markup.indexOf("\n\n", pos+2) ;
		}

		fp = fp.replaceAll("\n", " ") ;
		fp = fp.replaceAll("\\s+", " ") ;  //turn all whitespace into spaces, and collapse them.
		fp = fp.trim();

		
		
		//System.out.println("Unformatted:") ;
		//System.out.println(" - " + fp) ;
		
		fp = _formatter.format(fp, null, _wikipedia) ;
		//System.out.println("Formatted:") ;
		System.out.println(" - " + fp) ;
		
		
	}
	
	
	public static void main(String args[]) throws EnvironmentLockedException, ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		File sentenceModel = new File("models/en-sent.bin") ;
		
		
		File wikiConf = new File("configs/en.xml") ;
		
		Wikipedia wiki = new Wikipedia(wikiConf, false) ;
		
		
		
		SnippetExtractor se = new SnippetExtractor(wiki, sentenceModel) ;
		
		File artTitles = new File("data/sentenceArticles.txt") ;
		Vector<Article> articles = se.gatherArticles(artTitles) ;
		
		//se.testAllArticles(articles) ;
		
		se.testSentenceExtraction(wiki.getArticleByTitle("Carnivore")) ;
		
		
		
	}
}
