package br.com.caelum.vraptor.panettone.parser.rule;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import br.com.caelum.vraptor.panettone.parser.SourceCode;
import br.com.caelum.vraptor.panettone.parser.TextChunk;
import br.com.caelum.vraptor.panettone.parser.ast.InjectDeclarationNode;

public class InjectDeclarationRuleTest {

	private InjectDeclarationRule rule;
	@Before
	public void setUp() {
		rule = new InjectDeclarationRule();
	}
	
	@Test
	public void shouldUnderstandVariablesWithFullTypeName() {
		SourceCode sc = new SourceCode(
				"bla bla"+
				" (@inject pac1.pac2.Class end) "+
				"ble ble"
		);
		
		List<TextChunk> chunks = rule.getChunks(sc);
		Assert.assertEquals("(@inject pac1.pac2.Class end)", chunks.get(0).getText());
	}
	
	
	@Test
	public void shouldUnderstandVariablesWithSimpleTypes() {
		SourceCode sc = new SourceCode(
				"bla bla"+
				" (@inject String nome) "+
				" (@inject String end_) "+
				"ble ble"
		);
		
		List<TextChunk> chunks = rule.getChunks(sc);
		Assert.assertEquals(2, chunks.size());
		Assert.assertEquals("(@inject String nome)", chunks.get(0).getText());
		Assert.assertEquals("(@inject String end_)", chunks.get(1).getText());
	}
	
	@Test
	public void shouldCreateNode() {
		
		InjectDeclarationNode node = (InjectDeclarationNode) rule.getNode(new TextChunk("(@inject a.b.C nome)"));
		
		Assert.assertEquals("a.b.C", node.getType());
		Assert.assertEquals("nome", node.getName());
		
	}
}
