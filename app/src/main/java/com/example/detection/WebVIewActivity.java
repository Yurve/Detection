package com.example.detection;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class WebVIewActivity extends AppCompatActivity {
    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.webView);
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        WebSettings webSettings = webView.getSettings();
        //웹뷰에서 재생가능한 콘텐츠를 자동으로 재생할 수 있도록 설정
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        //웹뷰의 오디오 재생을 지원
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        //자바 스크립트를 쓸수 있게
        webSettings.setJavaScriptEnabled(true);
        // HTML 5 사양의 일부
        webSettings.setDomStorageEnabled(true);


        //웹뷰로 띄운 웹 페이지를 컨트롤하는 함수, 크롬에 맞춰  쾌적한 환경조성을 위한 세팅으로 보면 된다.
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // 권한 요청 승인 처리 코드
                request.grant(request.getResources());
            }
        });

        webView.loadUrl(url);
    }

    // 안드로이드 내에서 특정 키를 누를 때 동작하는 메소드
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}