module org.wikipedia.miner.db.struct {

  class DbPage {
     ustring Title;
     int Type;
     int depth;
  }
  
  class DbSenseForLabel {
  	int Id ;
  	long LinkOccCount ;
  	long LinkDocCount ;
  	boolean FromTitle ;
  	boolean FromRedirect ;
  }
  
  class DbLabel {
  	long LinkOccCount ;
  	long LinkDocCount ;
  	long TextOccCount ;
  	long TextDocCount ;
  
  	vector<DbSenseForLabel> Senses ;
  }
  
  class DbLabelForPage {
  	ustring Text ;
  	long LinkOccCount ;
  	long LinkDocCount ;
  	boolean FromTitle ;
  	boolean FromRedirect ;
  	boolean IsPrimary ;
  }
  
  class DbLabelForPageList {
    vector<DbLabelForPage> Labels ;
  }
  
  class DbPageLinkCounts {
  	int TotalLinksIn ;
  	int DistinctLinksIn ;
  	int TotalLinksOut ;
  	int DistinctLinksOut ;
  }
  
  class DbIntList {
  	vector<int> Values ;
  }
  
  class DbIntPair {
  	int ValueA ;
  	int ValueB ;  
  }
  
  class DbSentenceSplitList {
  	vector<int> SentenceSplits ;
  }
  
  class DbLinkLocation {
  	int LinkId ;
  	vector<int> SentenceIndexes ;
  }
  
  class DbLinkLocationList {
  	vector<DbLinkLocation> LinkLocations ;
  }
  
  class DbTranslations {
  	map<ustring,ustring> TranslationsByLangCode ;
  }
}


module org.wikipedia.miner.extraction.struct {
  
  class ExSenseForLabel {
  	long LinkOccCount ;
  	long LinkDocCount ;
  	boolean FromTitle ;
  	boolean FromRedirect ;
  }
  
  class ExLabel {
  	long LinkOccCount ;
  	long LinkDocCount ;
  	long TextOccCount ;
  	long TextDocCount ;
  	map<int,ExSenseForLabel> SensesById ;
  }
  
  class ExLinkKey {
  	int Id ;
  	boolean IsOut ;
  }  
}