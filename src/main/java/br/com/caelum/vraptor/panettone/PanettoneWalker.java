package br.com.caelum.vraptor.panettone;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.List;

import br.com.caelum.vraptor.panettone.parser.PanettoneParser;
import br.com.caelum.vraptor.panettone.parser.ast.ASTWalker;
import br.com.caelum.vraptor.panettone.parser.ast.CommentNode;
import br.com.caelum.vraptor.panettone.parser.ast.ExpressionNode;
import br.com.caelum.vraptor.panettone.parser.ast.HTMLNode;
import br.com.caelum.vraptor.panettone.parser.ast.InjectDeclarationNode;
import br.com.caelum.vraptor.panettone.parser.ast.Node;
import br.com.caelum.vraptor.panettone.parser.ast.PrintVariableNode;
import br.com.caelum.vraptor.panettone.parser.ast.ReusableVariableNode;
import br.com.caelum.vraptor.panettone.parser.ast.ScriptletNode;
import br.com.caelum.vraptor.panettone.parser.ast.ScriptletPrintNode;
import br.com.caelum.vraptor.panettone.parser.ast.VariableDeclarationNode;

public class PanettoneWalker implements ASTWalker {
	
	private final ELEvaluator el = new ELEvaluator();
	private final Variables variables = new Variables();
	private final StringBuilder injects = new StringBuilder();
	private final CodeBuilder code;
	private final CompilationListener[] listeners;
	private final String typeName;

	public PanettoneWalker(CodeBuilder code, String typeName, CompilationListener... listeners) {
		this.code = code;
		this.listeners = listeners;
		this.typeName = typeName;
	}
	@Override
	public void visitPrintVariable(PrintVariableNode node) {
		String value = node.getExpr();
		elValue(value, node.getBeginLine());
	}

	private void elValue(String value, int beginLine) {
		String expression = el.evaluate(value);
		code.append("write(" + expression +");");
		code.append("\n");
	}

	@Override
	public void visitVariableDeclaration(VariableDeclarationNode node) {
		variables.add(node.getType(), node.getName());
		
		if(node.getDefaultValue()!=null) {
			code.append(String.format("if(%s == null) %s = %s;\n", node.getName(), node.getName(), node.getDefaultValue()));
		}
	}

	@Override
	public void visitHTML(HTMLNode node) {
		linePrint(node.getHtml(), node.getBeginLine());
	}
	
	private void linePrint(String parts, int beginLine) {
		String[] allParts = parts.split("\n");
		int last = allParts.length - 1;
		for (int i = 0; i < allParts.length; i++) {
			String part = allParts[i];
			if(part.isEmpty()) continue;
			String slash = i == last ? "" : "\\n";
			code.append("write(\"" + escapeSlashesAndQuotes(part) + slash + "\");\n");
		}
	}

	private String escapeSlashesAndQuotes(String content) {
		return content.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void visitExpression(ExpressionNode node) {
		elValue(node.getExpr(), node.getBeginLine());
	}

	@Override
	public void visitScriptletPrint(ScriptletPrintNode node) {
		code.append("write(" + node.getExpr() + ");\n");
	}

	@Override
	public void visitScriptlet(ScriptletNode node) {
		code.append(node.getScriptlet());
		code.append("\n");
	}

	@Override
	public void visitInjectDeclaration(InjectDeclarationNode node) {
		injects.append("@javax.inject.Inject private " + node.getType() + " " + node.getName() + ";");
		injects.append("\n");
	}

	public String getJavaCode() {
		String prefix = getMethodSignature() + " {\n";
		String sufix = "}\n";
		
		List<String> variables = this.variables.asMethodDefinition();
		String extraMembers = stream(listeners)
			.map(cl -> cl.useParameters(variables, typeName))
			.collect(joining());
		
		String body = code.toString();
		return injects + prefix + body + sufix + extraMembers;
	}
	
	String getMethodSignature() {
		return "public void render(" + variables.asFullMethodDefinition() + ")";
	}

	@Override
	public void visitReusableVariable(ReusableVariableNode node) {
		String name = node.getName();
		code.append(format("Runnable %s = () -> {\n", name));
		String content = node.getContent();
		new PanettoneParser().parse(content).walk(this);
		code.append("};\n");
	}

	@Override
	public void visitComment(CommentNode node) {
		code.append("/*" + node.getComment() + "*/");
	}

	@Override
	public void visitBefore(Node node) {
		
	}

	@Override
	public void visitAfter(Node node) {
		
	}
	
	public Variables getVariables() {
		return variables;
	}
	
}