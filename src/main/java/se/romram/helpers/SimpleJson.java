package se.romram.helpers;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Created by micke on 2015-02-10.
 */
public class SimpleJson {
	Object json;
	Object currentNode;
	Stack<Object> parentNode = new Stack<>();

	public SimpleJson(String jsonAsString) {
        try {
            parse(jsonAsString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

	private SimpleJson(Map<String, Object> jsonMap) {
		this.json = jsonMap;
	}

	private SimpleJson(List<Object> jsonList) {
		this.json = jsonList;
	}

	public SimpleJson get(String name) {
		Object object = ((Map<String, Object>) json).get(name);
		if (object instanceof Map) {
			return new SimpleJson((Map<String, Object>) object);
		}
		if (object instanceof List) {
			return new SimpleJson((List<Object>) object);
		}
		return null;
	}

    public SimpleJson get(int index) {
        Object object = ((List<Object>) json).get(index);
        if (object instanceof Map) {
            return new SimpleJson((Map<String, Object>) object);
        }
        if (object instanceof List) {
            return new SimpleJson((List<Object>) object);
        }
        return new SimpleJson(object.toString());
    }

	private void parse(String jsonAsString) throws ParseException {
		String name = "";
		boolean value = true;
		StringTokenizer stringTokenizer = new StringTokenizer(jsonAsString, " ,{}[]:\n", true);
		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
//			System.out.print(token + " ==> ");
			switch (token.toLowerCase().trim()) {
				case "" : break;
				case "{" :
					currentNode = addNewNode(currentNode, name);
					value = false;
					break;
				case "}" :
					currentNode = parentNode.pop();
                    value = false;
					break;
				case "[" :
					currentNode = addNewListNode(currentNode, name);
                    value = true;
					break;
				case "]" :
                    currentNode = parentNode.pop();
                    value = false;
					break;
				case ":" :
					value = !value;
					break;
				case "," :
					break;
                case "\n" :
                    break;
				default :
					if (value) {
						value = addToCurrentNode(name, token);
					} else {
						name = trimChar(trimChar(token, '\"'), '\n');
					}
//				System.out.println(((Map<String, Object>)currentNode));
			}
		}
//		System.out.println(jsonMap);
	}

	private boolean addToCurrentNode(String name, String token) throws ParseException {
		if (currentNode instanceof Map) {
            Object value = naturalizeToken(token);
            ((Map<String, Object>) currentNode).put(name, value);
			System.out.println(name + "=" + token);
			return false;
		}
		if (currentNode instanceof List) {
            Object value = naturalizeToken(token);
            ((List<Object>)currentNode).add(value);
            return true;
		}
        if (currentNode == null) {
            Object value = naturalizeToken(token);
            json = value;
            currentNode = json;
        }
		return true;
	}

	private Object naturalizeToken(String token) throws ParseException {
        if (token.charAt(0)=='\"') {
            return trimChar(token, '\"');
		} else if ("false".equals(token.toLowerCase())) {
			return false;
		} else if ("null".equals(token.toLowerCase())) {
            return null;
        } else if ("true".equals(token.toLowerCase())) {
            return true;
        } else {
			Number number = NumberFormat.getInstance().parse(trimChar(trimChar(token, '\"'), '\n'));
			return number;
		}
	}

	private List<Object> addNewListNode(Object currentNode, String name) {
        if (currentNode == null) {
            json = new ArrayList<Object>();
            currentNode = json;
            parentNode.push(json);
            return ((List<Object>) currentNode);
        }
        List<Object> temp = new ArrayList<>();
        ((Map<String, Object>) currentNode).put(name, temp);
        parentNode.push(currentNode);
        currentNode = temp;
        return (List<Object>) currentNode;
    }

    private String trimChar(String string, char c) {
		string = string.charAt(0)==c ? string.substring(1) : string;
		string = string.charAt(string.length()-1)==c ? string.substring(0, string.length()-1) : string;
		return string;
	}

	private Map<String, Object> addNewNode(Object currentNode, String name) {
		if (currentNode == null) {
			json = new HashMap<>();
			currentNode = json;
			parentNode.push(json);
			return (Map<String, Object>) currentNode;
		}
		Map<String, Object> temp = new HashMap<>();
        if (currentNode instanceof Map) {
            ((Map<String, Object>) currentNode).put(name, temp);
        }
        if (currentNode instanceof List) {
            ((List<Object>) currentNode).add(temp);
        }
		parentNode.push(currentNode);
		currentNode = temp;
		return (Map<String, Object>) currentNode;
	}

    public Object toObject() {
        if (json instanceof Map || json instanceof List) {
            return this;
        }
        return json;
    }

    public String toString(int indent) {
        int level=0;
        StringBuffer buf = new StringBuffer();
        append(buf, json, level, indent);
        return buf.toString();
    }

    private void append(StringBuffer buf, Object object, int level, int indent) {
        if (object instanceof Map) {
            buf.append("{");
            boolean first = true;
            for (String key : ((Map<String, Object>) object).keySet()) {
                if (!first) {
                    buf.append(",");
                }
                first = false;
                buf.append("\n");
                appendIndent(buf, level+1, indent);
                buf.append("\"").append(key).append("\": ");
                append(buf, ((Map<String, Object>) object).get(key), level+1, indent);
            }
            buf.append("\n");
            appendIndent(buf, level, indent);
            buf.append("}");
        }
        if (object instanceof List) {
            buf.append("[");
            boolean first = true;
            for (Object element : ((List<Object>) object)) {
                if (!first) {
                    buf.append(",");
                }
                first = false;
                buf.append("\n");
                appendIndent(buf, level + 1, indent);
                append(buf, element, level+1, indent);
            }
            buf.append("\n");
            appendIndent(buf, level, indent);
            buf.append("]");
        }
        if (object instanceof String) {
            buf.append("\"").append(object).append("\"");
        }
        if (object == null || object instanceof Number || object instanceof Boolean) {
            buf.append(object);
        }
    }

    private void appendIndent(StringBuffer buf, int level, int indent) {
        buf.append("                                         ".substring(0, level*indent));
    }

    public static void main(String[] args) throws IOException {
        Path path = FileSystems.getDefault().getPath("src/test/resources/test.json");
        String json = new String(Files.readAllBytes(path));
        System.out.println(json);
		StopWatch stopWatch = new StopWatch().start();
		SimpleJson simpleJson = new SimpleJson(json);
		stopWatch.stop();
		System.out.println("Parsing took " + stopWatch.getTotalTime() + " ms.");
		System.out.println(simpleJson.toString(4));
		System.out.println(simpleJson.get("menu").get("popup").toString(4));
		System.out.println(simpleJson.get("menu").get("popup").get("menuitem").toString(4));
		System.out.println(simpleJson.toString(4));
	}

}
