package com.ltmonitor.jt808.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.dao.impl.DaoIbatisImpl;
import com.ltmonitor.entity.AlarmConfig;
import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.Enclosure;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.StringUtil;
import com.ltmonitor.jt808.service.IAlarmService;
import com.ltmonitor.jt808.service.IAreaAlarmService;
import com.ltmonitor.jt808.service.INewAlarmService;
import com.ltmonitor.jt808.service.ITransferGpsService;
import com.ltmonitor.service.ILocationService;
import com.ltmonitor.util.DateUtil;

/**
 * ������������
 * 
 * @author DELL
 * 
 */
public class AlarmNewService implements IAlarmService {

	private static Logger logger = Logger.getLogger(AlarmNewService.class);
	private ConcurrentLinkedQueue<AlarmRecord> dataQueue = new ConcurrentLinkedQueue<AlarmRecord>();

	public ConcurrentMap<String, GPSRealData> oldRealDataMap = new ConcurrentHashMap<String, GPSRealData>();
	private IBaseDao baseDao;

	private DaoIbatisImpl queryDao;

	private Thread processRealDataThread;

	// �ڴ��еı������Ѿ������ı���
	private ConcurrentHashMap<String, AlarmRecord> alarmMap = new ConcurrentHashMap<String, AlarmRecord>();
	private Boolean startAnalyze = true;
	// Χ����������
	private IAreaAlarmService areaAlarmService;

	private ITransferGpsService transferGpsService;

	private INewAlarmService newAlarmService;

	private ILocationService locationService;

	// private boolean parkingAlarmEnabled;
	//���� ���ü���
	private Map<String, AlarmConfig> alarmConfigMap = new HashMap<String, AlarmConfig>();

	public AlarmNewService() {

	}
	//���������������
	public void start() {
		this.getAlarmConfig();
		processRealDataThread = new Thread(new Runnable() {
			public void run() {
				processRealDataThreadFunc();
			}
		});
		//���������߳�
		processRealDataThread.start();
		try {
			String hql = "from AlarmRecord where status = ?";
			List ls = this.baseDao.query(hql, AlarmRecord.STATUS_NEW);
			for (Object obj : ls) {
				AlarmRecord r = (AlarmRecord) obj;
				String key = r.getPlateNo() + "_" + r.getChildType() + "_"
						+ r.getType();
				this.alarmMap.put(key, r);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(),ex);
		}
		this.getAlarmConfig();
	}

	@Override
	public void stopService() {
		startAnalyze = false;
		try {
			processRealDataThread.join(50000);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		// processRealDataThread.stop();
	}
	
	//���ر�������
	private void getAlarmConfig() {
		try {
			List ls = this.baseDao.query("from AlarmConfig");
			for (Object obj : ls) {
				AlarmConfig a = (AlarmConfig) obj;
				alarmConfigMap.put(a.getAlarmType() + "_" + a.getAlarmSource(),
						a);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.error(ex.getStackTrace());
		}
	}

	public boolean isAlarmEnabled(String alarmType, String alarmSource) {
		String key = alarmType + "_" + alarmSource;
		if (alarmConfigMap.containsKey(key)) {
			AlarmConfig a = alarmConfigMap.get(key);
			return a.isEnabled();
		}
		return false;
	}
	/**
	 * ������������ں�����ʵʱ���ݻ�ȡ�󣬵��ô˺�������ʼ��������
	 */
	@Override
	public void processRealData(GPSRealData rd) {
		if (dataQueue.size() > 500) {
			logger.error("������¼���ж�������������:" + dataQueue.size());
			// dataQueue.clear();
		}

		this.analyzeData(rd);
	}

	private void processRealDataThreadFunc() {
		//ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
		int count = 0;
		while (startAnalyze) {
			try {
				AlarmRecord tm = dataQueue.poll();
				final List<AlarmRecord> msgList = new ArrayList<AlarmRecord>();
				while (tm != null) {
					msgList.add(tm);
					if (msgList.size() > 30)
						break;
					tm = dataQueue.poll();
				}
				if (msgList.size() > 0) {
					saveAlarmRecord(msgList);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			if (count % 300 == 0) {
				getAlarmConfig();// ���¸����±�������
			}
			count++;
			if(count > Short.MAX_VALUE)
				count = 0;
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e1) {
			}
		}
	}

	/**
	 * ���汨��ͳ�Ƽ�¼
	 * @param msgList
	 */
	private void saveAlarmRecord(List<AlarmRecord> msgList) {

		for (AlarmRecord r : msgList) {
			try {
				// analyzeData(msg);
				if (r.getStatus().equals(AlarmRecord.STATUS_NEW)) {
					this.baseDao.save(r);
				} else {
					this.baseDao.saveOrUpdate(r);
				}
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}

	}

	private void analyzeData(GPSRealData rd) {

		try {
			String newStatus = rd.getStatus();
			String newAlarmState = rd.getAlarmState();
			createChangeRecord(AlarmRecord.STATE_FROM_TERM, newStatus, rd);
			createChangeRecord(AlarmRecord.ALARM_FROM_TERM, newAlarmState, rd);

			// ͣ������
			if (isAlarmEnabled(AlarmRecord.TYPE_PARKING,AlarmRecord.ALARM_FROM_PLATFORM)) {
				// �ж��Ƿ�ͣ��
				String alarmState = rd.getVelocity() < 1 ? AlarmRecord.TURN_ON
						: AlarmRecord.TURN_OFF;
				analyzeAlarm(rd, AlarmRecord.TYPE_PARKING,AlarmRecord.ALARM_FROM_PLATFORM, alarmState);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * ��������
	 * @param rd
	 * @param alarmType
	 * @param alarmSource
	 * @param alarmState
	 */
	private void analyzeAlarm(GPSRealData rd, String alarmType, String alarmSource, String alarmState) {
		String alarmKey = rd.getPlateNo() + "_" + alarmType + "_" + alarmSource;
		if (alarmState.equals(AlarmRecord.TURN_ON)) {
			// ��������
			if (alarmMap.containsKey(alarmKey) == false) {
				AlarmRecord item = new AlarmRecord(rd, alarmType, alarmSource);

				this.dataQueue.add(item);// ������н������
				alarmMap.put(alarmKey, item);// ������־פ�����ڴ��У������´��ж��Ƿ��Ѿ�����
			} else {
				AlarmRecord item = alarmMap.get(alarmKey);
				if (item.getStatus().equals(AlarmRecord.STATUS_OLD)) {
					AlarmRecord itemNew = new AlarmRecord(rd, alarmType,
							alarmSource);
					this.dataQueue.add(itemNew);// ������н������
					alarmMap.put(alarmKey, item);// ������־פ�����ڴ��У������´��ж��Ƿ��Ѿ�����
				}
			}
		} else if (alarmState.equals(AlarmRecord.TURN_OFF)) {
			// ��������,�رձ���
			if (alarmMap.containsKey(alarmKey)) {
				AlarmRecord item = alarmMap.get(alarmKey); // ����в����ı���������Ҫ��������
				// item.setOpen(false);
				alarmMap.remove(alarmKey);
				item.setStatus(AlarmRecord.STATUS_OLD);
				item.setEndTime(rd.getSendTime());
				item.setLatitude1(rd.getLatitude());
				item.setLongitude1(rd.getLongitude());
				double minutes = 0.1 * DateUtil.getSeconds(item.getStartTime(),item.getEndTime()) / 6;
				item.setTimeSpan(minutes);// ���������ʱ��
				this.dataQueue.add(item);
				// this.CreateWarnRecord(AlarmRecord.ALARM_FROM_TERM, alarmType,
				// alarmState, rd);
			}
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
	private void createChangeRecord(String alarmSource, String newStatus,
			GPSRealData rd) {

		char[] newChars = (char[]) newStatus.toCharArray();
		for (int m = 0; m < newChars.length; m++) {
			String alarmState = "" + newChars[m];
			int alarmId = 31 - m; // ����ת��Ϊ����ı������
			String alarmType = "" + alarmId;
			// ת��������Ϣ
			if (alarmSource.equals(AlarmRecord.ALARM_FROM_TERM)
					&& alarmState.equals(AlarmRecord.TURN_ON)
					&& this.isAlarmEnabled(alarmType, alarmSource)) {

				// ת��809����
				if (this.transferGpsService.isTransferTo809Enabled()) {
					transferAlarm(alarmSource, alarmType, rd);
				}

				// if (alarmId == 0) {
				// �����µĽ�������
				newAlarmService.insertAlarm(alarmSource, alarmType, rd);
				// }
			}
			if (alarmSource.equals(AlarmRecord.ALARM_FROM_TERM)) {
				if (alarmId == 20) {
					//�������򱨾�
					alarmType = rd.getEnclosureAlarm() == 0 ? AlarmRecord.TYPE_IN_AREA
							: AlarmRecord.TYPE_CROSS_BORDER;
				} else if (alarmId == 21) {
					//����·�߱���
					alarmType = rd.getEnclosureAlarm() == 0 ? AlarmRecord.TYPE_ON_ROUTE
							: AlarmRecord.TYPE_OFFSET_ROUTE;
				}
			}
			if (this.isAlarmEnabled(alarmType, alarmSource)) {
				this.analyzeAlarm(rd, alarmType, alarmSource, alarmState);
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

	public DaoIbatisImpl getQueryDao() {
		return queryDao;
	}

	public void setQueryDao(DaoIbatisImpl queryDao) {
		this.queryDao = queryDao;
	}

	public IAreaAlarmService getAreaAlarmService() {
		return areaAlarmService;
	}

	public void setAreaAlarmService(IAreaAlarmService areaAlarmService) {
		this.areaAlarmService = areaAlarmService;
	}

	public INewAlarmService getNewAlarmService() {
		return newAlarmService;
	}

	public void setNewAlarmService(INewAlarmService newAlarmService) {
		this.newAlarmService = newAlarmService;
	}

}
