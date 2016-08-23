package com.mifind.gankio.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.mifind.gankio.R;
import com.mifind.gankio.model.GankModel;
import com.mifind.gankio.onekeyshare.OnekeyShare;
import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import me.xiaopan.android.widget.ToastUtils;

/**
 * Created by Xuanjiawei on 2016/8/11.
 */
public class WebViewActivity extends BaseActivity {
    private final static int FINISH_ACTIVITY = 0;
    private final static int REQUEST_UPLOAD_FILE_CODE = 2;
    private ValueCallback<Uri> mUploadFile;
    private final String TAG = this.getClass().getSimpleName();
    private Handler handler = new MyHandler(this);
    private GankModel mGankModel;
    private String title, url;
    @Bind(R.id.webView)
    WebView mWebView;
    @Bind(R.id.mToolbar)
    Toolbar mToolbar;
    @Bind(R.id.toolbar_title)
    TextView mToolbarTitle;
    @Bind(R.id.web_fab)
    FloatingActionButton mFloatingActionButton;

    @Override
    public void initParms(Bundle parms) {
        mGankModel = (GankModel) parms.getSerializable("model");
        if (mGankModel != null) {
            title = mGankModel.getDesc();
            url = mGankModel.getUrl();
        } else {
            url = getIntent().getStringExtra("url");
        }
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                Window.PROGRESS_VISIBILITY_ON);
    }

    @Override
    public View bindView() {
        return null;
    }

    @Override
    public int bindLayout() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_webview;
    }

    @Override
    public void initView(View view) {
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setListener() {
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showShare();
            }
        });
    }

    @Override
    public void doBusiness(Context mContext) {
        //设置webview的settings和client
        configWebview();
        // 加载 页面
        loadURL();
    }

    private void loadURL() {
        try {
            mWebView.loadUrl(url);
        } catch (Exception e) {
            Logger.i(e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        mWebView.destroy();
        super.onDestroy();
    }

    private void configWebview() {
        // 允许javascript代码执行
        WebSettings settings = mWebView.getSettings();
        //告诉WebView先不要自动加载图片，等页面finish后再发起图片加载。
        if (Build.VERSION.SDK_INT >= 19) {
            settings.setLoadsImagesAutomatically(true);
        } else {
            settings.setLoadsImagesAutomatically(false);
        }
        settings.setJavaScriptEnabled(true); //支持js
        settings.setUseWideViewPort(true);//将图片调整到适合webview的大小
        settings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        // settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //关闭webview中缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT); //设置 缓存模式
        settings.setDomStorageEnabled(true);// 开启 DOM storage API 功能
        settings.setAppCacheMaxSize(8 * 1024 * 1024);
        settings.setRenderPriority(RenderPriority.HIGH);
        settings.setAppCacheEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);//支持通过JS打开新窗口
        settings.setAllowFileAccess(true); //设置可以访问文件
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setLoadsImagesAutomatically(true); //支持自动加载图片
        settings.setDefaultTextEncodingName("utf-8");//设置编码格式
        // 在当前页面打开链接，而不是启动用户手机上安装的浏览器打开
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mWebView.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                //TODO 这里对错误页面进行统一处理
                ToastUtils.toastS(mContext,description);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mWebView.getSettings().getLoadsImagesAutomatically()) {
                    mWebView.getSettings().setLoadsImagesAutomatically(true);
                }
            }
        });
        //WebChromeClient是辅助WebView处理Javascript的对话框，网站图标，网站title，加载进度等 :
        mWebView.setWebChromeClient(new WebChromeClient() {
            // 使webview可以更新进度条
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mToolbar.setTitle("加载中… " + newProgress + "%");
                if (newProgress == 100)
                    setActionbarTitle();
            }


            //使JS alert可以以Android的AlertDiaolog形式弹出
            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this)
                        .setMessage(message).setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                });
                builder.setCancelable(true);
                builder.show();
                return true;
            }

            //html中上传点击input type为file的控件时会调用下列方法，在Android4.4中无效，待修复
            // Android 4.1+
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadFile,
                                        String acceptType, String capture) {
                openFileChooser(uploadFile);
            }

            // Android 3.0 +
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadFile,
                                        String acceptType) {
                openFileChooser(uploadFile);
            }

            // Android 3.0
            public void openFileChooser(ValueCallback<Uri> uploadFile) {
                mUploadFile = uploadFile;
                startActivityForResult(createCameraIntent(),
                        REQUEST_UPLOAD_FILE_CODE);

            }
        });
        // 在js中用window.injs.方法名来调用InJavaScript类中的方法
        mWebView.addJavascriptInterface(new InJavaScript(), "android");
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    }

    private void setActionbarTitle() {
        if (!TextUtils.isEmpty(title)) {
            mToolbarTitle.setText(title);
            mToolbar.setTitle("");
        } else {
            mToolbar.setTitle("干货");
        }
    }


    private Intent createCameraIntent() {
        Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);// 选择图片文件
        imageIntent.setType("image/*");
        return imageIntent;
    }

    // 使后退键可以达到网页回退功能，而不是关闭activity
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * 给javascript调用的代码
     */
    private final class InJavaScript {
        //可以用JS关闭本Activity
        @android.webkit.JavascriptInterface
        public void finish() {
            handler.sendEmptyMessage(FINISH_ACTIVITY);
        }

        //可以用JS触发一个分享文本信息的intent
        @android.webkit.JavascriptInterface
        public void sharelink(String link) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "分享");
            i.putExtra(Intent.EXTRA_TEXT, "share this:" + link);
            startActivity(Intent.createChooser(i, "请选择分享方式"));
        }
    }

    /*
     * (non-Javadoc)左上角回退可以结束本activity,另有前进、后退、刷新
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home://关闭页面
                finish();
                break;
//            case R.id.menu_webview_refresh://刷新
//                webview.reload();
//                break;
//            case R.id.menu_webview_back://后退
//                if (webview.canGoBack())
//                    webview.goBack();
//                break;
//            case R.id.menu_webview_forward://前进
//                if (webview.canGoForward())
//                    webview.goForward();
//                break;break
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //选择文件后回调给JS一个URI
        if (requestCode == REQUEST_UPLOAD_FILE_CODE && resultCode == RESULT_OK) {
            if (null == mUploadFile)
                return;
            Uri result = (null == data) ? null : data.getData();//注，此处data.getData(),若为data则仅是contentProvider的地址将不能为JS识别
            if (null != result) {
                mUploadFile.onReceiveValue(result);
                mUploadFile = null;
            }
            //如果用户取消了选择文件操作，必须回调一个null的URI给JS，否则webview将会死掉
        } else if (requestCode == REQUEST_UPLOAD_FILE_CODE && resultCode == RESULT_CANCELED) {
            Uri result = null;
            mUploadFile.onReceiveValue(result);
            mUploadFile = null;
        }

    }

    //用来处理UI操作的handler，可扩展，暂无太大用处……
    private static class MyHandler extends Handler {
        WeakReference<WebViewActivity> weakReference;

        public MyHandler(WebViewActivity webViewActivity) {
            weakReference = new WeakReference<WebViewActivity>(webViewActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FINISH_ACTIVITY:
                    weakReference.get().finish();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }



    private void showShare() {
        ShareSDK.initSDK(this);
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        // 分享时Notification的图标和文字  2.5.9以后的版本不调用此方法
        //oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
        // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
//        oks.setTitle(getString(R.string.share));
        // titleUrl是标题的网络链接，仅在人人网和QQ空间使用
//        oks.setTitleUrl("http://sharesdk.cn");
        // text是分享文本，所有平台都需要这个字段
        oks.setText("我是分享文本");
        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
        //oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片
        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl("http://sharesdk.cn");
        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        oks.setComment("我是测试评论文本");
        // site是分享此内容的网站名称，仅在QQ空间使用
//        oks.setSite(getString(R.string.app_name));
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
//        oks.setSiteUrl("http://sharesdk.cn");

// 启动分享GUI
        oks.show(this);
    }
}
