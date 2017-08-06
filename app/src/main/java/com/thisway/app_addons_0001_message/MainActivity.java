package com.thisway.app_addons_0001_message;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.HandlerThread;/* 直接使用HanderThread */

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    private final String TAG = "MessageTest";
    private int ButtonCount = 0;
    private int mMessageCount = 0;

    /* 线程1  第二行代码P340第二种线程实现方式*/
    /* 缺点：无法扩展更多的函数 */
    /* 创建子线程，子线程如下，每隔3s发送一次Log */
    private Thread myThread;

    /* 实现一个runnable接口 */
    class MyRunnable implements Runnable{
        public void run(){
            int count = 0;
            for (;;){
                Log.d(TAG, "MyThread "+ count);
                count++;
                try {
                    Thread.sleep(3000);/* 延时 */
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* 线程2 第二行代码P340第一种线程实现方式*/
    /* 自定义类继承于Thread，复写run函数 */
    /*创建了一个looper接收消息*/
    private MyThread myThread2;
    private Handler mHandler2;

    class MyThread extends Thread{
        /* Tip: Ctrl+o可以选择类里面的方法 */

        private Looper mLooper;
        @Override
        public void run() {
            super.run();//Ctrl+o后自动跳出来的
            /* 添加 Looper.prepare 和 Looper.loop 就有了处理消息的功能 */
            Looper.prepare();/* 创建消息队列MessageQueue，与Looper.loop成对 */

            synchronized (this) {
                mLooper = Looper.myLooper();/* 一旦创建了Looper，mLooper这个变量就不是null，保证只唤醒一次*/
                notifyAll();/* 唤醒wait */
            }

            Looper.loop();/* 开始无限循环，从消息队列里面取出消息，与Looper.prepare成对 */
        }

        /* 假设getLooper先于run方法(即looper还未创建就先来获取looper)， */
        public Looper getLooper() {
            if (!isAlive()) { /* 假设如果成立：第一次进来线程是active状态，所以这个if不成立 */
                return null;
            }

            // If the thread has been started, wait until the looper has been created.用来保证start后保证先去执行Looper.prepare()，保证hander一定能获取到looper
            synchronized (this) {
                while (isAlive() && mLooper == null) { /* mLooper还处于NULL的话，就会等待(wait) */
                    try {
                        wait(); /*一旦Looper.prepare执行后要通知线程从wait里面唤醒过来*/
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mLooper;
            //return Looper.myLooper();/* 返回Looper */
        }
    }

    /* 线程3定义 */
    private HandlerThread myThread3;//HandlerThread的looper已经实现好不用手动添加
    private Handler mHandler3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Tip: Shift+F1跳转至说明文档 */
        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Log.d(TAG, "Send Message " + ButtonCount);//点了几次按钮
                ButtonCount++;

                /* 构造消息 */
                Message msg = new Message();
                /* 按键按下时由主线程发送message给线程2 */
                mHandler2.sendMessage(msg);

                mHandler3.post(new Runnable() {/* post实现构造消息并且把消息放到队列里面去 */
                    /* 消息处理函数 */
                    /* 因为post方法里的getPostMessage(r)的callback会调用runable的run，所以run就是收到消息后的处理*/
                    @Override
                    public void run() {
                        /* 线程收到消息后的处理函数 */
                        Log.d(TAG, "get Message for Thread3 "+ mMessageCount);
                        mMessageCount++;
                    }
                });

            }
        });

        /* 实例化对象(线程1) 第二行代码P340第二种线程实现方式*/
        myThread = new Thread(new MyRunnable(),"MessageThread");/* 线程及线程名字 */
        myThread.start();

        /* 实例化对象(线程2) 第二行代码P340第一种线程实现方式*/
        myThread2 = new MyThread();
        myThread2.start();//note :这里start后不一定能马上创建looper，而后面的程序去getlooper可能会出错

        /* 线程2构造handler,这个handler有多种构造方法,callback、looper.... */
        /* 第一个参数这里需要指定looper,mHandler2.sendMessage发送消息是发给哪个线程的looper，第二个参数添加处理方法（会调用下面覆写的handleMessage） */
        mHandler2 = new Handler(myThread2.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) { /* 当对应线程的looper收到消息后会调用这个handleMessage来处理 */
                Log.d(TAG, "get Message for Thread2 "+ mMessageCount);
                mMessageCount++;
                return false;
            }
        });

        /* 实例化对象(线程3) */
        myThread3 = new HandlerThread("MessageTestThread3");//带入一个线程名
        myThread3.start();

        mHandler3 = new Handler(myThread3.getLooper());//指明mHandler3给myThread3发消息并在myThread3收到消息的时候处理消息
    }
}