package de.dualuse.glow.swt;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

import java.nio.ByteBuffer;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import de.dualuse.glow.TextureSource;

public class ImageDataTextureSource implements TextureSource {
	final public ImageData source;
	
	/**
	 * Wraps a TextureSource around ImageData. Assumes ImageData source to be constant 
	 * @param source
	 */
	public ImageDataTextureSource(ImageData source) {
		this.source = source;
		
		getFormat(); //throws exception if ImageData is not supported
	}
	
	static boolean isGrayScalePalette(RGB[] colors) {
		for (int i=0;i<colors.length;i++)
			if (i!=colors[i].blue || i!=colors[i].green || i!=colors[i].red )
				return false;
		
		return true;
	}
	
	
	@Override
	public void grab(int offsetX, int offsetY, int width, int height, ByteBuffer to, int offset, int bytesPerScanLine) {
		/// XXX Range Check here!
		switch (getFormat()) {
		case GL_LUMINANCE: 
		case GL_BGR:
		case GL_RGB:
			to.position(offset);

			for (int y=offsetY,Y=y+height, s= source.bytesPerLine, o=offsetY*s+offsetX,S=bytesPerScanLine,O=offset;y<Y;y++,o+=s)
				to.put(source.data, o, source.bytesPerLine).position(O+=S); //use source.bytesPerLine instead of width*3, as there might be padding
			break;
		
		case GL_RGBA: 
		case GL_BGRA: 
			to.position(offset); //advance to starting offset
			for (int	y=offsetY, //from offsetY
					Y=y+height,//to offsetY+height lines
					p=offsetY*width+offsetX, //starting offset for the first pixel's alpha value (note that it does not use bytesPerLine, but width as scanline length  
					s=source.bytesPerLine, //number of bytes to skip in source offset index 'o' for jumping to the next line
					o=offsetY*s+offsetX, //starting offset for the first color in the first pixel 
					S=bytesPerScanLine, //number of bytes to skip in the destination offset index 'O' for jumping to the next line
					O=offset; //destination offset index variable
					y<Y; //loop condition
					y++, //advance loop counter
					p+=width, //advance starting offset for alpha values by one line
					o+=s, //advance starting offset for colors by one line
					to.position(O+=S) //advance starting offset for destination writing by one line 
				)
				for (int x=offsetX,X=x+width,o_=o,p_=p;x<X;x++) { //iterate over one line, copy source offset index variables for color and alpha 
					to.put(source.data[o_++]); //put red and advance source offset index
					to.put(source.data[o_++]); //put green and advance source offset index
					to.put(source.data[o_++]); //put blue and advance source offset index
					to.put(source.alphaData[p_++]); //put alpha and advance alpha offset index
				}
			
		}
	}

	@Override public int getWidth() { return source.width; }
	@Override public int getHeight() { return source.height; }
	@Override public int getDepth() { return source.depth+(source.depth==24&&source.alphaData!=null?8:0); }
	
	
	@Override public int getFormat() { 
		PaletteData pal = source.palette;
		if (pal.blueMask==0xFF0000 && pal.blueShift==-16 &&
			pal.greenMask==0xFF00 && pal.greenShift==-8 &&
			pal.redMask==0xFF && pal.redShift==0 &&
			source.depth==24 && pal.isDirect && 
			source.maskData == null && source.transparentPixel == -1)
				return source.alphaData==null? GL_BGR:GL_BGRA;
		else 
		if (pal.blueMask==0xFF && pal.blueShift==0 &&
			pal.greenMask==0xFF00 && pal.greenShift==-8 &&
			pal.redMask==0xFF0000 && pal.redShift==-16 &&
			source.depth==24 && pal.isDirect && 
			source.maskData == null && source.transparentPixel == -1)
				return source.alphaData==null? GL_RGB:GL_RGBA;
		else 
		if (!pal.isDirect && 
			 pal.colors!=null && 
			 pal.colors.length==256 && 
			 isGrayScalePalette(pal.colors)) 
				return GL_LUMINANCE;
		else
			throw new IllegalArgumentException();
	}
	
	@Override public int getType() { return GL_UNSIGNED_BYTE; }

	
	
	/*
	public static void main(String[] args) {
		Object[][] transparencyTypeLabelArray = {
				{ SWT.TRANSPARENCY_NONE, "TRANSPARENCY_NONE" },
				{ SWT.TRANSPARENCY_ALPHA, "TRANSPARENCY_ALPHA" },
				{ SWT.TRANSPARENCY_MASK, "TRANSPARENCY_MASK" },
				{ SWT.TRANSPARENCY_PIXEL, "TRANSPARENCY_PIXEL" },
		};
		Map<Integer,String> transparencyTypeLabel = new ArrayMap<>(transparencyTypeLabelArray); 	

		Object[][] typeLabelArray = {
				{ SWT.IMAGE_BMP, "IMAGE_BMP" },
				{ SWT.IMAGE_BMP_RLE, "IMAGE_BMP_RLE" },
				{ SWT.IMAGE_GIF, "IMAGE_GIF" },
				{ SWT.IMAGE_ICO, "IMAGE_ICO" },
				{ SWT.IMAGE_JPEG, "IMAGE_JPEG" },
				{ SWT.IMAGE_PNG, "IMAGE_PNG" },
				{ SWT.IMAGE_TIFF, "IMAGE_TIFF" },
		};
		Map<Integer,String> typeLabel = new ArrayMap<>(typeLabelArray); 	
		
//		ImageData id = new ImageData("/Users/holzschneider/Downloads/131954-151255-i_rc copy.png");
		ImageData id = new ImageData("/Users/holzschneider/Projects/Autonomos/tripviewer/test/xyz/autonomos/trip/oddnumbered-width-24bit-noalpha.png");
		System.out.println(id.data.length +" = dataLength");

		System.out.println(id.width+" x "+id.height);
		System.out.println(id.bytesPerLine+" = bytesPerLine");
		System.out.println(id.data.length);
//		System.out.println(id.alphaData.length);
		System.out.println(id.scanlinePad+" = scanlinePad vs "+id.width);
		System.out.println(id.type+" = "+typeLabel.get(id.type));
		System.out.println(id.transparentPixel +" = TransparentPixel");
		System.out.println(id.alpha+" = alpha");
		System.out.println(id.alphaData+" = alphaData");
		System.out.println(id.getTransparencyType()+" = "+transparencyTypeLabel.get(id.getTransparencyType()));
		System.out.println(id.depth+" = depth");
		System.out.println(id.palette.isDirect);
		System.out.println(Integer.toBinaryString(id.palette.redMask));
		System.out.println(id.palette.redShift);
		System.out.println(Integer.toBinaryString(id.palette.greenMask));
		System.out.println(id.palette.greenShift);
		System.out.println(Integer.toBinaryString(id.palette.blueMask));
		System.out.println(id.palette.blueShift);
	
		
		
	
//		System.out.println( id.palette.colors.length );
//		
//		
//		if (id.palette.colors!=null)
//			for (int i=0;i<id.palette.colors.length;i++)
//				System.out.println(toString(id.palette.colors[i]));
//		
	}
	
	static public String toString(RGB c) {
		return ("#"+Integer.toHexString(c.red)+Integer.toHexString(c.green)+Integer.toHexString(c.blue)).toUpperCase();
	}
	 */
}
