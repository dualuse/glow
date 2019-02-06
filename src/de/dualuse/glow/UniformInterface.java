package de.dualuse.glow;

public interface UniformInterface {

	public void uniform1i(int x);
	public void uniform1f(float x);	
	public void uniform1iv(int... v);
	public void uniform1fv(float... v);
	
	public void uniform2i(int x, int y);
	public void uniform2f(float x, float y);
	public void uniform2iv(int... v);
	public void uniform2fv(float... vs);

	public void uniform3i(int x, int y, int z);
	public void uniform3iv(int... v);
	public void uniform3f(float x, float y, float z);
	public void uniform3fv(float... v);

	
	public void uniform4i(int x, int y, int z, int w); 
	public void uniform4iv(int... v);
	public void uniform4f(float x, float y, float z, float w);
	public void uniform4fv(float... v); 

	public void uniformMatrix2fv(boolean transpose, float... v);
	public void uniformMatrix3fv(boolean transpose, float... v);
	public void uniformMatrix4fv(boolean transpose, float... v);
	
	public void uniform1f(double x);
	public void uniform1fv(double... v);
	
	public void uniform2f(double x, double y);
	public void uniform2fv(double... v);
	
	public void uniform3f(double x, double y, double z);
	public void uniform3fv(double... v);
	
	public void uniform4f(double x, double y, double z, double w);
	public void uniform4fv(double... v);	
	
	public void uniformMatrix2fv(boolean transpose, double... v);
	public void uniformMatrix3fv(boolean transpose, double... v);
	public void uniformMatrix4fv(boolean transpose, double... v);
	
}
