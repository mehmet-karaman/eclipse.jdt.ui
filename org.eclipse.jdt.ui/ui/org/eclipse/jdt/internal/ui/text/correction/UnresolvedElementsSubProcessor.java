package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class UnresolvedElementsSubProcessor {
	
	
	public static void getVariableProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		IBuffer buf= cu.getBuffer();
		String variableName= buf.getText(problemPos.getOffset(), problemPos.getLength());
		if (variableName.indexOf('.') != -1) {
			return;
		}

		// corrections
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, problemPos.getOffset(), variableName, SimilarElementsRequestor.VARIABLES);
		for (int i= 0; i < elements.length; i++) {
			SimilarElement curr= elements[i];
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changevariable.description", curr.getName()); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr.getName(), 3));
		}
		
		// new field
		IJavaElement elem= cu.getElementAt(problemPos.getOffset());
		if (elem instanceof IMember) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createfield.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.FIELD, variableName, 2));
		}
		if (elem instanceof IMethod) {
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createlocal.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.LOCAL, variableName, 1));
		
			label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createparameter.description", variableName); //$NON-NLS-1$
			proposals.add(new NewVariableCompletionProposal((IMember) elem, problemPos, label, NewVariableCompletionProposal.PARAM, variableName, 1));
		}			
		
		try {
			IScanner scanner= ASTResolving.createScanner(cu, problemPos.getOffset() + problemPos.getLength());
			if (scanner.getNextToken() == ITerminalSymbols.TokenNameDOT) {
				getTypeProposals(problemPos, SimilarElementsRequestor.REF_TYPES, proposals);
			}
		} catch (InvalidInputException e) {
		}
			
	}
	
	public static void getTypeProposals(ProblemPosition problemPos, int kind, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 1) {
			return;
		}
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String typeName= cu.getBuffer().getText(problemPos.getOffset(), problemPos.getLength());
		int bracketIndex= typeName.indexOf('[');
		if (bracketIndex != -1) {
			typeName= typeName.substring(0, bracketIndex);
		}
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, problemPos.getOffset(), typeName, kind);
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			
			ImportEdit importEdit= new ImportEdit(cu, settings);
			importEdit.addImport(curr);
			
			String simpleName= Signature.getSimpleName(curr);
			boolean importOnly= simpleName.equals(typeName);
			
			CUCorrectionProposal proposal= new CUCorrectionProposal("", cu, 0); //$NON-NLS-1$
			proposals.add(proposal);
			
			CompilationUnitChange change= proposal.getCompilationUnitChange();
			
			if (!importEdit.isEmpty()) {
				change.addTextEdit("Add Import", importEdit); //$NON-NLS-1$
			}
			if (!importOnly) {
				change.addTextEdit("Change", SimpleTextEdit.createReplace(problemPos.getOffset(), typeName.length(), simpleName)); //$NON-NLS-1$
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changetype.description", simpleName)); //$NON-NLS-1$
				proposal.setRelevance(3);
			} else {
				proposal.setDisplayName(CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.importtype.description", curr)); //$NON-NLS-1$
				proposal.setRelevance(5);
			}
		}
		
		// add type
		String addedCUName= typeName + ".java"; //$NON-NLS-1$
		if (!JavaConventions.validateCompilationUnitName(addedCUName).matches(IStatus.ERROR)) {
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			ICompilationUnit addedCU= pack.getCompilationUnit(addedCUName);
			if (!addedCU.exists()) {
				boolean isClass= (kind & SimilarElementsRequestor.CLASSES) != 0;
				String[] superTypes= (problemPos.getId() != IProblem.ExceptionTypeNotFound) ? null : new String[] { "java.lang.Exception" };
				
				CreateCompilationUnitChange change= new CreateCompilationUnitChange(addedCU, isClass, superTypes, settings);
				String name= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createtype.description", typeName); //$NON-NLS-1$
				ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, change, 0);
				proposal.setElementToOpen(addedCU);
				proposals.add(proposal);
			}
		}
	}
	
	public static void getMethodProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length < 3) {
			return;
		}
		
		ICompilationUnit cu= problemPos.getCompilationUnit();
		
		// corrections
		String methodName= args[1];
		String[] arguments= getArguments(args[2]);
				
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, problemPos.getOffset(), methodName, SimilarElementsRequestor.METHODS, arguments, null);
		for (int i= 0; i < elements.length; i++) {
			String curr= elements[i].getName();
			String label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.changemethod.description", curr); //$NON-NLS-1$
			proposals.add(new ReplaceCorrectionProposal(problemPos, label, curr, 2));
		}
		
		// new method
		String typeName= args[0];
		IType type= JavaModelUtil.findType(cu.getJavaProject(), typeName);
		if (type != null && type.getCompilationUnit() != null) {
			ICompilationUnit changedCU= type.getCompilationUnit();
			if (!changedCU.isWorkingCopy()) {
				changedCU= EditorUtility.getWorkingCopy(changedCU);
				if (changedCU == null) {
					// not yet supported, waiting for new working copy support
					return;
				}					
				type= (IType) JavaModelUtil.findMemberInCompilationUnit(changedCU, type);
				if (type == null) {
					return; // type does not exist in working copy
				}
			}
			String label;
			if (cu.equals(changedCU)) {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.description", methodName); //$NON-NLS-1$
			} else {
				label= CorrectionMessages.getFormattedString("UnresolvedElementsSubProcessor.createmethod.other.description", new Object[] { methodName, type.getElementName() } ); //$NON-NLS-1$
			}
			proposals.add(new NewMethodCompletionProposal(type, problemPos, label, methodName, arguments, 1));
		}
	}
	
	private static String[] getArguments(String signature) {
		StringTokenizer tok= new StringTokenizer(signature, ","); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			String str= tok.nextToken().trim();
			if (str.startsWith("<")) { //$NON-NLS-1$
				str= "java.lang.Object"; //$NON-NLS-1$
			}
			res[i]= str;
		}
		return res;
	}	


}
