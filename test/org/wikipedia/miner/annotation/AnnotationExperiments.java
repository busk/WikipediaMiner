package org.wikipedia.miner.annotation;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.wikipedia.miner.annotation.ArticleCleaner.SnippetLength;
import org.wikipedia.miner.annotation.weighting.LinkDetector;
import org.wikipedia.miner.comparison.ArticleComparer.DataDependency;
import org.wikipedia.miner.db.WDatabase.CachePriority;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.ArticleSet;
import org.wikipedia.miner.util.ArticleSetBuilder;
import org.wikipedia.miner.util.LabelIterator;
import org.wikipedia.miner.util.MemoryMeasurer;
import org.wikipedia.miner.util.ProgressTracker;
import org.wikipedia.miner.util.Result;
import org.wikipedia.miner.util.WikipediaConfiguration;

public class AnnotationExperiments {


	private enum ArtSetName{training, develop, testDisambig, testDetect} ; 

	public static HashMap<Double, Integer> getCachedLabelCounts(WikipediaConfiguration conf) {

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		HashMap<Double, Integer> cachedLabelsForLinkProb = new HashMap<Double, Integer>() ;

		for (double minLinkProb = 0 ; minLinkProb < 0.02 ; minLinkProb = minLinkProb + 0.001) {
			cachedLabelsForLinkProb.put(minLinkProb, 0) ;
		}


		long totalLabels = wikipedia.getEnvironment().getDbLabel(conf.getDefaultTextProcessor()).getDatabaseSize() ;

		ProgressTracker pt = new ProgressTracker(totalLabels, "Counting labels", AnnotationExperiments.class) ;

		LabelIterator iter = wikipedia.getLabelIterator(conf.getDefaultTextProcessor()) ;

		while (iter.hasNext()) {
			pt.update() ;

			Label l = iter.next();

			for (Map.Entry<Double, Integer> e:cachedLabelsForLinkProb.entrySet()) {
				if (l.getLinkProbability() >= e.getKey())
					e.setValue(e.getValue() + 1) ;
			}
		}
		iter.close() ;
		wikipedia.close();

		System.out.println("cached labels for minLinkProb") ;
		for (Map.Entry<Double, Integer> e:cachedLabelsForLinkProb.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue()) ;
		}

		return cachedLabelsForLinkProb ;
	}

	public static HashMap<ArtSetName,ArticleSet> loadArticleSets(WikipediaConfiguration conf, boolean featureArticles) throws IOException {

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		HashMap<ArtSetName,ArticleSet> articleSets = new HashMap<ArtSetName,ArticleSet>() ;

		articleSets.put(ArtSetName.training, new ArticleSet(getArticleSetFile(conf.getLangCode(), ArtSetName.training, featureArticles), wikipedia)) ;
		articleSets.put(ArtSetName.develop, new ArticleSet(getArticleSetFile(conf.getLangCode(), ArtSetName.develop, featureArticles), wikipedia)) ;
		articleSets.put(ArtSetName.testDisambig, new ArticleSet(getArticleSetFile(conf.getLangCode(), ArtSetName.testDisambig, featureArticles), wikipedia)) ;
		articleSets.put(ArtSetName.testDetect, new ArticleSet(getArticleSetFile(conf.getLangCode(), ArtSetName.testDetect, featureArticles), wikipedia)) ;

		wikipedia.close();

		return articleSets ;
	}

	public static HashMap<ArtSetName,ArticleSet> buildAndSaveArticleSets(WikipediaConfiguration conf, boolean featureArticles) throws IOException {

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		Pattern mustMatch = null ;

		if (featureArticles) {
			if (conf.getLangCode().equals("en"))
				mustMatch = Pattern.compile("\\{\\{Featured article") ;
			if (conf.getLangCode().equals("de"))
				mustMatch = Pattern.compile("\\{\\{Exzellent") ;
		}

		int[] sizes = {200,50,100,100} ;

		ArticleSet[] sets = new ArticleSetBuilder()
		.setMinInLinks(50)
		.setMinOutLinks(50)
		.setMinWordCount(500)
		.setMaxWordCount(5000)
		.setMaxListProportion(0.1)
		.setMustMatchPattern(mustMatch)
		.buildExclusiveSets(sizes, wikipedia) ;

		HashMap<ArtSetName,ArticleSet> articleSets = new HashMap<ArtSetName,ArticleSet>() ;

		articleSets.put(ArtSetName.training, sets[0]) ;
		articleSets.put(ArtSetName.develop, sets[1]) ;
		articleSets.put(ArtSetName.testDisambig, sets[2]) ;
		articleSets.put(ArtSetName.testDetect, sets[3]) ;

		sets[0].save(getArticleSetFile(conf.getLangCode(), ArtSetName.training, featureArticles)) ;
		sets[1].save(getArticleSetFile(conf.getLangCode(), ArtSetName.develop, featureArticles)) ;
		sets[2].save(getArticleSetFile(conf.getLangCode(), ArtSetName.testDisambig, featureArticles)) ;
		sets[3].save(getArticleSetFile(conf.getLangCode(), ArtSetName.testDetect, featureArticles)) ;

		wikipedia.close() ;


		return articleSets;
	}





	public static File getArticleSetFile(String lang, ArtSetName name, boolean featureArticles) {

		StringBuffer sb = new StringBuffer("data/annotate/") ;
		sb.append(lang) ;
		sb.append("_") ;

		if (featureArticles) 
			sb.append("featureArticles") ;
		else
			sb.append("randomArticles") ;

		sb.append("_") ;
		sb.append(name) ;
		sb.append(".txt") ;

		return new File(sb.toString()) ;

	}


	public static void doFinalTests() throws Exception {

		//set up en configuration and data files

		WikipediaConfiguration enConf = new WikipediaConfiguration(new File("configs/en.xml")) ;

		enConf.setMinSenseProbability(0.01F);
		enConf.setMinLinkProbability(0.005F);

		//Make absolutely sure no models are being used
		//enConf.setArticleComparisonModel(null) ;
		enConf.setLabelComparisonModel(null) ;
		enConf.setLabelDisambiguationModel(null) ;
		enConf.setTopicDisambiguationModel(null) ;
		enConf.setLinkDetectionModel(null) ;

		//load normal article sets, and make correct size
		//HashMap<ArtSetName,ArticleSet> enNormalArticleSets = loadArticleSets(enConf, false) ;
		//enNormalArticleSets.put(ArtSetName.training, enNormalArticleSets.get(ArtSetName.training).getRandomSubset(50)) ;
		//enNormalArticleSets.put(ArtSetName.develop, enNormalArticleSets.get(ArtSetName.develop).getRandomSubset(25)) ;
		//enNormalArticleSets.put(ArtSetName.testDisambig, enNormalArticleSets.get(ArtSetName.testDisambig).getRandomSubset(100)) ;
		//enNormalArticleSets.put(ArtSetName.testDetect, enNormalArticleSets.get(ArtSetName.testDetect).getRandomSubset(100)) ;

		//load feature article sets, and make correct size
		HashMap<ArtSetName,ArticleSet> enFeatureArticleSets = loadArticleSets(enConf, true) ;
		enFeatureArticleSets.put(ArtSetName.training, enFeatureArticleSets.get(ArtSetName.training).getRandomSubset(100)) ;


		//set up de configuration and data files
		/*
		WikipediaConfiguration deConf = new WikipediaConfiguration(new File("configs/de.xml")) ;

		deConf.setMinSenseProbability(0.001F);
		deConf.setMinLinkProbability(0.001F);

		//Make absolutely sure no models are being used
		deConf.setArticleComparisonModel(null) ;
		deConf.setLabelComparisonModel(null) ;
		deConf.setLabelDisambiguationModel(null) ;

		//load normal article sets, and make correct size
		HashMap<ArtSetName,ArticleSet> deNormalArticleSets = loadArticleSets(deConf, false) ;
		deNormalArticleSets.put(ArtSetName.training, deNormalArticleSets.get(ArtSetName.training).getRandomSubset(100)) ;
		deNormalArticleSets.put(ArtSetName.develop, deNormalArticleSets.get(ArtSetName.develop).getRandomSubset(50)) ;
		deNormalArticleSets.put(ArtSetName.testDisambig, deNormalArticleSets.get(ArtSetName.testDisambig).getRandomSubset(100)) ;
		deNormalArticleSets.put(ArtSetName.testDetect, deNormalArticleSets.get(ArtSetName.testDetect).getRandomSubset(100)) ;

		//load feature article sets, and make correct size
		HashMap<ArtSetName,ArticleSet> deFeatureArticleSets = loadArticleSets(deConf, true) ;
		deFeatureArticleSets.put(ArtSetName.training, deFeatureArticleSets.get(ArtSetName.training).getRandomSubset(100)) ;

		 */

		AnnotationExperiments ae = new AnnotationExperiments() ;


		ArrayList<DataDependency> d = new ArrayList<DataDependency>() ;
		boolean usingML = true ;

		//ArrayList<AnnotationDataPoint> srResults = new ArrayList<AnnotationDataPoint>() ;



		//investigate sr dependencies
		/*
		ArrayList<AnnotationDataPoint> srResults = new ArrayList<AnnotationDataPoint>() ;

		//pageLinksIn
		d.add(DataDependency.pageLinksIn) ;
		enConf = configureConf(enConf, d, usingML) ;
		srResults.add(ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.develop),enNormalArticleSets.get(ArtSetName.develop),false,false)) ;

		//pageLinksIn+linkCounts
		d.add(DataDependency.linkCounts) ;
		enConf = configureConf(enConf,d, usingML) ;
		srResults.add(ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.develop),enNormalArticleSets.get(ArtSetName.develop),false,false)) ;

		//pageLinksOut
		d.clear();
		d.add(DataDependency.pageLinksOut) ;
		enConf = configureConf(enConf, d, usingML) ;
		srResults.add(ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.develop),enNormalArticleSets.get(ArtSetName.develop),false,false)) ;


		//pageLinksOut+linkCounts
		d.add(DataDependency.linkCounts) ;
		enConf = configureConf(enConf,d, usingML) ;
		srResults.add(ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.develop),enNormalArticleSets.get(ArtSetName.develop),false,false)) ;


		//pageLinksIn+pageLinksOut+linkCounts
		d.add(DataDependency.pageLinksIn) ;
		enConf = configureConf(enConf,d, usingML) ;
		srResults.add(ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.develop),enNormalArticleSets.get(ArtSetName.develop),false,false)) ;

		System.out.println("\n\nFINAL SR DEPENDENCY RESULTS\n\n") ;
		for (AnnotationDataPoint p:srResults) {
			System.out.println(p) ;
		}

		 */

		//final evaluation, en 

		d.clear();
		d.add(DataDependency.pageLinksIn) ;
		enConf = configureConf(enConf,d, usingML) ;

		//AnnotationDataPoint enNormal = ae.doExperiment(enConf, enNormalArticleSets.get(ArtSetName.training),enNormalArticleSets.get(ArtSetName.testDisambig),enNormalArticleSets.get(ArtSetName.testDetect),false,false) ;

		//System.out.println("\n\nFINAL EN NORMAL RESULTS\n\n") ;
		//System.out.println(enNormal) ;


		AnnotationDataPoint enFeature = ae.doExperiment(enConf, enFeatureArticleSets.get(ArtSetName.training),enFeatureArticleSets.get(ArtSetName.testDisambig),enFeatureArticleSets.get(ArtSetName.testDetect),true,true) ;

		System.out.println("\n\nFINAL EN FEATURE RESULTS\n\n") ;
		System.out.println(enFeature) ;


		//final evaluation, de 
		/*
		d.clear();
		d.add(DataDependency.pageLinksIn) ;
		deConf = configureConf(deConf,d, usingML) ;

		AnnotationDataPoint deNormal = ae.doExperiment(deConf, deNormalArticleSets.get(ArtSetName.training),deNormalArticleSets.get(ArtSetName.testDisambig),deNormalArticleSets.get(ArtSetName.testDetect),false,false) ;

		System.out.println("\n\nFINAL DE NORMAL RESULTS\n\n") ;
		System.out.println(deNormal) ;


		AnnotationDataPoint deFeature = ae.doExperiment(deConf, deFeatureArticleSets.get(ArtSetName.training),deFeatureArticleSets.get(ArtSetName.testDisambig),deFeatureArticleSets.get(ArtSetName.testDetect),false,false) ;

		System.out.println("\n\nFINAL DE FEATURE RESULTS\n\n") ;
		System.out.println(deFeature) ;



		 */




	}

	public static void main(String args[]) throws Exception {

		doFinalTests() ;
		if (true)
			return ;
		/*
		WikipediaConfiguration conf = new WikipediaConfiguration(new File("configs/en.xml")) ;
		File trainingFile = new File("data/annotate/en_randomArticles_training.txt") ;
		File developFile = new File("data/annotate/en_randomArticles_develop.txt") ;
		File testDisambigFile = new File("data/annotate/en_randomArticles_testDisambig.txt") ;
		File testDetectFile = new File("data/annotate/en_randomArticles_testDetect.txt") ;

		File trainingFeatureFile = new File("data/annotate/en_featureArticles_training.txt") ;
		File testingFeatureFile = new File("data/annotate/en_featureArticles_testing.txt") ;
		 */

		WikipediaConfiguration conf = new WikipediaConfiguration(new File("configs/en.xml")) ;

		//WikipediaConfiguration conf = new WikipediaConfiguration(new File("configs/de.xml")) ;

		getCachedLabelCounts(conf) ;

		buildAndSaveArticleSets(conf, true) ;

		if (true)
			return ;


		//File trainingSet = new File("data/annotate/de_randomArticles_training.txt") ;
		//File developSet = new File("data/annotate/de_randomArticles_develop.txt") ;
		//File testDisambigSet = new File("data/annotate/de_randomArticles_testDisambig.txt") ;
		//File testDetectSet = new File("data/annotate/de_randomArticles_testDetect.txt") ;

		//File trainingFeatureFile = new File("data/annotate/de_featureArticles_training.txt") ;
		//File testingFeatureFile = new File("data/annotate/de_featureArticles_testing.txt") ;




		//Make absolutely sure no models are being used
		conf.setArticleComparisonModel(null) ;
		conf.setLabelComparisonModel(null) ;
		conf.setLabelDisambiguationModel(null) ;




		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		/*
		ArticleSet trainingSet = new ArticleSet(trainingFile, wikipedia) ;
		ArticleSet developSet = new ArticleSet(developFile, wikipedia) ;
		ArticleSet disambigSet = new ArticleSet(testDisambigFile, wikipedia) ;
		ArticleSet detectSet = new ArticleSet(testDetectFile, wikipedia) ;
		 */


		wikipedia.close();
		wikipedia = null ;








		/*
		//investigate impact of relatedness dependencies 

		AnnotationExperiments ae = new AnnotationExperiments(trainingSet, disambigSet, detectSet) ;

		ArrayList<AnnotationDataPoint> results = new ArrayList<AnnotationDataPoint>() ;

		ArrayList<DataDependency> d = new ArrayList<DataDependency>() ;
		boolean usingML = true ;

		//pageLinksIn
		d.add(DataDependency.pageLinksIn) ;
		conf = configureConf(conf, d, usingML) ;
		results.add(ae.doExperiment(conf)) ;

		//pageLinksIn+linkCounts
		d.add(DataDependency.linkCounts) ;
		conf = configureConf(conf,d, usingML) ;
		results.add(ae.doExperiment(conf)) ;

		//pageLinksOut
		d.clear();
		d.add(DataDependency.pageLinksOut) ;
		conf = configureConf(conf, d, usingML) ;
		results.add(ae.doExperiment(conf)) ;

		//pageLinksOut+linkCounts
		d.add(DataDependency.linkCounts) ;
		conf = configureConf(conf,d, usingML) ;
		results.add(ae.doExperiment(conf)) ;

		//pageLinksIn+pageLinksOut+linkCounts
		d.add(DataDependency.pageLinksIn) ;
		conf = configureConf(conf,d, usingML) ;
		results.add(ae.doExperiment(conf)) ;


		System.out.println("\n\nFINAL RESULTS\n\n") ;
		for (AnnotationDataPoint p:results) {
			System.out.println(p) ;
		}

		 */

		//investigate impact of training instances
		//trainingSet = trainingSet.getRandomSubset(50) ;
		/*
		developSet = developSet.getRandomSubset(50) ;

		ArrayList<DataDependency> d = new ArrayList<DataDependency>() ;
		d.add(DataDependency.pageLinksIn) ;
		conf = configureConf(conf, d, true) ;

		AnnotationExperiments ae = new AnnotationExperiments() ;

		//TreeMap<Double, AnnotationDataPoint> results = ae.investigateMinSenseProbability(conf, trainingSet, developSet) ;		
		//TreeMap<Double, AnnotationDataPoint> results = ae.investigateMinLinkProbability(conf, trainingSet, developSet) ;		

		//System.out.println("Min Sense Probability") ;
		//displayResultMap(results) ;

		conf.setMinSenseProbability(0.001F) ;
		conf.setMinLinkProbability(0.005F) ;



		TreeMap<Integer, AnnotationDataPoint> results2 = ae.investigateTrainingSize(conf, trainingSet, developSet) ;		
		System.out.println("Training amount") ;
		displayResultMap(results2) ;
		 */

	}

	private static WikipediaConfiguration configureConf(WikipediaConfiguration conf, ArrayList<DataDependency> d, boolean usingML) {

		conf.clearDatabasesToCache() ;

		//conf.setMinSenseProbability(0.02F) ;
		//conf.setMinLinkProbability(0.065F) ;

		EnumSet<DataDependency> dependencies = EnumSet.copyOf(d) ;
		conf.setArticleComparisonDependancies(dependencies) ;

		if (dependencies.contains(DataDependency.pageLinksIn))
			conf.addDatabaseToCache(DatabaseType.pageLinksInNoSentences, CachePriority.speed) ;

		if (dependencies.contains(DataDependency.pageLinksOut))
			conf.addDatabaseToCache(DatabaseType.pageLinksOutNoSentences, CachePriority.speed) ;

		if (dependencies.contains(DataDependency.linkCounts))
			conf.addDatabaseToCache(DatabaseType.pageLinkCounts, CachePriority.speed) ;

		conf.addDatabaseToCache(DatabaseType.articlesByTitle, CachePriority.space) ;
		conf.addDatabaseToCache(DatabaseType.label, CachePriority.space) ;

		if (usingML) {
			String artCmpModel = "models/compare/artCompare_" + getFileNameChunk(conf.getLangCode(), dependencies) + ".model" ;
			conf.setArticleComparisonModel(new File(artCmpModel)) ;
		} else {
			conf.setArticleComparisonModel(null) ;
		}

		return conf ;
	}

	private TreeMap<Double, AnnotationDataPoint> investigateMinSenseProbability(WikipediaConfiguration conf, ArticleSet trainingSet, ArticleSet developSet) throws Exception {

		AnnotationDataPoint metaPoint = new AnnotationDataPoint() ;

		conf.setMinSenseProbability(0.0F) ;


		Wikipedia wikipedia = prepareWikipedia(conf, metaPoint, false) ;

		TreeMap<Double, AnnotationDataPoint> results = new TreeMap<Double,AnnotationDataPoint>() ;


		for (double minSenseProb=0.000 ; minSenseProb<0.02 ; minSenseProb=minSenseProb+0.001) {

			AnnotationDataPoint p = new AnnotationDataPoint() ;
			p.dependencies = metaPoint.dependencies ;
			p.lang = metaPoint.lang ;
			p.usingML = metaPoint.usingML ;

			Disambiguator d = doDisambiguationExperiment(p, wikipedia, trainingSet, developSet, false, false, minSenseProb) ;

			//LinkDetector ld = doDetectionExperiment(p, wikipedia, d, trainingSet, developSet, false, false) ;

			System.out.println(minSenseProb + "\t" + p) ;

			results.put(minSenseProb,p) ;
		}

		wikipedia.close();

		return results ;



	}

	private TreeMap<Double, AnnotationDataPoint> investigateMinLinkProbability(WikipediaConfiguration conf, ArticleSet trainingSet, ArticleSet developSet) throws Exception {

		AnnotationDataPoint metaPoint = new AnnotationDataPoint() ;

		conf.setMinSenseProbability(0.01F) ;
		conf.setMinLinkProbability(0.0F) ;

		Wikipedia wikipedia = prepareWikipedia(conf, metaPoint, false) ;

		TreeMap<Double, AnnotationDataPoint> results = new TreeMap<Double,AnnotationDataPoint>() ;

		Disambiguator d = new Disambiguator(wikipedia) ;
		d.train(trainingSet, SnippetLength.full, "blah", null) ;
		d.buildDefaultClassifier() ;

		for (double minLinkProb=0.00 ; minLinkProb<0.02 ; minLinkProb=minLinkProb+0.001) {

			AnnotationDataPoint p = new AnnotationDataPoint() ;
			p.dependencies = metaPoint.dependencies ;
			p.lang = metaPoint.lang ;
			p.usingML = metaPoint.usingML ;

			d.setMinLinkProbability(minLinkProb) ;

			LinkDetector ld = doDetectionExperiment(p, wikipedia, d, trainingSet, developSet, false, false) ;

			System.out.println(minLinkProb + "\t" + p) ;

			results.put(minLinkProb,p) ;
		}

		wikipedia.close();

		return results ;



	}


	private Wikipedia prepareWikipedia(WikipediaConfiguration conf, AnnotationDataPoint p, boolean sleep) throws InterruptedException {

		if (sleep) {
			System.out.println("Sleeping for garbage collection...") ;
			System.gc() ;	
			Thread.sleep(60000) ;
		}

		p.dependencies = conf.getArticleComparisonDependancies() ;
		p.lang = conf.getLangCode() ;
		p.usingML = (conf.getArticleComparisonModel() != null) ;

		long memStart = MemoryMeasurer.getBytesUsed() ;
		long timeStart = System.currentTimeMillis() ;

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		long timeEnd = System.currentTimeMillis() ;
		long memEnd = MemoryMeasurer.getBytesUsed() ;

		p.cacheTime = timeEnd-timeStart ;
		p.cacheSpace = memEnd-memStart ;

		return wikipedia ;

	}

	private TreeMap<Integer, AnnotationDataPoint> investigateTrainingSize(WikipediaConfiguration conf, ArticleSet trainingSet, ArticleSet developSet) throws IOException, Exception {

		AnnotationDataPoint metaPoint = new AnnotationDataPoint() ;
		metaPoint.dependencies = conf.getArticleComparisonDependancies() ;
		metaPoint.lang = conf.getLangCode() ;
		metaPoint.usingML = (conf.getArticleComparisonModel() != null) ;

		long memStart = MemoryMeasurer.getBytesUsed() ;
		long timeStart = System.currentTimeMillis() ;

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		long timeEnd = System.currentTimeMillis() ;
		long memEnd = MemoryMeasurer.getBytesUsed() ;

		metaPoint.cacheTime = timeEnd-timeStart ;
		metaPoint.cacheSpace = memEnd-memStart ;


		TreeMap<Integer, AnnotationDataPoint> results = new TreeMap<Integer,AnnotationDataPoint>() ;


		for (int i=25 ; i<500 ; i=i+25) {

			AnnotationDataPoint p = new AnnotationDataPoint() ;
			p.dependencies = metaPoint.dependencies ;
			p.lang = metaPoint.lang ;
			p.usingML = metaPoint.usingML ;

			ArticleSet trainingSubset = trainingSet.getRandomSubset(i) ;

			Disambiguator d = doDisambiguationExperiment(p, wikipedia, trainingSubset, developSet, false, false, null) ;

			LinkDetector ld = doDetectionExperiment(p, wikipedia, d, trainingSubset, developSet, false, false) ;

			System.out.println(i + "\t" + p) ;

			results.put(i,p) ;
		}

		wikipedia.close();

		return results ;
	}


	private static void displayResultMap(Map<?,AnnotationDataPoint> results) {

		for (Map.Entry<?, AnnotationDataPoint> e:results.entrySet()) 
			System.out.println(e.getKey() + "\t" + e.getValue()) ;

	}


	private AnnotationDataPoint doExperiment(WikipediaConfiguration conf, ArticleSet trainingSet, ArticleSet disambigSet, ArticleSet detectSet, boolean saveTrainingData, boolean saveModels) throws IOException, Exception {

		System.out.println("Sleeping for garbage collection...") ;
		System.gc() ;	
		Thread.sleep(60000) ;


		AnnotationDataPoint point = new AnnotationDataPoint() ;
		point.dependencies = conf.getArticleComparisonDependancies() ;
		point.lang = conf.getLangCode() ;
		point.usingML = (conf.getArticleComparisonModel() != null) ;


		long memStart = MemoryMeasurer.getBytesUsed() ;
		long timeStart = System.currentTimeMillis() ;

		Wikipedia wikipedia = new Wikipedia(conf, false) ;

		long timeEnd = System.currentTimeMillis() ;
		long memEnd = MemoryMeasurer.getBytesUsed() ;

		point.cacheTime = timeEnd-timeStart ;
		point.cacheSpace = memEnd-memStart ;

		Disambiguator d = doDisambiguationExperiment(point, wikipedia, trainingSet, disambigSet, saveTrainingData, saveModels, null) ;

		LinkDetector ld = doDetectionExperiment(point, wikipedia, d, trainingSet, detectSet, saveTrainingData, saveModels) ;

		wikipedia.close();



		System.out.println(point) ;
		return point ;
	}





	private Disambiguator doDisambiguationExperiment(AnnotationDataPoint point, Wikipedia wikipedia, ArticleSet trainingSet, ArticleSet testingSet, boolean saveArff, boolean saveModel, Double minSenseProb) throws Exception {

		String fileNameChunk = getFileNameChunk(point.lang, point.dependencies) ;


		Disambiguator d = new Disambiguator(wikipedia) ;
		if (minSenseProb != null)
			d.setMinSenseProbability(minSenseProb) ;


		long disambigTrainStart = System.currentTimeMillis() ;
		d.train(trainingSet, SnippetLength.full, "blah", null) ;
		d.buildDefaultClassifier() ;
		long disambigTrainEnd = System.currentTimeMillis() ;

		if (saveModel) {
			File disambigModel = new File("models/annotate/disambig_" + fileNameChunk + ".model") ;
			d.saveClassifier(disambigModel) ;
		}

		if (saveArff) {
			File disambigArff = new File("data/annotate/disambig_" + fileNameChunk + ".arff") ;
			d.saveTrainingData(disambigArff) ;
		}

		d.clearTrainingData() ;

		point.disambig_trainTime = disambigTrainEnd - disambigTrainStart ;

		long disambigTestStart = System.currentTimeMillis() ;
		Result<Integer> disambigResult = d.test(testingSet, wikipedia, SnippetLength.full, null) ;
		System.out.println("final: " + disambigResult) ;
		long disambigTestEnd = System.currentTimeMillis() ;

		point.disambig_testTime = disambigTestEnd - disambigTestStart ;
		point.disambig_p = disambigResult.getPrecision() ;
		point.disambig_r = disambigResult.getRecall() ;
		point.disambig_f = disambigResult.getFMeasure() ;
		point.disambig_sensesConsidered = d.getSensesConsidered() ;


		return d ;
	}


	private LinkDetector doDetectionExperiment(AnnotationDataPoint point, Wikipedia wikipedia, Disambiguator d, ArticleSet trainingSet, ArticleSet testSet, boolean saveArff, boolean saveModel) throws IOException, Exception {

		String fileNameChunk = getFileNameChunk(point.lang, point.dependencies) ;

		TopicDetector td = new TopicDetector(wikipedia, d, false, false) ;

		LinkDetector ld = new LinkDetector(wikipedia) ;

		long detectTrainStart = System.currentTimeMillis() ;
		ld.train(trainingSet,  SnippetLength.full, "blah", td, null) ;
		ld.buildDefaultClassifier() ;
		long detectTrainEnd = System.currentTimeMillis() ;

		if (saveModel) {
			File comparisonModel = new File("models/annotate/detect_" + fileNameChunk + ".model") ;
			ld.saveClassifier(comparisonModel) ;
		}

		if (saveArff) {
			File comparisonArff = new File("data/annotate/detect_" + fileNameChunk + ".arff") ;
			ld.saveTrainingData(comparisonArff) ;
		}

		ld.clearTrainingData() ;

		point.detect_trainTime = detectTrainEnd - detectTrainStart ;

		long detectTestStart = System.currentTimeMillis() ;
		Result<Integer> detectResult  = ld.test(testSet, SnippetLength.full, td, null) ;
		long detectTestEnd = System.currentTimeMillis() ;

		System.out.println("final: " + detectResult) ;

		point.detect_testTime = detectTestEnd - detectTestStart ;
		point.detect_p = detectResult.getPrecision() ;
		point.detect_r = detectResult.getRecall() ;
		point.detect_f = detectResult.getFMeasure() ;
		point.detect_labelsConsidered = ld.getLinksConsidered() ;


		return ld ;

	}





	private class AnnotationDataPoint { 

		public EnumSet<DataDependency> dependencies ;

		public String lang ;
		public boolean usingML;

		public long disambig_trainTime ;
		public long disambig_testTime ;

		public long detect_trainTime ;
		public long detect_testTime ;

		public double disambig_p ;
		public double disambig_r ;
		public double disambig_f ;

		public int disambig_sensesConsidered ;

		public double detect_p ;
		public double detect_r ;
		public double detect_f ;

		public int detect_labelsConsidered ;

		public long cacheTime ;
		public long cacheSpace ;

		public String toString() {

			StringBuffer sb = new StringBuffer() ;
			sb.append(getDependencyString(dependencies)) ;
			sb.append("\t") ;
			sb.append(lang) ;
			sb.append("\t") ;
			sb.append(usingML) ;
			sb.append("\t") ;
			sb.append(disambig_p) ;
			sb.append("\t") ;
			sb.append(disambig_r) ;
			sb.append("\t") ;
			sb.append(disambig_f) ;
			sb.append("\t") ;
			sb.append(disambig_trainTime) ;
			sb.append("\t") ;
			sb.append(disambig_testTime) ;
			sb.append("\t") ;
			sb.append(disambig_sensesConsidered) ;
			sb.append("\t") ;
			sb.append(detect_p) ;
			sb.append("\t") ;
			sb.append(detect_r) ;
			sb.append("\t") ;
			sb.append(detect_f) ;
			sb.append("\t") ;
			sb.append(detect_trainTime) ;
			sb.append("\t") ;
			sb.append(detect_testTime) ;
			sb.append("\t") ;
			sb.append(detect_labelsConsidered) ;
			sb.append("\t") ;
			sb.append(cacheTime) ;
			sb.append("\t") ;
			sb.append(cacheSpace) ;


			return sb.toString() ;
		}
	}

	public static String getFileNameChunk(String lang, EnumSet<DataDependency> dependencies) {

		StringBuffer sb = new StringBuffer() ;

		sb.append(lang) ;
		sb.append("_") ;

		if (dependencies.contains(DataDependency.pageLinksIn)) 
			sb.append("In") ;

		if (dependencies.contains(DataDependency.pageLinksOut)) 
			sb.append("Out") ;

		if (dependencies.contains(DataDependency.linkCounts)) 
			sb.append("Counts") ;

		return sb.toString() ;

	}

	public String getDependencyString(EnumSet<DataDependency> dependencies) {

		StringBuffer sb = new StringBuffer() ;
		if (dependencies.contains(DataDependency.pageLinksIn)) 
			sb.append(DataDependency.pageLinksIn + "+") ;

		if (dependencies.contains(DataDependency.pageLinksOut)) 
			sb.append(DataDependency.pageLinksOut + "+") ;

		if (dependencies.contains(DataDependency.linkCounts)) 
			sb.append(DataDependency.linkCounts + "+") ;

		sb.deleteCharAt(sb.length() -1) ;
		return sb.toString() ;
	}




}
