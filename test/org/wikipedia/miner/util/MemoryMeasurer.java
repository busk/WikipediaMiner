package org.wikipedia.miner.util;

public class MemoryMeasurer {
	
	
	
		
	  private static long _sleepTime = 1000;
	  private static long _gcCycles = 5 ;

	  public static long getBytesUsed(){
	    putOutTheGarbage();
	    long totalMemory = Runtime.getRuntime().totalMemory();

	    putOutTheGarbage();
	    long freeMemory = Runtime.getRuntime().freeMemory();

	    return (totalMemory - freeMemory);
	  }

	  private static void putOutTheGarbage() {
		  
		for (int i=0 ; i<_gcCycles ; i++) {
			collectGarbage();
			collectGarbage();
		}
	  }

	  private static void collectGarbage() {
	    try {
	      System.gc();
	      Thread.sleep(_sleepTime);
	      System.runFinalization();
	      Thread.sleep(_sleepTime);
	    }
	    catch (InterruptedException ex){
	      ex.printStackTrace();
	    }
	  }

	
}
