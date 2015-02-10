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
	Map<String, Object> jsonMap;
	Object currentNode;
	Stack<Object> parentNode = new Stack<>();

	public SimpleJson(String jsonAsString) {
        try {
            parse(jsonAsString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

	private void parse(String jsonAsString) throws ParseException {
		String name = "";
		boolean value = true;
		StringTokenizer stringTokenizer = new StringTokenizer(jsonAsString, " ,{}[]:", true);
		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			System.out.print(token + " ==> ");
			switch (token.trim()) {
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
					System.out.println("comma");
					break;
				default :
					if (value) {
						if (currentNode instanceof Map) {
                            if (token.charAt(0)=='\"') {
                                ((Map<String, Object>) currentNode).put(name, trimChar(trimChar(token, '\"'), '\n'));
                            } else {
                                Number number = NumberFormat.getInstance().parse(trimChar(trimChar(token, '\"'), '\n'));
                                ((Map<String, Object>) currentNode).put(name, number);
                            }
                            System.out.println(name + "=" + token);
                            value = !value;
						}
                        if (currentNode instanceof List) {
                            if (token.charAt(0)=='\"') {
                                ((List<Object>)currentNode).add(trimChar(token, '\"'));
                            } else {
                                Number number = NumberFormat.getInstance().parse(trimChar(trimChar(token, '\"'), '\n'));
                                ((List<Object>)currentNode).add(number);
                            }
                        }
					} else {
						name = trimChar(trimChar(token, '\"'), '\n');
					}
//				System.out.println(((Map<String, Object>)currentNode));
			}
		}
		System.out.println(jsonMap);
	}

    private List<Object> addNewListNode(Object currentNode, String name) {
        if (currentNode == null) {

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
			jsonMap = new HashMap<>();
			currentNode = jsonMap;
			parentNode.push(jsonMap);
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

    public String toString(int indent) {
        int level=0;
        StringBuffer buf = new StringBuffer();
        append(buf, jsonMap, level, indent);
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
        if (object instanceof Number) {
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
        System.out.println(new SimpleJson(json).toString(4));
    }

}
