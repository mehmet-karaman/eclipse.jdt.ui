package org.eclipse.jdt.internal.debug.ui.display;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import java.util.ArrayList;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.debug.core.model.IStackFrame;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.eval.IEvaluationContext;import org.eclipse.jdt.debug.core.IJavaStackFrame;import org.eclipse.jdt.internal.debug.core.JDIDebugTarget;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.text.java.ResultCollector;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.text.JavaTextTools;import org.eclipse.jface.action.IAction;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.IToolBarManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.action.Separator;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.Document;import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.text.ITextOperationTarget;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.text.source.ISourceViewer;import org.eclipse.jface.text.source.SourceViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.VerifyKeyListener;import org.eclipse.swt.events.VerifyEvent;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Menu;import org.eclipse.ui.IActionBars;import org.eclipse.ui.IMemento;import org.eclipse.ui.IViewSite;import org.eclipse.ui.IWorkbenchActionConstants;import org.eclipse.ui.PartInitException;import org.eclipse.ui.help.ViewContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.part.ViewPart;import org.eclipse.ui.texteditor.ITextEditorActionConstants;import org.eclipse.ui.texteditor.IUpdate;public class DisplayView extends ViewPart {			/**	 * Display view identifier (value <code>"org.eclipse.debug.ui.DisplayView"</code>).	 */	public static final String ID_DISPLAY_VIEW= "org.eclipse.jdt.debug.ui.DisplayView"; //$NON-NLS-1$			class DataDisplay implements IDataDisplay {		/**		 * @see IDataDisplay#clear()		 */		public void clear() {			IDocument document= fSourceViewer.getDocument();			if (document != null) {				document.set(""); //$NON-NLS-1$			}		}				/**		 * @see IDataDisplay#displayExpression(String)		 */		public void displayExpression(String expression) {			ITextSelection selection= (ITextSelection)fSourceViewer.getSelection();			int offset= selection.getOffset() + selection.getLength();			String formatting= System.getProperty("line.separator") + "\t";			try {				fSourceViewer.getDocument().replace(offset, 0, formatting);					fSourceViewer.setSelectedRange(offset + formatting.length(), 0);					fSourceViewer.revealRange(offset, formatting.length());			} catch (BadLocationException ble) {			}		}						/**		 * @see IDataDisplay#displayExpressionValue(String)		 */		public void displayExpressionValue(String value) {			value= value + System.getProperty("line.separator");			ITextSelection selection= (ITextSelection)fSourceViewer.getSelection();			int offset= selection.getOffset();			int length= value.length();			int replace= selection.getLength() - offset;			if (replace < 0) {				replace= 0;			}			try {				fSourceViewer.getDocument().replace(offset, replace, value);				} catch (BadLocationException ble) {			}			fSourceViewer.setSelectedRange(offset + length, 0);				fSourceViewer.revealRange(offset, length);		}					/**		 * @see IDataDisplay#selectLineForEvaluation(ITextSelection)		 */		public void selectLineForEvaluation(ITextSelection selection) {					IDocument doc= fSourceViewer.getDocument();			try {				IRegion region= doc.getLineInformationOfOffset(selection.getOffset());				fSourceViewer.setSelectedRange(region.getOffset(), region.getLength());				fSourceViewer.revealRange(region.getOffset(), region.getLength());			} catch (BadLocationException ble) {			}		}	}			private IDataDisplay fDataDisplay= new DataDisplay();		protected SourceViewer fSourceViewer;	protected IAction fClearDisplayAction;	protected DisplayAction fDisplayAction;	protected IAction fInspectAction;	protected IAction fContentAssistAction;	protected Map fGlobalActions= new HashMap(4);	protected List fSelectionActions= new ArrayList(4);	protected String fRestoredContents= null;	/**	 * @see ViewPart#createChild(IWorkbenchPartContainer)	 */	public void createPartControl(Composite parent) {				int styles= SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION;		fSourceViewer= new SourceViewer(parent, null, styles);		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();		fSourceViewer.configure(new DisplayViewerConfiguration(textTools, this));		IDocument doc= null;		if (fRestoredContents != null) {			doc= new Document(fRestoredContents);		} else {			doc= new Document();		}		fSourceViewer.setDocument(doc);		fRestoredContents= null;		initializeActions();		initializeToolBar();		// create context menu		MenuManager menuMgr = new MenuManager("#PopUp", ID_DISPLAY_VIEW); //$NON-NLS-1$		menuMgr.setRemoveAllWhenShown(true);		menuMgr.addMenuListener(new IMenuListener() {			public void menuAboutToShow(IMenuManager mgr) {				fillContextMenu(mgr);			}		});				Menu menu = menuMgr.createContextMenu(fSourceViewer.getTextWidget());		fSourceViewer.getTextWidget().setMenu(menu);		getSite().registerContextMenu(menuMgr, fSourceViewer);		getSite().setSelectionProvider(fSourceViewer.getSelectionProvider());				WorkbenchHelp.setHelp(fSourceViewer.getTextWidget(), new ViewContextComputer(this, IJavaHelpContextIds.DISPLAY_VIEW));			}	/**	 * @see IWorkbenchPart	 */	public void setFocus() {		fSourceViewer.getControl().setFocus();	}		/**	 * Initialize the actions of this view	 */	private void initializeActions() {						fClearDisplayAction= new ClearDisplayAction(this);		fDisplayAction= new DisplayAction(this);		fInspectAction= new InspectAction(this);		IActionBars actionBars = getViewSite().getActionBars();				IAction action;				action= new DisplayViewAction(this, fSourceViewer.CUT);		action.setText(DisplayMessages.getString("DisplayView.Cut.label")); //$NON-NLS-1$		action.setToolTipText(DisplayMessages.getString("DisplayView.Cut.tooltip")); //$NON-NLS-1$		action.setDescription(DisplayMessages.getString("DisplayView.Cut.description")); //$NON-NLS-1$		setGlobalAction(actionBars, ITextEditorActionConstants.CUT, action);				action= new DisplayViewAction(this, fSourceViewer.COPY);		action.setText(DisplayMessages.getString("DisplayView.Copy.label")); //$NON-NLS-1$		action.setToolTipText(DisplayMessages.getString("DisplayView.Copy.tooltip")); //$NON-NLS-1$		action.setDescription(DisplayMessages.getString("DisplayView.Copy.description")); //$NON-NLS-1$		setGlobalAction(actionBars, ITextEditorActionConstants.COPY, action);				action= new DisplayViewAction(this, fSourceViewer.PASTE);		action.setText(DisplayMessages.getString("DisplayView.Paste.label")); //$NON-NLS-1$		action.setToolTipText(DisplayMessages.getString("DisplayView.Paste.tooltip")); //$NON-NLS-1$		action.setDescription(DisplayMessages.getString("DisplayView.Paste.Description")); //$NON-NLS-1$		setGlobalAction(actionBars, ITextEditorActionConstants.PASTE, action);				action= new DisplayViewAction(this, fSourceViewer.SELECT_ALL);		action.setText(DisplayMessages.getString("DisplayView.SelectAll.label")); //$NON-NLS-1$		action.setToolTipText(DisplayMessages.getString("DisplayView.SelectAll.tooltip")); //$NON-NLS-1$		action.setDescription(DisplayMessages.getString("DisplayView.SelectAll.description")); //$NON-NLS-1$		setGlobalAction(actionBars, ITextEditorActionConstants.SELECT_ALL, action);				fSelectionActions.add(ITextEditorActionConstants.CUT);		fSelectionActions.add(ITextEditorActionConstants.COPY);		fSelectionActions.add(ITextEditorActionConstants.PASTE);				initializeContentAssistAction();	}	private void initializeContentAssistAction() {		fContentAssistAction= new DisplayViewAction(this, ISourceViewer.CONTENTASSIST_PROPOSALS);		fContentAssistAction.setText("Co&ntent Assist@Ctrl+Space");		fContentAssistAction.setDescription("Content Assist");		fContentAssistAction.setToolTipText("Content Assist");				fSourceViewer.getTextWidget().addVerifyKeyListener(new VerifyKeyListener() {			public void verifyKey(VerifyEvent event) {				//do code assist for CTRL-SPACE				if (event.stateMask == SWT.CTRL && event.keyCode == 0 && event.character == 0x20 && fContentAssistAction.isEnabled()) {					fContentAssistAction.run();					event.doit= false;				}			}		});	}		protected void setGlobalAction(IActionBars actionBars, String actionID, IAction action) {		fGlobalActions.put(actionID, action);		actionBars.setGlobalActionHandler(actionID, action);	}	/**	 * Configures the toolBar.	 */	private void initializeToolBar() {		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();		tbm.add(fClearDisplayAction);		tbm.add(fDisplayAction);		tbm.add(fInspectAction);		getViewSite().getActionBars().updateActionBars();	}	/**	 * Adds the context menu actions for the display view.	 */	protected void fillContextMenu(IMenuManager menu) {				if (fSourceViewer.getDocument() == null) {			return;		} 		updateActions();			menu.add(fDisplayAction);		menu.add(fInspectAction);		menu.add(fContentAssistAction);		menu.add(new Separator());				menu.add((IAction) fGlobalActions.get(ITextEditorActionConstants.CUT));		menu.add((IAction) fGlobalActions.get(ITextEditorActionConstants.COPY));		menu.add((IAction) fGlobalActions.get(ITextEditorActionConstants.PASTE));		menu.add((IAction) fGlobalActions.get(ITextEditorActionConstants.SELECT_ALL));		menu.add(new Separator());		menu.add(fClearDisplayAction);		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));	}	/**	 * @see WorkbenchPart#getAdapter(Class)	 */	public Object getAdapter(Class required) {					if (ITextOperationTarget.class.equals(required)) {			return fSourceViewer.getTextOperationTarget();		}					if (IDataDisplay.class.equals(required)) {			return fDataDisplay;		}				return super.getAdapter(required);	}		protected void updateActions() {		Iterator iterator = fSelectionActions.iterator();		while (iterator.hasNext()) {			IAction action = (IAction) fGlobalActions.get((String)iterator.next());			if (action instanceof IUpdate) {				 ((IUpdate) action).update();			}		}	}				protected IJavaStackFrame getContext() {		IStackFrame stackFrame= fDisplayAction.getContext();		if (stackFrame == null) {			reportError("No stack frame context"); 			return null;		}				return (IJavaStackFrame) stackFrame.getAdapter(IJavaStackFrame.class);	}		protected IJavaProject getJavaProject(IJavaStackFrame stackFrame) {		IJavaElement javaElement= fDisplayAction.getJavaElement(stackFrame);		if (javaElement != null) {			return javaElement.getJavaProject();		}		return null;	}		protected void reportError(String message) {		Status status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, message, null);		reportError(status);	}		protected void reportError(IStatus status) {		ErrorDialog.openError(getSite().getShell(), "Error evaluating", null, status);	}		/**	 * @see IViewPart.	 */	public void saveState(IMemento memento) {		if (fSourceViewer != null) {			String contents= fSourceViewer.getDocument().get();			if (contents.length() > 0) {				memento.putString(ID_DISPLAY_VIEW, contents);			}			} else if (fRestoredContents != null) {			memento.putString(ID_DISPLAY_VIEW, fRestoredContents);		}	}		/**	 * @see IViewPart.	 */	public void init(IViewSite site, IMemento memento) throws PartInitException {		init(site);		if (memento != null) {			fRestoredContents= memento.getString(ID_DISPLAY_VIEW);		}	}		/**	 * Returns the snippet currently attempting to be code completed.	 */	protected String getSnippet() {		IDocument d= fSourceViewer.getDocument();		ITextSelection selection= (ITextSelection)fSourceViewer.getSelectionProvider().getSelection();		int start= selection.getOffset();		try {			return d.get(0, start);				} catch (BadLocationException ble) {		}		return "";	}		/**	 * Returns the entire contents of the current document.	 */	protected String getContents() {		return fSourceViewer.getDocument().get();	}}