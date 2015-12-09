package com.example.customcamera;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

public class ViewImageFile extends Activity {

	
	private static final int CROP_FROM_CAMERA = 2;
	private Uri mImageCaptureUri,picUri;
	private ImageView viewImage;
	
	public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.view_image);
	        viewImage = (ImageView) findViewById(R.id.viewImage);
	        if(CameraDemoActivity.imageViewFile.exists()){

	        	//mImageCaptureUri = CameraSurface.imageFilename.getAbsolutePath();
	            Bitmap myBitmap = BitmapFactory.decodeFile(CameraDemoActivity.imageViewFile.getAbsolutePath());

	            viewImage.setImageBitmap(myBitmap);
	            picUri = new Intent().getData();
	            mImageCaptureUri = new Intent().getData();
	            doCrop();
	        }
	        //viewImage.setImageURI(CameraSurface.imageFilename.getAbsolutePath());
	        
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode != RESULT_OK) return;
	    Bundle extras = data.getExtras();
		
        if (extras != null) {	        	
            Bitmap photo = extras.getParcelable("data");
            
            viewImage.setImageBitmap(photo);
        }

        File f = new File(mImageCaptureUri.getPath());            
        
        if (f.exists()) f.delete();
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
	            
	        	if (size == 1) {
	        		Intent i 		= new Intent(intent);
		        	ResolveInfo res	= list.get(0);
		        	
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

	 private void performCrop() {
	        // take care of exceptions
	        try {
	            // call the standard crop action intent (the user device may not
	            // support it)
	            Intent cropIntent = new Intent("com.android.camera.action.CROP");
	            // indicate image type and Uri
	            cropIntent.setDataAndType(picUri, "image/*");
	            // set crop properties
	            cropIntent.putExtra("crop", "true");
	            // indicate aspect of desired crop
	            cropIntent.putExtra("aspectX", 2);
	            cropIntent.putExtra("aspectY", 1);
	            // indicate output X and Y
	            cropIntent.putExtra("outputX", 256);
	            cropIntent.putExtra("outputY", 256);
	            // <span id="IL_AD7" class="IL_AD">retrieve data</span> on return
	            cropIntent.putExtra("return-data", true);
	            // start the activity - we handle returning in onActivityResult
	            startActivityForResult(cropIntent, CROP_FROM_CAMERA);
	        }
	        // respond to users whose devices do not support the crop action
	        catch (ActivityNotFoundException anfe) {
	            Toast toast = Toast
	                    .makeText(this, "This device doesn't support the crop action!", Toast.LENGTH_SHORT);
	            toast.show();
	        }
	    }
}
