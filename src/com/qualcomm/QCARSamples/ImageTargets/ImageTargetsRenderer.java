/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import com.qualcomm.QCAR.QCAR;

import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;

import java.io.InputStream;

/** The renderer class for the ImageTargets sample. */
public class ImageTargetsRenderer implements GLSurfaceView.Renderer
{
    public boolean mIsActive = false;
    
    /** Reference to main activity **/
    public ImageTargets mActivity;
    
    
    /** Native function for initializing the renderer. */
    public native void initRendering();
    
    
    /** Native function to update the renderer. */
    public native void updateRendering(int width, int height);
    
    
    /** Called when the surface is created or recreated. */
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        DebugLog.LOGD("GLRenderer::onSurfaceCreated");
        
        // Call native function to initialize rendering:
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        QCAR.onSurfaceCreated();
    }
    
    
    /** Called when the surface changed size. */
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        DebugLog.LOGD("GLRenderer::onSurfaceChanged");

		if (fb != null) {
			fb.dispose();
		}
		fb = new FrameBuffer(width, height);
		Config.viewportOffsetAffectsRenderTarget=true;   
		Config.viewportOffsetY = (float)0.12;
        
        // Call native function to update rendering when render surface
        // parameters have changed:
        updateRendering(width, height);
        
        // Call Vuforia function to handle render surface size changes:
        QCAR.onSurfaceChanged(width, height);
    }
    
    
    /** The native render function. */
    public native void renderFrame();
    
    
    /** Called to draw the current frame. */
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        
        // Update render view (projection matrix and viewport) if needed:
        mActivity.updateRenderView();
        
        // Call our native function to render content
        renderFrame();

        //jPCT
        updateCamera();

        world.renderScene(fb);
        world.draw(fb);
        fb.display(); 
    }
    
    
    
    /** Setting up jpct-ae **/
	private FrameBuffer fb;
	private World world;
	private float[] modelViewMat;
	private Light sun;
	private Object3D cube;
	private Camera cam;
	private float fov;
	private float fovy;
    
	//Initializing jPTC
    public void initJPCT() 
    {
		world = new World();
		world.setAmbientLight(20, 20, 20);

		sun = new Light(world);
		sun.setIntensity(250, 250, 250);

		// Create a texture out of the icon...:-)
		TextureManager txtMgr = TextureManager.getInstance();
		if (!txtMgr.containsTexture("texture")) {
			Texture texture = new Texture(BitmapHelper.rescale(
					BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.vuforia_splash)), 64, 64));
			txtMgr.addTexture("texture", texture);
		}

		cube = Primitives.getCylinder(20, 40);
		cube.calcTextureWrapSpherical();
		cube.setTexture("texture");
		cube.strip();
		cube.build();

		world.addObject(cube);

		cam = world.getCamera();

		SimpleVector sv = new SimpleVector();
		sv.set(cube.getTransformedCenter());
		sv.y += 100;
		sv.z += 100;
		
		sun.setPosition(sv);
		
		MemoryHelper.compact();    	
    }
    
    public void updateModelviewMatrix(float mat[]) {
        modelViewMat = mat;
    }
    
    public void updateCamera() {
		if (modelViewMat != null) {
			float[] m = modelViewMat;

			final SimpleVector camUp;
			
		    boolean isPortait = true; 
		    isPortait = (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ?  false : true;

			if (isPortait) {
				camUp = new SimpleVector(-m[0], -m[1], -m[2]);
			} else {
				camUp = new SimpleVector(-m[4], -m[5], -m[6]);
			}
			
			final SimpleVector camDirection = new SimpleVector(m[8], m[9], m[10]);
			final SimpleVector camPosition = new SimpleVector(m[12], m[13], m[14]);
			
			cam.setOrientation(camDirection, camUp);
			cam.setPosition(camPosition);
			
			cam.setFOV(fov);
			cam.setYFOV(fovy);
		}
    }
    
	public void setVideoSize(int videoWidth, int videoHeight) {
		
		DisplayMetrics displaymetrics = new DisplayMetrics();
		mActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;
		
		int widestVideo = videoWidth > videoHeight? videoWidth: videoHeight;
		int widestScreen = width > height? width: height;
		
		float diff = (widestVideo - widestScreen) / 2;
		
		Config.viewportOffsetY = diff / widestScreen;
	}
	
	public void setFov(float fov) {
		this.fov = fov;
	}
	
	public void setFovy(float fovy) {
		this.fovy = fovy;
	}

    private Object3D loadModel(InputStream is, float scale) {
        Object3D[] model = Loader.load3DS(is, scale);
        Object3D o3d = new Object3D(0);
        Object3D temp = null;
        for (int i = 0; i < model.length; i++) {
            temp = model[i];
            temp.setCenter(SimpleVector.ORIGIN);
            temp.rotateX((float)( -.5*Math.PI));
            temp.rotateMesh();
            temp.setRotationMatrix(new Matrix());
            o3d = Object3D.mergeObjects(o3d, temp);
            o3d.build();
        }
        
        return o3d;
    }
}
