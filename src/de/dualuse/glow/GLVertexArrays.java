package de.dualuse.glow;

import static java.lang.Math.*;
import static org.lwjgl.BufferUtils.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.ByteBuffer;

public class GLVertexArrays extends GLAPI implements VertexWriterTrait {
	static enum BasicType {
		VOID(0, 0),
		BYTE(1, GL_BYTE),
		SHORT(2, GL_SHORT),
		INT(4, GL_INT),
		FLOAT(4, GL_FLOAT),
		DOUBLE(8, GL_DOUBLE);
		
		final public int sizeOf;
		final public int glTypeCode;
		
		private BasicType(int sizeOf, int glTypeCode) { this.sizeOf = sizeOf; this.glTypeCode = glTypeCode; }
		boolean isOkWith(BasicType that) { return this==VOID || this==that; }
	}
	
	///////////////////////
	
	final public static double GROWTH = 1.5;
	
	private int length = 0;
	private int minCapacity = 32;
	
	private ByteBuffer buffers[] = new ByteBuffer[0];
	private GLAttribute attributes[] = new GLAttribute[0];
	private BasicType types[] = new BasicType[0];
	private Boolean normalize[] = new Boolean[0];
	private int counters[] = new int[0];
	
	private int plane = -1, planes = 0;
	private int mode = -1;
	int version = 0;
	
	
	@Override
	public VertexWriter attribute(GLAttribute a) {
		for (plane=0;plane<planes;plane++)
			if (attributes[plane] == a)
				return this;
		
		if (planes<attributes.length)
			attributes[planes++] = a;
		else {
			int targetCapacity = max(minCapacity, length);
			ByteBuffer buffer = createByteBuffer( targetCapacity * a.size * a.type.sizeOfType );
			
			buffers = join(buffers, buffer);
			attributes = join(attributes, a);
			types = join(types, BasicType.VOID);
			normalize = join(normalize, a.type.isFloatingPoint);
		}
		
		return this;
	}
	
	public GLVertexArrays capacity(int vertexCapacity) {
		minCapacity = vertexCapacity;
		return this;
	}
	
	public GLVertexArrays normalize(boolean doIt) {
		normalize[plane] = doIt;
		return this;
	}
	
	static private ByteBuffer ensureCapacity( ByteBuffer buf, int sizeAtLeast ) {
		if (buf.capacity()>sizeAtLeast)
			return buf;
		
		int actualSize = int.class.cast( sizeAtLeast * GROWTH );
		ByteBuffer newBuf = createByteBuffer( actualSize);
		
		buf.flip();
		newBuf.put( buf );
		return newBuf;
	}
	
	
	protected void check(GLValueType b, BasicType providedType) {
		if (attributes[plane].type!=b || 
				types[plane].isOkWith(providedType))
				throw new IllegalArgumentException();
	}
	
	protected ByteBuffer buffer(GLValueType attributeType, BasicType providedType) {
		check(attributeType,providedType);

		ensureCapacity(buffers[plane], max(minCapacity, buffers[plane].position()+attributeType.dimension * providedType.sizeOf) );
		
		counters[plane]++;
		types[plane] = providedType;
		return buffers[plane];
	}
	
	
	@Override public VertexWriter value(byte x) { buffer(GLValueType.INT, BasicType.BYTE).put(x); return this; }
	@Override public VertexWriter value(int x) { buffer(GLValueType.INT, BasicType.INT).putInt(x); return this; }
	@Override public VertexWriter value(short x) { buffer(GLValueType.INT, BasicType.SHORT).putShort(x); return this; }
	@Override public VertexWriter value(float x) { buffer(GLValueType.FLOAT, BasicType.FLOAT).putFloat(x); return this; }
	@Override public VertexWriter value(double x) { buffer(GLValueType.FLOAT, BasicType.DOUBLE).putDouble(x); return this; }

	@Override public VertexWriter vec2b(byte x, byte y) { buffer(GLValueType.INT_VEC2, BasicType.BYTE) .put(x).put(y); return this; }
	@Override public VertexWriter vec2i(int x, int y) { buffer(GLValueType.INT_VEC2, BasicType.INT) .putInt(x).putInt(y); return this; }
	@Override public VertexWriter vec2f(float x, float y) { buffer(GLValueType.FLOAT_VEC2, BasicType.FLOAT) .putFloat(x).putFloat(y); return this; }
	@Override public VertexWriter vec2d(double x, double y) { buffer(GLValueType.FLOAT_VEC2, BasicType.DOUBLE) .putDouble(x).putDouble(y); return this; }
	
	@Override public VertexWriter vec3b(byte x, byte y, byte z) { buffer(GLValueType.INT_VEC3, BasicType.BYTE) .put(x).put(y).put(z); return this; }
	@Override public VertexWriter vec3i(int x, int y, int z) { buffer(GLValueType.INT_VEC3, BasicType.INT) .putInt(x).putInt(y).putInt(z); return this; }	
	@Override public VertexWriter vec3f(float x, float y, float z) { buffer(GLValueType.FLOAT_VEC3, BasicType.FLOAT) .putFloat(x).putFloat(y).putFloat(z); return this; }
	@Override public VertexWriter vec3d(double x, double y, double z) { buffer(GLValueType.FLOAT_VEC3, BasicType.DOUBLE) .putDouble(x).putDouble(y).putDouble(z); return this; }
	
	@Override public VertexWriter vec4b(byte x, byte y, byte z, byte w) { buffer(GLValueType.INT_VEC4, BasicType.BYTE) .put(x).put(y).put(z).put(w); return this; }
	@Override public VertexWriter vec4i(int x, int y, int z, int w) { buffer(GLValueType.INT_VEC4, BasicType.INT) .putInt(x).putInt(y).putInt(z).putInt(w); return this; }
	@Override public VertexWriter vec4f(float x, float y, float z, float w) { buffer(GLValueType.FLOAT_VEC4, BasicType.FLOAT) .putFloat(x).putFloat(y).putFloat(z).putFloat(z); return this; }
	@Override public VertexWriter vec4d(double x, double y, double z, double w) { buffer(GLValueType.FLOAT_VEC4, BasicType.DOUBLE).putDouble(x).putDouble(y).putDouble(z).putDouble(w); return this; }
	
	
	public VertexWriter begin(int mode) {
		this.mode = mode;
		
		for (int i=0;i<planes;i++) {
			buffers[i].clear();
			attributes[i] = null;
			counters[i] = 0;
			types[i] = BasicType.VOID;
		}
		planes = 0;
		length = 0;
		
		return this;
	};
	
	
	protected int checkCompleteness() {
		int shortest = length;
		
		for (int i=0;i<planes;i++) {
			shortest = min(counters[i], shortest);
		}

		if (shortest<length)
			throw new IllegalStateException("Arrays not filled equally");
		
		return shortest;
	}
	
	
	@Override
	public void end() {
		version++;
		send();
		drawArrays();
	}
	
	public void send() {
		checkCompleteness();
		
		for (int i=0;i<planes;i++)
			buffers[i].position(0);
	}
	
	public void drawArrays() {
		drawArrays(0, checkCompleteness());
	}
	
	public void drawArrays(int first, int count) {
		for (int i=0;i<planes;i++) {
			glEnableVertexAttribArray(attributes[i].location);
			glVertexAttribPointer(attributes[i].location, attributes[i].type.dimension, types[i].glTypeCode, normalize[i], 0, buffers[i]);
		}
		
		glDrawArrays(mode, 0, count);
		
		for (int i=0;i<planes;i++)
			glDisableVertexAttribArray(attributes[i].location);
	}
	
	
//	public void send(GLVertexBufferObject vbo) {
//		send(); ///XXX VertexSource?
//	}
	
	///////////////////////////
	
	
	
	
	public static void main(String[] args) {
				
		String code = 
				"//höhö"
				+ "uniform mat4 modelviewprojection;"
				+ "attribute vec2 position;"
				+ "attribute vec2 texcoord;"
				+ "varying vec2 uv;"
				+ "void main() {"
				+ "	gl_Position = modelviewprojection*position;"
				+ " uv = texcoord;"
				+ "}";
		
		GLProgram p = new GLProgram(new GLShader(GLShader.Type.VERTEX_SHADER, code));
		
		GLAttribute position = p.getAttribute("position");
		GLAttribute texcoord = p.getAttribute("texcoord");
		
		
		GLVertexArrays va = new GLVertexArrays();
		
		va
		.begin(GL_QUADS)
			.attribute(position).vec2f(-1,-1).vec2f(+1,-1).vec2f(+1,+1).vec2f(-1,+1)
			.attribute(texcoord).vec2f( 0, 0).vec2f(+1, 0).vec2f(+1,+1).vec2f( 0,+1)
		.send();

		
		
	}
	
}
