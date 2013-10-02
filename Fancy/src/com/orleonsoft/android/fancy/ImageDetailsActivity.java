package com.orleonsoft.android.fancy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.devspark.appmsg.AppMsg;
import com.orleonsoft.android.fancy.util.Util;
import com.orleonsoft.android.fancy.views.MyViewPager;



/**
 * File: ImageDetailActivity.java Autor: Yesid Lazaro Mayoriano Modified :
 * Alexis Lecanu
 */

public class ImageDetailsActivity extends SherlockActivity {

	private static ShareActionProvider mShareActionProvider;
	private static HashMap<Uri, Integer> imageUriRotated;

	private Intent broadcastIntent;

	private static ViewPager mViewPager;
	private SamplePagerAdapter mPageAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		  getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		  ColorDrawable color = new ColorDrawable(Color.BLACK);
		  color.setAlpha(20);
		  getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		  

		imageUriRotated = new HashMap<Uri, Integer>();
		broadcastIntent = new Intent(AppConstants.LOAD_GALLERY_ACTION);
		int _idImage = 0;
		if (!getIntent().getExtras().isEmpty()
				|| getIntent().getExtras() != null) {
			try {
				_idImage = getIntent().getExtras()
.getInt(HomeActivity._ID_KEY);




			} catch (Exception e) {
				AppMsg.makeText(ImageDetailsActivity.this,
						"Error loading image ", AppMsg.STYLE_ALERT,
						R.layout.app_msg_detail).show();
			}

		}

		mViewPager = new MyViewPager(this);
		setContentView(mViewPager);
		mPageAdapter = new SamplePagerAdapter(this);
		mViewPager.setAdapter(mPageAdapter);


		mViewPager.setCurrentItem((_idImage));

	}



	@Override
	public void onResume() {
		super.onResume();
		getSupportActionBar().show();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		if (HomeActivity.PICTURE_ASSET) {
			getSupportMenuInflater().inflate(
					R.menu.activity_image_details_internal, menu);
		} else {
			getSupportMenuInflater().inflate(R.menu.activity_image_details,
					menu);
		}
		MenuItem actionItem = menu.findItem(R.id.action_share);
		mShareActionProvider = (ShareActionProvider) actionItem
				.getActionProvider();
		mShareActionProvider
				.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		mShareActionProvider.setShareIntent(createShareIntent(this));
		return true;
	}


	private void setWallpaper() {
		try {
			WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
			// import non-scaled bitmap wallpaper
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;

			Uri uriImage = HomeActivity.galleryUri.get(mViewPager
.getCurrentItem()).getUri();
			int rotate = 0;
			if (imageUriRotated.containsKey(uriImage)) {
				rotate = imageUriRotated.get(uriImage);
			}

			Bitmap wallpaper = Util.decodeUri(this, uriImage);
			wallpaper = Util.rotateBitmap(wallpaper, rotate, false);

			;

			if (wallpaperManager.getDesiredMinimumWidth() >= wallpaper
					.getWidth()
					&& wallpaperManager.getDesiredMinimumHeight() >= wallpaper
							.getHeight()) {
				// add padding to wallpaper so background image scales correctly
				int xPadding = Math.max(0, wallpaperManager.getDesiredMinimumWidth() - wallpaper.getWidth()) / 2;
				Bitmap paddedWallpaper = Bitmap.createBitmap(wallpaperManager.getDesiredMinimumWidth(), wallpaper.getHeight(),
						Bitmap.Config.ARGB_8888);
				int[] pixels = new int[wallpaper.getWidth() * wallpaper.getHeight()];
				wallpaper.getPixels(pixels, 0, wallpaper.getWidth(), 0, 0, wallpaper.getWidth(), wallpaper.getHeight());
				paddedWallpaper.setPixels(pixels, 0, wallpaper.getWidth(), xPadding, 0, wallpaper.getWidth(), wallpaper.getHeight());

				wallpaperManager.setBitmap(paddedWallpaper);
			} else {
				wallpaperManager.setBitmap(wallpaper);
			}
		} catch (IOException e) {

		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case android.R.id.home:
			goHome();

			break;
		case R.id.action_delete_img:
			new DeleteImageTask().execute();
			break;
		case R.id.action_rotate_right:
			rotateBitmap(90);
			break;

		case R.id.action_rotate_left:
			rotateBitmap(-90);
			break;
		case R.id.action_set_wallpaper:
			setWallpaper();
			Toast.makeText(ImageDetailsActivity.this,
					getString(R.string.succes_wallpaper), Toast.LENGTH_SHORT)
					.show();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void goHome() {
		Intent intent = new Intent(ImageDetailsActivity.this, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}

	public boolean deleteImage(Uri uriImage) {
		boolean result = false;
		
		int numRows = getContentResolver().delete(
uriImage, null,
				null);
		if (numRows == 1) {
			lauchFileScan();
			result = true;
		}
		return result;

	}

	private void lauchFileScan() {
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		mediaScanIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		ImageDetailsActivity.this.sendBroadcast(mediaScanIntent);
	}

	private static Intent createShareIntent(Context context) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("image/*");
		shareIntent.putExtra(Intent.EXTRA_STREAM,
				HomeActivity.galleryUri.get(mViewPager.getCurrentItem()).getUri());
		return shareIntent;
	}

	public void rotateBitmap(int grados) {

		Uri imageUriRotate = HomeActivity.galleryUri.get(mViewPager
.getCurrentItem()).getUri();
		int rotate = 0;
		if (imageUriRotated.containsKey(imageUriRotate)) {
			rotate = imageUriRotated.get(imageUriRotate);
		}
		imageUriRotated.put(imageUriRotate, grados + rotate);
		mPageAdapter.notifyDataSetChanged();
	}



	class DeleteImageTask extends AsyncTask<Void, Void, Boolean> {
		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			progressDialog = new ProgressDialog(ImageDetailsActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(false);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage("deleting image ...");
			progressDialog.show();

		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO Auto-generated method stub
			Boolean isThereError = false;
			if (isCancelled()) {
				ImageDetailsActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (progressDialog != null) {
							progressDialog.dismiss();
						}
					}
				});

			}

			try {
				if (!deleteImage(HomeActivity.galleryUri.get(
						mViewPager.getCurrentItem()).getUri())) {
					isThereError = true;
				}
			} catch (Exception e) {
				isThereError = true;
			}

			return isThereError;

		}

		@Override
		protected void onPostExecute(Boolean isThereError) {
			// TODO Auto-generated method stub
			super.onPostExecute(isThereError);
			progressDialog.dismiss();


			if (isThereError) {
				AppMsg.makeText(ImageDetailsActivity.this,
						"Error deleting image try again", AppMsg.STYLE_ALERT,
						R.layout.app_msg_detail).show();
			} else {
				goHome();
				sendBroadcast(broadcastIntent);
			}

		}

	}




	static class SamplePagerAdapter extends PagerAdapter {
		private final Context mContext;
		PhotoViewAttacher photoViewAttacher;

		public SamplePagerAdapter(Context context) {
			mContext = context;
		}
		@Override
		public int getCount() {

			return HomeActivity.galleryUri.size();

		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			LayoutInflater inflater = (LayoutInflater) container.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View view = inflater.inflate(R.layout.activity_image_details, null);

			PhotoView imageViewPhoto = (PhotoView) view
					.findViewById(R.id.img_photo);

			


				imageViewPhoto.setVisibility(View.VISIBLE);
			Uri uriImage = HomeActivity.galleryUri.get(position).getUri();
				int rotate =0;
				if(imageUriRotated.containsKey(uriImage)){
					rotate = imageUriRotated.get(uriImage);
				}

			try {
				Bitmap bitmap = Util.decodeUri(mContext, uriImage);
				bitmap = Util.rotateBitmap(bitmap, rotate, false);
				imageViewPhoto.setImageBitmap(bitmap);

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			((ViewPager) container).addView(view, 0);
			return view;

		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public void finishUpdate(ViewGroup container) {
			super.finishUpdate(container);
			if (mShareActionProvider != null) {
				mShareActionProvider.setShareIntent(createShareIntent(mContext));
			}
		}
	}



}
