package de.dualuse.glow;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;


public class GLFramebufferObject extends GLObjectWrapper {	
	public GLFramebufferObject() { }
	
	public GLFramebufferObject sendFramebufferTexture2D(int attachment, int textureTarget, GLTexture texture, int level) {
		return send ( target -> glFramebufferTexture2D(target, attachment, textureTarget, texture.update(textureTarget).name, level) );
	}
	
	public GLFramebufferObject sendFramebufferTexture2D(int attachment, int textureTarget, GLTexture texture) {
		return sendFramebufferTexture2D(attachment, textureTarget, texture, 0);
	}
	
	public GLFramebufferObject sendFramebufferRenderbuffer(int attachment, GLRenderbuffer buffer) {
		return send( target -> glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, buffer.update(GL_RENDERBUFFER).name) );
	}
	
	@Override protected int generateObject() { return glGenFramebuffers(); }
	@Override protected void bindObject(int target, int name) { glBindFramebuffer(target, name); }
	@Override protected int getObjectBinding(int target) { return glGetInteger(GL_FRAMEBUFFER_BINDING); }
	@Override protected void deleteObject(int name) { glDeleteFramebuffers(name); }
	
	////////////////////
	
	public boolean bindFramebuffer() {
		//TODO check and sysout completenes
		return super.bind(GL_FRAMEBUFFER);
	}
}