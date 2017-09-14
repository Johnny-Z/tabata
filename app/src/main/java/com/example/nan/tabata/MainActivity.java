package com.example.nan.tabata;
/**
 * created by johnny zhao
 */
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText exerciseET;
    private EditText restET;
    private Button startBT;
    private Button stopBT;
    private Chronometer usedCHR;
    private CountDownTimer execCDT;
    private ImageView numIMG;
    private ImageView bingPicIMG;
    private int iExec,iRest;
    private long iCount;
    public SoundPool pool;
    private int soundId,soundId1;
    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {//背景图和状态栏融合到一起
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.tobata_main);
        
        exerciseET= (EditText) findViewById(R.id.et_exercise);
        restET= (EditText) findViewById(R.id.et_rest);
        startBT= (Button) findViewById(R.id.bt_starttime);
        stopBT= (Button) findViewById(R.id.bt_stoptime);
        usedCHR= (Chronometer) findViewById(R.id.chr_used);
        bingPicIMG= (ImageView) findViewById(R.id.bing_pic_img);
        
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicIMG);
        } else {
            loadBingPic();//加载bing每日一图
        }
        pool = new SoundPool.Builder().setMaxStreams(20).build();
        soundId = pool.load(this, R.raw.dog, 1);
        soundId1 = pool.load(this, R.raw.countdown, 2);
        
        startBT.setOnClickListener(this);
        stopBT.setOnClickListener(this);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadBingPic();
            }
        });
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";

        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "刷新背景图失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(MainActivity.this).load(bingPic).into(bingPicIMG);
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_starttime:
                //use dialog show pic
                final Dialog dia = new Dialog(MainActivity.this, R.style.MyCommonDialog);
                dia.setContentView(R.layout.dialog_custom);

//                drawerLayout.openDrawer(GravityCompat.START);
                
                numIMG = (ImageView) dia.findViewById(R.id.iv_num);
                numIMG.setBackgroundResource(R.drawable.three);//加载dialog的方式实现倒计时动画
                dia.show();
                pool.play(soundId1, 1.0f, 1.0f, 0, 0, 1.0f);//倒计时音效
                //3秒倒计时dialog停留1秒以后自动消失
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dia.dismiss();
                        numIMG.setBackgroundResource(R.drawable.two);
                        dia.show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dia.dismiss();
                                numIMG.setBackgroundResource(R.drawable.one);
                                dia.show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        dia.dismiss();
                                        start();
                                    }
                                },1000);
                            }
                        },1000);
                    }
                },1000);
                break;
            case R.id.bt_stoptime:
                stop();
                break;
            default:
                break;
        }
    }

    private void start() {
        //开始计时
        usedCHR.setBase(SystemClock.elapsedRealtime());
        usedCHR.start();
        exerciseET.setCursorVisible(false);//去掉闪烁光标
        restET.setCursorVisible(false);
        //开始倒计时
        countDownStart(10);
    }

    private void stop() {
        usedCHR.stop();
        countDownStop();
    }
    
    private void countDownStart(int num) {
        if (execCDT==null) {
            if (exerciseET.getText().toString().isEmpty()||restET.getText().toString().isEmpty()) {
                iRest = 15;
                iExec = 45;
            } else {
                iExec = Integer.parseInt(exerciseET.getText().toString());//读取输入内容
                iRest = Integer.parseInt(restET.getText().toString());

            }
            if ((iRest+iExec)<1) {
                exerciseET.setCursorVisible(true);
                restET.setCursorVisible(true);
            } else {
                execCDT = new CountDownTimer(((iExec + iRest) * 1000) * 1000, 1000) {//1000次循环
                    @Override
                    public void onTick(long l) {
                        iCount = (l / 1000) % (iExec + iRest);
                        if (iCount <iRest) {
                            restET.setText((iCount) + "");
                            if(iCount==1){//最后2秒发出提示音
                                pool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
                            }
                        } else {
                            exerciseET.setText((iCount - iRest) + "");
                            if(iCount==(1+iRest)){
                                pool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
                            }
                        }
                    }
                    @Override
                    public void onFinish() {
                        exerciseET.setText(iExec);
                        restET.setText(iRest);
                        exerciseET.setCursorVisible(true);
                        restET.setCursorVisible(true);
                    }
                };
            }
        }
        if(execCDT!=null) {
            execCDT.start();
        }
    }

    private void countDownStop() {
        if(execCDT!=null){
            execCDT.cancel();
            execCDT=null;//注销倒计时控件

            exerciseET.setCursorVisible(true);
            restET.setCursorVisible(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(execCDT!=null){
            execCDT.cancel();
            execCDT=null;
        }
        if (pool!=null) {
            pool.release();
            pool=null;//注销音效控件
        }
    }
}

