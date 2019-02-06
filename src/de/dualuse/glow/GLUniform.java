package de.dualuse.glow;

public abstract 
class GLUniform implements UniformTrait
{
	public final String name;
	
	abstract public void uniform1i(int x);
	abstract public void uniform1f(float x);	
	abstract public void uniform1iv(int... v);
	abstract public void uniform1fv(float... v);
	
	abstract public void uniform2i(int x, int y);
	abstract public void uniform2f(float x, float y);
	abstract public void uniform2iv(int... v);
	abstract public void uniform2fv(float... vs);

	abstract public void uniform3i(int x, int y, int z);
	abstract public void uniform3iv(int... v);
	abstract public void uniform3f(float x, float y, float z);
	abstract public void uniform3fv(float... v);

	
	abstract public void uniform4i(int x, int y, int z, int w); 
	abstract public void uniform4iv(int... v);
	abstract public void uniform4f(float x, float y, float z, float w);
	abstract public void uniform4fv(float... v); 

	abstract public void uniformMatrix2fv(boolean transpose, float... v);
	abstract public void uniformMatrix3fv(boolean transpose, float... v);
	abstract public void uniformMatrix4fv(boolean transpose, float... v);
	
	public void uniform1f(double x) { uniform1f((float)x); }
	public void uniform1fv(double... v) { uniform1fv(copyOf(v, v.length)); }
	
	public void uniform2f(double x, double y) { uniform2f((float)x,(float)y); }
	public void uniform2fv(double... v) { uniform2fv(copyOf(v, v.length)); }
	
	public void uniform3f(double x, double y, double z) { uniform3f((float)x,(float)y,(float)z); }
	public void uniform3fv(double... v) { uniform3fv(copyOf(v, v.length)); }
	
	public void uniform4f(double x, double y, double z, double w) { uniform4f((float)x,(float)y,(float)z,(float)w); }
	public void uniform4fv(double... v) { uniform4fv(copyOf(v, v.length)); }	

	
	public void uniformMatrix2fv(boolean transpose, double... v) { uniformMatrix2fv(transpose, copyOf(v,v.length)); }
	public void uniformMatrix3fv(boolean transpose, double... v) { uniformMatrix3fv(transpose, copyOf(v,v.length)); }
	public void uniformMatrix4fv(boolean transpose, double... v) { uniformMatrix4fv(transpose, copyOf(v,v.length)); }
	
	
	/////////////////////
	
	protected GLUniform(String name) {
		this.name = name;
	}
	
	
	//////////////
	
	public static float[] copyOf(double[] doubles, int newLength) {
		float[] floats = new float[newLength];
		for (int i=0,I=floats.length;i<I;i++)
			floats[i] = (float) doubles[i];
		
		return floats;
	}
	


	
//	static enum Type {
//		UNSUPPORTED(-1,-1,-1),
//		INT(GL_INT,1,4),
//		INT_VEC2(GL_INT_VEC2,2,8),
//		INT_VEC3(GL_INT_VEC3,3,12),
//		INT_VEC4(GL_INT_VEC4,4,16),
//		FLOAT(GL_FLOAT,1,4),
//		FLOAT_VEC2(GL_FLOAT_VEC2,2,8),
//		FLOAT_VEC3(GL_FLOAT_VEC3,3,12),
//		FLOAT_VEC4(GL_FLOAT_VEC4,4,16),
//		FLOAT_MAT2(GL_FLOAT_MAT2,4,16),
//		FLOAT_MAT3(GL_FLOAT_MAT3,9,36),
//		FLOAT_MAT4(GL_FLOAT_MAT4,16,64);
//		
//		public final int type;
//		public final int dimension;
//		public final int sizeOfType;
//		
//		private Type(int type, int dimension, int sizeOfType) {
//			this.type = type;
//			this.dimension = dimension;
//			this.sizeOfType = sizeOfType;
//		}
//		
//		public static Type get(int typeCode) {
//			for (Type ut: Type.values()) 
//				if (ut.type == typeCode)
//					return ut;
//			
//			return Type.UNSUPPORTED;
//		}
//	}
}
