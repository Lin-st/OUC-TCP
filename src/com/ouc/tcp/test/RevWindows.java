package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

import java.util.Queue;

enum AckFlag{
    ORDERED,//在窗口里的
    UNORDERED,//不在窗口里的，丢了
    REPLICATED,//重发的
    NEED//当前窗口基序号
}

public class RevWindows {
    private int size;
    private RevSlider[] windows;
    private int nextIndex;
    public RevWindows(int size){
        this.size = size;
        this.windows = new RevSlider[size];
        for (int i = 0; i < size; i++) {
            windows[i] = new RevSlider();
        }//不初始化就会空指针异常
        this.nextIndex = 0;
    }

    public int getIndex(int now){
        return now % size;
    }
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public RevSlider[] getWindows() {
        return windows;
    }

    public void setWindows(RevSlider[] windows) {
        this.windows = windows;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public int packetFlag(TCP_PACKET packet){
        int seq = (packet.getTcpH().getTh_seq()-1) / packet.getTcpS().getData().length;
        if(seq < nextIndex){
            return AckFlag.REPLICATED.ordinal();
        }
        else if(seq >= nextIndex + size){
            return AckFlag.UNORDERED.ordinal();
        }
        windows[getIndex(seq)].setPacket(packet);
        if(seq == nextIndex) {
            return AckFlag.NEED.ordinal();
        }
        return AckFlag.ORDERED.ordinal();
    }

    public TCP_PACKET getPacket(){
        int now =getIndex(nextIndex);
        if(!windows[now].isReceived())
            return null;
        TCP_PACKET packet = windows[now].getPacket();
        windows[now].reset();
        nextIndex++;
        return packet;
    }
    public void deliver(Queue<int[]> dataQueue){
        TCP_PACKET packet = getPacket();
        while (packet != null){
            dataQueue.add(packet.getTcpS().getData());
            packet = getPacket();
        }
    }
}
