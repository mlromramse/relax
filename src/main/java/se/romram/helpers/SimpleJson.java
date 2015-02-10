package se.romram.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by micke on 2015-02-10.
 */
public class SimpleJson {
	Map<String, Object> jsonMap;
	Object currentNode;
	Object parentNode;

	public SimpleJson(String jsonAsString) {
		parse(jsonAsString);
	}

	private void parse(String jsonAsString) {
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
					value = !value;
					break;
				case "}" :
					currentNode = parentNode;
					break;
				case "[" :
					
					break;
				case "]" :
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
							((Map<String, Object>)currentNode).put(name, trimChar(token, '\"'));
						}
						System.out.println(name + "=" + token);
						value = !value;
					} else {
						name = trimChar(token, '\"');
					}
				System.out.println(((Map<String, Object>)currentNode));
			}
		}
		System.out.println(jsonMap);
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
			parentNode = jsonMap;
			return (Map<String, Object>) currentNode;
		}
		Map<String, Object> temp = new HashMap<>();
		((Map<String, Object>)currentNode).put(name, temp);
		parentNode = currentNode;
		currentNode = temp;
		return (Map<String, Object>) currentNode;
	}

	public static void main(String[] args) {
		new SimpleJson("{\"name\"   : \"value\", \"int\": 25, \"element\": \"string\", \"object\": {\"name\": \"value\", \"element\": \"string\"}}");
	}

}
