package com.ltmonitor.jt808.protocol.jt2012;

import java.util.ArrayList;
import java.util.List;

import com.ltmonitor.entity.VehicleRecorder;
import com.ltmonitor.jt808.entity.SpeedRecorder;
import com.ltmonitor.jt808.protocol.BitConverter;
import com.ltmonitor.jt808.tool.DateUtil;

/**
 * �ɼ�ָ���ĳ�ʱ��ʻ��¼ 0x11H
 */
public class Recorder_TimeOutDrivingRecord implements IRecorderDataBlock_2012 {
	private List<VehicleRecorder> drivingRecordList = new ArrayList<VehicleRecorder>();
	
	

	/**
	 * ������
	 */
	public final byte getCommandWord() {
		return 0x11;
	}

	/**
	 * ���ݿ鳤��
	 */
	public final short getDataLength() {
		return 87;
	}

	public final byte[] WriteToBytes() {
		byte[] bytes = null;
		return bytes;
	}

	public final void ReadFromBytes(byte[] bytes) {
		if (bytes != null) {
			for (int i = 0; i < bytes.length / 50; i++) {
				VehicleRecorder vr = new VehicleRecorder();
				// ��ȡ��������ʻ֤����
				byte[] nub = new byte[18];
				System.arraycopy(bytes, 0 + 50 * i, nub, 0, 18);

				String driverNub = BitConverter.getString(nub);
				if (driverNub.length() == 15) {
					String add = "00H";
					driverNub = driverNub + add;
				}
				String licenseNo = driverNub;
				vr.setDriverLicense(licenseNo);
				// ��ȡ��ʼʱ��
				byte[] ContinuousDrivingBeginTime = new byte[6];
				System.arraycopy(bytes, 18 + 50 * i,
						ContinuousDrivingBeginTime, 0, 6);
				String beginTime = new java.util.Date(java.util.Date.parse("20"
						+ String.format("%02X", ContinuousDrivingBeginTime[0])
						+ "-"
						+ String.format("%02X", ContinuousDrivingBeginTime[1])
						+ "-"
						+ String.format("%02X", ContinuousDrivingBeginTime[2])
						+ " "
						+ String.format("%02X", ContinuousDrivingBeginTime[3])
						+ ":"
						+ String.format("%02X", ContinuousDrivingBeginTime[4])
						+ ":"
						+ String.format("%02X", ContinuousDrivingBeginTime[5])))
						.toString();
				vr.setStartTime(DateUtil.stringToDateTime(beginTime));
				// ��ȡ����ʱ��
				byte[] ContinuousDrivingEndTime = new byte[6];
				System.arraycopy(bytes, 24 + 50 * i, ContinuousDrivingEndTime,
						0, 6);
				String endTime = new java.util.Date(java.util.Date.parse("20"
						+ String.format("%02X", ContinuousDrivingEndTime[0])
						+ "-"
						+ String.format("%02X", ContinuousDrivingEndTime[1])
						+ "-"
						+ String.format("%02X", ContinuousDrivingEndTime[2])
						+ " "
						+ String.format("%02X", ContinuousDrivingEndTime[3])
						+ ":"
						+ String.format("%02X", ContinuousDrivingEndTime[4])
						+ ":"
						+ String.format("%02X", ContinuousDrivingEndTime[5])))
						.toString();
				vr.setEndTime(DateUtil.stringToDate(endTime));
				SpeedRecorder sr1 = new SpeedRecorder();
				// ��ȡ��ʼʱ����Чλ��
				byte[] BeginTimePlace = new byte[10];
				System.arraycopy(bytes, 30 + 50 * i, BeginTimePlace, 0, 10);
				int longitude = BitConverter.ToUInt32(BeginTimePlace, 0);
				int latitude = BitConverter.ToUInt32(BeginTimePlace, 4);
				int altitude = BitConverter.ToUInt16(BeginTimePlace, 8);
				sr1.setAltitude(altitude);
				sr1.setLatitude(latitude);
				sr1.setLongitude(longitude);
				sr1.setRecorderDate(vr.getStartTime());
				// ��ȡ����ʱ����Чλ��
				SpeedRecorder sr2 = new SpeedRecorder();
				byte[] EndTimePlace = new byte[10];
				System.arraycopy(bytes, 40 + 50 * i, EndTimePlace, 0, 10);
				longitude = BitConverter.ToUInt32(EndTimePlace, 0);
				latitude = BitConverter.ToUInt32(EndTimePlace, 4);
				altitude = BitConverter.ToUInt16(EndTimePlace, 8);
				sr1.setAltitude(altitude);
				sr1.setLatitude(latitude);
				sr1.setLongitude(longitude);
				sr1.setRecorderDate(vr.getStartTime());
				vr.getSpeedList().add(sr1);
				vr.getSpeedList().add(sr2);
				drivingRecordList.add(vr);
			}
		}
	}

	/**
	 * ��ȡ�ص���Ϣ
	 * 
	 * @param placeInfo
	 * @return
	 */
	private String GetPlaceInfo(byte[] placeInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append("����Ϊ��"
				+ (int) ((int) (placeInfo[0] << 24)
						+ (int) (placeInfo[1] << 16)
						+ (int) (placeInfo[2] << 8) + (int) (placeInfo[3]))
				* 0.0001);

		sb.append("γ��Ϊ��"
				+ (int) ((int) (placeInfo[4] << 24)
						+ (int) (placeInfo[5] << 16)
						+ (int) (placeInfo[6] << 8) + (int) (placeInfo[7]))
				* 0.0001);

		sb.append("���θ߶�Ϊ��"
				+ (int) ((int) (placeInfo[8] << 8) + (int) (placeInfo[9])));

		return sb.toString();
	}

	public List<VehicleRecorder> getDrivingRecordList() {
		return drivingRecordList;
	}

	public void setDrivingRecordList(List<VehicleRecorder> drivingRecordList) {
		this.drivingRecordList = drivingRecordList;
	}
}
