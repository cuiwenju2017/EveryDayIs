package com.example.everydayis.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.everydayis.R;
import com.example.everydayis.utils.HttpUtils;
import com.example.everydayis.utils.StreamUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnScrollChangeListener {

    private TextView tv_title, tv_author, tv_wc;
    private WebView wv;
    //定义WebView内容两边对齐样式
    private static final String WEBVIEW_CONTENT = "<html><head></head><body style=\"text-align:justify;margin:10;text-indent:2em;\">%s</body></html>";
    private static final String WEBVIEW_CONTENT_SMALL = "<html><head></head><body style=\"text-align:justify;margin:10;text-indent:2em; font-size: 12px;\">%s</body></html>";
    private static final String WEBVIEW_CONTENT_LARGE = "<html><head></head><body style=\"text-align:justify;margin:10;text-indent:2em; font-size: 20px;\">%s</body></html>";
    private RadioButton rb_random, rb_curr, rb_next, rb_prev, rb_set, small, center, large;
    private String prev, next, curr;
    private RelativeLayout rl;
    private boolean mBackKeyPressed = false;//记录是否有首次按键
    private Dialog dialog;
    private View inflate;
    private JSONObject obj, obj2;
    SharedPreferences sprfMain;
    SharedPreferences.Editor editorMain;

    private String data;
    private final static int TIME_OUT = 2000;//超时时间
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESULT_OK:
                    try {
                        //解析服务器端返回的数据
                        obj = new JSONObject(data);
                        obj2 = new JSONObject(obj.getString("data"));
                        tv_title.setText(obj2.getString("title"));
                        tv_author.setText(obj2.getString("author"));
                        tv_wc.setText("字数：" + obj2.getString("wc"));
                        sprfMain = getSharedPreferences("counter", Context.MODE_PRIVATE);
                        int size = sprfMain.getInt("size", 0);
                        if (size == 0) {//判断设置字体大小并设置
                            wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT_SMALL, obj2.getString("content")), "text/html", "utf-8", null);
                        } else if (size == 1) {
                            wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT, obj2.getString("content")), "text/html", "utf-8", null);
                        } else if (size == 2) {
                            wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT_LARGE, obj2.getString("content")), "text/html", "utf-8", null);
                        }
                        JSONObject obj3 = new JSONObject(obj2.getString("date"));
                        prev = obj3.getString("prev");//前一天日期
                        curr = obj3.getString("curr");//今日日期
                        next = obj3.getString("next");//后一天日期
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");// HH:mm:ss
                        //获取当前时间
                        Date date = new Date(System.currentTimeMillis());
                        if (curr.equals(simpleDateFormat.format(date))) {
                            rb_next.setVisibility(View.GONE);//隐藏后一天
                        } else {
                            rb_next.setVisibility(View.VISIBLE);//显示后一天
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(MainActivity.this, "服务器繁忙……", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        //获取控件
        tv_title = findViewById(R.id.tv_title);//标题
        tv_author = findViewById(R.id.tv_author);//作者
        tv_wc = findViewById(R.id.tv_wc);//字数
        wv = findViewById(R.id.wv);//正文
        rb_random = findViewById(R.id.rb_random);//随机
        rb_prev = findViewById(R.id.rb_prev);//前一天
        rb_curr = findViewById(R.id.rb_curr);//今日
        rb_next = findViewById(R.id.rb_next);//后一天
        rb_set = findViewById(R.id.rb_set);//设置
        rl = findViewById(R.id.rl);//作者布局
        //设置监听
        wv.setOnScrollChangeListener(this);
        rb_random.setOnClickListener(this);
        rb_curr.setOnClickListener(this);
        rb_prev.setOnClickListener(this);
        rb_next.setOnClickListener(this);
        rb_set.setOnClickListener(this);
    }

    private void initData() {
        new Thread(new Runnable() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                try {
                    @SuppressWarnings("deprecation")
                    String PATH = HttpUtils.host + "/today?dev=1";
                    URL url = new URL(PATH);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    //配置参数
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(TIME_OUT);
                    connection.setReadTimeout(TIME_OUT);
                    //打开链接
                    connection.connect();
                    //获取状态码
                    int responseCode = connection.getResponseCode();
                    if (200 == responseCode) {
                        //获取返回值
                        InputStream inputStream = connection.getInputStream();
                        //将字节流输入流转换为字符串
                        data = StreamUtils.inputSteam2String(inputStream);
                        handler.obtainMessage(RESULT_OK, data).sendToTarget();
                    } else {
                        handler.obtainMessage(RESULT_CANCELED, responseCode).sendToTarget();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.small://小字体
                try {
                    wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT_SMALL, obj2.getString("content")), "text/html", "utf-8", null);
                    sprfMain = getSharedPreferences("counter", Context.MODE_PRIVATE);
                    editorMain = sprfMain.edit();
                    editorMain.putInt("size", 0);
                    editorMain.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.center://中字体
                try {
                    wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT, obj2.getString("content")), "text/html", "utf-8", null);
                    sprfMain = getSharedPreferences("counter", Context.MODE_PRIVATE);
                    editorMain = sprfMain.edit();
                    editorMain.putInt("size", 1);
                    editorMain.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.large://大字体
                try {
                    wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT_LARGE, obj2.getString("content")), "text/html", "utf-8", null);
                    sprfMain = getSharedPreferences("counter", Context.MODE_PRIVATE);
                    editorMain = sprfMain.edit();
                    editorMain.putInt("size", 2);
                    editorMain.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.rb_set://设置
                dialog = new Dialog(this, R.style.ActionSheetDialogStyle);
                //填充对话框的布局
                inflate = LayoutInflater.from(this).inflate(R.layout.dialog, null);
                //获取控件
                small = inflate.findViewById(R.id.small);
                center = inflate.findViewById(R.id.center);
                large = inflate.findViewById(R.id.large);
                //获取监听
                small.setOnClickListener(this);
                center.setOnClickListener(this);
                large.setOnClickListener(this);
                //将布局设置给Dialog
                dialog.setContentView(inflate);
                //获取当前Activity所在的窗体
                Window dialogWindow = dialog.getWindow();
                //设置Dialog从窗体底部弹出
                dialogWindow.setGravity(Gravity.BOTTOM);
                //获得窗体的属性
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.y = 0;//设置Dialog距离底部的距离
                //宽度填满
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                //将属性设置给窗体
                dialogWindow.setAttributes(lp);
                dialog.show();//显示对话框
                break;
            case R.id.rb_prev://前一天
                new Thread(new Runnable() {
                    @SuppressLint("HandlerLeak")
                    @Override
                    public void run() {
                        try {
                            @SuppressWarnings("deprecation")
                            String PATH = HttpUtils.host + "/day?dev=1&date=" + prev;
                            URL url = new URL(PATH);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            //配置参数
                            connection.setRequestMethod("GET");
                            connection.setConnectTimeout(TIME_OUT);
                            connection.setReadTimeout(TIME_OUT);
                            //打开链接
                            connection.connect();
                            //获取状态码
                            int responseCode = connection.getResponseCode();
                            if (200 == responseCode) {
                                //获取返回值
                                InputStream inputStream = connection.getInputStream();
                                //将字节流输入流转换为字符串
                                data = StreamUtils.inputSteam2String(inputStream);
                                handler.obtainMessage(RESULT_OK, data).sendToTarget();
                            } else {
                                handler.obtainMessage(RESULT_CANCELED, responseCode).sendToTarget();
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        } catch (IOException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        }
                    }
                }).start();
                break;
            case R.id.rb_curr://今日
                initData();
                break;
            case R.id.rb_next://后一天
                new Thread(new Runnable() {
                    @SuppressLint("HandlerLeak")
                    @Override
                    public void run() {
                        try {
                            @SuppressWarnings("deprecation")
                            String PATH = HttpUtils.host + "/day?dev=1&date=" + next;
                            URL url = new URL(PATH);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            //配置参数
                            connection.setRequestMethod("GET");
                            connection.setConnectTimeout(TIME_OUT);
                            connection.setReadTimeout(TIME_OUT);
                            //打开链接
                            connection.connect();
                            //获取状态码
                            int responseCode = connection.getResponseCode();
                            if (200 == responseCode) {
                                //获取返回值
                                InputStream inputStream = connection.getInputStream();
                                //将字节流输入流转换为字符串
                                data = StreamUtils.inputSteam2String(inputStream);
                                handler.obtainMessage(RESULT_OK, data).sendToTarget();
                            } else {
                                handler.obtainMessage(RESULT_CANCELED, responseCode).sendToTarget();
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        } catch (IOException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        }
                    }
                }).start();
                break;
            case R.id.rb_random://随机
                new Thread(new Runnable() {
                    @SuppressLint("HandlerLeak")
                    @Override
                    public void run() {
                        try {
                            @SuppressWarnings("deprecation")
                            String PATH = HttpUtils.host + "/random?dev=1";
                            URL url = new URL(PATH);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            //配置参数
                            connection.setRequestMethod("GET");
                            connection.setConnectTimeout(TIME_OUT);
                            connection.setReadTimeout(TIME_OUT);
                            //打开链接
                            connection.connect();
                            //获取状态码
                            int responseCode = connection.getResponseCode();
                            if (200 == responseCode) {
                                //获取返回值
                                InputStream inputStream = connection.getInputStream();
                                //将字节流输入流转换为字符串
                                data = StreamUtils.inputSteam2String(inputStream);
                                handler.obtainMessage(RESULT_OK, data).sendToTarget();
                            } else {
                                handler.obtainMessage(RESULT_CANCELED, responseCode).sendToTarget();
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        } catch (IOException e) {
                            e.printStackTrace();
                            handler.obtainMessage(RESULT_CANCELED, e.getMessage()).sendToTarget();
                        }
                    }
                }).start();
                break;
            default:
                break;
        }
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (scrollY > 100) {
            rl.setVisibility(View.GONE);//隐藏作者
        } else if (scrollY == 0) {
            rl.setVisibility(View.VISIBLE);//显示作者
        }
    }

    //点击两次退出程序
    public void onBackPressed() {
        if (!mBackKeyPressed) {
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            mBackKeyPressed = true;
            new Timer().schedule(new TimerTask() {//延时两秒，如果超出则清除第一次记录

                @Override
                public void run() {
                    mBackKeyPressed = false;
                }
            }, 2000);
        } else {
            //结束Activity
            finish();
        }
    }

}
