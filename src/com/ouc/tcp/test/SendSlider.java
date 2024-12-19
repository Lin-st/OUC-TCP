package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_RetransTask;
enum SendFlag{
    NACK,ACK
}
public class SendSlider {
    private TCP_PACKET packet;
    private int flag;
    private UDT_Timer timer;

    public SendSlider() {
        this.packet = null;
        this.flag = SendFlag.NACK.ordinal();
        this.timer = null;
    }

    public void reset(){
        this.packet = null;
        this.flag = SendFlag.NACK.ordinal();
    }

    public void resetTimer(){
        timer = new UDT_Timer();
    }

    public TCP_PACKET getPacket() {
        return packet;
    }

    public void setPacket(TCP_PACKET packet) {
        try {
            this.packet = packet.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        this.flag = SendFlag.NACK.ordinal();
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public UDT_Timer getTimer() {
        return timer;
    }

    public boolean isAcked(){
        return this.flag == SendFlag.ACK.ordinal();
    }

    public void acked(){
        this.flag = SendFlag.ACK.ordinal();
        this.timer.cancel();
    }

    public void scheduleTimer(UDT_RetransTask retransTask, int delay, int period) {
        this.timer = new UDT_Timer();
        this.timer.schedule(retransTask, delay, period);
    }

}
