/*******************************************************************************
 * Copyright (c) 2011, 2017 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.tests;

import org.eclipse.emf.ecore.EValidator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtend.core.xtend.XtendPackage;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil;
import org.eclipse.xtext.ui.testing.util.TargetPlatformUtil;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author Jan Koehnlein - Initial contribution and API
 */
@InjectWith(XtendIDEInjectorProvider.class)
@RunWith(XtextRunner.class)
public abstract class AbstractXtendUITestCase extends Assert {

	@Inject
	private Injector injector;
	
	@Before
	public void assertHasValidator() {
		EValidator xtendValidator = EValidator.Registry.INSTANCE.getEValidator(XtendPackage.eINSTANCE);
		assertNotNull(xtendValidator);
		
		EValidator xbaseValidator = EValidator.Registry.INSTANCE.getEValidator(XbasePackage.eINSTANCE);
		assertNotNull(xbaseValidator);
		
		EValidator typesValidator = EValidator.Registry.INSTANCE.getEValidator(TypesPackage.eINSTANCE);
		assertNotNull(typesValidator);
	}

	@Before
	public void setUp() throws Exception {
		//getInjector().injectMembers(this);
	}

	@After
	public void tearDown() throws Exception {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	@BeforeClass
	public static void setUpProject() throws Exception {
		TargetPlatformUtil.setTargetPlatform(AbstractXtendTestCase.class);
		IResourcesSetupUtil.cleanWorkspace();
		WorkbenchTestHelper.createPluginProject(WorkbenchTestHelper.TESTPROJECT_NAME);
	}

	@AfterClass
	public static void tearDownProject() throws Exception {
		IResourcesSetupUtil.cleanWorkspace();
	}

	public final Injector getInjector() {
		return injector;
	}

	public final <T> T get(Class<T> clazz) {
		return getInjector().getInstance(clazz);
	}

	protected void setJavaVersion(JavaVersion javaVersion) throws Exception {
		IJavaProject javaProject = JavaProjectSetupUtil.findJavaProject(WorkbenchTestHelper.TESTPROJECT_NAME);
		Pair<String,Boolean> result = WorkbenchTestHelper.changeBree(javaProject, javaVersion);
		IExecutionEnvironment execEnv = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(result.getKey());
		Assume.assumeNotNull("Execution environment not found for: " + javaVersion.getLabel(), execEnv);
		Assume.assumeTrue("No compatible VM was found for: " + javaVersion.getLabel(),
				execEnv.getCompatibleVMs().length > 0);
		if(result.getValue()) {
			WorkbenchTestHelper.makeCompliantFor(javaProject, javaVersion);
			IResourcesSetupUtil.reallyWaitForAutoBuild();
		}
	}

}
