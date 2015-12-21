package com.example.customcamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.GINGERBREAD) public class CameraDemoActivity extends Activity implements CameraCallback, Callback,
		OnClickListener, OnGestureListener {

	private int currentColorEffect = 0;
    private int currentWhiteBalance = 0;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private ImageView flipCamera;
	private ImageView flashCameraButton;
	private ImageView captureImage;
	private int cameraId;
	private boolean flashmode = false;
	private int rotation;
	public static File imageViewFile;
	private ImageView picImage;
	private ImageView whitebalance,effects;
	public static final String CROP_VERSION_SELECTED_KEY = "crop";
	public static final int VERSION_1 = 1;
	public static final int VERSION_2 = 2;
    private String[] supportedColorEffects = null;
    private String[] supportedWhiteBalances = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camerademo_activity);
		// camera surface view created
		flipCamera = (ImageView) findViewById(R.id.flipCamera);
		flashCameraButton = (ImageView) findViewById(R.id.flash);
		captureImage = (ImageView) findViewById(R.id.captureImage);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		picImage = (ImageView) findViewById(R.id.picImage);
		whitebalance = (ImageView) findViewById(R.id.whitebalance);
		effects = (ImageView) findViewById(R.id.effects);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		//setupPictureMode();
		flipCamera.setOnClickListener(this);
		captureImage.setOnClickListener(this);
		flashCameraButton.setOnClickListener(this);
		whitebalance.setOnClickListener(this);
		effects.setOnClickListener(this);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (Camera.getNumberOfCameras() > 1) {
			flipCamera.setVisibility(View.VISIBLE);
		}
		if (!getBaseContext().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FLASH)) {
			flashCameraButton.setVisibility(View.GONE);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
			alertCameraDialog();
		}
	}

	private boolean openCamera(int id) {
		boolean result = false;
		cameraId = id;
		releaseCamera();
		try {
			camera = Camera.open(cameraId);
			List<Camera.Size> supportedSizes;
					
		            final List<String> coloreffects = camera.getParameters().getSupportedColorEffects();
		            final List<String> whiteBalances = camera.getParameters().getSupportedWhiteBalance();
		            
		            supportedColorEffects = new String[coloreffects.size()];
		            supportedWhiteBalances = new String[whiteBalances.size()];
		            
		            coloreffects.toArray(supportedColorEffects);
		            whiteBalances.toArray(supportedWhiteBalances);
		            Camera.Parameters params = camera.getParameters();

		            supportedSizes = params.getSupportedPictureSizes();
					for (Camera.Size sz : supportedSizes) {
						setPictureSize(sz.width, sz.height);
						Log.d("camera size", "supportedPictureSizes " + sz.width + "x" + sz.height);
						break;
					}
		            List<String> flashModes = params.getSupportedFlashModes();
		            if (flashModes.size() > 0)
		                params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

		            // Action mode take pictures of fast moving objects
		            List<String> sceneModes = params.getSupportedSceneModes();
		            if (sceneModes.contains(Camera.Parameters.SCENE_MODE_ACTION))
		                params.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
		            else
		                params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

		            // if you choose FOCUS_MODE_AUTO remember to call autoFocus() on
		            // the Camera object before taking a picture 
		            params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
		            
		            camera.setParameters(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (camera != null) {
			try {
				Log.d("camera", "not null");
				setUpCamera(camera);
				camera.setErrorCallback(new ErrorCallback() {

					@Override
					public void onError(int error, Camera camera) {

					}
				});
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				result = false;
				releaseCamera();
			}
		}
		return result;
	}

	@SuppressLint("InlinedApi") 
	private void setUpCamera(Camera c) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degree = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degree = 0;
			break;
		case Surface.ROTATION_90:
			degree = 90;
			break;
		case Surface.ROTATION_180:
			degree = 180;
			break;
		case Surface.ROTATION_270:
			degree = 270;
			break;

		default:
			break;
		}

		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			// frontFacing
			rotation = (info.orientation + degree) % 330;
			rotation = (360 - rotation) % 360;
		} else {
			// Back-facing
			rotation = (info.orientation - degree + 360) % 360;
		}
		c.setDisplayOrientation(rotation);
		Parameters params = c.getParameters();
		
		showFlashButton(params);
		
		List<String> focusModes = params.getSupportedFlashModes();
		if (focusModes != null) {
			if (focusModes
					.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
				params.setFlashMode(Parameters.FLASH_MODE_AUTO);
			    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			}
		}
		params.setRotation(rotation);
	}

	private void setPictureSize(int width, int height) {
		Camera.Parameters params = camera.getParameters();
		params.setPictureSize(width, height);
		camera.setParameters(params);
	}
	private void showFlashButton(Parameters params) {
		boolean showFlash = (getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
				&& params.getSupportedFlashModes() != null
				&& params.getSupportedFocusModes().size() > 1;

		flashCameraButton.setVisibility(showFlash ? View.VISIBLE
				: View.INVISIBLE);

	}
  
	private void releaseCamera() {
		try {
			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.setErrorCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("error", e.toString());
			camera = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD) @SuppressLint("InlinedApi") @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("Id", String.valueOf(cameraId));
		int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
				: CameraInfo.CAMERA_FACING_BACK);
		Log.d("CameraId :", String.valueOf(id));
		openCamera(id);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
		}
		camera = null;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.flash:
			flashOnButton();
			break;
		case R.id.flipCamera:
			flipCamera();
			break;
		case R.id.captureImage:
			takeImage();
			break;
		case R.id.whitebalance:
			displayWhiteBalanceDialog();
			break;
		case R.id.effects:
			displayColorEffectDialog();          
			break;
		default:
			break;
		}
	}
	
	private void takeImage() {
		camera.takePicture(null, null, new PictureCallback() {

			private File imageFile;

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				try {
					String state = Environment.getExternalStorageState();
					File folder = null;
					if (state.contains(Environment.MEDIA_MOUNTED)) {
						folder = new File(Environment
								.getExternalStorageDirectory() + "/Demo");
					} else {
						folder = new File(Environment
								.getExternalStorageDirectory() + "/Demo");
					}

					boolean success = true;
					if (!folder.exists()) {
						success = folder.mkdirs();
					}
					if (success) {
						java.util.Date date = new java.util.Date();
						imageFile = new File(folder.getAbsolutePath()
								+ File.separator
								+ new Timestamp(date.getTime()).toString()
								+ "Image.jpg");

						imageFile.createNewFile();
					} else {
						Toast.makeText(getBaseContext(), "Image Not saved",
								Toast.LENGTH_SHORT).show();
						return;
					}

			        if (imageFile == null) {
			            Log.d("Camera Error", "Error creating media file, check storage permissions : PICTURE FILE IS NULL");
			            return;
			        }

			        try {
			            FileOutputStream fos = new FileOutputStream(imageFile);
			            fos.write(data);
			            fos.close();
			            finish();
			        } catch (FileNotFoundException e) {
			            Log.d("Camera Error", "File not found: " + e.getMessage());
			        } catch (IOException e) {
			            Log.d("Camera Error", "Error accessing file: " + e.getMessage());
			        }
					
			        ContentValues values = new ContentValues();
					values.put(Images.Media.DATE_TAKEN,
							System.currentTimeMillis());
					values.put(Images.Media.MIME_TYPE, "image/jpeg");
					values.put(MediaStore.MediaColumns.DATA,
							imageFile.getAbsolutePath());

					CameraDemoActivity.this.getContentResolver().insert(
						Images.Media.EXTERNAL_CONTENT_URI, values);

					
					imageViewFile = imageFile;
					Log.e("image path:", imageViewFile.getAbsolutePath());
					flipCamera.setVisibility(View.INVISIBLE);
					flashCameraButton.setVisibility(View.INVISIBLE);
					surfaceView.setVisibility(View.INVISIBLE);
					captureImage.setVisibility(View.INVISIBLE);
					whitebalance.setVisibility(View.INVISIBLE);
					effects.setVisibility(View.INVISIBLE);
					picImage.setVisibility(View.VISIBLE);
					 if(imageFile.exists()){

				      Intent intent = new Intent(CameraDemoActivity.this, CropActivity.class);
				      intent.putExtra(CROP_VERSION_SELECTED_KEY, VERSION_1);
				      intent.putExtra("status", imageFile.getAbsolutePath());
				      startActivity(intent);
				            Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
				            picImage.setImageBitmap(myBitmap);

					 }
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});
	}

	private void flipCamera() {
		int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
				: CameraInfo.CAMERA_FACING_BACK);
		if (!openCamera(id)) {
			alertCameraDialog();
		}
		else{
			List<Camera.Size> supportedSizes;
			Camera.Parameters params = camera.getParameters();

            supportedSizes = params.getSupportedPictureSizes();
			for (Camera.Size sz : supportedSizes) {
				setPictureSize(sz.width, sz.height);
				Log.d("camera size", "supportedPictureSizes " + sz.width + "x" + sz.height);
				break;
			}

		}
	}

	 private void displayWhiteBalanceDialog()
     {
             final AlertDialog.Builder builder = new AlertDialog.Builder(this);

     builder.setTitle(getString(R.string.white_balance));
     builder.setSingleChoiceItems(getSupportedWhiteBalances(), 
                     getCurrentWhiteBalance(), new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                             setWhiteBalance(which);
                             
                             dialog.dismiss();
                     }
             });
     
     builder.show();
     }
	 public int getCurrentColorEffect(){
         return currentColorEffect;
 }
 
 public int getCurrentWhiteBalance(){
         return currentWhiteBalance;
 }
 
 public String[] getSupportedColorEffects(){
         return supportedColorEffects;
 }
 
 public String[] getSupportedWhiteBalances(){
         return supportedWhiteBalances;
 }
 
 @SuppressLint("InlinedApi") public void setColorEffect(int effect){
         Camera.Parameters parameters = camera.getParameters();
         
         parameters.setColorEffect(supportedColorEffects[effect]);
         
         camera.setParameters(parameters);
         
         currentColorEffect = effect;
 }
 
 public void setWhiteBalance(int effect){
         Camera.Parameters parameters = camera.getParameters();
         
         parameters.setWhiteBalance(supportedWhiteBalances[effect]);
         
         camera.setParameters(parameters);
         
         currentWhiteBalance = effect;
 }
	 
	private void alertCameraDialog() {
		AlertDialog.Builder dialog = createAlert(CameraDemoActivity.this,
				"Camera info", "error to open camera, not find front camera.");
		dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				flipCamera();
			}
		});

		dialog.show();
	}

	@SuppressLint("InlinedApi") private Builder createAlert(Context context, String title, String message) {

		AlertDialog.Builder dialog = new AlertDialog.Builder(
				new ContextThemeWrapper(context,
						android.R.style.Theme_Holo_Light_Dialog));
		dialog.setIcon(R.drawable.ic_launcher);
		if (title != null)
			dialog.setTitle(title);
		else
			dialog.setTitle("Information");
		dialog.setMessage(message);
		dialog.setCancelable(false);
		return dialog;

	}

	private void flashOnButton() {
		if (camera != null) {
			try {
				Parameters param = camera.getParameters();
				param.setFlashMode(!flashmode ? Parameters.FLASH_MODE_TORCH
						: Parameters.FLASH_MODE_OFF);
				camera.setParameters(param);
				flashmode = !flashmode;
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}
	
	private void displayColorEffectDialog()
    {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle(getString(R.string.color_effect));
    builder.setSingleChoiceItems(getSupportedColorEffects(), 
                    getCurrentColorEffect(), new DialogInterface.OnClickListener() {
                    @SuppressLint("InlinedApi") @Override
                    public void onClick(DialogInterface dialog, int which) {
                            setColorEffect(which);
                            
                            dialog.dismiss();
                    }
            });
    
    builder.show();
    }


	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShutter() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRawPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onJpegPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String onGetVideoFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}
	
}