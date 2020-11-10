package nachos.proj1;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.*;

import java.util.Random;

public class Test {
    public static void main(String[] args) {

    }

    public static void test(){
        new JoinTest().test1();
        new JoinTest().test2();
        new Condition2Test().test();
        new AlarmTest().test();
        new CommunicatorTest().test();
    }
}

class testSubject implements Runnable {
    private int id;
    private int loopCount;
    testSubject(int id, int loopCount) {
        this.id = id;
        this.loopCount = loopCount;
    }

    public void run() {
        for (int i=0; i<loopCount; i++) {
            System.out.println("Thread " + id + " looped "
                    + i + " times");
            KThread.yield();
        }
    }
}
class JoinTest{
    JoinTest(){

    }
    public void test1(){
        System.out.println("-----------------------");
        System.out.println("Starting JOIN test 1\n");
        KThread t0 = new KThread(new testSubject(0,5)).setName("Thread 0");
        for (int i=0; i<10; i++){
            System.out.println("Main loop printed " + i + " times");
            if (i==4){
                t0.fork();
            }
        }
        t0.join();
        System.out.println("\nFinishing JOIN test 1");
        System.out.println("-----------------------\n");
    }

    public void test2(){
        System.out.println("-----------------------");
        System.out.println("Starting JOIN test 2\n");
        KThread t0 = new KThread(new testSubject(0,5)).setName("Thread 0");
        for (int i=0; i<10; i++){
            System.out.println("Main loop printed " + i + " times");
            if (i==4){
                t0.fork();
                t0.join();
            }
        }
        System.out.println("\nFinishing JOIN test 2");
        System.out.println("-----------------------\n");
    }
}

class Condition2Test{
    Lock lock = null;
    Condition2 cond = null;
    public Condition2Test() {
    }

    public void test(){
        System.out.println("-----------------------");
        System.out.println("Starting CONDITION2 test\n");
        Lib.assertTrue(lock == null);
        Lib.assertTrue(cond == null);
        lock = new Lock();
        cond = new Condition2(lock);
        KThread s1 = new KThread(new sleeper(1)).setName("Sleeper 1");
        KThread s2 = new KThread(new sleeper(2)).setName("Sleeper 2");
        KThread s3 = new KThread(new sleeper(3)).setName("Sleeper 3");
        KThread w1 = new KThread(new singleWaker(1)).setName("Singlewaker 1");
        KThread w2 = new KThread(new allWaker(1)).setName("Allwaker 1");
        s1.fork();
        s2.fork();
        w1.fork();
        s1.join();
        s3.fork();
        w2.fork();
        s3.join();
        System.out.println("\nFinishing CONDITION2 test");
        System.out.println("-----------------------\n");
    }

    class sleeper implements Runnable{

        int id;
        sleeper(int id){
            this.id = id;
        }

        @Override
        public void run() {
            Lib.assertTrue(lock != null);
            Lib.assertTrue(cond != null);
            System.out.println("Started sleeper " + id);
            lock.acquire();
            System.out.println("Sleeper " + id + " going to sleep");
            cond.sleep();
            System.out.println("Sleeper " + id + " woke up");
            System.out.println("Sleeper " + id + "'s work is finished");
            lock.release();
        }
    }

    class singleWaker implements Runnable{

        int id;
        singleWaker(int id){
            this.id = id;
        }

        @Override
        public void run() {
            Lib.assertTrue(lock != null);
            Lib.assertTrue(cond != null);
            System.out.println("Started singlewaker " + id);
            lock.acquire();
            System.out.println("Singlewaker " + id + " is waking up a sleeper");
            cond.wake();
            System.out.println("Singlewaker " + id + "'s work is finished");
            lock.release();
        }
    }

    class allWaker implements Runnable{

        int id;
        allWaker(int id){
            this.id = id;
        }

        @Override
        public void run() {
            Lib.assertTrue(lock != null);
            Lib.assertTrue(cond != null);
            System.out.println("Started allwaker " + id);
            lock.acquire();
            System.out.println("Allwaker " + id + " is waking all up");
            cond.wakeAll();
            System.out.println("Allwaker " + id + " finished its work");
            lock.release();
        }
    }

}

class AlarmTest {
    public AlarmTest(){

    }

    void test(){
        System.out.println("-----------------------");
        System.out.println("Starting ALARM test\n");
        long time1 = 900000;
        long time2 = 1000000;
        long time3 = 600000;

        //Alarm alarm = new Alarm();
        Alarm alarm = ThreadedKernel.alarm;
        KThread t1 = new KThread(new AlarmClass(time1, alarm)).setName("Alarm thread 1");
        KThread t2 = new KThread(new AlarmClass(time2, alarm)).setName("Alarm thread 2");
        KThread t3 = new KThread(new AlarmClass(time3, alarm)).setName("Alarm thread 3");

        alarm.waitUntil(time1);
        t1.fork();
        t2.fork();
        t3.fork();
        t1.join();
        t2.join();
        t3.join();
        System.out.println("\nFinishing ALARM test");
        System.out.println("-----------------------\n");
    }

}

class AlarmClass implements Runnable{

    private final long time;
    private final Alarm alarm;

    AlarmClass(long time, Alarm alarm) {
        this.time = time;
        this.alarm = alarm;
    }

    @Override
    public void run() {
        System.out.println(KThread.currentThread().getName() + " rings at " + Machine.timer().getTime());
        alarm.waitUntil(time);
        System.out.println(KThread.currentThread().getName() + " rings at " + Machine.timer().getTime());
    }
}

class CommunicatorTest{
    public CommunicatorTest(){

    }

    void test(){
        System.out.println("-----------------------");
        System.out.println("Starting COMMUNICATOR test\n");
        Communicator c = new Communicator();

        KThread l1 = new KThread(new Listener(1,c)).setName("Listener thread 1");
        KThread l2 = new KThread(new Listener(2,c)).setName("Listener thread 2");
        KThread l3 = new KThread(new Listener(3,c)).setName("Listener thread 3");
        KThread l4 = new KThread(new Listener(4,c)).setName("Listener thread 4");
        KThread l5 = new KThread(new Listener(5,c)).setName("Listener thread 5");
        KThread l6 = new KThread(new Listener(6,c)).setName("Listener thread 6");

        KThread s1 = new KThread(new Speaker(1,c)).setName("Speaker thread 1");
        KThread s2 = new KThread(new Speaker(2,c)).setName("Speaker thread 2");
        KThread s3 = new KThread(new Speaker(3,c)).setName("Speaker thread 3");
        KThread s4 = new KThread(new Speaker(4,c)).setName("Speaker thread 4");
        KThread s5 = new KThread(new Speaker(5,c)).setName("Speaker thread 5");
        KThread s6 = new KThread(new Speaker(6,c)).setName("Speaker thread 6");

        //Listener before speaker
        l1.fork();
        s1.fork();

        //Speaker before listener
        s2.fork();
        l2.fork();

        //Multilistener before multispeaker
        l3.fork();
        l4.fork();
        s3.fork();
        s4.fork();

        //Multispeaker before multilistener
        s5.fork();
        s6.fork();
        l5.fork();
        l6.fork();

        l1.join();
        s1.join();
        s2.join();
        l2.join();
        l3.join();
        l4.join();
        s3.join();
        s4.join();
        s5.join();
        s6.join();
        l5.join();
        l6.join();

        System.out.println("\nFinishing COMMUNICATOR test");
        System.out.println("-----------------------\n");
    }
}

class Speaker implements Runnable{

    int id;
    Communicator communicator;

    public Speaker(int id, Communicator communicator) {
        this.id = id;
        this.communicator = communicator;
    }

    @Override
    public void run() {
        Random random = new Random();
        int number = random.nextInt(100);
        communicator.speak(number);
    }
}

class Listener implements Runnable{

    int id;
    Communicator communicator;

    public Listener(int id, Communicator communicator) {
        this.id = id;
        this.communicator = communicator;
    }

    @Override
    public void run() {
        int number = communicator.listen();
    }
}
