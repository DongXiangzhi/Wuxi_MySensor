package cn.edu.ldu.mysensor;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;

public class MyJobService extends JobService {
    public static final String TAG="MyJobService";
    public MyJobService() {
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "onStartJob: "+jobParameters.getJobId());
        
        //异步任务
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                SmsManager smsManager=SmsManager.getDefault();
                smsManager.sendTextMessage("手机号码",null,"恭喜你中奖了！",null,null);

                Log.i(TAG, "run: 短信发送完毕！！");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("MyJobService"));
                jobFinished(jobParameters,false);
            }
        };
        
       Thread thread=new Thread(runnable);
       thread.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "onStopJob: "+jobParameters.getJobId());
        return false;
    }


}
