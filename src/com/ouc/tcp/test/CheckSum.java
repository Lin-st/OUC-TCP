package com.ouc.tcp.test;

import java.util.ArrayList;
import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		TCP_HEADER tcph = tcpPack.getTcpH();
		CRC32 crc32 = new CRC32();
		crc32.update(tcph.getTh_seq());
		crc32.update(tcph.getTh_ack());
		for (int datum : tcpPack.getTcpS().getData()) {
			crc32.update(datum);
		}
		checkSum = (int)crc32.getValue();
		return (short) checkSum;
	}
	
}
