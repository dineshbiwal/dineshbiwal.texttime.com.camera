package com.example.customcamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ShowImage extends Activity implements OnClickListener {

	private Button save, crop;
	private File imageFile;
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.show_image);
	        
	        ImageView showImage = (ImageView) findViewById(R.id.crop_image);
	        showImage.setImageBitmap(CropActivity.croppedImg);
	        save = (Button) findViewById(R.id.save);
	        crop = (Button) findViewById(R.id.crop);
	        save.setOnClickListener(this);
	        crop.setOnClickListener(this);
	 }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v == save)
		{
			saveImage();
		}
		if(v == crop)
		{
			saveImage();
			Intent intent = new Intent(this, CropActivity.class);
			intent.putExtra(CameraDemoActivity.CROP_VERSION_SELECTED_KEY, CameraDemoActivity.VERSION_1);
			Log.d("IMAGEURL",imageFile.getAbsolutePath());
			intent.putExtra("status",imageFile.getAbsolutePath());
			startActivity(intent);
		}
	}
	private void saveImage()
	{
		try{
			
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
				File file = new File(getIntent().getExtras().getString("imagePath"));
				if(file.delete())
					Log.d("File :", " has been deleted");
			} else {
				Toast.makeText(getBaseContext(), "Image Not saved",
						Toast.LENGTH_SHORT).show();
				return;
			}

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			
			// save image into gallery
			CropActivity.croppedImg.compress(CompressFormat.JPEG, 100, ostream);
			FileOutputStream fout = new FileOutputStream(imageFile);
			fout.write(ostream.toByteArray());
			fout.close();
			ContentValues values = new ContentValues();
			values.put(Images.Media.DATE_TAKEN,
					System.currentTimeMillis());
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(MediaStore.MediaColumns.DATA,
					imageFile.getAbsolutePath());

			ShowImage.this.getContentResolver().insert(
				Images.Media.EXTERNAL_CONTENT_URI, values);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

	}
}
