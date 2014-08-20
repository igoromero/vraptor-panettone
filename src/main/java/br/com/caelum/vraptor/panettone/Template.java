package br.com.caelum.vraptor.panettone;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Template {

	private final String content;

	public Template(Reader reader) {
		this.content = CompiledTemplate.toString(reader);
	}
	public Template(String content) {
		this.content = content;
	}
	
	public String renderType() {
		int position = 0;
		StringBuilder builder = new StringBuilder();
		List<String> variables = new ArrayList<>();
		List<String> methods = new ArrayList<>();
		
		while (position < content.length()) {
			int next = nextDelimiter(content, position);
			if (next == -1) {
				String part = content.substring(position);
				linePrint(builder, part);
				position = content.length();
				continue;
			}
			if (position != next) {
				String part = content.substring(position, next);
				linePrint(builder, part);
			}

			// BUG: a random %> in the middle of some java code
			String startDelimiter = content.substring(next, next + 2);
			String endDelimiter = endDelimiterFor(startDelimiter);
			int finish = content.indexOf(endDelimiter, next + 1);
			if(finish==-1) {
				throw new InvalidTemplate(String.format("Template did not finish %s entry.", endDelimiter));
			}
			
			if(startDelimiter.equals("<%")) {
				String evaluation = content.substring(next + 2, finish);
				if(evaluation.startsWith("=")) {
					builder.append("out.write(" + evaluation.substring(1) + ");\n");
				} else if(evaluation.startsWith("--")){
					// comments
				} else if(evaluation.startsWith("@")){
					String variableFull = evaluation.substring(1);
					int equalsPosition = variableFull.indexOf("=");
					if(equalsPosition==-1) {
						variables.add(variableFull);
					} else {
						String definition = variableFull.substring(0, equalsPosition);
						variables.add(definition);
						
						String name = new LinkedList<String>(asList(definition.split("\\s+"))).getLast();
						String value = variableFull.substring(equalsPosition + 1);
						builder.append(format("if(%s == null) %s = %s;\n", name, name, value));
					}
				} else if(evaluation.startsWith("$")){
					methods.add(evaluation.substring(1) + "\n");
				} else {
					builder.append(evaluation + "\n");
				}
			} else {
				String evaluation = content.substring(next + 2, finish);
				builder.append("out.write(" + evaluateEL(evaluation) + ");\n");
			}
			position = finish + endDelimiter.length();
		}
		String parameters = variables.stream().collect(joining(","));
		String methodCode = methods.stream().collect(joining("\n\n"));
		String render = "public void render(" + parameters + ") {\n" + builder.toString() + "}\n";
		String result = methodCode + render;
		return result;
	}
	private String evaluateEL(String evaluation) {
		// TODO create a EL supplier. and make this optional.
		StringBuilder sb = new StringBuilder();
		// TODO can be faster
		for(int i=0;i<evaluation.length();i++) {
			if(evaluation.charAt(i)=='[') {
				sb.append(".get(");
				int end = evaluation.indexOf("]", i);
				if (end == -1) {
					throw new CompilationIOException("Not found a closing ] for your expression");
				}
				// TODO no support to nested ] so far
				sb.append(evaluation.substring(i + 1, end));
				sb.append(")");
				i = end;
			} else if(evaluation.charAt(i)=='.') {
				sb.append(".get");
				sb.append(Character.toUpperCase(evaluation.charAt(i+1)));
				i++;
				int nextDelimiter = getNextDelimiter(evaluation, i);
				if(nextDelimiter==-1) {
					sb.append(evaluation.substring(i+1));
					sb.append("()");
					i = evaluation.length();
				} else {
					sb.append(evaluation.substring(i+1, nextDelimiter));
					sb.append("()");
					i = nextDelimiter-1;
				}
			} else {
				sb.append(evaluation.charAt(i));
			}
		}
		return sb.toString().replace("'", "\"");
	}
	private int getNextDelimiter(String evaluation, int i) {
		int nextDelimiter = evaluation.indexOf(".", i);
		if(nextDelimiter == -1) nextDelimiter = evaluation.indexOf("[", i);
		return nextDelimiter;
	}
	private String endDelimiterFor(String delimiter) {
		if(delimiter.equals("${")) return "}";
		return "%>";
	}
	private int nextDelimiter(String content, int position) {
		for(int current = position; current < content.length() - 1; current++) {
			String nextTwo = content.substring(current, current+2);
			if(nextTwo.equals("${") || nextTwo.equals("<%"))
				return current;
		}
		return -1;
	}
	private void linePrint(StringBuilder builder, String parts) {
		String[] allParts = parts.split("\n");
		for(int i=0;i<allParts.length;i++) {
			String part = allParts[i].trim();
			if(part.isEmpty()) continue;
			String slash = i == allParts.length-1 ? "" : "\\n";
			builder.append("out.write(\"" + escapeQuotes(part) + slash + "\");\n");
		}
	}

	private String escapeQuotes(String content) {
		return content.replace("\"", "\\\"");
	}

	public class InvalidTemplate extends RuntimeException {

		private static final long serialVersionUID = 3508082279487674463L;

		public InvalidTemplate(String message) {
			super(message);
		}
		
	}

}
