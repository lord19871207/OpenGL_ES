package com.opengl.youyang.openglpageturn;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.opengl.youyang.openglpageturn.modle.Cube;
import com.opengl.youyang.openglpageturn.render.OpenGLRenderer;
import com.opengl.youyang.openglpageturn.view.SmoothSlipView;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        GLSurfaceView view=new GLSurfaceView(this);
//        OpenGLRenderer renderer=new OpenGLRenderer();
//        view.setRenderer(renderer);
//        setContentView(view);

//        Cube cube=new Cube(1, 1, 1);
//        cube.loadBitmap(BitmapFactory.decodeResource(getResources(),
//                R.drawable.jay));
//        renderer.addMesh(cube);
        SmoothSlipView view=new SmoothSlipView(this);
        setContentView(view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
