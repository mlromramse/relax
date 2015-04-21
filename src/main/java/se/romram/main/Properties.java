package se.romram.main;

import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by micke on 2015-01-26.
 */
public class Properties {
    public int port = 8080;
	public int threads = 10;
    public String path = ".";
	public String execute = null;
	public Map<String, String> argMap = new HashMap<>();
    public RelaxHandler handler;

    public Properties(String[] args) {
		handler = new DefaultFileHandler(path);
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
			if (arg.toLowerCase().startsWith("path=")) {
				path = getValue(arg);
				continue;
			}
            if (arg.toLowerCase().startsWith("port=")) {
                port = getIntValue(arg);
				continue;
            }
			if (arg.toLowerCase().startsWith("threads=")) {
				threads = getIntValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("execute=")) {
				execute = getValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("handlerclass=")) {
				String handlerClass = getValue(arg);
				try {
					ClassLoader classLoader = this.getClass().getClassLoader();
					Class cls = classLoader.loadClass(handlerClass);
					handler = (RelaxHandler) cls.newInstance();
					System.out.printf("Your master handler is %s.", handler.getClass().getSimpleName());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			//TODO Optimize
			String[] split = arg.split("=");
			argMap.put(split[0], split.length>1 ? split[1] : "true");
        }
    }

    private int getIntValue(String arg) {
        return Integer.parseInt(getValue(arg));
    }

    private String getValue(String arg) {
        String[] split = arg.split("=");
        if (split.length==1) {
            throw new RuntimeException(String.format("The argument %s is malformed. An argument should be entered as name=value.", arg));
        }
        return split[1];
    }


}
