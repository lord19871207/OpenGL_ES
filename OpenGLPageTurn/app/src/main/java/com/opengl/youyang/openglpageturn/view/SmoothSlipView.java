package com.opengl.youyang.openglpageturn.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.opengl.youyang.openglpageturn.modle.Plane;
import com.opengl.youyang.openglpageturn.render.OpenGLRenderer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by youyang on 15-4-9.
 * opengl实现的平滑翻页模式
 */
public class SmoothSlipView extends GLSurfaceView implements OpenGLRenderer.IOpenGLDemo {
    Plane plane;
    private float x;
    private float y;
    private float prex;
    private float prey;
    float dx,dy;
    float a=0;


    public SmoothSlipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmoothSlipView(Context context) {
        this(context, null);
    }

    private void init() {
        OpenGLRenderer renderer = new OpenGLRenderer(this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); //
        plane = new Plane();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = event.getX();
        y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                dx = x - prex;
                dy = y - prey;

                if(dx>0){
                    a++;
                }else{
                    a--;
                }
                requestRender();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        prex = x;
        prey = y;

        return true;
    }

    @Override
    public void drawScene(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glColor4f(0.0f, 0.0f, 0.7f, 0.5f);
        // Replace the current matrix with the identity matrix
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -2);
        // Translates 4 units into the screen.
        gl.glTranslatef(a/30, 0, 0);
        // Draw our scene.
        plane.draw(gl);

    }

    @Override
    public void initScene(GL10 gl) {

    }
}
