package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;

public class SendWindows {
    private int size;
    private SendSlider[] windows;
    private int head;
    private int rear;
    private int nextIndex;//下一个发送的序号

    public SendWindows(int size){
        this.size = size;
        this.windows = new SendSlider[size];
        for (int i = 0; i < size; i++) {
            windows[i] = new SendSlider();
        }//不初始化就会空指针异常
        this.head = 0;
        this.rear = 0;
        this.nextIndex = 0;
    }
    public int getIndex(int now){
        return now % size;
    }
    public boolean full(){
        return head + size == rear;
    }
    public boolean empty(){
        return head == rear;
    }
    public boolean allSent(){
        return nextIndex == rear;
    }
    public void pushPacket(TCP_PACKET packet){
        int now = getIndex(rear);
        windows[now].setPacket(packet);
        //windows[now].setFlag(SendFlag.NACK.ordinal());
        rear++;
    }
    public void sendPacket(TCP_Sender sender, Client client,int delay,int period){
        if(this.empty()||this.allSent()){
            return;
        }
        int now = getIndex(nextIndex);
        TCP_PACKET packet = windows[now].getPacket();
        windows[now].scheduleTimer(new UDT_RetransTask(client,packet),delay,period);
        nextIndex++;
        sender.udt_send(packet);
    }
    public void setAcked(int seq){
        for(int i=head;i!=rear;i++){
            int now =getIndex(i);
            if(!windows[now].isAcked() && windows[now].getPacket().getTcpH().getTh_seq() == seq){
                windows[now].acked();
                break;
            }
        }
        while(!this.empty() && windows[getIndex(head)].isAcked()){
            int now = getIndex(head);
            windows[now].reset();
            head++;
        }
    }
}
