/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;


import java.util.function.Supplier;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * The factory which is capable of recreating class file editor
 * inputs stored in a memento.
 */
public class ClassFileEditorInputFactory implements IElementFactory {

	public final static String ID=  "org.eclipse.jdt.ui.ClassFileEditorInputFactory"; //$NON-NLS-1$
	public final static String KEY= "org.eclipse.jdt.ui.ClassFileIdentifier"; //$NON-NLS-1$

	public ClassFileEditorInputFactory() {
	}

	/*
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		String identifier= memento.getString(KEY);
		if (identifier == null) {
			return null;
		}
		IJavaElement element= JavaCore.create(identifier);
		try {
			if (!element.exists() && element instanceof IOrdinaryClassFile) {
				/*
				 * Let's try to find the class file,
				 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=83221
				 */
				IOrdinaryClassFile cf= (IOrdinaryClassFile)element;
				IType type= cf.getType();
				IJavaProject project= element.getJavaProject();
				if (project != null) {
					type= project.findType(type.getFullyQualifiedName());
					if (type == null) {
						if (!JavaCore.hasResolvedAllClasspathContainers(project)) {
							return new DelayedEditorInput(project, (Supplier<IEditorInput>) () -> {
								return (IEditorInput) createElement(memento);
							});
						} else {
							return null;
						}
					} else {
						element= type.getParent();
					}
				}
			}
			return EditorUtility.getEditorInput(element);
		} catch (JavaModelException x) {
			// Don't report but simply return null
			return null;
		}
	}

	public static void saveState(IMemento memento, InternalClassFileEditorInput input) {
		IClassFile c= input.getClassFile();
		memento.putString(KEY, c.getHandleIdentifier());
	}
}
