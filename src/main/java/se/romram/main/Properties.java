package se.romram.main;

import se.romram.handler.DefaultFileHandler;
import se.romram.handler.RelaxHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by micke on 2015-01-26.
 */
public class Properties {
    public int port = 8080;
	public int threads = 10;
	public int queue = 50;
    public String path = ".";
	public String method = null;
	public String url = "";
	public List<String> headerList = new ArrayList<>();
	public String execute = null;
	public Map<String, String> argMap = new HashMap<>();
    public List<String> handlerClassNameList = new ArrayList<>();

    public Properties(String[] args) {
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
			if (arg.toLowerCase().startsWith("get=")) {
				method = "GET";
				url = getValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("header=")) {
				headerList.add(getValue(arg));
				continue;
			}
			if (arg.toLowerCase().startsWith("threads=")) {
				threads = getIntValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("queue=")) {
				queue = getIntValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("execute=")) {
				execute = getValue(arg);
				continue;
			}
			if (arg.toLowerCase().startsWith("handlerclass=")) {
				String handlerClass = getValue(arg);
				handlerClassNameList.add(handlerClass);
				continue;
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
		String value = "";
		for (int index=1; index<split.length; index++) {
			value += index==1 ? split[index] : "=" + split[index];
		}
		if (value.charAt(0)=='"' && value.charAt(value.length()-1)=='"') {
			return value.substring(1, value.length()-2);
		}
        return value;
    }


}
