package com.ltmonitor.jt808.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.entity.Alarm;
import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.Enclosure;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.StringUtil;
import com.ltmonitor.jt808.service.AlarmItem;
import com.ltmonitor.jt808.service.IAlarmService;
import com.ltmonitor.jt808.service.IAreaAlarmService;
import com.ltmonitor.jt808.service.ITransferGpsService;
import com.ltmonitor.service.ILocationService;
import com.ltmonitor.util.DateUtil;

/**
 * ������������
 * 
 * @author DELL
 * 
 */
public class AlarmService  {

	private static Logger logger = Logger.getLogger(AlarmService.class);
	private ConcurrentLinkedQueue<GPSRealData> dataQueue = new ConcurrentLinkedQueue();

	public ConcurrentMap<String, GPSRealData> oldRealDataMap = new ConcurrentHashMap<String, GPSRealData>();
	private IBaseDao baseDao;

	private Thread processRealDataThread;

	// �ڴ��еı������Ѿ������ı���
	private ConcurrentHashMap<String, AlarmItem> alarmMap = new ConcurrentHashMap<String, AlarmItem>();
	private Boolean startAnalyze = true;
	// Χ����������
	private IAreaAlarmService areaAlarmService;

	private ITransferGpsService transferGpsService;

	private ILocationService locationService;

	private boolean parkingAlarmEnabled;

	public AlarmService() {
		processRealDataThread = new Thread(new Runnable() {
			public void run() {
				processRealDataThreadFunc();
			}
		});

		processRealDataThread.start();

	}

	public void stopService() {
		startAnalyze = false;
		try {
			processRealDataThread.join(50000);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		// processRealDataThread.stop();
	}

	public void processRealData(GPSRealData rd) {
		// if (processRealDataThread.isAlive() == false) {
		// }
		dataQueue.add(rd);
	}

	private void processRealDataThreadFunc() {
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
		while (startAnalyze) {
			try {
				GPSRealData tm = dataQueue.poll();
				final List<GPSRealData> msgList = new ArrayList<GPSRealData>();
				while (tm != null) {
					msgList.add(tm);
					if (msgList.size() > 30)
						break;
					tm = dataQueue.poll();
				}
				if (msgList.size() > 0) {
					fixedThreadPool.execute(new Runnable() {
						@Override
						public void run() {
							analyzeData(msgList);
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					});
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e1) {
			}
		}
	}

	private void analyzeData(List<GPSRealData> msgList) {

		for (GPSRealData msg : msgList) {
			try {
				analyzeData(msg);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}

	}

	private void analyzeData(GPSRealData rd) {
		GPSRealData oldRd = GetOldRealData(rd.getSimNo());
		if (oldRd == null)
			return;

		try {
			String oldStatus = oldRd.getStatus();
			String newStatus = rd.getStatus();
			String oldAlarmState = oldRd.getAlarmState();
			String newAlarmState = rd.getAlarmState();
			createChangeRecord(AlarmRecord.STATE_FROM_TERM, oldStatus,
					newStatus, rd);
			createChangeRecord(AlarmRecord.ALARM_FROM_TERM, oldAlarmState,
					newAlarmState, rd);

			// ͣ������
			if (parkingAlarmEnabled)
				parkingAlarm(rd);
			
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		oldRd.setAlarmState(rd.getAlarmState());
		oldRd.setStatus(rd.getStatus());
		oldRd.setSendTime(rd.getSendTime());
		// �����ڻ����У��ȴ���һ�����ݱȶ�
		/**
		 * GPSRealData oldRd = oldRealDataMap.get(rd.getSimNo()); if (oldRd ==
		 * null || oldRd.getOnline() == false) { // ���߼�¼
		 * this.createOnlineChangeRecord(rd); }
		 */

	}

	private void analyzeAlarm(GPSRealData rd, String alarmType,
			String alarmState) {
		String alarmKey = rd.getPlateNo() + "_" + AlarmRecord.TYPE_PARKING;
		if (alarmState.equals(AlarmRecord.TURN_ON)) {
			// ��������
			if (alarmMap.containsKey(alarmKey) == false) {
				// ��һ��ͣ��
				//AlarmItem item = new AlarmItem(alarmKey, rd.getSendTime());
				//this.CreateWarnRecord(AlarmRecord.ALARM_FROM_TERM, alarmType,
				//		alarmState, rd);
				//alarmMap.put(alarmKey, item);// ������־פ�����ڴ��У������´��ж��Ƿ��Ѿ�����
			}
		} else if (alarmState.equals(AlarmRecord.TURN_OFF)) {
			// ��������,�رձ���
			if (alarmMap.containsKey(alarmKey)) {
				AlarmItem item = alarmMap.get(alarmKey); // ����в����ı���������Ҫ��������
				//item.setOpen(false);
				alarmMap.remove(alarmKey);
				this.CreateWarnRecord(AlarmRecord.ALARM_FROM_TERM, alarmType,
						alarmState, rd);
			}
		}
	}

	private void parkingAlarm(GPSRealData rd) {
		// �ж��Ƿ�ͣ��
		String alarmState = rd.getVelocity() < 1 ? AlarmRecord.TURN_ON : AlarmRecord.TURN_OFF;
		analyzeAlarm(rd, AlarmRecord.TYPE_PARKING, alarmState);
	}

	/**
	 * �������뵽���ݿ��У��ȴ����͵�ǰ̨��������
	 * 
	 * @param alarmType
	 * @param alarmSource
	 * @param rd
	 */
	public void insertAlarm(String alarmSource, String alarmType,
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

	/**
	 * ����״̬λ�仯�ļ�¼����¼�仯����ֹʱ��ͼ��������γ������
	 * 
	 * @param alarmSource
	 * @param oldStatus
	 * @param newStatus
	 * @param rd
	 */
	private void createChangeRecord(String alarmSource, String oldStatus,
			String newStatus, GPSRealData rd) {

		if (alarmSource.equals(AlarmRecord.ALARM_FROM_TERM)) {
			char[] newChars = (char[]) newStatus.toCharArray();
			for (int m = 0; m < newChars.length; m++) {
				String alarmState = "" + newChars[m];
				int alarmId = 31 - m; // ����ת��Ϊ����ı������
				String alarmType = "" + alarmId;
				// ת��������Ϣ
				if (alarmState.equals("1")) {
					// ת��809����
					if (this.transferGpsService.isTransferTo809Enabled()) {
						transferAlarm(alarmSource, alarmType, rd);
					}
					// �����µı���
					insertAlarm(alarmSource, alarmType, rd);
				}

			}
		}

		if (StringUtil.isNullOrEmpty(oldStatus)
				|| StringUtil.isNullOrEmpty(newStatus)
				|| oldStatus.length() != newStatus.length()
				|| oldStatus.equals(newStatus))
			return;

		char[] oldChars = (char[]) oldStatus.toCharArray();

		char[] newChars = (char[]) newStatus.toCharArray();

		for (int m = 0; m < oldChars.length; m++) {
			// ת��������Ϣ
			if (newChars[m] == 1
					&& alarmSource.equals(AlarmRecord.ALARM_FROM_TERM)
					&& this.transferGpsService.isTransferTo809Enabled())
				transferAlarm("" + m, alarmSource, rd);

			if (oldChars[m] != newChars[m]) {
				int alarmId = 31 - m; // ����ת��Ϊ����ı������
				String alarmType = "" + alarmId;
				String alarmState = "" + newChars[m];

				if (alarmId == 20) {
					alarmType = rd.getEnclosureAlarm() == 0 ? AlarmRecord.TYPE_IN_AREA
							: AlarmRecord.TYPE_CROSS_BORDER;
					// alarmState = ""+rd.getEnclosureAlarm();
				} else if (alarmId == 21) {
					alarmType = rd.getEnclosureAlarm() == 0 ? AlarmRecord.TYPE_ON_ROUTE
							: AlarmRecord.TYPE_OFFSET_ROUTE;
					// alarmState = ""+rd.getEnclosureAlarm();
				}

				CreateWarnRecord(alarmSource, alarmType, alarmState, rd);
			}
		}
	}

	/**
	 * ����������¼ OperateType��ʾ���ͣ�״̬λ�仯(State)���Ǳ�����־λ�仯(Warn)�� childType
	 * ��ʾ��־λ���ֽ�32λ���, ��Acc��־λ��32��״̬λ�ĵ�һ��λ��,���ٱ����ڱ���λ�ĵڶ���λ��. warnState ��1�������� ��
	 * 0����������
	 */
	private void CreateWarnRecord(String OperateType, String childType,
			String warnState, GPSRealData rd) {
		AlarmRecord sr = CreateRecord(OperateType, childType, warnState, rd);
		if (sr != null)
			getBaseDao().saveOrUpdate(sr);
	}

	/**
	 * ������ת�����ϼ�ƽ̨
	 */
	private void transferAlarm(String alarmSource, String alarmType,
			GPSRealData rd) {
		if (transferGpsService.isTransferTo809Enabled() == false)
			return;
		AlarmRecord ar = new AlarmRecord();
		ar.setPlateNo(rd.getPlateNo());
		ar.setStartTime(rd.getSendTime());
		ar.setChildType(alarmType);
		ar.setType(alarmSource);
		ar.setVehicleId(rd.getVehicleId());
		this.transferGpsService.transfer(ar, rd);
	}

	public AlarmRecord CreateRecord(String alarmSource, String alarmType,
			String alarmState, GPSRealData rd) {
		String hsql = "from AlarmRecord rec where startTime > ? and  rec.plateNo = ? and rec.status = ? and rec.type = ? and rec.childType = ?";
		// �鿴�Ƿ���δ�����ı�����¼
		Date startDate = DateUtil.getDate(new Date(), Calendar.DAY_OF_YEAR, -5);
		AlarmRecord sr = (AlarmRecord) getBaseDao().find(
				hsql,
				new Object[] { startDate, rd.getPlateNo(),
						AlarmRecord.STATUS_NEW, alarmSource, alarmType });

		if (sr == null) {
			if (AlarmRecord.TURN_OFF.equals(alarmState))
				return null;

			sr = new AlarmRecord();
			// ͣ������
			if (alarmType.equals("19")) {
				/**
				 * Enclosure ec = IsInEnclosure(rd); if (ec != null) {
				 * sr.Station = ec.Name; sr.Location = ec.Name; }
				 */
			}

			if (rd.getEnclosureId() > 0) {
				String hql = "from Enclosure where enclosureId = ?";
				Enclosure ec = (Enclosure) baseDao.find(hql,
						rd.getEnclosureId());
				if (ec != null) {
					sr.setLocation(ec.getName());
				}

			}
			sr.setVehicleId(rd.getVehicleId());
			sr.setType(alarmSource);
			sr.setPlateNo(rd.getPlateNo());
			sr.setStartTime(rd.getSendTime());
			sr.setStatus(AlarmRecord.STATUS_NEW);
			sr.setEndTime(new Date());
			sr.setLatitude(rd.getLatitude());
			sr.setLongitude(rd.getLongitude());
			String location = rd.getLocation();
			if (StringUtil.isNullOrEmpty(location))
				location = locationService.getLocation(sr.getLatitude(),
						sr.getLongitude());
			sr.setLocation(location);
			sr.setVelocity(rd.getVelocity());
			sr.setChildType(alarmType);
			sr.setResponseSn(rd.getResponseSn());

		} else {
			sr.setEndTime(new Date());
			double minutes = DateUtil.getSeconds(sr.getStartTime(),
					rd.getSendTime()) / 60;
			sr.setTimeSpan(minutes);// ���������ʱ��
			if (alarmState.equals(AlarmRecord.TURN_OFF)) {
				sr.setStatus(AlarmRecord.STATUS_OLD);
				sr.setEndTime(rd.getSendTime());

				sr.setLatitude1(rd.getLatitude());
				sr.setLongitude1(rd.getLongitude());
			} else
				return null;

		}

		sr.setType(alarmSource);
		sr.setChildType(alarmType);
		return sr;
	}

	private GPSRealData getRalData(String plateNo) {
		GPSRealData rd = oldRealDataMap.get(plateNo);
		if (rd == null) {
			rd = new GPSRealData();
			rd.setOnline(false);
		}
		return rd;
	}


	/**
	 * �ӻ�����ȡ���ɵ�ʵʱ���ݣ����бȶԣ���������״̬λ������״̬�ı仯
	 */
	public GPSRealData GetOldRealData(String simNo) {
		GPSRealData oldRd = null;
		if (oldRealDataMap.containsKey(simNo)) {
			oldRd = (GPSRealData) oldRealDataMap.get(simNo);
		}

		if (oldRd == null) {
			oldRd = new GPSRealData();
			oldRd.setOnline(false);
			oldRd.setAlarmState(String.format("%032d", 0));
			oldRd.setStatus(String.format("%032d", 0));
			oldRd.setSendTime(new Date());
			oldRd.setSimNo(simNo);
			oldRealDataMap.put(simNo, oldRd);
		}

		return oldRd;
	}

	public IBaseDao getBaseDao() {
		return baseDao;
	}

	public void setBaseDao(IBaseDao baseDao) {
		this.baseDao = baseDao;
	}


	public ITransferGpsService getTransferGpsService() {
		return transferGpsService;
	}

	public void setTransferGpsService(ITransferGpsService transferGpsService) {
		this.transferGpsService = transferGpsService;
	}

	public ILocationService getLocationService() {
		return locationService;
	}

	public void setLocationService(ILocationService locationService) {
		this.locationService = locationService;
	}

	public boolean isParkingAlarmEnabled() {
		return parkingAlarmEnabled;
	}

	public void setParkingAlarmEnabled(boolean parkingAlarmEnabled) {
		this.parkingAlarmEnabled = parkingAlarmEnabled;
	}

	public IAreaAlarmService getAreaAlarmService() {
		return areaAlarmService;
	}

	public void setAreaAlarmService(IAreaAlarmService areaAlarmService) {
		this.areaAlarmService = areaAlarmService;
	}

}
