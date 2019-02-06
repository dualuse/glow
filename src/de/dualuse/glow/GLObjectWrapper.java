package de.dualuse.glow;

import static org.lwjgl.opengl.GL11.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

@SuppressWarnings("unchecked")
abstract class GLObjectWrapper extends GLAPI {
	int name = INVALID_NAME;
	final static int INVALID_NAME = -1;
	
	protected Queue<IntConsumer> updates = new ConcurrentLinkedQueue<>();
	
	abstract protected int generateObject();
	abstract protected void bindObject(int target, int name);
	abstract protected int getObjectBinding(int target);
	abstract protected void deleteObject(int name);
	
	protected<T extends GLObjectWrapper> T send(IntConsumer update) { updates.add(update); return (T)this; };
	
	protected void delete() {
		updates.clear();
		deleteObject(name);
	}
	
	protected boolean bind(int target) {
		if (name == INVALID_NAME)
			name = glGenTextures();
		
		bindObject(target, name);

		// first all updates, cuz Mipmap generierung is configured here before uploading, sicher-ist-sicher <- who cares!!!
		if (!updates.isEmpty())
			for (int updatesPending = updates.size(); updatesPending>0; updatesPending--)
				updates.poll().accept(target);
		
		return updates.size()==0; // up-to-date or not!
	}
	
	protected <T extends GLObjectWrapper> T update(int target) {
		if (name!=INVALID_NAME && !updates.isEmpty())
			return (T)this;

		int prv = getObjectBinding(target);
		bind(target);
		bindObject(target,prv);
		return (T)this;
	}
	
}
