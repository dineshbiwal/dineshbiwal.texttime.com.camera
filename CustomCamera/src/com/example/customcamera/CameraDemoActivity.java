package com.example.customcamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraDemoActivity extends Activity implements CameraCallback, Callback,
		OnClickListener, OnGestureListener {

	 private int currentColorEffect = 0;
     private int currentWhiteBalance = 0;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	private ImageView flipCamera;
	 private GestureDetector gesturedetector = null;
	private ImageView flashCameraButton;
	private ImageView captureImage;
	private int cameraId;
	private boolean flashmode = false;
	private int rotation;
	public static File imageViewFile;
	private static final int PICK_FROM_CAMERA = 1;
	private static final int CROP_FROM_CAMERA = 2;
	private static final int PICK_FROM_FILE = 3;
	private Uri mImageCaptureUri,picUri;
	private ImageView picImage;
	private ImageView whitebalance,effects;
	private FrameLayout cameraholder;
	
	 private CameraCallback callback = null;
     private String[] supportedColorEffects = null;
     private String[] supportedWhiteBalances = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camerademo_activity);
		// camera surface view created
		cameraId = CameraInfo.CAMERA_FACING_BACK;
		cameraholder = (FrameLayout) findViewById(R.id.cameraholder);
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
		            camera.setPreviewDisplay(surfaceHolder);
		            camera.setPreviewCallback(new Camera.PreviewCallback() {
		                    @Override
		                    public void onPreviewFrame(byte[] data, Camera camera) {
		                            if(null != callback) callback.onPreviewFrame(data, camera);
		                    }
		            });
		            
		            final List<String> coloreffects = camera.getParameters().getSupportedColorEffects();
		            final List<String> whiteBalances = camera.getParameters().getSupportedWhiteBalance();
		            
		            supportedColorEffects = new String[coloreffects.size()];
		            supportedWhiteBalances = new String[whiteBalances.size()];
		            
		            coloreffects.toArray(supportedColorEffects);
		            whiteBalances.toArray(supportedWhiteBalances);
		            
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (camera != null) {
			try {
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
				params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			}
		}

		params.setRotation(rotation);
	}

	private void showFlashButton(Parameters params) {
		boolean showFlash = (getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
				&& params.getSupportedFlashModes() != null
				&& params.getSupportedFocusModes().size() > 1;

		flashCameraButton.setVisibility(showFlash ? View.VISIBLE
				: View.INVISIBLE);

	}
	 /*private void setupPictureMode(){
	        camerasurface = new CameraSurface(this);
	        
	        cameraholder.addView(camerasurface, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	        
	        camerasurface.setCallback(this);
	    }*/
	  public void setCallback(CameraCallback callback){
          this.callback = callback;
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

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}
	private SurfaceHolder holder;
	 private void initialize() {
         holder = new SurfaceView(getBaseContext()).getHolder();
         
         holder.addCallback(this);
         holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
         
         gesturedetector = new GestureDetector(this);
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
	 private void setupPictureMode(){
		 
		//initialize();
		//cameraholder.removeAllViews();
	        cameraholder.addView(surfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	        
	        setCallback(this);
	    }
	private void takeImage() {
		camera.takePicture(null, null, new PictureCallback() {

			private File imageFile;

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				try {
					// convert byte array into bitmap
					Bitmap loadedImage = null;
					Bitmap rotatedBitmap = null;
					loadedImage = BitmapFactory.decodeByteArray(data, 0,
							data.length);
					// rotate Image
					Matrix rotateMatrix = new Matrix();
					//rotateMatrix.postRotate(rotation);
					rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
							loadedImage.getWidth(), loadedImage.getHeight(),
							rotateMatrix, false);
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

					ByteArrayOutputStream ostream = new ByteArrayOutputStream();
					
					// save image into gallery
					rotatedBitmap.compress(CompressFormat.JPEG, 100, ostream);
					FileOutputStream fout = new FileOutputStream(imageFile);
					fout.write(ostream.toByteArray());
					fout.close();
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

				        	//mImageCaptureUri = CameraSurface.imageFilename.getAbsolutePath();
				            Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

				            picImage.setImageBitmap(myBitmap);
					 }
					// Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					 mImageCaptureUri = Uri.fromFile(imageFile);
				    // intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
					startActivityForResult(new Intent(), CROP_FROM_CAMERA);
					//startActivity(new Intent(CameraDemoActivity.this, ViewImageFile.class));
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
 
 public void setColorEffect(int effect){
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

	private Builder createAlert(Context context, String title, String message) {

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
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            setColorEffect(which);
                            
                            dialog.dismiss();
                    }
            });
    
    builder.show();
    }

	private void doCrop() {
		final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();
    	
    	Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        
        List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, 0 );
        
        int size = list.size();
        
        if (size == 0) {	        
        	Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();
        	
            return;
        } else {
        	intent.setData(mImageCaptureUri);
            
            intent.putExtra("outputX", 400);
            intent.putExtra("outputY", 400);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);
            Log.e("Size",String.valueOf(size));
        	if (size == 1) {
        		Intent i 		= new Intent(intent);
	        	ResolveInfo res	= list.get(0);
	        	 Log.e("From Camera", "croping");
	        	i.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
	        	
	        	startActivityForResult(i, CROP_FROM_CAMERA);
        	} else {
		        for (ResolveInfo res : list) {
		        	final CropOption co = new CropOption();
		        	
		        	co.title 	= getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
		        	co.icon		= getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
		        	co.appIntent= new Intent(intent);
		        	
		        	co.appIntent.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
		        	
		            cropOptions.add(co);
		        }
		        Log.e("From Gallary", "croping");
		        CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);
		        
		        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setTitle("Choose Crop App");
		        builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
		            public void onClick( DialogInterface dialog, int item ) {
		                startActivityForResult( cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
		            }
		        });
	        
		        builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
		            @Override
		            public void onCancel( DialogInterface dialog ) {
		               
		                if (mImageCaptureUri != null ) {
		                    getContentResolver().delete(mImageCaptureUri, null, null );
		                    mImageCaptureUri = null;
		                }
		            }
		        } );
		        
		        AlertDialog alert = builder.create();
		        
		        alert.show();
        	}
        }
	}
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode != RESULT_OK) return;
	    switch (requestCode) {
		    case PICK_FROM_CAMERA:
		    	doCrop();
		    	
		    	break;
		    	
		    case PICK_FROM_FILE: 
		    	mImageCaptureUri = data.getData();
		    	doCrop();
	    
		    	break;	    	
	    
		    case CROP_FROM_CAMERA:	    	
		        Bundle extras = data.getExtras();
	
		        if (extras != null) {	        	
		            Bitmap photo = extras.getParcelable("data");
		            
		            picImage.setImageBitmap(photo);
		        }
	
		        File f = new File(mImageCaptureUri.getPath());            
		        
		        if (f.exists()) f.delete();
	
		        break;

	    }
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
