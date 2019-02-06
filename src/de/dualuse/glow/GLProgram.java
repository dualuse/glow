package de.dualuse.glow;


import static java.util.Arrays.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;



public class GLProgram extends GLAPI {
	public final static GLProgramUniform[] UNIFORM_ARRAY = new GLProgramUniform[0];
	public final static String DEFAULT_STATUS = null;
	
	public final static GLProgram DEFAULT = new GLProgram(0);

	private final GLShader[] shaders;
	private final int[] versions;
	
	private int globalShaderChangeCounter = 0;
	private int globalGlobalsChangeCounter = 0;
	
	////////////
	
	private GLProgramUniform[] active = new GLProgramUniform[0];
	private GLProgramUniform[] inactive = new GLProgramUniform[0];
	private GLProgramUniform[] updated = new GLProgramUniform[0];
	private int uniformResolutionCounter = 0, uniformDeclarationCounter = 0; 
	
	
	private GLAttribute[] attributes = new GLAttribute[0];
	private int attributeResolutionCounter = 0, attributeDeclarationCounter = 0; 
	
	private String status = DEFAULT_STATUS;
	
	final static int UNINITIALIZED = -1;
	public int program = UNINITIALIZED ;
	
	private GLProgram(int program) {
		this.program = program;
		this.shaders = new GLShader[0];
		this.versions = new int[0];
	}

	public GLProgram(GLShader... toBeAttached) {
		shaders = toBeAttached.clone();
		versions = new int[shaders.length];
	}


	/**
	 * Deletes the opengl programm resource and sets this GLProgram object to UNINITIALIZED state
	 */
	public void deleteProgram() {
		glDeleteProgram(program);
		program = UNINITIALIZED;
		
		for (GLShader shader: shaders)
			if (shader.refCounter.decrementAndGet()==0)
				shader.deleteShader();
		
		Arrays.fill(versions, 0);
	}
	
	public void useProgram() {
		if (program == UNINITIALIZED) {
			program = glCreateProgram();
			for (GLShader shader: shaders)
				shader.refCounter.incrementAndGet();
		}

		boolean programHasChanged = false;
		
		//if there are shaders and some shader codes changed globally, assumes shader changes are rare 
		if (shaders.length>0 && GLShader.globalChangeCounter!=this.globalShaderChangeCounter) { 
			//check for shader versions that indicate that they changed
			for (int i=0,I=shaders.length;i<I;i++) {
				//check if changed and remember it
				programHasChanged |= shaders[i].changeCounter!=versions[i];
				
				//update program's own shader version counter it is going to be handled
				versions[i] = shaders[i].changeCounter; 
			}
			
			//if at least one has changed 
			if (programHasChanged) {
				compileAndLinkProgram();
				
				for (GLProgramUniform u: join(active,inactive))
					u.spamCounter = 0;
			}
		}
		
		if (programHasChanged || uniformResolutionCounter!=uniformDeclarationCounter)
			resolveUniformDeclarations();
		
		if (programHasChanged || attributeResolutionCounter!=attributeDeclarationCounter)
			resolveAttributeDeclarations();
		
		if (programHasChanged || GLGlobalUniform.globalsChangeCounter!=globalGlobalsChangeCounter)
			resolveGloballyUpdatedUniforms();

		glUseProgram(program);
		
		applyGlobalUpdateValues();

	}
	
	
	private void compileAndLinkProgram() {
		//recompile them all
		for (GLShader s: shaders)
			glAttachShader(program, s.compileShader());

		//link the program
		glLinkProgram(program);
		
		//detach shaders
		for (GLShader s: shaders)
			glDetachShader(program, s.name);

		// check and report link status
		if (glGetProgrami(program, GL_LINK_STATUS)!=GL_TRUE) {
			System.err.println(this);
			System.err.println(status = glGetProgramInfoLog(program));
		}
		else
			status = DEFAULT_STATUS;
	}
	
	
	private void resolveUniformDeclarations() {
		//initialize with all (previously active) and inactive Uniforms, that will be resolved and activated
		//ArrayCollection<>
		ArrayList<GLProgramUniform> declared = new ArrayList<>(asList(join(active,inactive)));
		ArrayList<GLProgramUniform> resolved = new ArrayList<>(); //collects resolved and activated uniforms
		
		int len = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
		u: for (int i=0;i<len;i++) //scan every active uniform
			try (MemoryStack frame = stackPush()) {
				//query name, size & type and resolve it's uniform memory location 
				IntBuffer size = frame.ints(0), type = frame.ints(0);
				String name = glGetActiveUniform(program, i, size, type);
				int location = glGetUniformLocation(program, name);
				
				//scan through declared uniforms 
				for (int j=0,J=declared.size();j<J;j++)
					if (declared.get(j).name.equals(name)) { //and pick matching uniform
						//activate it, remove it from 'declared' add it to 'resolved'
						resolved.add( declared.remove(j--).activate(location, size.get(), type.get()) );
						continue u; //continue with next uniform
					}
				
				//create a new resolved/activated uniform
				resolved.add( new GLProgramUniform(name).activate(location, size.get(), type.get()) );
			}
		
		//deactivate all remaining unresolved but declared uniforms 
		for (int j=0,J=declared.size();j<J;j++)
			declared.get(j).deactivate();
		
		//fill arrays of inactive and active uniforms
		inactive = declared.toArray(new GLProgramUniform[declared.size()]);
		active = resolved.toArray(new GLProgramUniform[resolved.size()]);
		
		uniformResolutionCounter = uniformDeclarationCounter;
	}

	//XXX depending of active or inactive state for GLAttributes, the GLVertexArrays could ignore the specified data
	private void resolveAttributeDeclarations() {
		//initialize with all (previously active) and inactive Uniforms, that will be resolved and activated
		ArrayList<GLAttribute> active = new ArrayList<>(asList(attributes));

		int len = glGetProgrami(program, GL_ACTIVE_ATTRIBUTES);
		u: for (int i=0;i<len;i++) //scan every active uniform
			try (MemoryStack frame = stackPush()) {
				//query name, size & type and resolve it's uniform memory location
				IntBuffer size = frame.ints(0), type = frame.ints(0);
				String name = glGetActiveAttrib(program, i, size, type);
				int location = glGetAttribLocation(program, name);
				
				//scan through declared uniforms
				for (int j=0,J=active.size();j<J;j++)
					if (active.get(j).name.equals(name)) { //and pick matching uniform
						//activate it, remove it from 'declared' add it to 'resolved'
						active.get(j).set(location, size.get(), GLValueType.forIdentifier( type.get() ) );
						continue u; //continue with next uniform
					}

				//create a new resolved/activated uniform
				active.add( new GLAttribute(name).set(location, size.get(), GLValueType.forIdentifier( type.get() )) );
			}
		
		attributes = active.toArray(new GLAttribute[active.size()]);
	}

	private void resolveGloballyUpdatedUniforms() {
		ArrayList<GLProgramUniform> updated = new ArrayList<GLProgramUniform>(Arrays.asList(active));
		
		for (GLProgramUniform u: active) 
			if (GLGlobalUniform.globals.containsKey(u.name))
				u.updater = GLGlobalUniform.globals.get(u.name); // attach it
			else
				updated.remove(u);
		
		this.updated = updated.toArray(new GLProgramUniform[updated.size()]);
		this.globalGlobalsChangeCounter = GLGlobalUniform.globalsChangeCounter;
	}
	
	private void applyGlobalUpdateValues() {
		for (GLProgramUniform u: updated)
			u.update();
	}
	
	////////////////////
	public synchronized GLProgramUniform getUniform(String name) {
		// lookup uniform instance in already active or inactive uniforms
		for (GLProgramUniform u: join(active,inactive))
			if (name.equals(u.name))
				return u;
		
		// if this uniform has not been added yet, create one, deactivated
		return registerUniform( new GLProgramUniform(name) );
	}
	
	synchronized GLProgramUniform registerUniform(GLProgramUniform u) {
		inactive = join(inactive, u.deactivate());
		uniformDeclarationCounter ++;
		return u;
	}
	

	public synchronized GLAttribute getAttribute(String name) {
		for (int i = 0, I = attributes.length; i < I; i++)
			if (name.equals(attributes[i].name))
				return attributes[i];
		
		return registerAttribute(new GLAttribute(name));
	}
	
	synchronized GLAttribute registerAttribute(GLAttribute a) {
		attributes = join(attributes, a);
		attributeDeclarationCounter ++;
		return a;		
	}
	

	@Override
	public String toString() {
		final String statusString = (status!=DEFAULT_STATUS?"status:'\"+status+\"', ":"");
		final String inactiveString = (inactive.length>0?", inactive:"+Arrays.toString(inactive):"");
		return "GLProgram{ "
				+statusString
				+"active:"+Arrays.toString(active)
				+inactiveString+", "
				+"shaders:"+Arrays.toString(shaders)
				+" }";
	}
	
	
}
