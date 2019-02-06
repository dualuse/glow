package de.dualuse.glow;

public class GLAttribute {
	static final int UNINITIALIZED = -1;
	int location = UNINITIALIZED;
	
    public final String name;
    GLValueType type;
    int size;

    GLAttribute(String name) {
        this.name = name;
    }

    GLAttribute set(int location, int size, GLValueType type) {
        this.location = location;
        this.size = size;
        return this;
    }
}
