package com.ltmonitor.jt808.service;

import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.EWayBill;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.GnssData;
import com.ltmonitor.entity.TakePhotoModel;
import com.ltmonitor.jt809.entity.DriverModel;
import com.ltmonitor.jt809.entity.VehicleRegisterInfo;
/**
 * ת������ӿ�
 * @author tianfei
 *
 */
public interface ITransferGpsService {

	public abstract void transfer(GnssData gd);

	public abstract void setTransferTo809Enabled(boolean startTransfer);

	public abstract void transfer(AlarmRecord ar, GPSRealData rd);

	public abstract void transfer(EWayBill ebill);

	/**
	 * �������Ӧ��
	 * 
	 * @param plateNo
	 * @param plateColor
	 * @param result
	 *            0 �ɹ� 1ʧ��
	 */
	public abstract void transferListenTerminalAck(String plateNo,
			int plateColor, byte result);

	public abstract void transferEmergencyAccessAck(String plateNo,
			int plateColor, byte result);

	public abstract void transferTextInfoAck(String plateNo, int plateColor,
			int msgId, byte result);

	public abstract void transferRecorderData(String plateNo, int plateColor,
			byte cmdType, byte[] cmdData);

	public abstract void transfer(TakePhotoModel ph);

	void transferRegisterInfo(VehicleRegisterInfo vm);

	void transferDriverInfo(DriverModel d);

	void start();

	void stop();

	boolean isTransferTo809Enabled();

	boolean isTransferAlarmTo809Enabled();

}