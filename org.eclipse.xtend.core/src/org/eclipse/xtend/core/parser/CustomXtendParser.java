/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.parser;

import java.io.Reader;
import java.util.stream.Collectors;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.xtend.core.parser.antlr.XtendParser;
import org.eclipse.xtend.core.parser.antlr.internal.FlexerFactory;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.nodemodel.impl.NodeModelBuilder;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.validation.CompositeEValidator;
import org.eclipse.xtext.validation.CompositeEValidator.EValidatorEqualitySupport;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Custom XtendParser that uses a JFlex based lexer implementation rather
 * than Antlr's lexer.
 * 
 * @since 2.5
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@Singleton
public class CustomXtendParser extends XtendParser {

	{
		EValidator v = EValidator.Registry.INSTANCE.getEValidator(TypesPackage.eINSTANCE);
		if (!(v instanceof CompositeEValidator)) {
			throw new AssertionError("Validator for TypesPackage is not a composite validator: " + v);
		}
		CompositeEValidator casted = (CompositeEValidator) v;
		if (casted.getContents().size() <= 1) {
			throw new AssertionError("Validator for TypesPackage is not a composite validator: " + 
					casted.getContents().stream().map(it->it.getDelegate().getClass().getName()).collect(Collectors.joining(", ")));
		}
	}
	
	@Inject
	private FlexerFactory flexerFactory;
	
	@Override
	protected TokenSource createLexer(CharStream stream) {
		if (stream instanceof ReaderCharStream) {
			Reader reader = ((ReaderCharStream) stream).getReader();
			return flexerFactory.createTokenSource(reader);
		}
		throw new IllegalArgumentException(stream.getClass().getName());
	}
	
	@Override
	public IParseResult parse(ParserRule rule, Reader reader) {
		IParseResult parseResult = parse(rule.getName(), new ReaderCharStream(reader));
		return parseResult;
	}
	
	@Override
	public IParseResult doParse(Reader reader) {
		return parse(getDefaultRuleName(), new ReaderCharStream(reader));
	}
	
	@Override
	public IParseResult parse(RuleCall ruleCall, Reader reader, int initialLookAhead) {
		NodeModelBuilder builder = createNodeModelBuilder();
		builder.setForcedFirstGrammarElement(ruleCall);
		IParseResult parseResult = doParse(ruleCall.getRule().getName(), new ReaderCharStream(reader), builder, initialLookAhead);
		return parseResult;
	}
	
}
