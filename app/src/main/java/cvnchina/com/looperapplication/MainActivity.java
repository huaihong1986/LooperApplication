package cvnchina.com.looperapplication;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends ListActivity {
    private TextView tv_info;
    private CalThread calThread;//一个子线程
    private boolean ifLooperPrepare = false;//是否在子线程中的创建Handler【前】初始化Looper
    private boolean ifLooperLoop = false;//是否在子线程中创建Handler【后】执行loop方法
    private boolean sendMessageToUi = false;//是否将子线程中的消息转发到主线程中
    private boolean sendMessageToZi = false;//是否将主线程中的消息转发到子线程中
    public static final int MSG_WHAT_1 = 1;
    @SuppressLint("HandlerLeak")
    private Handler uiHandler = new Handler() {//在主线程中，系统已经初始化了一个Looper对象，所以我们直接创建Handler对象就可以进行信息的发送与处理了
        public void handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, "主线程收到消息：" + msg.obj, Toast.LENGTH_SHORT).show();
            tv_info.setText("主线程收到消息：" + msg.obj);
            if (sendMessageToZi) {
                Message newMsg = Message.obtain(null, msg.what, new SimpleDateFormat("  HH:mm:ss", Locale.getDefault()).format(new Date()));
                calThread.getThreadHandler().sendMessageAtTime(newMsg, SystemClock.uptimeMillis() + 4500);// 从开机到现在的毫秒数（不包括手机睡眠的时间）
            }
        }
    };
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] array = { "在子线程中的创建Handler前，必须先通过Looper.prepare()初始化Looper",//
                "若要子线程能收到消息，在子线程中的创建Handler后，必须调用Looper.loop()",//
                "开启子线程，并在子线程中创建Handler \n注意：此线程会一直阻塞在Looper.loop()", //
                "在主线程中，通过子线程的Handler向子线程发消息", //
                "将子线程中的收到的消息（不）转发到主线程",//
                "将主线程中的收到的消息（不）转发到子线程",//
                "演示Handler的post方法" };
        for (int i = 0; i < array.length; i++) {
            array[i] = i + "、" + array[i];
        }
        tv_info = new TextView(this);
        tv_info.setTextColor(Color.BLUE);
        tv_info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv_info.setPadding(20, 10, 20, 10);
        getListView().addFooterView(tv_info);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(array))));
        calThread = new CalThread();
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                ifLooperPrepare = true;
                break;
            case 1:
                ifLooperLoop = true;
                break;
            case 2:
                calThread.start();
                break;
            case 3:
                String obj = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date());
                calThread.getThreadHandler().sendMessage(Message.obtain(null, MSG_WHAT_1, obj));//这种方式时，Message.obtain的第一个参数可直接设为null
                //注意，并不是说Message.obtain的第一个参数Handler没有用(其作用是指定msg.target)，而是后面调用sendMessage方法时重新指定了当前Handler为msg.target
                break;
            case 4:
                sendMessageToUi = !sendMessageToUi;
                break;
            case 5:
                sendMessageToZi = !sendMessageToZi;
                break;
            case 6:
                uiHandler.post(new Runnable() {//其实这个Runnable并没有创建什么线程，而是发送了一条消息，当Handler收到此消息后回调run()方法
                    @Override
                    public void run() {
                        tv_info.setText("演示Handler的post方法");
                    }
                });
                break;
        }
    }
    /** 定义一个线程，用于执行耗时的操作  */
    class CalThread extends Thread {
        private Handler ziHandler;
        public Handler getThreadHandler() {
            return ziHandler;
        }
        @SuppressLint("HandlerLeak")
        public void run() {
            if (ifLooperPrepare) {
                Looper.prepare();//①为当前线程创建唯一的Looper对象  ②在它的构造方法中会创建一个的MessageQueue对象
                //prepare()方法只能被调用一次，否则抛异常；这保证了在一个线程中只有一个Looper实例，同时一个Looper实例也只有一个MessageQueue实例
            }
            ziHandler = new Handler(Looper.myLooper()) {//任何线程都可通过此Handler发送信息！
                //在初始化Handler之前必须先调用Looper.prepare()，否则报RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                @Override
                public void handleMessage(Message msg) {
                    Toast.makeText(MainActivity.this, "子线程收到消息：" + msg.obj, Toast.LENGTH_SHORT).show();//在子线程中是可以弹土司的，因为土司机制比较特殊！
                    //在子线程中创建Handler的目的完全是为了和其他线程（包括UI线程）通讯，绝对不是（也不能）更新UI，若要更新UI，还必须通过UI线程才可以！
                    if (sendMessageToUi) {
                        uiHandler.sendMessageDelayed(Message.obtain(msg), 2500);//注意，不能直接把消息转发出去，否则IllegalStateException: This message is already in use
                    }
                }
            };
            if (ifLooperLoop) {
                Looper.loop();//启动一个死循环，不断从MessageQueue中取消息，没有消息则阻塞等待，有则通过将消息传给当前Handler去处理。此方法必须在prepar之后运行
            }
        }
    }
}
