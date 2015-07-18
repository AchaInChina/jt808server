package com.ltmonitor.jt808.protocol.jt2012;

import java.util.ArrayList;
import java.util.List;

import com.ltmonitor.jt808.protocol.BitConverter;

/**
 * �ɼ�ָ���ļ�ʻ����ݼ�¼
 */
public class Recorder_DriverIdentity implements IRecorderDataBlock_2012 {
	// ÿ�βɼ�ָ���ļ�ʻ����ݼ�¼
	private List<RecorderEvent> eventList = new ArrayList<RecorderEvent>();

	/**
	 * ������
	 */
	public final byte getCommandWord() {
		return 0x06;
	}

	/**
	 * ���ݿ鳤��
	 */
	// C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct
	// equivalent in Java:
	// ORIGINAL LINE: public ushort getDataLength()
	public final short getDataLength() {
		return 87;
	}

	public final byte[] WriteToBytes() {
		byte[] bytes = null;
		return bytes;
	}

	public final void ReadFromBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();

		if (bytes != null) {
			for (int i = 0; i < bytes.length / 25; i++) {
				// ��ȡ�¼�����ʱ�䲢��ӵ������
				byte[] EventOccursTime = new byte[6];
				System.arraycopy(bytes, 0 + 25 * i, EventOccursTime, 0, 6);
				String time = new java.util.Date(java.util.Date.parse("20"
						+ String.format("%02X", EventOccursTime[0]) + "-"
						+ String.format("%02X", EventOccursTime[1]) + "-"
						+ String.format("%02X", EventOccursTime[2]) + " "
						+ String.format("%02X", EventOccursTime[3]) + ":"
						+ String.format("%02X", EventOccursTime[4]) + ":"
						+ String.format("%02X", EventOccursTime[5])))
						.toString();
				String strEventTime = time;
				// ��ȡ��������ʻ֤����
				byte[] nub = new byte[18];
				System.arraycopy(bytes, 6 + 25 * i, nub, 0, 18);

				String driverNub = BitConverter.getString(nub);
				if (driverNub.length() == 15) {
					String add = "00H";
					driverNub = driverNub + add;
				}

				String strLicense = driverNub;
				// ��ȡʱ������
				String bu = String.format("%02X", bytes[24 + 25 * i]);
				String strEventType = "����";
				if (String.format("%02X", bytes[24 + 25 * i]).equals("01")) {
					strEventType = "��½";
				} else if (String.format("%02X", bytes[24 + 25 * i]).equals(
						"02")) {
					strEventType = "�˳�";
				}
				this.eventList.add(new RecorderEvent(strEventTime, strEventType, strLicense));

			}
		}
	}

}
