package de.dualuse.glow;

import static java.lang.Math.min;

public interface FlowControl {
	double allocate(double requested);
	
	
	static public class Capped implements FlowControl {
		public double uploadLimit;
		
		public Capped(double uploadLimit) {
			this.uploadLimit = uploadLimit;
		}
		
		@Override
		public double allocate(double howmuch) {
			return min(uploadLimit,howmuch);
		}
	}
		
	static public class Rationed implements FlowControl {
		public final int nozzleRadius;
		private int permittedFlow = 0;
		
		public Rationed(int permittedFlow) {
			this.permittedFlow = permittedFlow;
			this.nozzleRadius = Integer.MAX_VALUE; 
		}

		public Rationed(int permittedFlow, int maxBiteSize) {
			this.permittedFlow = permittedFlow;
			this.nozzleRadius = maxBiteSize; 
		}
		
		public void permitFlow(int howmuch) {
			permittedFlow+=howmuch;
		}
		
		@Override
		public double allocate(double howmuch) {
			double allocated = min(permittedFlow,min(nozzleRadius,howmuch));
			permittedFlow-=allocated;
			return allocated;
		}
	}
	
	
	/**
	 * keep in mind, modern PCI x4 has several GB/s in upload speed, which we cant use fully for texture upload 
	 * so 1GB/s means 1000/60 = 16MB per frame (or 8MB per frame if you have Dima's 120Hz display). 
	 * If exceeded, there will be frame rate drops. and couple of MB/s is nothing.
	 * 
	 * @author holzschneider
	 *
	 */
	static class BandwidthLimited implements FlowControl {
		
		public int bandwidth, min, max;
		
		public BandwidthLimited(int bytesPerSecond, int minBiteSize, int maxBiteSize) {
			this.bandwidth = bytesPerSecond;
			this.min = minBiteSize;
			this.max = maxBiteSize;
		}
		
		public BandwidthLimited(int bytesPerSecond) {
			this.bandwidth = bytesPerSecond;
			this.min = 1; // ~50% of a 256x256 tile, sounds like a reasonble number
			this.max = Integer.MAX_VALUE;
		}
		
		double timestamp = 0;
		
//		public void begin(GLTexture e); 
//		public void end(GLTexture e);
		//or just consume all permits, for a new texture upload to prevent upload spikes
		
		@Override
		public double allocate(double howmuch) { 
			double now = System.nanoTime()/1e9;

			double permits = (now-timestamp)*bandwidth;
			
			double toBeSpent = min(permits, howmuch);
			
			if (toBeSpent<=min) {
				return 0;
			} else {
				double remaining = permits-toBeSpent;
				
				timestamp = now-remaining/bandwidth; //set timestamp, such that another call to allocate would yield the remaining permits

				if (toBeSpent>=max)
					return max;
				else
					return toBeSpent; 
			}
		}

	}
}


