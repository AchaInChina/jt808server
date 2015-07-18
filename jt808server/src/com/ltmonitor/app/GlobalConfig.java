package com.ltmonitor.app;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.ltmonitor.jt808.model.Parameter;
import com.ltmonitor.jt808.protocol.T808Message;

public class GlobalConfig {

	public static Parameter paramModel = new Parameter();

	private static ConcurrentLinkedQueue<T808Message> messageQueueForDisplay = new ConcurrentLinkedQueue();
	/**
	 * ���ڽ�������ʾ��Ϣ��ֻ��ʾָ�����ʱ�򿪣�����ʱҪ�رգ���ֹ�����ڴ�
	 */
	public static boolean displayMsg = false;
	/**
	 * �����Ƿ��ڽ�������ʾ������Ϣ
	 */
	public static boolean displayConnection = false;
	/**
	 * �ܰ���
	 */
	public static long totalPacketNum = 0;
	//�ܶ�λ����
	public static long totalLocationPacketNum = 0;
	
	public static long connectNum = 0;
	public static String filterSimNo = "";

	public static void putMsg(T808Message tm) {
		if (displayMsg == false) {
			int msgType = tm.getMessageType();
			if (msgType == 0x0002 || msgType == 0x0200 || msgType == 0x8001
					|| msgType == 0x0102 || msgType == 0x8100
					|| msgType == 0x0100
					|| msgType == 0x0003
					)
				return;
		}
		if(filterSimNo!= null && filterSimNo.length() > 0 )
		{
			if(tm.getSimNo() == null || tm.getSimNo().indexOf(filterSimNo) < 0)
				return;
		}
		messageQueueForDisplay.add(tm);
	}

	public static T808Message getMsgForDisplay() {
		return messageQueueForDisplay.poll();
	}

}
