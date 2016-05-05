package com.xyp260466.dubbo.test;

public class ReferStart {
    private static boolean start = false;


    private static class ThreadBean implements Runnable{

        private ReferTest refer;

        ThreadBean(int i, String name){
            refer = new ReferTest(name, i);
        }


        public void run() {
            while (!start){
                try {
                    Thread.sleep(1);
                }catch (Exception e){

                }
            }

            refer.main();
        }
    }


    public static void main(String[] args) {


        for(int i = 0; i < 10; i++){
            new Thread(new ThreadBean(i, "xiaoming "+i)).start();
        }

        start = true;


    }


}
