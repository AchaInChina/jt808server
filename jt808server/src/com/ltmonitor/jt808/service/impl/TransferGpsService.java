package com.ltmonitor.jt808.service.impl;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.ltmonitor.app.ServiceLauncher;
import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.BasicData;
import com.ltmonitor.entity.EWayBill;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.GnssData;
import com.ltmonitor.entity.TakePhotoModel;
import com.ltmonitor.entity.VehicleData;
import com.ltmonitor.entity.WarnData;
import com.ltmonitor.jt808.service.ITransferGpsService;
import com.ltmonitor.jt809.entity.DriverModel;
import com.ltmonitor.jt809.entity.VehicleRegisterInfo;
import com.ltmonitor.service.IBasicDataService;
import com.ltmonitor.service.ITransferService;

public class TransferGpsService implements ITransferGpsService {

	private Thread processRealDataThread;

	private ITransferService tService;
	
	private IBasicDataService basicDataService;

	private IBaseDao baseDao;
	/**
	 * �����Ƿ�ת���Ŀ���
	 */
	private boolean transferTo809Enabled;
	/**
	 * �Ƿ�ת������
	 */
	private boolean transferAlarmTo809Enabled;
	
	private boolean continueTransfer;

	private static Logger logger = Logger.getLogger(AckService.class);
	private ConcurrentLinkedQueue<GnssData> dataQueue = new ConcurrentLinkedQueue<GnssData>();

	public void start() {
		continueTransfer = true;
		processRealDataThread = new Thread(new Runnable() {
			public void run() {
				ProcessRealDataThreadFunc();
			}
		});
		processRealDataThread.start();
	}
	
	public void stop()
	{
		continueTransfer = false;
		if(processRealDataThread != null){
			try {
				processRealDataThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void ProcessRealDataThreadFunc() {
		while (continueTransfer) {
			try {
				if(dataQueue.size() > 3000)
				{
					logger.error("ת�����й���ֱ�����");
					dataQueue.clear();
				}
				GnssData tm = dataQueue.poll();
				while (tm != null)
				{
					transferTo809(tm);
					tm = dataQueue.poll();
				}
			} catch (Exception ex) {
				logger.error(ex.getMessage());
				//logger.error(ex.getStackTrace());
			}
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e1) {
			}
		}
	}

	/**
	 * ת����Ƭ����
	 * 
	 * @param ph
	 */
	@Override
	public void transfer(TakePhotoModel ph) {
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpCtrlMsgTakePhotoAck(ph);
	}
	
	public void transferRegisterInfo(VehicleRegisterInfo vm)
	{
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpExgMsgRegister(vm);
	}

	/**
	 * ת����¼������
	 * 
	 * @param plateNo
	 * @param plateColor
	 * @param cmdType
	 *            ��¼��������
	 * @param cmdData
	 *            ��¼���ֽ�����
	 */
	@Override
	public void transferRecorderData(String plateNo, int plateColor,
			byte cmdType, byte[] cmdData) {
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpCtrlMsgTakeTravelAck(plateNo, plateColor,
				cmdType, cmdData);
	}
	
	public void transferDriverInfo(DriverModel d)
	{
		if(transferTo809Enabled == false || getTransferService() == null)
			return;
		getTransferService().UpExgMsgReportDriverInfo(d);
	}
	

	/**
	 * �ն��·���ϢӦ��
	 */
	@Override
	public void transferTextInfoAck(String plateNo, int plateColor, int msgId,
			byte result) {
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpCtrlMsgTextInfoAck(plateNo, plateColor, msgId,
				result);
	}
	/**
	 * ��������Ӧ��
	 */
	@Override
	public void transferEmergencyAccessAck(String plateNo, int plateColor,
			byte result) {
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpCtrlMsgEmergencyMonitoringAck(plateNo,
				plateColor, result);
	}

	@Override
	public void transferListenTerminalAck(String plateNo, int plateColor,
			byte result) {
		if(transferTo809Enabled == false)
			return;
		getTransferService().UpCtrlMsgMonitorVehicleAck(plateNo, plateColor,
				result);
	}

	/**
	 * ת�������˵�
	 * 
	 * @param ebill
	 */
	@Override
	public void transfer(EWayBill ebill) {
		if(transferTo809Enabled == false || getTransferService() == null)
			return;
		getTransferService().UpExgMsgReportTakeEWayBill(ebill.getPlateNo(),
				ebill.getPlateColor(), ebill.geteContent());
	}

	private ITransferService getTransferService() {
		if (tService == null) {
			try {
				tService = (ITransferService) ServiceLauncher
						.getBean("transferService");
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		return tService;
	}

	/**
	 * ת��ʵʱ����
	 */
	@Override
	public void transfer(GnssData gd) {
		// ���û�д�ת�����񣬽��ر�ת��
		if (this.transferTo809Enabled == false)
			return;

		if (tService == null) {
			try {
				tService = (ITransferService) ServiceLauncher
						.getBean("transferService");
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
		dataQueue.add(gd);
	}

	private VehicleData getVehicleData(String plateNo) {
		String hsql = "from VehicleData where plateNo = ?";
		if(baseDao == null)
		{
			baseDao = ServiceLauncher.getBaseDao();
		}
		VehicleData vd = (VehicleData) baseDao.find(hsql, plateNo);
		return vd;
	}

	@Override
	public void transfer(AlarmRecord ar, GPSRealData rd) {
		if(transferTo809Enabled == false || getTransferService()==null)
			return;
		String alarmType = ar.getChildType();
		VehicleData vd = getVehicleData(rd.getPlateNo());
		if(vd == null)
		{
			logger.error("ת��ʱ��û�д˳��ƺ�:"+ rd.getPlateNo() + "��Ӧ�ĳ�������");
			return;
		}
		WarnData wd = new WarnData();
		wd.setPlateNo(rd.getPlateNo());
		wd.setPlateColor(vd.getPlateColor());
		wd.setInfoId(ar.getEntityId());
		if(AlarmRecord.ALARM_FROM_PLATFORM.equals(ar.getType()))
			wd.setSrc(WarnData.FROM_PLATFROM);
		else
		    wd.setSrc(WarnData.FROM_TERMINAL);
		wd.setWarnTime(rd.getSendTime());
		BasicData bd = basicDataService.getBasicDataByCode(ar.getChildType(), "AlarmType");
		wd.setContent(bd!= null ? bd.getName() : "");
		int warnType = getWarnType(alarmType);
		wd.setType(warnType);
		getTransferService().UpWarnMsgAdptInfo(wd);
	}

	private int getWarnType(String alarmType) {
		int warnType = 0;
		if (alarmType.equals("0"))
			warnType = WarnData.EMERGENCY;
		else if (alarmType.equals("1"))
			warnType = WarnData.OVER_SPEED;
		else if (alarmType.equals("2"))
			warnType = WarnData.TIRED;
		else if (alarmType.equals(AlarmRecord.TYPE_IN_AREA))
			warnType = WarnData.IN_AREA;
		else if (alarmType.equals(AlarmRecord.TYPE_CROSS_BORDER))
			warnType = WarnData.OUT_AREA;
		else if (alarmType.equals(AlarmRecord.TYPE_OFFSET_ROUTE))
			warnType = WarnData.OFFSET_ROUTE;
		else
			warnType = WarnData.OTHER;
		return warnType;
	}
	

	private void transferTo809(GnssData gd) {
		if(transferTo809Enabled == false)
			return;
		// ����ʵʱ��λ��Ϣ
		tService.UpExgMsgRealLocation(gd);
	}

	public IBaseDao getBaseDao() {
		return baseDao;
	}

	public void setBaseDao(IBaseDao baseDao) {
		this.baseDao = baseDao;
	}

	public IBasicDataService getBasicDataService() {
		return basicDataService;
	}

	public void setBasicDataService(IBasicDataService basicDataService) {
		this.basicDataService = basicDataService;
	}

	public boolean isTransferTo809Enabled() {
		return transferTo809Enabled;
	}

	public void setTransferTo809Enabled(boolean transferTo809Enabled) {
		this.transferTo809Enabled = transferTo809Enabled;
	}

	public boolean isTransferAlarmTo809Enabled() {
		return transferTo809Enabled && transferAlarmTo809Enabled;
	}

	public void setTransferAlarmTo809Enabled(boolean transferAlarmTo809Enabled) {
		this.transferAlarmTo809Enabled = transferAlarmTo809Enabled;
	}


}
