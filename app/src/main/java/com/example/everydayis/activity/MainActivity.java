package com.example.everydayis.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_title, tv_author, tv_wc;
    private WebView wv;
    //定义WebView内容两边对齐样式
    private static final String WEBVIEW_CONTENT = "<html><head></head><body style=\"text-align:justify;margin:10;text-indent:2em;\">%s</body></html>";
    private RadioButton rb_random, rb_curr, rb_next, rb_prev;
    SharedPreferences sprfMain;
    private String prev, next;

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
                        JSONObject obj = new JSONObject(data);
                        JSONObject obj2 = new JSONObject(obj.getString("data"));
                        tv_title.setText(obj2.getString("title"));
                        tv_author.setText(obj2.getString("author"));
                        tv_wc.setText("字数：" + obj2.getString("wc"));
                        wv.loadDataWithBaseURL(null, String.format(WEBVIEW_CONTENT, obj2.getString("content")), "text/html", "utf-8", null);
                        JSONObject obj3 = new JSONObject(obj2.getString("date"));
                        prev = obj3.getString("prev");//前一天日期
                        next = obj3.getString("next");//后一天日期
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        //rb_next.setVisibility(View.GONE);
    }

    private void initView() {
        tv_title = findViewById(R.id.tv_title);//标题
        tv_author = findViewById(R.id.tv_author);//作者
        tv_wc = findViewById(R.id.tv_wc);//字数
        wv = findViewById(R.id.wv);//正文
        rb_random = findViewById(R.id.rb_random);//随机
        rb_prev = findViewById(R.id.rb_prev);//前一天
        rb_curr = findViewById(R.id.rb_curr);//今日
        rb_next = findViewById(R.id.rb_next);//后一天
        rb_random.setOnClickListener(this);
        rb_curr.setOnClickListener(this);
        rb_prev.setOnClickListener(this);
        rb_next.setOnClickListener(this);
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
}
