/*
   Copyright 2009 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GLException;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.gavrog.box.gui.Invoke;

import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.softviewer.SoftViewer;
import de.jreality.tools.ClickWheelCameraZoomTool;
import de.jreality.tools.DraggingTool;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;
import de.jreality.util.CameraUtility;
import de.jreality.util.ImageUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.RenderTrigger;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public class ViewerFrame extends JFrame {
	final private SceneGraphComponent rootNode = new SceneGraphComponent();
	final private SceneGraphComponent cameraNode = new SceneGraphComponent();
	final private SceneGraphComponent geometryNode = new SceneGraphComponent();
	final private SceneGraphComponent lightNode = new SceneGraphComponent();
	final private SceneGraphComponent contentNode;
	
	final private Map<String, SceneGraphComponent> lights =
		new HashMap<String, SceneGraphComponent>();
	
	final private SoftViewer softwareViewer;
	final private RenderTrigger renderTrigger = new RenderTrigger();
	private Viewer viewer;
    private double lastCenter[] = null;
    

	public ViewerFrame(final SceneGraphComponent content) {
		contentNode = content;

		rootNode.addChild(geometryNode);
		rootNode.addChild(cameraNode);
		rootNode.addChild(lightNode);
		geometryNode.addChild(contentNode);

		contentNode.addTool(new RotateTool());
		contentNode.addTool(new DraggingTool());
		contentNode.addTool(new ClickWheelCameraZoomTool());

		Camera camera = new Camera();
		cameraNode.setCamera(camera);
		MatrixBuilder.euclidean().translate(0, 0, 3).assignTo(cameraNode);

		final Appearance rootApp = new Appearance();
		rootApp.setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.DARK_GRAY);
		rootApp.setAttribute(CommonAttributes.DIFFUSE_COLOR, Color.RED);
		rootNode.setAppearance(rootApp);

		SceneGraphPath camPath = new SceneGraphPath();
		camPath.push(rootNode);
		camPath.push(cameraNode);
		camPath.push(camera);
		
		SceneGraphPath emptyPickPath = new SceneGraphPath();
		emptyPickPath.push(rootNode);
		emptyPickPath.push(geometryNode);
		emptyPickPath.push(contentNode);

		softwareViewer = new SoftViewer();
		softwareViewer.setSceneRoot(rootNode);
		softwareViewer.setCameraPath(camPath);
		setupToolSystem(softwareViewer, emptyPickPath);
		
		try {
			viewer = new de.jreality.jogl.Viewer();
			viewer.setSceneRoot(rootNode);
			viewer.setCameraPath(camPath);
			setupToolSystem(viewer, emptyPickPath);
		} catch (Exception ex) {
			System.err.println("OpenGL viewer could not be initialized.");
			viewer = softwareViewer;
		}
		
		setViewer(viewer);
		setViewerSize(new Dimension(640, 400));
		pack();
		
		if (viewer instanceof de.jreality.jogl.Viewer) {
			Invoke.andWait(new Runnable() {
				public void run() {
					try {
						((de.jreality.jogl.Viewer) viewer).run();
						System.err.println("OpenGL okay!");
					} catch (GLException ex) {
						System.err.println("OpenGL viewer could not render.");
						setViewer(softwareViewer);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}

		renderTrigger.addSceneGraphComponent(rootNode);
	}
	
	private void setupToolSystem(final Viewer viewer,
			final SceneGraphPath emptyPickPath) {
		final ToolSystem ts = ToolSystem.toolSystemForViewer(viewer);
		ts.initializeSceneTools();
		ts.setEmptyPickPath(emptyPickPath);
	}
	
	public Component getViewingComponent() {
		return (Component) viewer.getViewingComponent();
	}
	
	public void setLight(final String name, final Light light,
			final Transformation t) {
		if (!lights.containsKey(name)) {
			final SceneGraphComponent node = new SceneGraphComponent();
			lights.put(name, node);
			lightNode.addChild(node);
		}
		final SceneGraphComponent node = lights.get(name);
		node.setLight(light);
		node.setTransformation(t);
	}
	
	public void removeLight(final String name) {
		final SceneGraphComponent node = lights.get(name);
		if (node != null) {
			lightNode.removeChild(node);
		}
		lights.remove(name);
	}
	
	public void startRendering() {
		renderTrigger.finishCollect();
	}
	
	public void pauseRendering() {
		renderTrigger.startCollect();
	}
	
	public void encompass() {
		// --- extract parameters from scene and viewer
		final ToolSystem ts = ToolSystem.toolSystemForViewer(viewer);
		final SceneGraphPath avatarPath = ts.getAvatarPath();
		final SceneGraphPath scenePath = ts.getEmptyPickPath();
		final SceneGraphPath cameraPath = viewer.getCameraPath();
		final double aspectRatio = CameraUtility.getAspectRatio(viewer);
		
        // --- compute scene-to-avatar transformation
		final Matrix toAvatar = new Matrix();
		scenePath.getMatrix(toAvatar.getArray(), 0, scenePath.getLength() - 2);
		toAvatar.multiplyOnRight(avatarPath.getInverseMatrix(null));
		
		// --- compute bounding box of scene
		final Rectangle3D bounds = GeometryUtility.calculateBoundingBox(
				toAvatar.getArray(), scenePath.getLastComponent());
		if (bounds.isEmpty()) {
			return;
		}
		
		// --- compute best camera position based on bounding box and viewport
        final Camera camera = (Camera) cameraPath.getLastElement();
		final Rectangle2D vp = CameraUtility.getViewport(camera, aspectRatio);
		final double[] e = bounds.getExtent();
		final double radius = Math
				.sqrt(e[0] * e[0] + e[2] * e[2] + e[1] * e[1]) / 2.0;
		final double front = e[2] / 2;

		final double xscale = e[0] / vp.getWidth();
		final double yscale = e[1] / vp.getHeight();
		double camdist = Math.max(xscale, yscale) * 1.1;
		if (!camera.isPerspective()) {
			camdist *= camera.getFocus(); // adjust for viewport scaling
			camera.setFocus(camdist);
		}

		// --- compute new camera position and adjust near/far clipping planes
		final double[] c = bounds.getCenter();
		c[2] += front + camdist;
		camera.setFar(camdist + front + 5 * radius);
		camera.setNear(0.1 * camdist);
		
		// --- make rotateScene() recompute the center
		lastCenter = null;
		
		// --- adjust the avatar position to make scene fit
		final Matrix camMatrix = new Matrix();
		cameraPath.getInverseMatrix(camMatrix.getArray(), avatarPath
				.getLength());
		final SceneGraphComponent avatar = avatarPath.getLastComponent();
		final Matrix m = new Matrix(avatar.getTransformation());
		MatrixBuilder.euclidean(m).translate(c).translate(
				camMatrix.getColumn(3)).assignTo(avatar);
	}

	public void rotateScene(final double axis[], final double angle) {
		final SceneGraphComponent root = contentNode;

		if (lastCenter == null) {
			// --- compute the center of the scene in world coordinates
			final Rectangle3D bounds = GeometryUtility
					.calculateBoundingBox(root);
			if (bounds.isEmpty()) {
				return;
			} else {
				lastCenter = new Matrix(root.getTransformation())
						.getInverse().multiplyVector(bounds.getCenter());
			}
		}
		
		// --- rotate around the last computed scene center
		final Matrix tOld = new Matrix(root.getTransformation());
		final Matrix tNew = MatrixBuilder.euclidean().rotate(angle, axis)
				.times(tOld).getMatrix();
		final double p[] = tOld.multiplyVector(lastCenter);
		final double q[] = tNew.multiplyVector(lastCenter);
		MatrixBuilder.euclidean().translateFromTo(q, p).times(tNew).assignTo(
				root);
	}
	
	public void screenshot(final Dimension size, final int antialias,
			final File file) {
		final int width = (int) size.width;
		final int height = (int) size.height;
		final BufferedImage img = softwareViewer.renderOffscreen(width
				* antialias, height * antialias);
		final BufferedImage scaledImg = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = (Graphics2D) scaledImg.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		scaledImg.getGraphics().drawImage(
				img.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH),
				0, 0, null);
		ImageUtility.writeBufferedImage(file, scaledImg);
	}
	
	public Viewer getViewer() {
		return this.viewer;
	}

	public void setViewer(final Viewer viewer) {
		final Dimension d = getViewerSize();
		getContentPane().removeAll();
		getContentPane().add((Component) viewer.getViewingComponent());
		renderTrigger.removeViewer(this.viewer);
		renderTrigger.addViewer(viewer);
		this.viewer = viewer;
		setViewerSize(d);
	}
	
	public Dimension getViewerSize() {
		if (viewer == null) {
			return new Dimension(0, 0);
		} else {
			return viewer.getViewingComponentSize();
		}
	}

	public void setViewerSize(final Dimension newSize) {
		((Component) viewer.getViewingComponent()).setPreferredSize(newSize);
		pack();
	}
	
	
	public static void main(String args[]) {
		final SceneGraphComponent content = new SceneGraphComponent();
		final IndexedFaceSet ifs = Primitives.icosahedron();
		content.setGeometry(ifs);
		
		final ViewerFrame frame = new ViewerFrame(content);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				System.exit(0);
			}
		});
		frame.setJMenuBar(new JMenuBar());
		frame.getJMenuBar().add(new JMenu("File"));
		final Light l1 = new DirectionalLight();
		l1.setIntensity(0.8);
		final Transformation t1 = new Transformation();
		MatrixBuilder.euclidean().rotateX(degrees(-30)).rotateY(degrees(-30))
				.assignTo(t1);
		frame.setLight("Main Light", l1, t1);
		final Light l2 = new DirectionalLight();
		l2.setIntensity(0.2);
		final Transformation t2 = new Transformation();
		MatrixBuilder.euclidean().rotateX(degrees(10)).rotateY(degrees(20))
				.assignTo(t2);
		frame.setLight("Fill Light", l2, t2);
		
		frame.validate();
		frame.setVisible(true);
		frame.startRendering();

		frame.setViewerSize(new Dimension(800, 600));
	}

	private static double degrees(final double d) {
		return d / 180.0 * Math.PI;
	}
}
