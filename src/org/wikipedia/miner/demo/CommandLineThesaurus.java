package org.wikipedia.miner.demo;

import org.wikipedia.miner.model.*;
import org.wikipedia.miner.util.MarkupStripper;
import org.wikipedia.miner.util.WikipediaConfiguration;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentLockedException;

import java.io.*;

public class CommandLineThesaurus {

	private Wikipedia wikipedia ;
	private BufferedReader input ;
	private MarkupStripper stripper ;
	
	
	public CommandLineThesaurus(WikipediaConfiguration conf) throws EnvironmentLockedException, DatabaseException {
		
		
		
		
		wikipedia = new Wikipedia(conf, false) ;	
		input = new BufferedReader(new InputStreamReader(System.in));
		stripper = new MarkupStripper() ;
	}
	
	
	public void start() throws IOException, DatabaseException {
	
		while (true) {
			String term = this.requestString("Please enter a term or phrase to search for") ;
			
			if (term == null) 
				break ;
			else
				lookupTerm(term) ;
		}
		
		System.out.println() ;
		System.out.println("Goodbye") ;
	}
	
	
	
	
	private void lookupTerm(String term) throws IOException, DatabaseException {
		
		Label lbl = new Label(wikipedia.getEnvironment(), term, null) ;
		
		if (!lbl.exists()) {
			System.out.println("No concepts found for '" + term + "'") ;
			return ;
		}
		
		Label.Sense[] senses = lbl.getSenses() ;
		
		if (senses.length == 1) {
			
			System.out.println("'" + term + "' has only one sense:") ;
			System.out.println("  " + senses[0].getTitle() + ": " + getGloss(senses[0])) ;
			System.out.println() ;
			
			boolean showDetails = requestBoolean("Would you like to explore this sense?") ;
			
			if (showDetails) {
				lookupSense(senses[0]) ;
			}
			
			return ;
		}
		
		if (senses.length > 1) {
			
			System.out.println("'" + term + "' has " + senses.length + " possible senses:") ;
			
			int index = 0 ;
			for (Label.Sense sense:senses) {
				index ++ ;	
				System.out.println("  [" + index + "] " + sense.getTitle() + ": " + getGloss(sense)) ;
			}
			System.out.println() ;
			
			Integer selection = requestNumber(1,senses.length, "Which sense would you like to explore?") ;
			
			if (selection != null) {
				lookupSense(senses[selection-1]) ;
			}
		}
	}
	
	
	
	private void lookupSense(Label.Sense sense) throws DatabaseException {
		System.out.println(sense) ;
		
		Article linksOut[] = sense.getLinksOut() ;
		
		for (Article linkOut:linksOut) {
			System.out.println(" - " + linkOut) ;
			
		}
	}
	
	private String getGloss(Article art) throws DatabaseException {
		
		String gloss = art.getSentenceMarkup(0) ;
		return stripper.stripToPlainText(gloss, null) ;
		
	}
	
	
	private String requestString(String prompt) throws IOException {
		
		System.out.println(prompt + " (or ENTER for NONE)") ;
		String response = input.readLine().trim() ;
		
		if (response.length() == 0)
			return null ;
		else 
			return response ;
	}
	
	private boolean requestBoolean(String prompt) throws IOException {
		
		while (true) {
			System.out.println(prompt + " (Y/N)") ;
			String response = input.readLine();
			
			if (response.matches("[YyNn]")) 
				return response.equalsIgnoreCase("Y") ;
			else
				System.out.println("INVALID RESPONSE: try again.") ;
		}
	}
	
	private Integer requestNumber(int min, int max, String prompt) throws IOException {
		
		while (true) {
			System.out.println(prompt + " (" + min + "-" + max + " or ENTER for NONE)") ;
			
			String response = input.readLine().trim();
			
			if (response.matches("[0-9]+")) {
				
				Integer num = Integer.parseInt(response) ;
				
				if (num < min || num > max)
					System.out.println("INVALID RESPONSE: try again.") ;
				else 
					return num ;
			} else {
				
				if (response.length() == 0)
					return null ;
				else 
					System.out.println("INVALID RESPONSE: try again.") ;
			}
		}
	}
	
	
	
	public static void main(String[] args) throws EnvironmentLockedException, DatabaseException, IOException {
		
		if (args.length != 1) {		
			System.out.println("Please specify a directory containing a fully prepared Wikipedia database") ;
			return ;
		}
		
		File envDir = new File(args[0]) ;
		
		WikipediaConfiguration conf = new WikipediaConfiguration("en", envDir) ;

		CommandLineThesaurus clt = new CommandLineThesaurus(conf) ;
		clt.start() ;
		
		
		
	}
	
	
	
}
