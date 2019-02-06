package de.dualuse.glow;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>OpenGL Shader Code Box</h2>
 * <p />
 * 
 * <pre>
 * - encapsulates code, shader name and compilation state  
 * - loads code from String, File or URL
 * - tracks changes to code and lazily compiles 
 * </pre>
 * 
 * @author Philipp Holzschneider, Stefan Mader
 */
public class GLShader extends GLAPI {
	static public enum Type {
		VERTEX_SHADER(GL_VERTEX_SHADER),
		FRAGMENT_SHADER(GL_FRAGMENT_SHADER);
		
		final public int typename;
		private Type(int type) {
			this .typename = type;
		}
	}
	
	final static private int CUTOFF_LENGTH = 240;
	final static private String DEFAULT_STATUS = null;
	final static public int VERTEX_SHADER = GL_VERTEX_SHADER;
	final static public int FRAGMENT_SHADER = GL_FRAGMENT_SHADER;
	
	static int globalChangeCounter = 0;
	
	final int type;
	private String code;
	int changeCounter = 0;
	int compileCounter = 0;
	final AtomicInteger refCounter = new AtomicInteger(0); 

	private String status = DEFAULT_STATUS;

	
	final static public int UNITIALIZED = 0;
	int name = UNITIALIZED;
	
	public GLShader(int type, Reader code) throws IOException {
		this(type, loadFromReader(code));
	}
	
	public GLShader(int type, String code) {
		this(type);
		this.sendCode(code);
	}
	
	public GLShader(int type) {
		this.type = type;
	}

	
	public GLShader(GLShader.Type type, Reader code) throws IOException {
		this(type.typename, loadFromReader(code));
	}
	
	public GLShader(GLShader.Type type, String code) {
		this(type);
		this.sendCode(code);
	}
	
	public GLShader(GLShader.Type type) {
		this.type = type.typename;
	}

		
	
	public GLShader sendCode(Reader code) throws IOException {
		this.sendCode(loadFromReader(code));
		return this;
	}
	
	public GLShader sendCode(String code) {
		if (code.equals(this.code))
			return this;
		
		this.code = code;
		changeCounter++;
		globalChangeCounter++;
		return this;
	}
	
	public String getCode() {
		return code;
	}
	
	public boolean hasChanged() {
		return changeCounter != compileCounter;
	}
	
	public String getStatus() {
		if (name==UNITIALIZED || changeCounter!=compileCounter)
			throw new IllegalStateException();
		else
			return status;
	}
	
	///////////////////////////////////////////
	
	int compileShader() {
		if (name!=UNITIALIZED && changeCounter==compileCounter)
			return name;
		
		if (name!=UNITIALIZED)
			glDeleteShader(name);
		
		name = glCreateShader(type);
		glShaderSource(name, code);
		glCompileShader(name);

		if (glGetShaderi(name, GL_COMPILE_STATUS) != GL_TRUE) {
			System.err.println(this);
			System.err.println(status = glGetShaderInfoLog(name));
		}
		else 
			status = DEFAULT_STATUS;
		
		compileCounter = changeCounter;
		
		return name;
	}
	

	void deleteShader() {
		if (name!=UNITIALIZED)
			glDeleteShader(name);
		
		name = UNITIALIZED;
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		// warn if shader gets finalized and never disposed!
	}

	
	@Override
	public String toString() {
		final String typeString;
		switch (type) {
		case GL_FRAGMENT_SHADER: typeString = "GL_FRAGMENT_SHADER"; break;
		case GL_VERTEX_SHADER: typeString = "GL_VERTEX_SHADER"; break;
		default: typeString = "<UNKNOWN>";
		}
		
		final String truncatedCode = (code.length()>CUTOFF_LENGTH?code.substring(0,CUTOFF_LENGTH)+"...":code);
		final String codeString = truncatedCode.replace("\n", " ").replaceAll("\r", "").replaceAll("\\s+", " ").trim();
		final String statusString = status == DEFAULT_STATUS?"":", status:'"+status+"'";
		
		return "GLShader{ type:"+typeString+statusString+", code:'"+codeString+"' }";
	}
	
	////////////////////////////////////
	
	static private String loadFromReader(Reader isr) throws IOException {
		char[] buffer = new char[4*1024];
		StringBuilder sb = new StringBuilder(4*1024);
		for (int bytesRead=0;bytesRead!=-1;bytesRead=isr.read(buffer))
			sb.append(buffer,0,bytesRead);
		
		return sb.toString();
	}

	
}





