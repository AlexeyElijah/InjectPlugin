package com.zwc.inject.plugin;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.zwc.inject.plugin.common.Defintions;
import com.zwc.inject.plugin.common.Utils;
import com.zwc.inject.plugin.form.EntryList;
import com.zwc.inject.plugin.iface.ICancelListener;
import com.zwc.inject.plugin.iface.IConfirmListener;
import com.zwc.inject.plugin.model.Element;

import javax.swing.*;
import java.util.ArrayList;

public class InjectAction extends BaseGenerateAction implements IConfirmListener, ICancelListener {

	protected JFrame mDialog;

	@SuppressWarnings("unused")
	public InjectAction() {
		super(null);
	}

	@SuppressWarnings("unused")
	public InjectAction(CodeInsightActionHandler handler) {
		super(handler);
	}

	@Override
	protected boolean isValidForClass(final PsiClass targetClass) {
		//取消判断是否加载了butterknife的逻辑
//		PsiClass injectViewClass = JavaPsiFacade.getInstance(targetClass.getProject()).findClass("butterknife.InjectView", new EverythingGlobalScope(targetClass.getProject()));
//		return (injectViewClass != null && super.isValidForClass(targetClass) && Utils.findAndroidSDK() != null && !(targetClass instanceof PsiAnonymousClass));
	return true;
	}

	@Override
	public boolean isValidForFile(Project project, Editor editor, PsiFile file) {
		//取消判断是否加载了butterknife的逻辑
		//PsiClass injectViewClass = JavaPsiFacade.getInstance(project).findClass("butterknife.InjectView", new EverythingGlobalScope(project));
		//return (injectViewClass != null && super.isValidForFile(project, editor, file) && Utils.getLayoutFileFromCaret(editor, file) != null);
		return true;
	}


	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = event.getData(PlatformDataKeys.PROJECT);
		Editor editor = event.getData(PlatformDataKeys.EDITOR);

		actionPerformedImpl(project, editor);
	}

	@Override
	public void actionPerformedImpl(Project project, Editor editor) {
		PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
		PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

		if (layout == null) {
			Utils.showErrorNotification(project, "No layout found");
			return; // no layout found
		}

		ArrayList<Element> elements = Utils.getIDsFromLayout(layout);
		if (!elements.isEmpty()) {
			showDialog(project, editor, elements);
		} else {
			Utils.showErrorNotification(project, "No IDs found in layout");
		}
	}

	public void onConfirm(Project project, Editor editor, ArrayList<Element> elements, String fieldNamePrefix, boolean createHolder) {
		PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
		PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

		closeDialog();

		if (Utils.getInjectCount(elements) > 0 || Utils.getClickCount(elements) > 0) { // generate injections
			new InjectWriter(file, getTargetClass(editor, file), "Generate Injections", elements, layout.getName(), fieldNamePrefix, createHolder).execute();
		} else { // just notify user about no element selected
			Utils.showInfoNotification(project, "No injection was selected");
		}
	}



	public void onCancel() {
		closeDialog();
	}

	protected void showDialog(Project project, Editor editor, ArrayList<Element> elements) {
		PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
		PsiClass clazz = getTargetClass(editor, file);

		// get parent classes and check if it's an adapter
//		boolean createHolder = false;
		boolean createHolder = true;
		PsiReferenceList list = getTargetClass(editor, file).getExtendsList();
		for (PsiJavaCodeReferenceElement element : list.getReferenceElements()) {
			if (Defintions.adapters.contains(element.getQualifiedName())) {
				createHolder = true;
			}
		}

		// get already generated injections
		ArrayList<String> ids = new ArrayList<String>();
		PsiField[] fields = clazz.getAllFields();
		String[] annotations;
		String id;

		for (PsiField field : fields) {
			annotations = field.getFirstChild().getText().split(" ");

			for (String annotation : annotations) {
				id = Utils.getInjectionID(annotation.trim());
				if (!Utils.isEmptyString(id)) {
					ids.add(id);
				}
			}
		}

		EntryList panel = new EntryList(project, editor, elements, ids, createHolder, this, this);

		mDialog = new JFrame();
		mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		mDialog.getRootPane().setDefaultButton(panel.getConfirmButton());
		mDialog.getContentPane().add(panel);
		mDialog.pack();
		mDialog.setLocationRelativeTo(null);
		mDialog.setVisible(true);
	}

	protected void closeDialog() {
		if (mDialog == null) {
			return;
		}

		mDialog.setVisible(true);
		mDialog.dispose();
	}
}
