package de.dualuse.glow;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * 
 * <h2>Uniform-to-Program Adapter</h2>
 * 
 * @author Philipp Holzschneider, Stefan Mader
 *
 */
class GLProgramUniform extends GLUniform {
	private static final String VALUETYPE_VARIABLE="$valuetype", UNIFORMTYPE_VARIABLE="$uniformtype";
	private static final String WRONG_NUMBER_OF_ELEMENTS = "Wrong number of elements";
	private static final String INCOMPATIBLE_TYPE_OR_SIZE = "Incompatible Type or Size. Cannot apply GL_"+VALUETYPE_VARIABLE+" to "+UNIFORMTYPE_VARIABLE;

	private static final int INVALID = -1;
	
	int location = INVALID;
	int type = INVALID;
	int size = INVALID;
	
	boolean lenient = false;
	int spamCounter = 0;
	
	GLGlobalUniform updater = null;
	int updateCounter=0;
	
	
	GLProgramUniform(String name) {
		super(name);
		deactivate();
	};
	
	public GLProgramUniform(String name, GLProgram program) {
		super(name);
		program.registerUniform(this);
	}
	
	GLProgramUniform activate(int location, int size, int type) {
		switch(type) {
			// patching the gl_sampler to be accessible with gluniform1i, should be refactored..
			case GL_SAMPLER_1D:
			case GL_SAMPLER_2D:
			case GL_SAMPLER_3D:
				type = GL_INT; break;
		}
		this.location = location;
		this.type = type;
		this.size = size;
		
		return this;
	}
	
	GLProgramUniform deactivate() {
		this.location = INVALID;
		this.type = INVALID;
		this.size = INVALID;
		
		return this;
	}
	
	boolean active() {
		return location != INVALID;
	}
	
	boolean update() {
		if (updater==null)
			return false;
		
		updater.update(this);
		
		return true;
	}
	
	
	//////////////////////////////////////
	///// floats
	
	private boolean check(int type, int size, int excessElements) {
		if (excessElements!=0)
			if (!lenient && active())
				throw new IllegalArgumentException(WRONG_NUMBER_OF_ELEMENTS);
			else {
				if (spamCounter++==0)
					System.err.println(WRONG_NUMBER_OF_ELEMENTS);
				
				return false;
			}
		
		return check(type, size);
	}
	
	private boolean check(int type, int size) {
		if (this.type!=type || this.size!=size) {
			String message = INCOMPATIBLE_TYPE_OR_SIZE
					.replace(UNIFORMTYPE_VARIABLE, this.toString())
					.replace(VALUETYPE_VARIABLE, GLValueType.forIdentifier(type)+(size>1?" times "+size:"") );
			
			if (!lenient && active())
				throw new IllegalArgumentException(message); // specify more
			else {
				if (spamCounter++==0) //XXX switch to Spam-Free Logging System
					System.err.println(message);
				
				return false;
			}
		}
		
		updateCounter--; //remember changes made by uniform calls, such that updates from GLGlobal get applied
		return true;
	}
	
	public void uniform1f(float x) { if (check(GL_FLOAT, 1)) glUniform1f(location, x); }
	public void uniform2f(float x, float y) { if (check(GL_FLOAT_VEC2, 1)) glUniform2f(location, x, y); }
	public void uniform3f(float x, float y, float z) { if (check(GL_FLOAT_VEC3, 1)) glUniform3f(location, x, y, z); }
	public void uniform4f(float x, float y, float z, float w) { if (check(GL_FLOAT_VEC4, 1)) glUniform4f(location, x, y, z, w); }
	
	public void uniform1fv(float... v) { if (check(GL_FLOAT, v.length)) glUniform1fv(location, v); };
	public void uniform2fv(float... v) { if (check(GL_FLOAT_VEC2, v.length/2)) glUniform2fv(location, v); };
	public void uniform3fv(float... v) { if (check(GL_FLOAT_VEC3, v.length/3)) glUniform3fv(location, v); };
	public void uniform4fv(float... v) { if (check(GL_FLOAT_VEC4, v.length/4)) glUniform4fv(location, v); };

	///// integers
	
	public void uniform1i(int x) { if (check(GL_INT,1)) glUniform1i(location, x); }
	public void uniform2i(int x, int y) { if (check(GL_INT_VEC2,1)) glUniform2i(location, x, y); }
	public void uniform3i(int x, int y, int z) { if (check(GL_INT_VEC3,1)) glUniform3i(location, x, y, z); }
	public void uniform4i(int x, int y, int z, int w) { if (check(GL_INT_VEC4,1)) glUniform4i(location, x, y, z, w); }
	
	public void uniform1iv(int... v) { if (check(GL_INT,v.length)) glUniform1iv(location, v); };
	public void uniform2iv(int... v) { if (check(GL_INT_VEC2,v.length/2,v.length%2)) glUniform2iv(location, v); };
	public void uniform3iv(int... v) { if (check(GL_INT_VEC3,v.length/3,v.length%3)) glUniform3iv(location, v); };
	public void uniform4iv(int... v) { if (check(GL_INT_VEC4,v.length/4,v.length%4)) glUniform4iv(location, v); };
	
	
	///// matrices
	
	public void uniformMatrix2fv(boolean transpose, float... v) { if (check(GL_FLOAT_MAT2,v.length/4,v.length%4)) glUniformMatrix2fv(location, transpose, v); }
	public void uniformMatrix3fv(boolean transpose, float... v) { if (check(GL_FLOAT_MAT3,v.length/9,v.length%9)) glUniformMatrix3fv(location, transpose, v); }
	public void uniformMatrix4fv(boolean transpose, float... v) { if (check(GL_FLOAT_MAT4,v.length/16,v.length%16)) glUniformMatrix4fv(location, transpose, v); }
	
//	public void uniformMatrix3fv(Matrix3d m) {
//		if (check(GL_FLOAT_MAT3,1)) {
//			
//			// extract matrix in column major and hope that this allocation is escape-analyzed away 
//			float[] values = {	(float) m.m00,(float) m.m10,(float) m.m20,
//								(float) m.m01,(float) m.m11,(float) m.m21,
//								(float) m.m02,(float) m.m12,(float) m.m22		};
//			glUniformMatrix3fv(location, false, values);
//		}
//	}
//	
//	public void uniformMatrix3fv(Matrix3d... ms) {
//		if (check(GL_FLOAT_MAT3,ms.length))
//			try (MemoryStack frame = stackPush()) {
//				FloatBuffer floats = frame.mallocFloat(ms.length*9);
//				
//				for (Matrix3d m: ms)
//					floats
//					.put((float)m.m00).put((float)m.m10).put((float)m.m20)
//					.put((float)m.m01).put((float)m.m11).put((float)m.m21)
//					.put((float)m.m02).put((float)m.m12).put((float)m.m22);
//				
//				glUniformMatrix3fv(location, false, floats);
//			}
//	}
//	
//	public void uniformMatrix4fv(Matrix4d m) {
//		if (check(GL_FLOAT_MAT4,1)) {
//			// extract matrix in column major and hope that this allocation is escape-analyzed away 
//			float[] values = {	(float) m.m00,(float) m.m10,(float) m.m20,(float) m.m30,
//								(float) m.m01,(float) m.m11,(float) m.m21,(float) m.m31,
//								(float) m.m02,(float) m.m12,(float) m.m22,(float) m.m32,
//								(float) m.m03,(float) m.m13,(float) m.m23,(float) m.m33	};
//			
//			glUniformMatrix4fv(location, false, values);
//		}
//	}
//	
//	public void uniformMatrix4fv(Matrix4d... ms) { 
//		if (check(GL_FLOAT_MAT4,ms.length))
//		try (MemoryStack frame = stackPush()) {
//			FloatBuffer floats = frame.mallocFloat(ms.length*9);
//			
//			for (Matrix4d m: ms)
//				floats
//				.put((float)m.m00).put((float)m.m10).put((float)m.m20).put((float)m.m30)
//				.put((float)m.m01).put((float)m.m11).put((float)m.m21).put((float)m.m31)
//				.put((float)m.m02).put((float)m.m12).put((float)m.m22).put((float)m.m32)
//				.put((float)m.m03).put((float)m.m13).put((float)m.m23).put((float)m.m33);
//			
//			glUniformMatrix4fv(location, false, floats);
//		}
//	}
	
	
	//////////////
	
	
	@Override
	public String toString() {
		final String bindingString = "location:"+location;
		final String updaterString = (updater==null?"":", global:"+updater);
		final String typeString = "type:GL_"+GLValueType.forIdentifier(type)+(size>1?", size:"+size:"") + "(" + Integer.toString(type) + ")";
		return "GLUniform{ name:'"+name+"'"+(location==INVALID?"":", "+typeString+", "+bindingString)+updaterString+" }";
	}
	
}

