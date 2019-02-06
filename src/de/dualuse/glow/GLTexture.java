package de.dualuse.glow;

import static java.lang.Math.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;


public class GLTexture extends GLObjectWrapper {

	@Override protected int generateObject() { return glGenTextures(); }
	@Override protected void bindObject(int target, int name) { glBindTexture(target, name); }
	@Override protected int getObjectBinding(int target) { return glGetInteger(bindingForTarget(target)); }
	@Override protected void deleteObject(int name) { glDeleteTextures(name); }
	
	private interface TextureUpdate extends IntConsumer {};
	public interface TextureParameter extends TextureUpdate {};
	public boolean bindTexture(int target) { return bind(target); }
	public void deleteTexture() { delete(); }
	
	
	private static int bindingForTarget(int target) {
		switch (target) {
		case GL_TEXTURE_1D: return GL_TEXTURE_BINDING_1D;
		case GL_TEXTURE_2D: return GL_TEXTURE_BINDING_2D;
		case GL_TEXTURE_3D: return GL_TEXTURE_BINDING_3D;
		default: 
			throw new IllegalArgumentException();
		}
	}
	
	///////////////// Constructors ///////////////////////////
	
	public GLTexture() { }
	
	public GLTexture(TextureSource src, TextureParameter... params) 
	{ this.sendTexImage2D(0, src).sendTexParameter(params); }
	
	public GLTexture(int internalformat, TextureSource src, TextureParameter...params) 
	{ this.sendTexImage2D(0, internalformat, src).sendTexParameter(params); }
	
	public GLTexture(int internalformat, int width, int height, TextureParameter... params) 
	{ this.sendTexImage2D(0, internalformat, width, height).sendTexParameter(params); }
	
//	public GLTexture(int level, int internalformat, TextureSource src) { sendTexImage2D(level, internalformat, src); }
	
	public GLTexture(int internalformat, int width, int height, TextureSource src, TextureParameter ... params) 
	{ this.sendTexImage2D(0, internalformat, width, height, src).sendTexParameter(params); }
	
	public GLTexture(int level, int internalformat, int width, int height, TextureSource src, TextureParameter ... params) 
	{ this.sendTexImage2D(level, internalformat, width, height, src).sendTexParameter(params); }
	
	

	/////////////////////////////////////////
	
	private FlowControl flowController = requested -> requested;
	
	public GLTexture setFlow(FlowControl flow) {
		flowController = flow;
		return this;
	}

	
	static int internalFormatForFormat( int format ) {
		switch (format) {
		case GL_RED_INTEGER: return GL_RED;
		
		case GL_RG_INTEGER: return GL_RG;
		
		case GL_BGR:
		case GL_RGB_INTEGER: 
		case GL_BGR_INTEGER: return GL_RGB;
			
		case GL_BGRA:
		case GL_RGBA_INTEGER:
		case GL_BGRA_INTEGER: return GL_RGBA;
		
		default: //GL_RED, GL_RG, GL_RGB, GL_BGR, GL_RGBA, GL_BGRA, GL_DEPTH_COMPONENT, GL_DEPTH_STENCIL
			return format; 
		}
	}
	
	
	static final private int INITIAL_TRANSFER_BUFFER_CAPACITY = 10*1024*1024; // 10MB
	
	static private ByteBuffer transferBuffer = ByteBuffer.allocateDirect(INITIAL_TRANSFER_BUFFER_CAPACITY).order(ByteOrder.nativeOrder()); 
	static private void ensureTransferBufferCapacity(int minimumTransferBufferCapacity) {
		transferBuffer.clear();
		if (transferBuffer.capacity()<minimumTransferBufferCapacity) //allocate a way bigger buffer in case its too small 
			transferBuffer = ByteBuffer.allocateDirect( minimumTransferBufferCapacity*3/2 ).order(ByteOrder.nativeOrder());
	}
	
	static private int pad(int value, int granularity) {
		return (value+granularity-1)/granularity*granularity;
	}
	
	/**
	 * enqueues an update to a tex image blalbalbal DOES NOT EXECUTE IT RIGHT AWAY!
	 * @param level
	 * @param internalFormat
	 * @param textureWidth
	 * @param textureHeight
	 * @param border
	 * @param src
	 */
	public GLTexture sendTexImage2D(int level, int internalformat, int textureWidth, int textureHeight, TextureSource src) {
		flowController.allocate(Double.POSITIVE_INFINITY); //warm up the flow controller
		updates.add( target -> texImage2D(target, level, internalformat, textureWidth, textureHeight, src) );
		return this;
	}

	public GLTexture sendTexImage2D(int level, int internalformat, int width, int height) {
		flowController.allocate(Double.POSITIVE_INFINITY); //warm up the flow controller
		updates.add( target -> glTexImage2D(target, level, internalformat, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0) );
		return this; 
	}

	public GLTexture sendTexImage2D(int level, int internalformat, TextureSource src) {
		return sendTexImage2D(level,internalformat, src.getWidth(), src.getHeight(), src);
	}

	public GLTexture sendTexImage2D(int level, TextureSource src) {
		return sendTexImage2D(level, internalFormatForFormat(src.getFormat()), src);
	}

	public GLTexture sendTexImage2D(TextureSource src) {
		if (src==null)
			return this;
			
		return sendTexImage2D(0, src);
	}
	
	
	
	public void texImage2D(int target, int level, int internalformat, int textureWidth, int textureHeight, TextureSource src) {
		//determine actual size of pixel area to upload (texImage2D calls may request a bigger size than the texture source provides) 
		int uploadWidth = min(textureWidth, src.getWidth());
		int uploadHeight = min(textureHeight, src.getHeight());
		
		//retrieve the currently set alignment value (i know, i know, never "get" state values, but its just an int! and we are calling expensive texUploads anyways)
		int alignment = glGetInteger(GL_UNPACK_ALIGNMENT); // or assume 4 / 16?
		
		int paddedBytesPerLine =  pad(src.getDepth()/8*uploadWidth, alignment); //compute the size of a line with alignment in mind (paddedBytesPerLine = pad(13,4) = 16)

		//compute the actual bufferSize needed to upload the portion of the source image with padded linesize in mind  
		int requestedBufferSize = paddedBytesPerLine * uploadHeight; 
		int allowedBufferSize = (int) flowController.allocate( requestedBufferSize ); //find out how many bytes the throttling system allows us to actually upload

		//in case this is (more than) enough to upload texture in one blow
		if (allowedBufferSize>=requestedBufferSize) synchronized (GLTexture.class) { //synchronize-out concurrent uses of the transferBuffer (wont happen most likely? -> use atomic-stuff instead?

			//ensure the transferbuffer's capacity
			ensureTransferBufferCapacity(requestedBufferSize);

			//then upload the smaller section
			src.grab(0, 0, uploadWidth, uploadHeight, (ByteBuffer)transferBuffer.clear(), 0, paddedBytesPerLine); //grab the upload region from the TextureSource 
			
			//if the uploaded texture image is smaller than the requested texture size
			if (uploadWidth<textureWidth || uploadHeight<textureHeight) { //initialize the texture in memory, then upload a subimage
				glTexImage2D(target, level, internalformat, uploadWidth, uploadHeight, 0, src.getFormat(), src.getType(), 0); //does not fill texture with zeros!
				glTexSubImage2D(target, level, 0, 0, uploadWidth, uploadHeight, src.getFormat(), src.getType(), (ByteBuffer)transferBuffer.clear() ); //then upload just that
			} else
				//initialize and upload the texture in one blow 
				glTexImage2D(target, level, internalformat, uploadWidth, uploadHeight, 0, src.getFormat(), src.getType(), (ByteBuffer)transferBuffer.clear() );
			
		} else if (allowedBufferSize>0) synchronized (GLTexture.class) { //if the upload is allowed in principle
			//pad the allowedBufferSize to the next line (just upload lines only, don't bother with individual pixels, c'mon!!) 
			int usedBufferSize = pad(allowedBufferSize, paddedBytesPerLine); 

			ensureTransferBufferCapacity(usedBufferSize);
			
			//compute and grab the lines to upload into the transferbuffer
			int linesToUpload = usedBufferSize/(uploadWidth*src.getDepth()/8);
			src.grab(0, 0, uploadWidth, linesToUpload, transferBuffer, 0, paddedBytesPerLine);
			
			//initialize the texture, then upload the section we're allowed to upload
			glTexImage2D(target, level, internalformat, uploadWidth, uploadHeight, 0, src.getFormat(), src.getType(), 0); //clear texture
			glTexSubImage2D(target, level, 0, 0, uploadWidth, linesToUpload, src.getFormat(), src.getType(), (ByteBuffer)transferBuffer.clear());

			//issue a new texture update to upload the rest if possible
			//XXX but this issues a new enqueued object for each upload part, maybe redo the mechanism to allow apply(..) to mention that it's not done yet and not get expunged from update list
			updates.add( trgt -> texSubImage2D(trgt, level, 0, linesToUpload, uploadWidth, uploadHeight-linesToUpload, src, 0, linesToUpload) );
		} else {
			//no permits at all? then initialize texture at least and schedule a texSubImage upload for next frame
			glTexImage2D(target, level, internalformat, uploadWidth, uploadHeight, 0, src.getFormat(), src.getType(), 0); //clear texture
			updates.add( trgt -> texSubImage2D(trgt, level, 0, 0, uploadWidth, uploadHeight, src, 0, 0) );
		}
	}
	
	
	//////////////
	
	public GLTexture sendTexSubImage2D(int level, int xoffset, int yoffset, int width, int height, TextureSource src) {
		flowController.allocate(Double.POSITIVE_INFINITY); //warm up the flow controller
		updates.add( target -> texSubImage2D(target, level, xoffset, yoffset, width, height, src,0, 0) );
		return this;
	}

	public GLTexture sendTexSubImage2D(int level, int xoffset, int yoffset, TextureSource src) { return sendTexSubImage2D(level,xoffset,yoffset,src.getWidth(),src.getHeight(),src); }
	public GLTexture sendTexSubImage2D(int level, TextureSource src) { return sendTexSubImage2D(level, 0, 0, src); }
	public GLTexture sendTexSubImage2D(TextureSource src) { return sendTexSubImage2D(0, src); }
	
	
	void texSubImage2D(int target, int level, int xoffset, int yoffset, int subTextureWidth, int subTextureHeight, TextureSource src, int skipX, int skipY ) {
		
		//determine actual size of pixel area to upload (texImage2D calls may request a bigger size than the texture source provides) 
		int uploadWidth = min(subTextureWidth, src.getWidth());
		int uploadHeight = min(subTextureHeight, src.getHeight());
		
		//retrieve the currently set alignment value (i know, i know, never "get" state values, but its just an int! and we are calling expensive texUploads anyways)
		int alignment = glGetInteger(GL_UNPACK_ALIGNMENT); // or assume 4 / 16?
		int paddedBytesPerLine =  pad(src.getDepth()/8*uploadWidth, alignment); //compute the size of a line with alignment in mind (paddedBytesPerLine = pad(13,4) = 16)

		//compute the actual bufferSize needed to upload the portion of the source image with padded linesize in mind  
		int requestedBufferSize = paddedBytesPerLine * uploadHeight; 
		int allowedBufferSize = (int) flowController.allocate( requestedBufferSize ); //find out how many bytes the throttling system allows us to actually upload

		
		if (allowedBufferSize>=requestedBufferSize) synchronized (GLTexture.class) { //synchronize-out concurrent uses of the transferBuffer (wont happen most likely? -> use atomic-stuff instead?
			//ensure the transferbuffer's capacity
			ensureTransferBufferCapacity(requestedBufferSize);

			//then upload the smaller section
			src.grab(skipX, skipY, uploadWidth, uploadHeight, (ByteBuffer)transferBuffer.clear(), 0, paddedBytesPerLine); //grab the upload region from the TextureSource 
			glTexSubImage2D(target, level, xoffset, yoffset, uploadWidth, uploadHeight, src.getFormat(), src.getType(), (ByteBuffer)transferBuffer.clear() ); //then upload just that
		} else if (allowedBufferSize>0) {
			//pad the allowedBufferSize to the next line (just upload lines only, don't bother with individual pixels, c'mon!!) 
			int usedBufferSize = pad(allowedBufferSize, paddedBytesPerLine); 

			ensureTransferBufferCapacity(usedBufferSize);
			
			//compute and grab the lines to upload into the transferbuffer
			int linesToUpload = usedBufferSize/(uploadWidth*src.getDepth()/8);
			src.grab(skipX, skipY, uploadWidth, linesToUpload, transferBuffer, 0, paddedBytesPerLine);
			
			glTexSubImage2D(target, level, xoffset, yoffset, uploadWidth, linesToUpload, src.getFormat(), src.getType(), (ByteBuffer)transferBuffer.clear());
			updates.add( trgt -> texSubImage2D(trgt, level, xoffset, yoffset+linesToUpload, uploadWidth, uploadHeight-linesToUpload, src, skipX, skipY+linesToUpload) );
		} else
			//no permits at all? then reschedule texSubImage Upload for next frame (brrr, there's a "new") 
			updates.add( trgt -> texSubImage2D(trgt, level, xoffset, yoffset, uploadWidth, uploadHeight, src, skipX, skipY) );

	}

	
	
	///////////////// Parameter ////////////////////////
	
	public GLTexture sendTexParameter(final int pname, final int param) { return send(target -> glTexParameteri(target, pname, param) ); }
	public GLTexture sendTexParameter(final int pname, final float param) { return send( target -> glTexParameterf(target, pname, param) ); }
	public GLTexture sendTexParameter(final int pname, final int... param) { return send( target -> glTexParameteriv(target, pname, param) ); }
	public GLTexture sendTexParameter(final int pname, final float... param) { return send( target -> glTexParameterfv(target, pname, param) ); }
	public GLTexture sendTexParameter(TextureParameter param) { return send( param ); }
	public GLTexture sendTexParameter(TextureParameter... params) {
		for (TextureUpdate param: params)
			updates.add(param);
		return this;
	}
	

	/////////////// 

	public GLTexture sendTexParameterMagFilter(MagFilter param) { return sendTexParameter(param); }
	public static enum MagFilter implements TextureParameter {
		NEAREST(GL_NEAREST),
		LINEAR(GL_LINEAR);
		
		final int param;
		private MagFilter(int param) { this.param = param; }
		public void accept(int target) { glTexParameteri(target, GL_TEXTURE_MAG_FILTER, param	); 	}
	}

	public GLTexture sendTexParameterMinFilter(MinFilter param) { return sendTexParameter(param); }
	public static enum MinFilter implements TextureParameter {
		NEAREST(GL_NEAREST),
		LINEAR(GL_LINEAR),
		LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST),
		LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR);
		
		final int param;
		private MinFilter(int param) { this.param = param; }
		public void accept(int target) { glTexParameteri(target, GL_TEXTURE_MIN_FILTER, param	); 	}
	}

	public GLTexture sendTexParameterWrap(Wrap param) { return sendTexParameter(param); }
	public GLTexture sendTexParameterWrap(WrapR param) { return sendTexParameter(param); }
	public GLTexture sendTexParameterWrap(WrapS param) { return sendTexParameter(param); }
	public GLTexture sendTexParameterWrap(WrapT param) { return sendTexParameter(param); }
	
	public static enum Wrap implements TextureParameter{
		REPEAT(GL_REPEAT),
		MIRROR(GL_MIRRORED_REPEAT),
		CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE);
		
		final int param;
		private Wrap(int param) { this.param = param; }
		public void accept(int target) {
			glTexParameteri(target, GL_TEXTURE_WRAP_R, param);
			glTexParameteri(target, GL_TEXTURE_WRAP_S, param);	
			glTexParameteri(target, GL_TEXTURE_WRAP_T, param);	
		}
	}

	public static enum WrapR implements TextureParameter {
		REPEAT(GL_REPEAT),
		MIRROR(GL_MIRRORED_REPEAT),
		CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE);
		
		final int param;
		private WrapR(int param) { this.param = param; }
		public void accept(int target) {
			glTexParameteri(target, GL_TEXTURE_WRAP_R, param);
		}
	}

	public static enum WrapS implements TextureParameter {
		REPEAT(GL_REPEAT),
		MIRROR(GL_MIRRORED_REPEAT),
		CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE);
		
		final int param;
		private WrapS(int param) { this.param = param; }
		public void accept(int target) {
			glTexParameteri(target, GL_TEXTURE_WRAP_S, param);
		}
	}

	public static enum WrapT implements TextureParameter {
		REPEAT(GL_REPEAT),
		MIRROR(GL_MIRRORED_REPEAT),
		CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE);
		
		final int param;
		private WrapT(int param) { this.param = param; }
		public void accept(int target) {
			glTexParameteri(target, GL_TEXTURE_WRAP_T, param);
		}
	}

	public GLTexture sendTexParameterGenerateMipmap(boolean param) { return sendTexParameter(param?GenerateMipMap.TRUE:GenerateMipMap.FALSE); }
	public GLTexture sendTexParameterGenerateMipmap(GenerateMipMap param) { return sendTexParameter(param); }
	public static enum GenerateMipMap implements TextureParameter {
		TRUE(GL_TRUE),
		FALSE(GL_FALSE);
		
		final int param;
		private GenerateMipMap(int param) { this.param = param; }
		@Override public void accept(int target) { glTexParameteri(target, GL_GENERATE_MIPMAP, param); }
	}
	
}
