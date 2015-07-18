package com.ltmonitor.jt808.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.ltmonitor.dao.impl.DaoIbatisImpl;
import com.ltmonitor.entity.Alarm;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.jt808.service.INewAlarmService;
import com.ltmonitor.util.DateUtil;
/**
 * �����������
 * @author v5-552
 *
 */
public class NewAlarmService implements INewAlarmService {
	

	private static Logger logger = Logger.getLogger(NewAlarmService.class);
	private DaoIbatisImpl queryDao;

	private ConcurrentLinkedQueue<Alarm> dataQueue = new ConcurrentLinkedQueue<Alarm>();
	
	private boolean continueProcess = true;
	
	private Thread processAlarmThread;
	

	public void start() {
		try {
			Date today = new Date();
			String alarmTableName = "NewAlarm"
					+ DateUtil.toStringByFormat(today, "yyyyMM");
			queryDao.createNewAlarmTableIfNotExist(alarmTableName);
		} catch (Exception ex) {
			logger.error(ex.getMessage(),ex);
		}
		processAlarmThread = new Thread(new Runnable() {
			public void run() {
				processRealDataThreadFunc();
			}
		});
		processAlarmThread.start();
	}


	public void stopService() {
		continueProcess = false;
		try {
			processAlarmThread.join(50000);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		// processRealDataThread.stop();
	}

	public void enQueue(Alarm newAlarm)
	{
		if(dataQueue.size() > 2000)
		{
			logger.error("�������ж���:"+dataQueue.size());
			this.dataQueue.clear();
		}
		dataQueue.add(newAlarm);
	}
	

	private void processRealDataThreadFunc() {
		//int count = 0;
		while (continueProcess) {
			try {
				Alarm tm = dataQueue.poll();
				final List<Alarm> msgList = new ArrayList<Alarm>();
				while (tm != null) {
					msgList.add(tm);
					if (msgList.size() > 30)
						break;
					tm = dataQueue.poll();
				}
				batchInsertAlarm(msgList);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

			//count++;
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e1) {
			}
		}
	}
	
	private void batchInsertAlarm(List<Alarm> alarmList)
	{
		if(alarmList.size() == 0)
			return;
		String statementName = "insertNewAlarm";
		queryDao.batchInsert(statementName, alarmList);
	}
	
	
	public void insertAlarm(String alarmSource, String alarmType, GPSRealData rd) {
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

			String alarmTalbeName = "NewAlarm"
					+ DateUtil.toStringByFormat(ar.getAlarmTime(), "yyyyMM");
			ar.setTableName(alarmTalbeName);
			this.enQueue(ar);
			// this.baseDao.saveOrUpdate(ar);
			//String statementName = "insertNewAlarm";
			//queryDao.insert(statementName, ar);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	public DaoIbatisImpl getQueryDao() {
		return queryDao;
	}
	public void setQueryDao(DaoIbatisImpl queryDao) {
		this.queryDao = queryDao;
	}
	

}
