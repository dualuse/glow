package de.dualuse.glow;

interface VertexWriter {
	public VertexWriter value(byte x);
	public VertexWriter value(short x);
	public VertexWriter value(int x);
	public VertexWriter value(float x);
	public VertexWriter value(double x);
	
	public VertexWriter vec2b(byte x, byte y);
	public VertexWriter vec3b(byte x, byte y, byte z);
	public VertexWriter vec4b(byte x, byte y, byte z, byte w);
	
	public VertexWriter vec2i(int x, int y);	
	public VertexWriter vec3i(int x, int y, int z);
	public VertexWriter vec4i(int x, int y, int z, int w);
	
	public VertexWriter vec2f(float x, float y);
	public VertexWriter vec3f(float x, float y, float z);
	public VertexWriter vec4f(float x, float y, float z, float w);

	public VertexWriter vec2d(double x, double y);
	public VertexWriter vec3d(double x, double y, double z);
	public VertexWriter vec4d(double x, double y, double z, double w);
	
	public VertexWriter attribute(GLAttribute a);
	
	public void send();
	public void end();
	
}