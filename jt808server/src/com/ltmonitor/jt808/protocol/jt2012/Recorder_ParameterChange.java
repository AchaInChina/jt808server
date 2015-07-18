package com.ltmonitor.jt808.protocol.jt2012;

import java.util.ArrayList;
import java.util.List;

/** 
 �ɼ�ָ���Ĳ����޸ļ�¼
 
*/
public class Recorder_ParameterChange implements IRecorderDataBlock_2012
{
	private  List<RecorderEvent> eventList = new ArrayList<RecorderEvent>();

	 /** 
	 ������
	 
	 */
	public final byte getCommandWord()
	{
		return 0x06;
	}

	/** 
	 ���ݿ鳤��
	 
	*/
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ushort getDataLength()
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
		if (bytes != null)
		{
			for (int i = 0; i < bytes.length / 7; i++)
			{
				
				byte[] EventTime = new byte[6];
				System.arraycopy(bytes, 0 + 7 * i, EventTime, 0, 6);
				String strTime =  new java.util.Date(java.util.Date.parse("20" + String.format("%02X", EventTime[0]) + "-" + String.format("%02X", EventTime[1]) + "-" + String.format("%02X", EventTime[2]) + " " + String.format("%02X", EventTime[3]) + ":" + String.format("%02X", EventTime[4]) + ":" + String.format("%02X", EventTime[5]))).toString();
				String eventType = "";
				switch (bytes[6 + 7 * i])
				{
					case 0x00:
						eventType = "00H �ɼ���¼��ִ�б�׼�汾";
						break;
					case 0x01:
						eventType = "01H �ɼ���ǰ��ʻ����Ϣ";
						break;
					case 0x02:
						eventType = "O2H �ɼ���¼�ǵ�ʵʱʱ��";
						break;
					case 0x03:
						eventType = "03H �ɼ��ۼ���ʻ���";
						break;
					case 0x04:
						eventType = "04H �ɼ���¼������ϵ��";
						break;
					case 0x05:
						eventType = "05H �ɼ�������Ϣ";
						break;
					case 0x06:
						eventType = "06H �ɼ���¼��״̬�ź�������Ϣ";
						break;
					case 0x07:
						eventType = "07H �ɼ���¼��Ψһ�Ա��";
						break;
					case 0x08:
						eventType = "08H �ɼ�ָ������ʻ�ٶȼ�¼";
						break;
					case 0x09:
						eventType = "09H �ɼ�ָ����λ����Ϣ��¼";
						break;
					case 0x10:
						eventType = "10H �ɼ�ָ�����¹��ɵ��¼";
						break;
					case 0x11:
						eventType = "11H �ɼ�ָ���ĳ�ʱ��ʻ��¼";
						break;
					case 0x12:
						eventType = "12H �ɼ�ָ���ļ�ʻ����ݼ�¼";
						break;
					case 0x13:
						eventType = "13H �ɼ�ָ�����ⲿ�����¼";
						break;
					case 0x14:
						eventType = "14H �ɼ�ָ���Ĳ����޸ļ�¼";
						break;
					case 0x15:
						eventType = "15H �ɼ�ָ�����ٶ�״̬��־";
						break;
					case 0x16:
						eventType = "16H-1FH Ԥ��";
						break;
					default:
						eventType = "����";
						break;
				}
				this.eventList.add(new RecorderEvent(strTime, eventType));
			}
		}
	}

	public List<RecorderEvent> getEventList() {
		return eventList;
	}

	public void setEventList(List<RecorderEvent> eventList) {
		this.eventList = eventList;
	}
}

