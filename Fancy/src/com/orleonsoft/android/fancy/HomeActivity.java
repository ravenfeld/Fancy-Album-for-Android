package com.orleonsoft.android.fancy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.devspark.appmsg.AppMsg;
import com.orleonsoft.android.fancy.util.Util;

public class HomeActivity extends SherlockActivity implements
		OnItemClickListener, OnItemLongClickListener {

	private static final int ACTION_TAKE_PHOTO = 1;
	public static String _ID_KEY = "_id";
	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	private LayoutInflater mLayoutInflater;
	private Cursor galleryCursor;
	public static boolean PICTURE_ASSET = true;
	private static int FIRST_PICTURE = R.drawable.image_001;
	private static int COUNT_PICTURE = 5;
	public static ArrayList<myPicture> galleryUri;
	private GridView gridPhotos;
	private AdapterGridPhotos adapterGridPhotos;
	private ActionMode mMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		gridPhotos = (GridView) findViewById(R.id.grid_photos);
		gridPhotos.setOnItemClickListener(this);
		gridPhotos.setOnItemLongClickListener(this);
		mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}

		new LoadPhotoAlbumTask(this).execute();

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		registerReceiver(broadcastReceiver, new IntentFilter(
				AppConstants.LOAD_GALLERY_ACTION));

	}

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("FANCY", "RECEIVER");
			new LoadPhotoAlbumTask(context).execute();
		}
	};

	/**
	 * launch image capture intent
	 * 
	 * @param actionCode
	 */
	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		File file = null;
		try {
			file = setUpPhotoFile();
			mCurrentPhotoPath = file.getAbsolutePath();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(file));
		} catch (IOException e) {
			e.printStackTrace();
			file = null;
			mCurrentPhotoPath = null;
		}
		startActivityForResult(takePictureIntent, actionCode);
	}

	private void handleCameraPhoto(Intent intent) {
		if (mCurrentPhotoPath != null) {
			galleryAddPic();
			mCurrentPhotoPath = null;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					sendBroadcast(new Intent(AppConstants.LOAD_GALLERY_ACTION));
				};
				// wait 3 sec to load again the gallery
			}, 3000);
		}

	}

	/**
	 * Photo album for this application
	 **/
	private String getAlbumName() {
		return getString(R.string.app_name);
	}

	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {

			storageDir = mAlbumStorageDirFactory
					.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (!storageDir.mkdirs()) {
					if (!storageDir.exists()) {
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} else {
			Log.v(getString(R.string.app_name),
					"External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX,
				albumF);
		return imageF;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		if (PICTURE_ASSET) {
			getSupportMenuInflater().inflate(R.menu.activity_main_internal,
					menu);
		} else {
			getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	private File setUpPhotoFile() throws IOException {

		File file = createImageFile();
		mCurrentPhotoPath = file.getAbsolutePath();
		return file;
	}

	/**
	 * add picture to gallery from file
	 */
	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				"android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		HomeActivity.this.sendBroadcast(mediaScanIntent);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO:
			if (resultCode == RESULT_OK) {
				handleCameraPhoto(data);
				AppMsg.makeText(HomeActivity.this,
						"Picture Captured sucesfully", AppMsg.STYLE_CONFIRM)
						.show();

			}
			if (resultCode == RESULT_CANCELED) {
				AppMsg.makeText(HomeActivity.this,
						"Picture Captured was canceled", AppMsg.STYLE_ALERT)
						.show();

			}

			break;

		default:
			break;
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_about) {
			startActivity(new Intent(HomeActivity.this, AboutActivity.class));
		}
		if (item.getItemId() == R.id.action_take_photo) {

			dispatchTakePictureIntent(ACTION_TAKE_PHOTO);

		}
		if (item.getItemId() == R.id.action_refresh) {

			new LoadPhotoAlbumTask(this).execute();

		}
		return super.onOptionsItemSelected(item);

	}

	class LoadPhotoAlbumTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog mProgressDialog;
		private final Context mContext;

		public LoadPhotoAlbumTask(Context context) {
			mContext = context;
		}

		@Override
		protected void onPreExecute() {

			super.onPreExecute();
			mProgressDialog = new ProgressDialog(HomeActivity.this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMessage("Loading pictures ...");
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean isThereAnError = false;
			if (isCancelled()) {
				HomeActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
						}
					}
				});

			}
			try {
				galleryUri = new ArrayList<myPicture>();
				if (PICTURE_ASSET) {

					for (int i = 0; i < COUNT_PICTURE; i++) {
						String fileName = "android.resource://"
								+ mContext.getPackageName() + "/"
								+ (FIRST_PICTURE + i);

						galleryUri.add(new myPicture(Uri.parse(fileName),
								getResources().getResourceEntryName(
										(FIRST_PICTURE + i))));
					}
				} else {
				galleryCursor = MediaStore.Images.Media.query(
						getContentResolver(),
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
						null, "date_added DESC");
					for (int i = 0; i < galleryCursor.getCount(); i++) {
						galleryUri.add(new myPicture( imageUri(i),galleryCursor.getString(3)));
					}
				
				}
				adapterGridPhotos = new AdapterGridPhotos(mContext,
						mLayoutInflater);

			} catch (Exception e) {
				isThereAnError = true;
			}

			return isThereAnError;
		}

		@Override
		protected void onPostExecute(Boolean isThereAnError) {
			// TODO Auto-generated method stub
			super.onPostExecute(isThereAnError);
			mProgressDialog.dismiss();
			if (isThereAnError) {
				AppMsg.makeText(HomeActivity.this,
						"Error loading gallery ,try again", AppMsg.STYLE_ALERT)
						.show();
			} else {

				gridPhotos.setAdapter(adapterGridPhotos);
			


				AppMsg.makeText(
						HomeActivity.this,
						"Gallery Loaded " + galleryUri.size()
								+ " pictures", AppMsg.STYLE_INFO).show();
			}
		}

		private Uri imageUri(int position) {

			galleryCursor.moveToPosition(position);
			int _id = galleryCursor.getInt(0);
				Uri imageUri = Uri.withAppendedPath(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + _id);

				return imageUri;

		}
	}

	class AdapterGridPhotos extends BaseAdapter {

		class ViewHolder {
			ImageView imgPhoto;
			TextView labName;
		}

		LayoutInflater mInflater;
		private final Context mContext;



		public AdapterGridPhotos(Context context, LayoutInflater inflater) {
			mContext = context;
			mInflater = inflater;
		}

		@Override
		public int getCount() {
				return galleryUri.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {

				return position;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {

			ViewHolder holder;
			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
			Point diplaySize = new Point();
			display.getSize(diplaySize);
			int size = diplaySize.x > diplaySize.y ? diplaySize.y / 2
					: diplaySize.x / 2;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.thumbnail_view, null);
				holder = new ViewHolder();
				holder.imgPhoto = (ImageView) convertView
						.findViewById(R.id.img_thumbnail);
				holder.labName = (TextView) convertView
						.findViewById(R.id.lab_dysplay_name);

				convertView
.setLayoutParams(new GridView.LayoutParams(size,
						size));
				convertView.setTag(holder);
			}
			holder = (ViewHolder) convertView.getTag();


				try {
				myPicture picture = galleryUri.get(position);
				Uri uri = picture.getUri();
					Bitmap bitmap = Util.decodeUri(mContext,
 uri, size, size);
					holder.imgPhoto.setImageBitmap(bitmap);
				holder.labName.setText(picture.getName());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			// set dysplay name


			return convertView;
		}

	}

	private final class AnActionModeOfEpicProportions implements
			ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Used to put dark icons on light action bar

			getSupportMenuInflater().inflate(R.menu.action_mode_grid, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Toast.makeText(HomeActivity.this, "Got click: " + item,
					Toast.LENGTH_SHORT).show();
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mMode = null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position,
			long id) {
		if (mMode == null) {
			Intent intentDetail = new Intent(HomeActivity.this,
					ImageDetailsActivity.class);
			intentDetail.putExtra(_ID_KEY,
 position);
			startActivity(intentDetail);
		} else {
			if (view.isSelected()) {
				view.setSelected(false);
			} else {
				view.setSelected(true);
			}
		}

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View view,
			int position, long id) {
		if (mMode == null) {
			mMode = startActionMode(new AnActionModeOfEpicProportions());
		}
		mMode.setTitle("" + position);
		if (view.isSelected()) {
			view.setSelected(false);
		} else {
			view.setSelected(true);
		}
		return true;

	}

}
