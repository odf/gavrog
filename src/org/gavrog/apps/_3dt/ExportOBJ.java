package org.gavrog.apps._3dt;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JComponent;

import de.jreality.scene.Viewer;
import de.jreality.ui.viewerapp.FileLoaderDialog;
import de.jreality.ui.viewerapp.actions.AbstractJrAction;
import de.jreality.writer.WriterOBJ;

public class ExportOBJ extends AbstractJrAction {
	private static final long serialVersionUID = -728178540191377982L;

	private Viewer viewer;

	private JComponent options;

	public ExportOBJ(String name, Viewer viewer, Component parentComp) {
		super(name, parentComp);

		if (viewer == null)
			throw new IllegalArgumentException("Viewer is null!");
		this.viewer = viewer;

		setShortDescription("Export the current scene as an OBJ file");
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		File file = FileLoaderDialog.selectTargetFile(parentComp, options,
				"obj", "Wavefront OBJ Files");
		if (file == null)
			return; // dialog cancelled

		try {
			WriterOBJ.write(viewer.getSceneRoot(), new FileOutputStream(file));
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
