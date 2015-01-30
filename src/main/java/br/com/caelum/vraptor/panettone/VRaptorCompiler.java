package br.com.caelum.vraptor.panettone;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VRaptorCompiler {

	public static final String VIEW_INTERFACES_OUTPUT = "target/view-interfaces";
	public static final String VIEW_OUTPUT = "target/view-classes";
	public static final String VIEW_INPUT = "src/main/views";
	
	private final PrintStream err = System.err;
	private final Compiler compiler;
	
	public VRaptorCompiler(List<String> imports) {
		this(new File("."), imports);
	}
	
	public VRaptorCompiler(File baseDir, List<String> imports) {
		File templates = new File(baseDir, VIEW_INPUT);
		File classes = new File(baseDir, VIEW_OUTPUT);
		File interfaces = new File(baseDir, VIEW_INTERFACES_OUTPUT);
		VRaptorCompilationListener listener = new VRaptorCompilationListener();
		this.compiler = new Compiler(templates, classes, interfaces, imports, listener);
	}
	
	public VRaptorCompiler(File baseDir) {
		this(baseDir, new ArrayList<>());
	}
	
	public void start() {
		compiler.startWatch();
	}
	
	public void stop() {
		compiler.stopWatch();
	}

	public void compileAll() {
		compileAndRetrieveErrors().stream()
			.map(Exception::getMessage).forEach(err::println);
	}

	public List<Exception> compileAndRetrieveErrors() {
		return compiler.compileAll();
	}
	
	public void clear() {
		compiler.clear();
	}
	
	public void removeJavaVersionOf(String path) {
		compiler.removeJavaVersionOf(path);
	}

	public Optional<Exception> compile(File file) {
		return compiler.compile(file);
	}

}
