/*******************************************************************************
 * Copyright (c) 2019 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.validation.DiagnosticConverterImpl;
import org.eclipse.xtext.validation.Issue;

/**
 * @author zarnekow - Initial contribution and API
 */
public class EObjectValidatorDetector extends DiagnosticConverterImpl {

	@Override
	public void convertValidatorDiagnostic(Diagnostic diagnostic, IAcceptor<Issue> acceptor) {
		Severity severity = getSeverity(diagnostic);
		if (severity == null)
			return;
		if (EObjectValidator.DIAGNOSTIC_SOURCE.equals(diagnostic.getSource())) {
			throw new AssertionError("Unexpected EObjectValidatorDiagnostic: " + diagnostic);
		}
		super.convertValidatorDiagnostic(diagnostic, acceptor);
	}
}
