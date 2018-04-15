package cn.edu.ldu.mysensor;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    private static final int JOB_ID = 1001;
    private static final int REQUEST_CODE = 2001;
    private TextView txtMonitor;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastTime=0;
    private float lastX,lastY,lastZ;
    public static final int SHAKE_SPEED=300;

    private TextToSpeech tts;
    private boolean ttsInitialized;

    private BroadcastReceiver mReciever=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            txtMonitor.append("收到了来自MyJobservice的数据，短信发送完毕！");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtMonitor=findViewById(R.id.txtMonitor);
        sensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        tts=new TextToSpeech(getApplicationContext(),this);
        checkPermissions();
    }
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReciever,new IntentFilter("MyJobService"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReciever);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor=sensorEvent.sensor;
        if (sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
           float x=sensorEvent.values[0];
           float y=sensorEvent.values[1];
           float z=sensorEvent.values[2];
           long currentTime=System.currentTimeMillis();
           if (currentTime-lastTime>200) {
               long diffTime=currentTime-lastTime;
               lastTime=currentTime;
               float speed=Math.abs(x+y+z-lastX-lastY-lastZ)/diffTime*10000;
               if (speed>SHAKE_SPEED) {
                   txtMonitor.setText("恭喜你中奖了！");
                   JobScheduler jobScheduler= (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                   JobInfo jobInfo=new JobInfo.Builder(JOB_ID,new ComponentName(this,MyJobService.class)).setMinimumLatency(0).build();

                   jobScheduler.schedule(jobInfo);
                   saySomething("恭喜你中奖了！短信已发出，继续摇一摇吗？");
               }

           }
            lastX=x;lastY=y;lastZ=z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void startTask(View view) {


    }
    private boolean checkPermissions() {
        int permission= ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        if (permission== PackageManager.PERMISSION_GRANTED) {
            txtMonitor.setText("已经开启了短信权限");
            return true;
        }else {
            txtMonitor.setText("还不具备发送短信权限");
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},REQUEST_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==REQUEST_CODE) {
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                txtMonitor.setText("你刚刚同意了短信权限！");
            }else {
                txtMonitor.setText("你禁用了短信权限！");
            }
        }
    }

    @Override
    public void onInit(int i) {
        if (i==TextToSpeech.SUCCESS) {
            int result=tts.setLanguage(Locale.CHINA);
            if (result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED) {
                txtMonitor.setText("不支持中文！");
            }else {
                ttsInitialized=true;
            }

        }else {
            txtMonitor.setText("语音初始化失败！");
        }
    }
    private void saySomething(String speech) {
        if (!ttsInitialized) {
            txtMonitor.setText("语音初始化失败！");
            return;
        }
        tts.speak(speech,TextToSpeech.QUEUE_FLUSH,null,"speech");
    }
}
