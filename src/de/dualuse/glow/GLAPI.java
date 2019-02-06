package de.dualuse.glow;

import java.util.Arrays;

abstract class GLAPI {
	
	
	
	/////////////////////////////////////////////// 
	
	static <T> T[] join(T[] left, T right) {
		T[] joined = Arrays.copyOf(left, left.length+1);
		joined[joined.length-1] = right;
		return joined;				
	}
	
	static <T> T[] join(T[] left, T[] right) {
		T[] joined = Arrays.copyOf(left, left.length+right.length);
		
		for (int i=0,I=right.length;i<I;i++)
			joined[left.length+i] = right[i];
			
		return joined;				
	}

}
