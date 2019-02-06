//package de.dualuse.glow.awt;
//
//import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL12.*;
//
//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferInt;
//import java.nio.ByteBuffer;
//import java.nio.IntBuffer;
//import java.util.Map;
//
//import de.dualuse.glow.TextureSource;
//
//
//class BufferedImageTextureSource implements TextureSource {
//	
//	public final BufferedImage source;
//	
//	public BufferedImageTextureSource(BufferedImage bi) {
//		this.source = bi;
//		
//		if (!checkFormatAndType())
//			throw new UnsupportedOperationException("BufferedImageTextureSource does not support type: "+source.getType());
//	}
//	
//	
//	private boolean checkFormatAndType() {
//		switch (source.getType()) {
//		case BufferedImage.TYPE_INT_ARGB:
//		case BufferedImage.TYPE_INT_RGB:
//		case BufferedImage.TYPE_BYTE_GRAY:
//		case BufferedImage.TYPE_3BYTE_BGR:
//		case BufferedImage.TYPE_USHORT_GRAY:
//		case BufferedImage.TYPE_USHORT_555_RGB:
//		case BufferedImage.TYPE_USHORT_565_RGB:
//			return true;
//		}
//		
//		return false;
//	}
//	
//
//	@Override public int getWidth() { return source.getWidth(); }
//	@Override public int getHeight() { return source.getHeight(); }
//	@Override public int getDepth() { 
//		switch (source.getType()) {
//		case BufferedImage.TYPE_INT_ARGB:
//			return 32;
//		case BufferedImage.TYPE_INT_RGB:
//		case BufferedImage.TYPE_3BYTE_BGR:
//			return 24;
//			
//		case BufferedImage.TYPE_BYTE_GRAY:
//			return 8;
//
//		case BufferedImage.TYPE_USHORT_GRAY:
//		case BufferedImage.TYPE_USHORT_555_RGB:
//		case BufferedImage.TYPE_USHORT_565_RGB:
//			return 16;
//		}
//		throw new IllegalArgumentException();
//	}
//
//
//	static final private Object[][] formats = { 
//		{ BufferedImage.TYPE_INT_RGB, new Integer[] { GL_BGRA, GL_UNSIGNED_BYTE } },
//		{ BufferedImage.TYPE_BYTE_GRAY, new Integer[] { GL_LUMINANCE, GL_UNSIGNED_BYTE }  }
//	};
//
//	static final public Map<Integer,Integer[]> formatMap = new ArrayMap<>(formats);
//	@Override public int getFormat() { return formatMap.get(source.getType())[0]; }
//	
//
//	@Override public int getType() { return formatMap.get(source.getType())[1]; }
//
//
//	//////////////
//	
//	@Override
//	public void grab(int x, int y, int width, int height, ByteBuffer to, int offset, int scan) {
//		switch (source.getType()) {
//		
//		case BufferedImage.TYPE_INT_RGB: 
//			int[] pixels = ((DataBufferInt)source.getData().getDataBuffer()).getData();
//			if (x==0 && y==0 && width == getWidth() && height == getHeight() && scan == source.getWidth())
//				((IntBuffer)to.asIntBuffer().position(offset)).put(pixels);
//			else 
//				; //line by line
//			break;
//			
//		
//		}
//	}
//	
//}