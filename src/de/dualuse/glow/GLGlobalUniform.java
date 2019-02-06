package de.dualuse.glow;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * <h1> Global named storage for uniform values.</h1>
 * 
 * <pre>
 * - compatible to GLUniform interface
 * - stores uniform values in compact data array representation (created on first time, re-used after) 
 * - infers type and size from store call
 * - is thread-safe and may be set anytime from anywhere
 * - is evaluated lazy and values are set upon change only  
 * - is automatically resolved and fed into it's corresponding uniform as initialization value upon program usage
 * </pre>
 *   
 * 
 * @author Stefan Mader, Philipp Holzschneider
 *
 */

public class GLGlobalUniform extends GLUniform { //name suggestions so far: "GLGlobal", "GLConstant", "GLStatic"
	/*package*/ static int globalsChangeCounter = 1;
	/*package*/ static Map<String,GLGlobalUniform> globals = new HashMap<String,GLGlobalUniform>(); 

	public static synchronized GLGlobalUniform get(String name) {
		if (globals.containsKey(name))
			return globals.get(name);
		
		return new GLGlobalUniform(name);
	}
	
	public GLGlobalUniform(String name) {
		super(name);
		synchronized(GLGlobalUniform.class) {
			if (globals.put(name, this)!=null)
				throw new IllegalStateException("Global Constants need to be unique");
			
			globalsChangeCounter++;
		}
	}
	
	///////////////////////////////////
	
	void update(GLProgramUniform u) {
		if (u.type!=type && u.size!=size) {
			String message = "Incompatible Type or Size: "+this+" vs "+u;
			if (!u.lenient && u.active())
				throw new IllegalArgumentException(message);
			else
				if (u.spamCounter++>0) //XXX switch to Spam-Free Logging System
					System.err.println(message);
		}
		
		if (u.updateCounter!=valueChangeCounter) synchronized (this) {
			switch (type) { // only use glUniform vector variants to set uniforms -> way less cases
			case GL_FLOAT: u.uniform1fv(floats); break;
			case GL_FLOAT_VEC2: u.uniform2fv(floats); break;
			case GL_FLOAT_VEC3: u.uniform3fv(floats); break;
			case GL_FLOAT_VEC4: u.uniform4fv(floats); break;
			
			case GL_INT: u.uniform1iv(ints); break;
			case GL_INT_VEC2: u.uniform2iv(ints); break;
			case GL_INT_VEC3: u.uniform3iv(ints); break;
			case GL_INT_VEC4: u.uniform4iv(ints); break;
			
			case GL_FLOAT_MAT2: u.uniformMatrix2fv(false, floats); break;
			case GL_FLOAT_MAT3: u.uniformMatrix3fv(false, floats); break;
			case GL_FLOAT_MAT4: u.uniformMatrix4fv(false, floats); break;
			}
			
			u.updateCounter = valueChangeCounter;
		}
	}
	
	
	private int valueChangeCounter = 0;
	private float floats[] = new float[0]; //XXX replace this by some sort of a ValueHolder
	private int ints[] = new int[0];
	private int type, size;
	
	//XXX implement this
	public void push() {
	}
	
	public void pop() {
		
	}
	
	@Override
	public synchronized void uniform1f(float x) {
		if (floats.length!=1)
			floats = new float[] {x};
		else
			if (x==floats[0])
				return;
			else 
				floats[0]=x;

		valueChangeCounter++;
		type = GL_FLOAT;
		size = 1;
	}

	@Override
	public synchronized void uniform2f(float x, float y) {
		if (floats.length!=2)
			floats = new float[] {x,y};
		else
			if (x==floats[0] && y==floats[1])
				return;
			else {
				floats[0]=x;
				floats[1]=y;
			}

		valueChangeCounter++;
		type = GL_FLOAT_VEC2;
		size = 1;
	}

	@Override
	public synchronized void uniform3f(float x, float y, float z) {
		if (floats.length!=3)
			floats = new float[] {x,y,z};
		else
			if (x==floats[0] && y==floats[1] && z==floats[2])
				return;
			else {
				floats[0]=x;
				floats[1]=y;
				floats[2]=z;
			}
		
		valueChangeCounter++;
		type = GL_FLOAT_VEC3;
		size=1;
	}

	@Override
	public synchronized void uniform4f(float x, float y, float z, float w) {
		if (floats.length!=4)
			floats = new float[] {x,y,z,w};
		else
			if (x==floats[0] && y==floats[1] && z==floats[2] && w==floats[3])
				return;
			else {
				floats[0]=x;
				floats[1]=y;
				floats[2]=z;
				floats[3]=w;
			}
		
		valueChangeCounter++;
		type = GL_FLOAT_VEC4;
		size = 1;
	}

	@Override
	public synchronized void uniform1fv(float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT;
			size = v.length;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform2fv(float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_VEC2;
			size = v.length/2;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform3fv(float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_VEC3;
			size = v.length/3;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform4fv(float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_VEC4;
			size = v.length/4;
			valueChangeCounter++;
		}
	}
	

	@Override
	public synchronized void uniformMatrix2fv(boolean transpose, float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_MAT2;
			size = v.length/4;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniformMatrix3fv(boolean transpose, float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_MAT3;
			size = v.length/9;
			valueChangeCounter++;
		}
	}
	
	@Override
	public synchronized void uniformMatrix4fv(boolean transpose, float... v) {
		if (uniformfv(v)) {
			type = GL_FLOAT_MAT4;
			size = v.length/16;
			valueChangeCounter++;
		}
	}
	
	private synchronized boolean uniformfv(float... v) {
		if (v.length==floats.length)
			if (Arrays.equals(v, floats))
				return false;
			else
				System.arraycopy(v, 0, floats, 0, v.length);
		else
			floats = v.clone();
		
		return true;
	}
		
	
	
	@Override
	public synchronized void uniform1i(int x) {
		if (ints.length!=1)
			ints = new int[] {x};
		else
			if (x==ints[0])
				return;
			else 
				ints[0]=x;

		valueChangeCounter++;
		type = GL_INT;
		size = 1;
	}

	@Override
	public synchronized void uniform2i(int x, int y) {
		if (ints.length!=2)
			ints = new int[] {x,y};
		else
			if (x==ints[0] && y==ints[1])
				return;
			else {
				ints[0]=x;
				ints[1]=y;
			}

		valueChangeCounter++;
		type = GL_INT_VEC2;
		size = 1;
	}

	@Override
	public synchronized void uniform3i(int x, int y, int z) {
		if (ints.length!=3)
			ints = new int[] {x,y,z};
		else
			if (x==ints[0] && y==ints[1] && z==ints[2])
				return;
			else {
				ints[0]=x;
				ints[1]=y;
				ints[2]=z;
			}
		
		valueChangeCounter++;
		type = GL_INT_VEC3;
		size=1;
	}

	@Override
	public synchronized void uniform4i(int x, int y, int z, int w) {
		if (ints.length!=4)
			ints = new int[] {x,y,z,w};
		else
			if (x==ints[0] && y==ints[1] && z==ints[2] && w==ints[3])
				return;
			else {
				ints[0]=x;
				ints[1]=y;
				ints[2]=z;
				ints[3]=w;
			}
		
		valueChangeCounter++;
		type = GL_INT_VEC4;
		size = 1;
	}
	
	
	@Override
	public synchronized void uniform1iv(int... v) {
		if (uniformiv(v)) {
			type = GL_FLOAT;
			size = v.length;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform2iv(int... v) {
		if (uniformiv(v)) {
			type = GL_FLOAT_VEC2;
			size = v.length/2;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform3iv(int... v) {
		if (uniformiv(v)) {
			type = GL_FLOAT_VEC3;
			size = v.length/3;
			valueChangeCounter++;
		}
	}

	@Override
	public synchronized void uniform4iv(int... v) {
		if (uniformiv(v)) {
			type = GL_FLOAT_VEC4;
			size = v.length/4;
			valueChangeCounter++;
		}
	}

	private synchronized boolean uniformiv(int... v) {
		if (v.length==ints.length)
			if (Arrays.equals(v, ints))
				return false;
			else
				System.arraycopy(v, 0, ints, 0, v.length);
		else
			ints = v.clone();
		
		return true;
	}

	
	
	////////////////
	
	@Override
	public String toString() {
		return "GLGlobalUniform{ name:'"+name+"', type:"+GLValueType.forIdentifier(type)+(size>1?", size:"+size:"")+" }";
	}
}


