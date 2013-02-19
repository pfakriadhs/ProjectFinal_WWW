package com.example.android.apis.graphics;

import android.os.AsyncTask;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
// ----------------------------------------------------------------------

public class CameraPreview extends Activity {    
    private Preview mPreview;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview(this);
        setContentView(mPreview);
    }

}

// ----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {  
    SurfaceHolder mHolder;  
      
    Camera mCamera;  
      
    //This variable is responsible for getting and setting the camera settings  
    private Parameters parameters;  
    //this variable stores the camera preview size   
    private Size previewSize;  
    //this array stores the pixels as hexadecimal pairs   
    private int[] pixels;  
      
    Preview(Context context) {  
        super(context);  
          
        // Install a SurfaceHolder.Callback so we get notified when the  
        // underlying surface is created and destroyed.  
        mHolder = getHolder();  
        mHolder.addCallback(this);  
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
    }  
  
    public void surfaceCreated(SurfaceHolder holder) {  
        // The Surface has been created, acquire the camera and tell it where  
        // to draw.  
        mCamera = Camera.open();  
        try {  
           mCamera.setPreviewDisplay(holder);  
             
           //sets the camera callback to be the one defined in this class  
           mCamera.setPreviewCallback(this);  
           //mCamera.startPreview();    
           ///initialize the variables
           mCamera.startPreview();
           parameters = mCamera.getParameters();  
           previewSize = parameters.getPreviewSize();  
           pixels = new int[previewSize.width * previewSize.height]; 
           
             
        } catch (IOException exception) {  
            mCamera.release();  
            mCamera = null; 
            
            // TODO: add more exception handling logic here  
        }  
    }  
  
    public void surfaceDestroyed(SurfaceHolder holder) {  
        // Surface will be destroyed when we return, so stop the preview.  
        // Because the CameraDevice object is not a shared resource, it's very  
        // important to release it when the activity is paused.  
        mCamera.stopPreview();  
        mCamera.release();  
        mCamera = null;  
    }  
  
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {  
        // Now that the size is known, set up the camera parameters and begin  
        // the preview. 
    	parameters = mCamera.getParameters();
        parameters.setPreviewSize(w, h);
    	//parameters.getSupportedPreviewSizes();
        //set the camera's settings  
        mCamera.setParameters(parameters);  
        mCamera.startPreview();  
    }  
  
    @Override  
    public void onPreviewFrame(byte[] data, Camera camera) {  
        //transforms NV21 pixel data into RGB pixels  
        //decodeYUV420SP(pixels, data, previewSize.width,  previewSize.height);  
        //Start async post data to server
		new FrameUploadTask().execute(); 
		//Outuput the value of the top left pixel in the preview to LogCat 
        Log.i("Preview", "The top right pixel has the following RGB (hexadecimal) values:"  
                +Integer.toHexString(pixels[0]));     
    }  
      
    //Method from Ketai project! Not mine! See below...  
    /*void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {  
              
            final int frameSize = width * height;  
  
            for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
              for (int i = 0; i < width; i++, yp++) {  
                int y = (0xff & ((int) yuv420sp[yp])) - 16;  
                if (y < 0)  
                  y = 0;  
                if ((i & 1) == 0) {  
                  v = (0xff & yuv420sp[uvp++]) - 128;  
                  u = (0xff & yuv420sp[uvp++]) - 128;  
                }  
  
                int y1192 = 1192 * y;  
                int r = (y1192 + 1634 * v);  
                int g = (y1192 - 833 * v - 400 * u);  
                int b = (y1192 + 2066 * u);  
  
                if (r < 0)                  r = 0;               else if (r > 262143)  
                   r = 262143;  
                if (g < 0)                  g = 0;               else if (g > 262143)  
                   g = 262143;  
                if (b < 0)                  b = 0;               else if (b > 262143)  
                   b = 262143;  
  
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);  
              }  
            }  
          }  */
	class FrameUploadTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... unsued) {
			try {
				HttpClient httpClient = new DefaultHttpClient();
				HttpContext localContext = new BasicHttpContext();
				HttpPost httpPost = new HttpPost(
						URL);//Missing URL

				MultipartEntity entity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);

				/*ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bitmap.compress(CompressFormat.JPEG, 100, bos);*/
				//byte[] data = bos.toByteArray();
				ByteArrayBody bab = new ByteArrayBody(data, "frame");
				//entity.addPart("returnformat", new StringBody("json"));
				entity.addPart("uploaded", bab);
				/*entity.addPart("photoCaption", new StringBody(caption.getText()
						.toString()));*/
				httpPost.setEntity(entity);
				HttpResponse response = httpClient.execute(httpPost,
						localContext);
				/*BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent(), "UTF-8"));

				String sResponse = reader.readLine();
				return sResponse;*/
			} catch (Exception e) {
				/*if (dialog.isShowing())
					dialog.dismiss();
				Toast.makeText(getApplicationContext(),
						e.getMessage(),
						Toast.LENGTH_LONG).show();*/
				Log.e(e.getClass().getName(), e.getMessage(), e);
				return null;
			}

			// (null);
		}

		@Override
		protected void onPostExecute(String sResponse) {
		....
			/*try {
				if (dialog.isShowing())
					dialog.dismiss();

				if (sResponse != null) {
					JSONObject JResponse = new JSONObject(sResponse);
					int success = JResponse.getInt("SUCCESS");
					String message = JResponse.getString("MESSAGE");
					if (success == 0) {
						Toast.makeText(getApplicationContext(), message,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getApplicationContext(),
								"Photo uploaded successfully",
								Toast.LENGTH_SHORT).show();
						caption.setText("");
					}
				}
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(),
						e.getMessage(),
						Toast.LENGTH_LONG).show();
				Log.e(e.getClass().getName(), e.getMessage(), e);
			}
			*/
		}
	}
	  
}  