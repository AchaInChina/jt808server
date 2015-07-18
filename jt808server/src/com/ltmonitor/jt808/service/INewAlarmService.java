package com.ltmonitor.jt808.service;

import com.ltmonitor.entity.Alarm;
import com.ltmonitor.entity.GPSRealData;

public interface INewAlarmService {

	public abstract void start();

	public abstract void stopService();

	public abstract void enQueue(Alarm newAlarm);

	/**
	 * �������뵽���ݿ��У��ȴ����͵�ǰ̨��������
	 * 
	 * @param alarmType
	 * @param alarmSource
	 * @param rd
	 */
	public abstract void insertAlarm(String alarmSource, String alarmType,
			GPSRealData rd);

}