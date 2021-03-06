package se.romram.helpers;

import se.romram.exceptions.UncheckedNotApplicableException;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Created by mikael.larsson@romram.se on 2015-02-10.
 *
 * This class is a very simple implementation of a JSON parser.
 * Its main goal is code size and to be lenient.
 * The drawbacks of this is that it can accept and produce JSON that
 * might not be a correct result.
 * The benefit is that it accepts both names of name-value pairs that
 * are surrounded with or without quotation marks.
 * Every value that is not quoted is treated as boolean, null or a number.
 */
public class SimpleJson {
    private static final String SPACES = "                                                               ";
	private NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
	private Object json;
	private Object currentNode;
	private Stack<Object> parentNode = new Stack<>();

	public SimpleJson(String jsonAsString) throws ParseException {
		numberFormat.setParseIntegerOnly(false);
        parse(jsonAsString);
    }

	private SimpleJson(Map<String, Object> jsonMap) {
		this.json = jsonMap;
	}

	private SimpleJson(List<Object> jsonList) {
		this.json = jsonList;
	}

    private SimpleJson(Object object) throws ParseException {
        if (object instanceof Map) {
            this.json = object;
        } else if (object instanceof List) {
            this.json = object;
        } else if (object instanceof String) {
            if (((String) object).charAt(0)=='{' || ((String) object).charAt(0)=='[') {
                parse(object.toString());
            } else {
                parse("\"" + object + "\"");
            }
        } else if (object == null) {
            this.json = null;
        } else {
            parse(object.toString());
        }

    }

	public SimpleJson get(String name) throws ParseException {
		if (!((Map<String, Object>) json).containsKey(name)) {
			throw new NoSuchElementException("The element with name " + name + " could not be found!");
		}
		Object object = ((Map<String, Object>) json).get(name);
        return new SimpleJson(object);
	}

    public SimpleJson get(String name, SimpleJson defaultValue) {
        try {
            return get(name);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

	public Boolean getBoolean(String name) throws ParseException {
		return (Boolean) get(name).toObject();
	}

	public Boolean getBoolean(String name, boolean defaultValue) {
		try {
			return getBoolean(name);
		} catch (ParseException e) {
			return defaultValue;
		} catch (NoSuchElementException e) {
			return defaultValue;
		}
	}

	public long getLong(String name) throws ParseException {
		return (Long) get(name).toObject();
	}

    public long getLong(String name, long defaultValue) {
        try {
            return getLong(name);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public String getString(String name) throws ParseException {
		return get(name).toObject().toString();
	}

    public String getString(String name, String defaultValue) {
        try {
            return getString(name);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public SimpleJson get(int index) throws ParseException {
		if (((List<Object>) json).size()<=index) {
			throw new NoSuchElementException("The list only contains " + ((List<Object>) json).size() + " elements!");
		}
        Object object = ((List<Object>) json).get(index);
        return new SimpleJson(object);
    }

    public Boolean getBoolean(int index) throws ParseException {
        return (Boolean) get(index).toObject();
    }

    public Boolean getBoolean(int index, boolean defaultValue) {
        try {
            return getBoolean(index);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public long getLong(int index) throws ParseException {
		return (Long) get(index).toObject();
	}

    public long getLong(int index, long defaultValue) {
        try {
            return getLong(index);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public String getString(int index) throws ParseException {
		return get(index).toObject().toString();
	}

    public String getString(int index, String defaultValue) {
        try {
            return getString(index);
        } catch (ParseException e) {
            return defaultValue;
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public int length() {
		if (json instanceof Map) {
			return ((Map<String, Object>) json).size();
		}
		if (json instanceof List) {
			return json !=null ? ((List<Object>) json).size() : 0;
		}
		return -1;
	}

    public SimpleJson put(String name, Object object) throws ParseException {
        if (! (json instanceof Map)) {
            throw new UncheckedNotApplicableException("Current SimpleJson node has to be of type Map.");
        }
        SimpleJson temp = new SimpleJson(object);
        ((Map<String, Object>) json).put(name, temp.toObject());
        return temp;
    }

    public SimpleJson add(Object object) throws ParseException {
        if (! (json instanceof List)) {
            throw new UncheckedNotApplicableException("Current SimpleJson node has to be of type List.");
        }
        SimpleJson temp = new SimpleJson(object);
        ((List<Object>) json).add(temp.toObject());
        return temp;
    }

    public Object toObject() {
        return json;
    }

    public String toString() {
        return toString(0);
    }

    public String toString(int indent) {
        int level=0;
        StringBuffer buf = new StringBuffer();
        append(buf, json, level, indent);
        return buf.toString();
    }

    private void parse(String jsonAsString) throws ParseException {
		String name = "";
		String value = "";
		boolean isValue = true;
		boolean isOpenString = false;
        int currentRow = 1;
		StringTokenizer stringTokenizer = new StringTokenizer(jsonAsString, "\"\\ ,{}[]:\n\t", true);
		while (stringTokenizer.hasMoreTokens()) {
			String token = stringTokenizer.nextToken();
			if (isOpenString && !"\n".equals(token) && !"\\".equals(token)) {
				value += token;
			}
			switch (token.toLowerCase()) {
				case " " :
					break;
				case "" : break;
				case "{" :
					if (isOpenString) break;
					currentNode = addNewNode(currentNode, name);
					isValue = false;
					break;
				case "}" :
					if (isOpenString) break;
					currentNode = parentNode.pop();
                    isValue = false;
					break;
				case "[" :
					if (isOpenString) break;
					currentNode = addNewListNode(currentNode, name);
                    isValue = true;
					break;
				case "]" :
					if (isOpenString) break;
                    currentNode = parentNode.pop();
                    isValue = false;
					break;
				case "\"" :
					isOpenString = !isOpenString;
					if (!isOpenString) {
						if (isValue) {
							isValue = addToCurrentNode(name, value);
						} else {
							name = trimChar(trimChar(value, '\"'), '\n');
						}
						value = "";
					} else {
						value += token;
					}
					break;
				case ":" :
					if (isOpenString) break;
					isValue = !isValue;
					break;
				case "," :
					break;
                case "\\" :
                    if (isOpenString) value += "\\";
                    break;
                case "\n" :
                    currentRow++;
                    if (isOpenString) value += "\\n";
                    break;
				case "\t" :
					break;
				default :
					if (isOpenString) break;
					value += token;
					if (isValue) {
                        try {
                            isValue = addToCurrentNode(name, value);
                        } catch (ParseException e) {
                            throw new ParseException("Unparseable NaN found at row " + currentRow, currentRow);
                        }
                    } else {
						name = trimChar(trimChar(value, '\"'), '\n');
					}
					value = "";
			}
		}
	}

	private boolean addToCurrentNode(String name, String token) throws ParseException {
		if (currentNode instanceof Map) {
            Object value = naturalizeToken(token);
            ((Map<String, Object>) currentNode).put(name, value);
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
            Object object = numberFormat.parseObject(trimChar(trimChar(token, '\"'), '\n'));
            return object;
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
        buf.append(SPACES.substring(0, level*indent));
    }


}
