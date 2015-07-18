package com.ltmonitor.jt808.service.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.entity.Alarm;
import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.VehicleData;
import com.ltmonitor.jt808.service.AlarmItem;
import com.ltmonitor.jt808.service.IAlarmService;
import com.ltmonitor.jt808.service.INightDrivingAlarmService;
import com.ltmonitor.service.IRealDataService;
import com.ltmonitor.service.IVehicleService;

public class NightDrivingAlarmService implements INightDrivingAlarmService {
	
	private IRealDataService realDataService;

	private IVehicleService vehicleService;
	/**
	 * �����ʻ�ٶȣ��������ٶ���Ϊ��ʻ�����ڴ��ٶȣ���Ϊͣ��
	 */
	private int minSpeed = 2;
	/**
	 * ֻ���ĳһ���͵ĳ���
	 */
	private String vehicleType;

	private IBaseDao baseDao;

	private IAlarmService alarmService;
	

	private ConcurrentHashMap<String, AlarmItem> alarmMap = new ConcurrentHashMap<String, AlarmItem>();

	private static Logger logger = Logger.getLogger(AlarmService.class);

	public void checkNightDrivingVehicle() {
		logger.error("���ҹ����ʻ");
		try {
			List<GPSRealData> ls = realDataService.getOnlineRealDataList();
			for (GPSRealData rd : ls) {
				String alarmKey = rd.getPlateNo() + "_"
						+ AlarmRecord.TYPE_NIGHT_DRIVING;

				if (rd.getVelocity() < minSpeed || rd.getOnline() == false) {
					if (alarmMap.containsKey(alarmKey))
					{
						this.alarmService.CreateRecord(
								AlarmRecord.ALARM_FROM_PLATFORM,
								AlarmRecord.TYPE_NIGHT_DRIVING, AlarmRecord.TURN_OFF, rd);
					}
					continue;// ��ͣ���У������б�������
				}
				VehicleData vd = vehicleService.getVehicleData(rd.getVehicleId());
				if (vd != null && getVehicleType().equals(getVehicleType())) {
					insertAlarm(AlarmRecord.ALARM_FROM_PLATFORM,
							AlarmRecord.TYPE_NIGHT_DRIVING,  rd);
					// ��������
					if (alarmMap.containsKey(alarmKey) == false) {
						this.alarmService.CreateRecord(
								AlarmRecord.ALARM_FROM_PLATFORM,
								AlarmRecord.TYPE_NIGHT_DRIVING, AlarmRecord.TURN_ON, rd);

					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	/**
	 * �������뵽���ݿ��У��ȴ����͵�ǰ̨��������
	 * 
	 * @param alarmType
	 * @param alarmSource
	 * @param rd
	 */
	private void insertAlarm(String alarmSource, String alarmType,
			GPSRealData rd) {
		try {
			Alarm ar = new Alarm();
			ar.setVehicleId(rd.getVehicleId());
			ar.setPlateNo(rd.getPlateNo());
			ar.setAlarmTime(rd.getSendTime());
			ar.setAckSn(rd.getResponseSn());// �����ն���Ϣ����ˮ�ţ����ն��·��������ʱ����Ҫ����ˮ�Ž������
			ar.setLatitude(rd.getLatitude());
			ar.setLongitude(rd.getLongitude());
			ar.setSpeed(rd.getVelocity());
			ar.setAlarmType(alarmType);
			ar.setAlarmSource(alarmSource);
			ar.setLocation(rd.getLocation());
			this.baseDao.saveOrUpdate(ar);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public IVehicleService getVehicleService() {
		return vehicleService;
	}

	public void setVehicleService(IVehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public int getMinSpeed() {
		return minSpeed;
	}

	public void setMinSpeed(int minSpeed) {
		this.minSpeed = minSpeed;
	}

	public IBaseDao getBaseDao() {
		return baseDao;
	}

	public void setBaseDao(IBaseDao baseDao) {
		this.baseDao = baseDao;
	}

	public IAlarmService getAlarmService() {
		return alarmService;
	}

	public void setAlarmService(IAlarmService alarmService) {
		this.alarmService = alarmService;
	}

	public IRealDataService getRealDataService() {
		return realDataService;
	}

	public void setRealDataService(IRealDataService realDataService) {
		this.realDataService = realDataService;
	}

}
