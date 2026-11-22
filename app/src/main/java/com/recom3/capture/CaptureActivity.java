package com.recom3.capture;

import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.view.*;
import android.widget.*;

public final class CaptureActivity extends Activity {
    private TextView status;private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){CaptureService s=CaptureService.current;status.setText(s==null?"Stopped":"ICE3 Capture\n"+s.address+":"+CaptureService.PORT+"   Code: "+s.code+"\n"+s.lastError);handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);startService(new Intent(this,CaptureService.class));
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(12,4,12,4);
        status=new TextView(this);status.setTextSize(18);root.addView(status);
        button(root,"Screenshot in 5 seconds",new View.OnClickListener(){public void onClick(View v){if(CaptureService.current!=null){CaptureService.current.delayedScreenshot();finish();}}});
        button(root,"Keep running / return to apps",new View.OnClickListener(){public void onClick(View v){finish();}});
        button(root,"Stop Capture",new View.OnClickListener(){public void onClick(View v){stopService(new Intent(CaptureActivity.this,CaptureService.class));finish();}});
        setContentView(root);
    }
    private void button(LinearLayout root,String label,View.OnClickListener click){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setOnClickListener(click);root.addView(b,new LinearLayout.LayoutParams(-1,44));}
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
