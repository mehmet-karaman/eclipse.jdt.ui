/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.core.runtime.CoreException;

public final class CopyTargetEdit extends AbstractTransferEdit {

	private CopySourceEdit fSource;

	public CopyTargetEdit(int offset) {
		super(offset, 0);
	}

	public CopyTargetEdit(int offset, CopySourceEdit source) {
		this(offset);
		setSourceEdit(source);
	}

	private CopyTargetEdit(CopyTargetEdit other) {
		super(other);
	}

	public CopySourceEdit getSourceEdit() {
		return fSource;
	}
		
	public void setSourceEdit(CopySourceEdit edit) {
		if (fSource != edit) {
			fSource= edit;
			fSource.setTargetEdit(this);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		return new CopyTargetEdit(this);
	}

	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fSource != null) {
			((CopyTargetEdit)copier.getCopy(this)).setSourceEdit((CopySourceEdit)copier.getCopy(fSource));
		}
	}
	
	protected void connect(TextBuffer buffer) throws TextEditException {
		if (fSource == null)
			throw new TextEditException(getParent(), this, TextManipulationMessages.getString("CopyTargetEdit.no_source")); //$NON-NLS-1$
		if (fSource.getTargetEdit() != this)
			throw new TextEditException(getParent(), this, TextManipulationMessages.getString("CopyTargetEdit.different_target")); //$NON-NLS-1$
	}
	
	public void perform(TextBuffer buffer) throws CoreException {
		if (++fSource.fCounter == 2 && !getTextRange().isDeleted()) {
			try {
				buffer.replace(getTextRange(), getSourceContent());
			} finally {
				fSource.clearContent();
			}
		}
	}
	
	/**
	 * Gets the content from the source edit.
	 */
	protected String getSourceContent() {
		return fSource.getContent();
	}		
}
