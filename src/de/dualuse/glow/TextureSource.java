package de.dualuse.glow;

import java.nio.ByteBuffer;

public interface TextureSource {
	///XXX probably also support that grab returns a new bytebuffer, such that the buffer where the data is written to is optional
	public void grab(int x, int y, int width, int height, ByteBuffer to, int offset, int bytesPerLine); // <- for the glTextureCall
	
//	public TextureSource subsection(int x, int y, int width, int height);
	
	
	public int getWidth();
	public int getHeight();
	public int getDepth();
	
	public int getFormat();
	public int getType();
	
//	default public void grabRGB(int x, int y, int width, int height, int[] argb, int offset, int scan) { grab(x,y,width,height,argb,offset,scan); /* remove alpha here*/ };
//	public void grabLuminance(int x, int y, int width, int height, byte[] lum, int offset, int scan);
//	public void grabLuminance(int x, int y, int width, int height, short[] Y, int offset, int scan);
//	public void grabLuminanceAlpha(int x, int y, int width, int height, short[] la, int offset, int scan);
}