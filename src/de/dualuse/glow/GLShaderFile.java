package de.dualuse.glow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Timer;
import java.util.TimerTask;

public class GLShaderFile extends GLShader {
	public static final long DEFAULT_PERIOD = 1000;
	
	public static final Timer watchTimer = new Timer(true);
	
	TimerTask watchTask = null;
	
	public GLShaderFile(int type, File sourceFile) throws FileNotFoundException, IOException {
		this(type,sourceFile, DEFAULT_PERIOD);
	}
	
	public GLShaderFile(int type, File sourceFile, long period) throws FileNotFoundException, IOException {
		super(type);
		sendCode(new FileReader(sourceFile));
		
		watchTask = new TimerTask() {
			long prevModified = sourceFile.lastModified();
			
			@Override
			public void run() {
				if (!sourceFile.exists())
					cancel();
				else
					if (sourceFile.lastModified()!=prevModified) try {
						sendCode(new FileReader(sourceFile));
						prevModified = sourceFile.lastModified();
					} catch (IOException ioe) {
						System.out.println(ioe);
					}
			}
		};
		
		watchTimer.schedule(watchTask, period, period);
	}
	
	@Override
	public GLShader sendCode(Reader code) throws IOException {
		if (watchTask!=null)
			watchTask.cancel();
		
		super.sendCode(code);
		
		return this;
	}
	
	@Override
	public GLShader sendCode(String code) {
		if (watchTask!=null)
			watchTask.cancel();
		
		super.sendCode(code);
		
		return this;
	}
}
