package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class SendWindows {
    private int max_size;
    private int cwnd = 1;
    private int ssthresh = 16;
    //private int size;
    private SendSlider[] windows;
    private int head;
    private int rear;
    private int nextIndex;//下一个发送的序号
    private UDT_Timer timer;
    private int preCount = 0;
    private int preSeq = -10086;
    boolean quickRe = false;

    private class GBNTask extends TimerTask {

        private TCP_Sender sender;
        private Client client;
        private int delay;
        private int period;
        private SendWindows windows;

        public GBNTask(TCP_Sender sender, Client client, int delay, int period, SendWindows windows) {
            this.sender = sender;
            this.client = client;
            this.delay = delay;
            this.period = period;
            this.windows = windows;
        }

        @Override
        public void run() {
            //拥塞，乘法减小
            int preC = cwnd;
            ssthresh = (cwnd+1)/2;
            cwnd = ssthresh;
            SendSlider[] w=new SendSlider[preC];
            for (int i = 0; i < w.length; i++) {
                w[i] = new SendSlider();
            }
            if(rear - head > cwnd){
                rear = head + cwnd;
            }
            for(int i=head;i<rear;i++){
                w[i%preC].setPacket(windows.windows[i%preC].getPacket());
            }
            for(int i=head;i<rear;i++){
                int now = getIndex(i);
                windows.windows[now].setPacket(w[i%preC].getPacket());
            }

//            windows.windows[0].setPacket(windows.windows[getIndex(head)].getPacket());
//            for (int i = 1; i < windows.windows.length; i++) {
//                windows.windows[i] = new SendSlider();
//            }
//            cwnd = ssthresh;
//            head = 0;
//            rear = head+1;
            windows.nextIndex = head;
            while (windows.nextIndex < windows.rear){
                windows.sendPacket(sender,client,delay,period);
            }
        }
    }
    private GBNTask task;

    public SendWindows(int size){
        this.max_size = size;
        this.windows = new SendSlider[size];
        for (int i = 0; i < size; i++) {
            windows[i] = new SendSlider();
        }//不初始化就会空指针异常
        this.head = 0;
        this.rear = 0;
        this.nextIndex = 0;
        this.timer = new UDT_Timer();
    }
    public int getIndex(int now){
        return now % cwnd;//size;
    }
    public boolean full(){
        return head + cwnd == rear;//size == rear;
    }
    public boolean empty(){
        return head == rear;
    }
    public boolean allSent(){
        return nextIndex == rear;
    }

    public void resetTime(){
        this.timer.cancel();
        this.timer = new UDT_Timer();
        GBNTask task1 = new GBNTask(task.sender,task.client,task.delay,task.period,this);
        timer.schedule(task1,task1.delay,task1.period);
        this.task = task1;
    }
    public void pushPacket(TCP_PACKET packet){
        int now = getIndex(rear);
        windows[now].setPacket(packet);
        windows[now].setFlag(SendFlag.NACK.ordinal());
        rear++;
    }
    public void sendPacket(TCP_Sender sender, Client client,int delay,int period){
        if(this.allSent()){
            return;
        }
        else if(this.empty()){
            timer.cancel();
            return;
        }

        int now = getIndex(nextIndex);
        TCP_PACKET packet = windows[now].getPacket();
        if(nextIndex == head){
            GBNTask task = new GBNTask(sender,client,delay,period,this);
            this.task = task;
            timer.schedule(task,delay,period);
        }
        //windows[now].scheduleTimer(new UDT_RetransTask(client,packet),delay,period);
        nextIndex++;
        sender.udt_send(packet);
    }
    public void setAcked(int seq){
        int now = getIndex(head);
        boolean flag = false;
        while(head < rear && !windows[now].isAcked() && windows[now].getPacket().getTcpH().getTh_seq() <= seq){
            windows[now].acked();
            head++;
            now = getIndex(head);
        }

        //慢开始和拥塞避免
        int preC = cwnd;
        if(cwnd<max_size){
            if (quickRe || cwnd >= ssthresh)
                cwnd += 1;
            else if (2 * cwnd < ssthresh)
                cwnd *= 2;
            else
                cwnd = ssthresh;
        }
        SendSlider[] w=new SendSlider[preC];
        for (int i = 0; i < w.length; i++) {
            w[i] = new SendSlider();
        }
        for(int i=head;i<rear;i++){
            w[i%preC].setPacket(windows[i%preC].getPacket());
        }
        for(int i=head;i<rear;i++){
            now = getIndex(i);
            windows[now].setPacket(w[i%preC].getPacket());
        }

        resetTime();

        if(seq == preSeq){
            preCount++;
        }
        else {
            preSeq = seq;
            preCount = 1;
        }
        if(preCount >= 3){
            if(empty()){
                timer.cancel();
                return;
            }
            task.sender.udt_send(windows[getIndex(head)].getPacket());
            preCount = 0;
            quickRe = true;
            preC = cwnd;
            ssthresh = (cwnd+1)/2;
            cwnd = ssthresh;
            if(rear - head >cwnd){
                rear = head + cwnd;
                for(int i=head;i<rear;i++){
                    w[i%preC].setPacket(windows[i%preC].getPacket());
                }
                for(int i=head;i<rear;i++){
                    now = getIndex(i);
                    windows[now].setPacket(w[i%preC].getPacket());
                }
            }
        }
    }
}
