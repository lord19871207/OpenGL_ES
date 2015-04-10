package com.opengl.youyang.openglpageturn.render;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;

import com.opengl.youyang.openglpageturn.modle.Cube;
import com.opengl.youyang.openglpageturn.modle.Group;
import com.opengl.youyang.openglpageturn.modle.Mesh;
import com.opengl.youyang.openglpageturn.modle.Plane;

public class OpenGLRenderer implements Renderer {
	private Mesh root;
	private int angle=20;
	private IOpenGLDemo openGLDemo;

	public OpenGLRenderer(IOpenGLDemo openGLDemo) {
		this.openGLDemo=openGLDemo;
		// Initialize our cube.
		Group group = new Group();
		Cube cube = new Cube(1, 1, 1);
		cube.rx = 45;
		cube.ry = 45;
		Plane plane=new Plane();
		group.add(plane);
		root = group;
	}
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background color to black ( rgba ).
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
		// Enable Smooth Shading, default not really needed.
		gl.glShadeModel(GL10.GL_SMOOTH);
		// Depth buffer setup.
		gl.glClearDepthf(1.0f);
		// Enables depth testing.
		gl.glEnable(GL10.GL_DEPTH_TEST);
		// The type of depth testing to do.
		gl.glDepthFunc(GL10.GL_LEQUAL);
		// Really nice perspective calculations.
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.
	 * khronos.opengles.GL10)
	 */
	public void onDrawFrame(GL10 gl) {
		// Clears the screen and depth buffer.
//		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//		gl.glColor4f(0.0f, 0.0f, 0.7f, 0.5f);
//		// Replace the current matrix with the identity matrix
//		gl.glLoadIdentity();
//		// Translates 4 units into the screen.
//		gl.glTranslatef(0, 0, -2);
//
//		gl.glRotatef(0, 0, 0, 2);
//		// Draw our scene.
//		root.draw(gl);
//		angle++;
		openGLDemo.drawScene(gl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition
	 * .khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// Sets the current view port to the new size.
		gl.glViewport(0, 0, width, height);
		// Select the projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		// Reset the projection matrix
		gl.glLoadIdentity();
		// Calculate the aspect ratio of the window
		GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f,
				100.0f);
		// Select the modelview matrix
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		// Reset the modelview matrix
		gl.glLoadIdentity();
	}
	
	/**
     * Adds a mesh to the root.
     * 
     * @param mesh
     *            the mesh to add.
     */
    public void addMesh(Mesh mesh) {
        ((Group) root).add(mesh);
    }


	public interface IOpenGLDemo{
		public void drawScene(GL10 gl);
		public void initScene(GL10 gl);
	}


}
