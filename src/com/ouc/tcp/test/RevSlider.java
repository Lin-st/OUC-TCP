package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

enum RevFlag{
    WAIT,
    REV_NACK
}
public class RevSlider {
    private TCP_PACKET packet;
    private int flag;

    public RevSlider(){
        this.packet = null;
        this.flag = RevFlag.WAIT.ordinal();
    }

    public void reset(){
        this.packet = null;
        this.flag = RevFlag.WAIT.ordinal();
    }

    public TCP_PACKET getPacket() {
        try {
            return packet.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPacket(TCP_PACKET packet) {
        try {
            this.packet = packet.clone();
            this.flag = RevFlag.REV_NACK.ordinal();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isReceived(){
        return this.flag == RevFlag.REV_NACK.ordinal();
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
