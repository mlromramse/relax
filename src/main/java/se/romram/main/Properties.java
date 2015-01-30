package se.romram.main;

import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxHandler;

/**
 * Created by micke on 2015-01-26.
 */
public class Properties {
    int port = 8080;
	int threads = 10;
    String path = ".";
    RelaxHandler handler;

    public Properties(String[] args) {
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
			if (arg.toLowerCase().startsWith("path=")) {
				path = getValue(arg);
			}
            if (arg.toLowerCase().startsWith("port=")) {
                port = getIntValue(arg);
            }
			if (arg.toLowerCase().startsWith("threads=")) {
				threads = getIntValue(arg);
			}
        }
        handler = new DefaultFileHandler(path);
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
