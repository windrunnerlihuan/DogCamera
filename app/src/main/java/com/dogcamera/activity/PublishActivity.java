package com.dogcamera.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.dogcamera.R;
import com.dogcamera.base.BaseActivity;
import com.dogcamera.utils.FileUtils;
import com.dogcamera.utils.ShareProvider;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;

public class PublishActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.publish_video_img)
    ImageView mVideoImg;
    @BindView(R.id.publish_video_path)
    TextView mVideoPathTxt;

    private String mVideoPath;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_publish;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        initValues();
        mVideoPathTxt.setText(mVideoPath);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "是否要继续拍摄?", Snackbar.LENGTH_LONG)
                .setAction("是", v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("dog://camera")))).show());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getVideoFirstFrame();
    }

    private void initValues() {
        mVideoPath = getIntent().getStringExtra("outPath");
    }

    private void getVideoFirstFrame() {
        Bitmap bm = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {

            retriever.setDataSource(mVideoPath);
            bm = retriever.getFrameAtTime();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            retriever.release();
        }
        if(bm != null){
            mVideoImg.setImageBitmap(bm);
        }
    }

    @OnClick(R.id.publish_video_img)
    void playVideo() {
        Uri uri = Uri.parse(mVideoPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/mp4");
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.publish_drawer_project) {
            Uri uri = Uri.parse("https://github.com/windrunnerlihuan/DogCamera");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            this.startActivity(intent);
        } else if (id == R.id.publish_drawer_question) {
            String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + "937874128";
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else if (id == R.id.publish_drawer_share) {
            new ShareProvider.Builder(this)
                    .setContentType(ShareProvider.ShareContentType.TEXT)
                    .setTextContent("狗头相机 https://github.com/windrunnerlihuan/DogCamera")
                    .setTitle("分享一款好玩的抖音功能相机app")
                    .build()
                    .shareBySystem();
        } else if (id == R.id.publish_drawer_blog) {
            Uri uri = Uri.parse("http://windrunnerlihuan.com");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            this.startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
