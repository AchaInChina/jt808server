package com.ltmonitor.jt808.protocol.jt2012;

import java.util.ArrayList;
import java.util.List;

import com.ltmonitor.entity.VehicleRecorder;
import com.ltmonitor.jt808.tool.DateUtil;


/** 
 �ɼ�ָ�����ⲿ�����¼ 0x13H
*/
public class Recorder_ExternalPowerSupply implements IRecorderDataBlock_2012
{
	//���������ɼ�ָ�����ⲿ�����¼

	private  List<VehicleRecorder> eventList = new ArrayList<VehicleRecorder>();

	 /** 
	 ������
	 */
	public final byte getCommandWord()
	{
		return 0x13;
	}

	/** 
	 ���ݿ鳤��
	*/
	public final short getDataLength()
	{
		return 87;
	}

	public final byte[] WriteToBytes()
	{
		byte[] bytes = null;
		return bytes;
	}

	public final void ReadFromBytes(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
		String info = null;
		if (bytes != null) {
			for (int i = 0; i < bytes.length / 7; i++) {
				VehicleRecorder vr = new VehicleRecorder();
				//ʱ�� 
				byte[] EventTime = new byte[6];
				System.arraycopy(bytes, 0, EventTime, 0, 6);
				String strEventTime = new java.util.Date(java.util.Date.parse("20" + String.format("%02X", EventTime[0]) + "-" + String.format("%02X", EventTime[1]) + "-" + String.format("%02X", EventTime[2]) + " " + String.format("%02X", EventTime[3]) + ":" + String.format("%02X", EventTime[4]) + ":" + String.format("%02X", EventTime[5]))).toString();
				vr.setStartTime(DateUtil.stringToDateTime(strEventTime));
				//�¼�����
				String strEventType = "����";
				if (String.format("%02X", bytes[6 + 7 * i]).equals("01"))
				{
					strEventType = "ͨ��";
				}
				else if (String.format("%02X", bytes[6 + 7 * i]).equals("02"))
				{
					strEventType = "�ϵ�";
				}
				vr.setRemark(strEventType);
				eventList.add(vr);
			}
		}
	}

	public List<VehicleRecorder> getEventList() {
		return eventList;
	}

	public void setEventList(List<VehicleRecorder> eventList) {
		this.eventList = eventList;
	}

}
