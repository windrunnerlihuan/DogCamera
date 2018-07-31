package com.dogcamera.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class ShareProvider {

    public static final String TAG = "ShareProvider";

    @StringDef({ShareContentType.TEXT, ShareContentType.IMAGE,
            ShareContentType.AUDIO, ShareContentType.VIDEO, ShareContentType.FILE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShareContentType {
        /**
         * Share Text
         */
        final String TEXT = "text/plain";

        /**
         * Share Image
         */
        final String IMAGE = "image/*";

        /**
         * Share Audio
         */
        final String AUDIO = "audio/*";

        /**
         * Share Video
         */
        final String VIDEO = "video/*";

        /**
         * Share File
         */
        final String FILE = "*/*";
    }

        /**
         * Current activity
         */
        private Activity activity;

        /**
         * Share content type
         */
        private @ShareContentType String contentType;

        /**
         * Share title
         */
        private String title;

        /**
         * Share file Uri
         */
        private Uri shareFileUri;

        /**
         * Share content text
         */
        private String contentText;

        /**
         * Share to special component PackageName
         */
        private String componentPackageName;

        /**
         * Share to special component ClassName
         */
        private String componentClassName;

        /**
         * Share complete onActivityResult requestCode
         */
        private int requestCode;

        /**
         * Forced Use System Chooser
         */
        private boolean forcedUseSystemChooser;

        private ShareProvider(@NonNull Builder builder) {
            this.activity = builder.activity;
            this.contentType = builder.contentType;
            this.title = builder.title;
            this.shareFileUri = builder.shareFileUri;
            this.contentText = builder.textContent;
            this.componentPackageName = builder.componentPackageName;
            this.componentClassName = builder.componentClassName;
            this.requestCode = builder.requestCode;
            this.forcedUseSystemChooser = builder.forcedUseSystemChooser;
        }

        /**
         * shareBySystem
         */
        public void shareBySystem () {
            if (checkShareParam()) {
                Intent shareIntent = createShareIntent();

                if (shareIntent == null) {
                    Log.e(TAG, "shareBySystem cancel.");
                    return;
                }

                if (title == null) {
                    title = "";
                }

                if (forcedUseSystemChooser) {
                    shareIntent = Intent.createChooser(shareIntent, title);
                }

                if (shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                    try {
                        if (requestCode != -1) {
                            activity.startActivityForResult(shareIntent, requestCode);
                        } else {
                            activity.startActivity(shareIntent);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            }
        }

        private Intent createShareIntent() {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.addCategory("android.intent.category.DEFAULT");

            if (!TextUtils.isEmpty(this.componentPackageName) && !TextUtils.isEmpty(componentClassName)){
                ComponentName comp = new ComponentName(componentPackageName, componentClassName);
                shareIntent.setComponent(comp);
            }

            switch (contentType) {
                case ShareContentType.TEXT :
                    shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);
                    shareIntent.setType("text/plain");
                    break;
                case ShareContentType.IMAGE :
                case ShareContentType.AUDIO :
                case ShareContentType.VIDEO :
                case ShareContentType.FILE:
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addCategory("android.intent.category.DEFAULT");
                    shareIntent.setType(contentType);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    Log.d(TAG, "Share uri: " + shareFileUri.toString());

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            activity.grantUriPermission(packageName, shareFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                    break;
                default:
                    Log.e(TAG, contentType + " is not support share type.");
                    shareIntent = null;
                    break;
            }

            return shareIntent;
        }


        private boolean checkShareParam() {
            if (this.activity == null) {
                Log.e(TAG, "activity is null.");
                return false;
            }

            if (TextUtils.isEmpty(this.contentType)) {
                Log.e(TAG, "Share content type is empty.");
                return false;
            }

            if (ShareContentType.TEXT.equals(contentType)) {
                if (TextUtils.isEmpty(contentText)) {
                    Log.e(TAG, "Share text context is empty.");
                    return false;
                }
            } else {
                if (this.shareFileUri == null) {
                    Log.e(TAG, "Share file path is null.");
                    return false;
                }
            }

            return true;
        }

        public static class Builder {
            private Activity activity;
            private @ShareContentType String contentType = ShareContentType.FILE;
            private String title;
            private String componentPackageName;
            private String componentClassName;
            private Uri shareFileUri;
            private String textContent;
            private int requestCode = -1;
            private boolean forcedUseSystemChooser = true;

            public Builder(Activity activity) {
                this.activity = activity;
            }

            /**
             * Set Content Type
             * @param contentType {@link ShareContentType}
             * @return Builder
             */
            public Builder setContentType(@ShareContentType String contentType) {
                this.contentType = contentType;
                return this;
            }

            /**
             * Set Title
             * @param title title
             * @return Builder
             */
            public Builder setTitle(@NonNull String title) {
                this.title = title;
                return this;
            }

            /**
             * Set share file path
             * @param shareFileUri shareFileUri
             * @return Builder
             */
            public Builder setShareFileUri(Uri shareFileUri) {
                this.shareFileUri = shareFileUri;
                return this;
            }

            /**
             * Set text content
             * @param textContent  textContent
             * @return Builder
             */
            public Builder setTextContent(String textContent) {
                this.textContent = textContent;
                return this;
            }

            /**
             * Set Share To Component
             * @param componentPackageName componentPackageName
             * @param componentClassName componentPackageName
             * @return Builder
             */
            public Builder setShareToComponent(String componentPackageName, String componentClassName) {
                this.componentPackageName = componentPackageName;
                this.componentClassName = componentClassName;
                return this;
            }

            /**
             * Set onActivityResult requestCode, default value is -1
             * @param requestCode requestCode
             * @return Builder
             */
            public Builder setOnActivityResult (int requestCode) {
                this.requestCode = requestCode;
                return this;
            }

            /**
             * Forced Use System Chooser To Share
             * @param enable default is true
             * @return Builder
             */
            public Builder forcedUseSystemChooser (boolean enable) {
                this.forcedUseSystemChooser = enable;
                return this;
            }

            /**
             * build
             * @return ShareProvider
             */
            public ShareProvider build() {
                return new ShareProvider(this);
            }

        }

    /**
     * Get file uri
     *
     * @param context          context
     * @param shareContentType shareContentType {@link ShareContentType}
     * @param file             file
     * @return Uri
     */
    public static Uri getFileUri(Context context, @ShareContentType String shareContentType, File file) {

        if (context == null) {
            Log.e(TAG, "getFileUri current activity is null.");
            return null;
        }

        if (file == null || !file.exists()) {
            Log.e(TAG, "getFileUri file is null or not exists.");
            return null;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getFileUri miss WRITE_EXTERNAL_STORAGE permission.");
            return null;
        }

        Uri uri = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {

            if (TextUtils.isEmpty(shareContentType)) {
                shareContentType = "*/*";
            }

            switch (shareContentType) {
                case ShareContentType.IMAGE:
                    uri = FileUtils.getImageContentUri(context, file);
                    break;
                case ShareContentType.VIDEO:
                    uri = FileUtils.getVideoContentUri(context, file);
                    break;
                case ShareContentType.AUDIO:
                    uri = FileUtils.getAudioContentUri(context, file);
                    break;
                case ShareContentType.FILE:
                    uri = FileUtils.getFileContentUri(context, file);
                    break;
                default:
                    break;
            }
        }

        if (uri == null) {
            uri = FileUtils.forceGetFileUri(file);
        }

        return uri;
    }

}
