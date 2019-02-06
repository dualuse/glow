package de.dualuse.glow;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;

class GLRenderbuffer extends GLObjectWrapper {
	
	public GLRenderbuffer() { }
	
	//XXX also add named/enumed types to this 
	public GLRenderbuffer sendRenderbufferStorage( int internalFormat, int width, int height ) {
		return send ( target ->  glRenderbufferStorage(target, internalFormat, width, height) );
	}

	public GLRenderbuffer sendRenderbufferStorageMultisample( int samples, int internalFormat, int width, int height ) {
		return send ( target -> glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, internalFormat, width, height) );
	}
	
	@Override protected int generateObject() { return glGenRenderbuffers(); }
	@Override protected void bindObject(int target, int name) { glBindRenderbuffer(target, name); }
	@Override protected int getObjectBinding(int target) { return glGetInteger(GL_RENDERBUFFER_BINDING); }
	@Override protected void deleteObject(int name) { glDeleteRenderbuffers(name); }
}