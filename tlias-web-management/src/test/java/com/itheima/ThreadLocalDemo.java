package com.itheima;

/**
 * ThreadLocal - 是JDK中提供的线程的局部变量, 线程之间是隔离的, 哪个线程存储的, 只能哪个线程获取
 */
public class ThreadLocalDemo {

    private static ThreadLocal<String> local = new ThreadLocal<>();

    public static void main(String[] args) {
        //main 线程存储的
        local.set("Main Message");

        //开启线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                //子线程存储的
                local.set("Sub Message");

                //子线程获取的
                String s = local.get();
                System.out.println(Thread.currentThread().getName() + " : " + s);
            }
        }).start();

        //main 线程获取的
        String s = local.get();
        System.out.println(Thread.currentThread().getName() + " : " + s);
    }

}
