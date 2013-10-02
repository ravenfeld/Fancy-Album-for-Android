package com.orleonsoft.android.fancy;

import android.net.Uri;

public class myPicture {
	private final String mName;
	private final Uri mUri;

	public myPicture(Uri uri, String name) {
		mUri = uri;
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public Uri getUri() {
		return mUri;
	}

}
