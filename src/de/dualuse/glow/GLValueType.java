package de.dualuse.glow;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

enum GLValueType {
	UNSUPPORTED(-1,-1,-1,false),
	
	INT(GL_INT,1,4,false),
	INT_VEC2(GL_INT_VEC2,2,8,false),
	INT_VEC3(GL_INT_VEC3,3,12,false),
	INT_VEC4(GL_INT_VEC4,4,16,false),
	
	FLOAT(GL_FLOAT,1,4,true),
	FLOAT_VEC2(GL_FLOAT_VEC2,2,8,true),
	FLOAT_VEC3(GL_FLOAT_VEC3,3,12,true),
	FLOAT_VEC4(GL_FLOAT_VEC4,4,16,true),
	FLOAT_MAT2(GL_FLOAT_MAT2,4,16,true),
	FLOAT_MAT3(GL_FLOAT_MAT3,9,36,true),
	FLOAT_MAT4(GL_FLOAT_MAT4,16,64,true);
	
	public final int type;
	public final int dimension;
	public final int sizeOfType;
	
	public final boolean isFloatingPoint;
	
	private GLValueType(int type, int dimension, int sizeOfType, boolean isFloatintPoint) {
		this.type = type;
		this.dimension = dimension;
		this.sizeOfType = sizeOfType;
		this.isFloatingPoint = isFloatintPoint;
	}
	
	public static GLValueType forIdentifier(int typeCode) {
		for (GLValueType ut: GLValueType.values()) 
			if (ut.type == typeCode)
				return ut;
		
		return GLValueType.UNSUPPORTED;
	}
}