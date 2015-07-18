package com.ltmonitor.jt808.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.dao.impl.DaoIbatisImpl;
import com.ltmonitor.entity.AlarmRecord;
import com.ltmonitor.entity.Enclosure;
import com.ltmonitor.entity.EnclosureBinding;
import com.ltmonitor.entity.GPSRealData;
import com.ltmonitor.entity.LineSegment;
import com.ltmonitor.entity.PointLatLng;
import com.ltmonitor.entity.StringUtil;
import com.ltmonitor.jt808.service.AlarmItem;
import com.ltmonitor.jt808.service.IAreaAlarmService;
import com.ltmonitor.jt808.service.INewAlarmService;
import com.ltmonitor.service.IRealDataService;
import com.ltmonitor.service.MapFixService;
import com.ltmonitor.util.Constants;
import com.ltmonitor.util.DateUtil;

/**
 * ���򱨾���·��ƫ�Ʊ�����������
 * 
 * @author DELL
 * 
 */
public class AreaAlarmService implements IAreaAlarmService {

	protected Logger logger = Logger.getLogger(getClass());

	public static String TURN_ON = "1"; // ������
	public static String TURN_OFF = "0"; // �����ر�
	private IBaseDao baseDao;

	private INewAlarmService newAlarmService;

	private boolean areaAlarmEnabled;

	private IRealDataService realDataService;

	private DaoIbatisImpl queryDao;

	/**
	 * ��������������߳�
	 */
	private Thread analyzeThread;

	private boolean continueAnalyze = true;

	// ���泵����ǰ���ڵذ���
	private Map offsetRouteWarn = new HashMap();
	private Map onRouteWarn = new HashMap();
	private Map routePointMap = new HashMap();
	private Map<String, AlarmItem> alarmMap = new HashMap<String, AlarmItem>();

	Map<String,GPSRealData> realDataMap = new HashMap<String,GPSRealData>();
	public ConcurrentMap<String, Enclosure> enclosureMap = new ConcurrentHashMap<String, Enclosure>();
	public ConcurrentMap<String, LineSegment> overSpeedMap = new ConcurrentHashMap<String, LineSegment>();

	public void start() {
		if (areaAlarmEnabled) {
			analyzeThread = new Thread(new Runnable() {
				public void run() {
					analyzeThreadFunc();
				}
			});
			analyzeThread.start();
		}
	}

	private void analyzeThreadFunc() {
		while (continueAnalyze) {

			analyze();
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException e1) {
			}
		}
	}

	public void stopService() {
		continueAnalyze = false;
		if(analyzeThread == null)
			return;
		try {
			analyzeThread.join(50000);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private void analyze() {
		Map params = new HashMap();
		Date d = new Date();
		d = DateUtil.getDate(d, Calendar.MINUTE, -5);
		params.put("onlineDate", d);// ֻ�����߳����İ�����
		String queryId = "selectAllBindAreas";// ��ѯƽ̨���а󶨵�����sql�����Ibatis�����ļ�vehicleInfo.xml��Ӧ��querID��
		List bindings = queryDao.queryForList(queryId, params);
		Map<Integer, Enclosure> areaMap = new HashMap<Integer, Enclosure>();
		for (Object obj : bindings) {
			Map eb = (Map) obj;
			int areaId = Integer.parseInt("" + eb.get("enclosureId"));
			int bindId = Integer.parseInt("" + eb.get("bindId"));
			String simNo = "" + eb.get("simNo");

			GPSRealData rd = this.realDataService.get(simNo);
			if (rd != null) {
				GPSRealData oldRd = realDataMap.get(simNo);
				if(oldRd!=null)
				{
					if(oldRd.getLatitude() == rd.getLatitude() && oldRd.getLongitude() == rd.getLongitude())
					{
						continue;//�������û�仯��û��Ҫ��������
					}
				}else
				{
					oldRd = new GPSRealData();
				}

				oldRd.setLatitude(rd.getLatitude());
				oldRd.setLongitude(rd.getLongitude());
				realDataMap.put(simNo, oldRd);
				
				Enclosure ec = areaMap.get(areaId);
				if (ec == null) {
					ec = (Enclosure) this.baseDao.load(Enclosure.class, areaId);
					areaMap.put(areaId, ec);
				}
				
				this.AnalyzeAreaAlarm(rd, ec);
			}
		}
	}

	// ��ǰʱ���Ƿ��ڰ��ߵļ������ʱ�����
	private Boolean IsInTimeSpan(Enclosure br) {
		/**
		 * String str = new Date().toString("HH:mm"); Date now = new Date();
		 * Date start = Date.Parse(new Date().ToString("yyyy-MM-dd ") +
		 * br.StartTime); Date end = Date.Parse(new
		 * Date().ToString("yyyy-MM-dd ") + br.EndTime);
		 * 
		 * return now.CompareTo(start) >= 0 && now.CompareTo(end) <= 0 ;
		 */
		return true;
	}

	/**
	 * �����ֶ����ٳ��ٱ���
	 * 
	 * @param rd
	 * @param seg
	 */
	private void AnalyzeOverSpeed(GPSRealData rd, LineSegment seg, Enclosure ec) {
		String key = rd.getPlateNo();
		LineSegment alarmSeg = overSpeedMap.get(key);

		if (seg == null && alarmSeg != null) {
			// ���ƫ����·�λ����뿪��·�Σ���������
			this.overSpeedMap.remove(key);
			CreateWarnRecord(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_OVER_SPEED_ON_ROUTE, TURN_OFF, rd,
					alarmSeg.getEntityId(), null);
		}

		// �鿴�Ƿ�򿪷ֶ����ٿ���
		if (seg == null || seg.getLimitSpeed() == false)
			return;

		// ��ȡ��ǰ�ڳ��ٱ�����·��
		if (alarmSeg != null && alarmSeg.getEntityId() != seg.getEntityId()) {
			// ��������
			this.overSpeedMap.remove(key);
			CreateWarnRecord(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_OVER_SPEED_ON_ROUTE, TURN_OFF, rd,
					alarmSeg.getEntityId(), null);
			alarmSeg = null;
		}
		// ����
		if (rd.getVelocity() > seg.getMaxSpeed()) {
			insertAlarm(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_OVER_SPEED_ON_ROUTE, rd, ec.getName()
							+ ",�յ�:" + seg.getName());
			if (alarmSeg == null) {
				// ��һ�α���
				CreateWarnRecord(AlarmRecord.ALARM_FROM_PLATFORM,
						AlarmRecord.TYPE_OVER_SPEED_ON_ROUTE, TURN_ON, rd,
						seg.getEntityId(),
						ec.getName() + ",·��:" + seg.getName());
				this.overSpeedMap.put(key, seg);
			}

		} else {
			if (alarmSeg != null) {
				// ��������
				this.overSpeedMap.remove(key);
				CreateWarnRecord(AlarmRecord.ALARM_FROM_PLATFORM,
						AlarmRecord.TYPE_OVER_SPEED_ON_ROUTE, TURN_OFF, rd,
						seg.getEntityId(), null);
			}
		}
	}

	private void AnalyzeOffsetRoute(GPSRealData rd, Enclosure ec, PointLatLng mp) {
		Date start = new Date();
		String alarmType = AlarmRecord.TYPE_OFFSET_ROUTE;
		String alarmSource = AlarmRecord.ALARM_FROM_PLATFORM;
		int maxAllowedOffsetTime = ec.getOffsetDelay(); // �ƫ��ʱ��
		if (IsInTimeSpan(ec)) {
			// �ж��Ƿ��ڰ�����
			LineSegment seg = IsInLineSegment(mp, ec);
			boolean isOnRoute = seg != null;
			logger.info(rd.getPlateNo() + ",����" + ec.getName() + ","
					+ (seg == null ? "ƫ��" : "��·����") + rd.getSendTime());
			String alarmKey = rd.getPlateNo() + "_" + ec.getEntityId();
			AlarmItem offsetAlarm = (AlarmItem) offsetRouteWarn.get(alarmKey);
			AlarmItem onRouteAlarm = (AlarmItem) onRouteWarn.get(alarmKey);
			if (isOnRoute == false) {
				if (offsetAlarm == null) {
					this.AnalyzeOverSpeed(rd, null, ec);
					offsetAlarm = new AlarmItem(rd, alarmType, alarmSource);
					// ��һ��ƫ��
					offsetRouteWarn.put(alarmKey, offsetAlarm);
					offsetAlarm.setStatus("");
				}
				Date offsetTime = offsetAlarm.getAlarmTime();
				double ts = DateUtil.getSeconds(offsetTime, rd.getSendTime());
				// �����������ƫ��ʱ��
				if (ts > maxAllowedOffsetTime
						&& offsetAlarm.getStatus().equals("")) {
					offsetAlarm.setStatus(AlarmRecord.STATUS_NEW); // ����Ϊ����״̬
					this.insertAlarm(alarmSource, alarmType, rd, ec.getName());
					// ƫ�뱨����ʼ
					Date originTime = rd.getSendTime();
					rd.setSendTime(offsetTime);
					// ����ƫ�Ƽ�¼
					AlarmRecord sr = CreateRecord(
							AlarmRecord.ALARM_FROM_PLATFORM,
							AlarmRecord.TYPE_OFFSET_ROUTE, TURN_ON, rd,
							ec.getEntityId());
					if (sr != null) {
						sr.setStation(ec.getEntityId());
						sr.setLocation(ec.getName());
						baseDao.saveOrUpdate(sr);
					}
					// �رս�����·������¼
					sr = CreateRecord(AlarmRecord.ALARM_FROM_PLATFORM,
							AlarmRecord.TYPE_ON_ROUTE, TURN_OFF, rd,
							ec.getEntityId());
					if (sr != null) {
						sr.setStation(ec.getEntityId());
						sr.setLocation(ec.getName());
						baseDao.saveOrUpdate(sr);
					}
					rd.setSendTime(originTime);
					onRouteWarn.remove(alarmKey);
				}

			} else {
				AnalyzeOverSpeed(rd, seg, ec);// �ֶ����ٱ���
				if (offsetAlarm != null) {
					offsetAlarm.setStatus(AlarmRecord.STATUS_OLD);// �����ر�
					logger.info(rd.getPlateNo() + "�ػ�·��," + rd.getSendTime());
					// ���»ص�·���У������ر�
					CreateWarnRecord(AlarmRecord.ALARM_FROM_PLATFORM,
							AlarmRecord.TYPE_OFFSET_ROUTE, TURN_OFF, rd,
							ec.getEntityId(), null);
					offsetRouteWarn.remove(alarmKey);
				}
				if (onRouteAlarm == null) {
					onRouteAlarm = new AlarmItem(rd, AlarmRecord.TYPE_ON_ROUTE,
							alarmSource);
					// ��һ�ν���·��
					onRouteWarn.put(alarmKey, onRouteAlarm);
					// ͬʱ����������·�ļ�¼
					AlarmRecord sr = CreateRecord(
							AlarmRecord.ALARM_FROM_PLATFORM,
							AlarmRecord.TYPE_ON_ROUTE, TURN_ON, rd,
							ec.getEntityId());
					if (sr != null) {
						sr.setStation(ec.getEntityId());
						sr.setLocation(ec.getName());
						baseDao.saveOrUpdate(sr);
					}
				}
			}
		}

		double ts2 = DateUtil.getSeconds(start, new Date());
		if (ts2 > 0.2)
			logger.info(rd.getPlateNo() + "," + ec.getName() + "����·ƫ�ƺ�ʱ" + ts2);

	}

	/**
	 * ����������¼
	 */
	private void CreateWarnRecord(String OperateType, String childType,
			String warnState, GPSRealData rd, int enclosureId, String location) {
		AlarmRecord sr = CreateRecord(OperateType, childType, warnState, rd,
				enclosureId);
		if (sr != null) {
			if (location != null)
				sr.setLocation(location);
			baseDao.saveOrUpdate(sr);
		}
	}

	private LineSegment IsInLineSegment(PointLatLng mp, Enclosure ec) {
		logger.info("��ʼ�����·:" + ec.getName());
		List segments = GetLineSegments(ec.getEntityId());
		for (Object obj : segments) {
			LineSegment seg = (LineSegment) obj;
			/**
			 * �ɵĻ������㷨 //List<PointLatLng> bps =
			 * GetBufferPoints(seg.getEntityId());
			 * 
			 * //Boolean result = MapFixService.IsInPolygon(mp, bps);
			 */
			PointLatLng p1 = new PointLatLng(seg.getLongitude1(),
					seg.getLatitude1());
			PointLatLng p2 = new PointLatLng(seg.getLongitude2(),
					seg.getLatitude2());
			boolean result = MapFixService.IsPointOnLine(p1, p2, mp,
					seg.getLineWidth());
			if (result)
				return seg;
		}

		return null;
	}

	private List<LineSegment> GetLineSegments(int enclosureId) {
		String hsql = "from LineSegment where enclosureId = ?";
		List ls = baseDao.query(hsql, enclosureId);
		return ls;
	}

	private Enclosure getOldEnclosure(String plateNo, int enclosureId) {
		Enclosure oldEc = null;
		String key = plateNo + "_" + enclosureId;
		if (enclosureMap.containsKey(key)) {
			oldEc = enclosureMap.get(key);
		}
		return oldEc;
	}

	private void CrossBorder(Enclosure ec, GPSRealData rd, boolean isInEnclosure) {
		Enclosure oldEc = getOldEnclosure(rd.getPlateNo(), ec.getEntityId());
		if (isInEnclosure && oldEc == null) {
			insertAlarm(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_IN_AREA, rd, ec.getName());

			// ˵����һ�ν���Χ��
			CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_IN_AREA, TURN_ON, rd, ec);

			// CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
			// AlarmRecord.TYPE_CROSS_BORDER, TURN_OFF, rd, ec);

			String key = rd.getPlateNo() + "_" + ec.getEntityId();
			enclosureMap.put(key, ec); // �������ڴ��У�������ǰ���������Χ����
		}

		if (isInEnclosure == false && oldEc != null) {

			insertAlarm(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_CROSS_BORDER, rd, ec.getName());

			// ��һ���뿪Χ��
			// CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
			// AlarmRecord.TYPE_CROSS_BORDER, TURN_ON, rd, ec);

			CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
					AlarmRecord.TYPE_IN_AREA, TURN_OFF, rd, ec);
			String key = rd.getPlateNo() + "_" + ec.getEntityId();
			enclosureMap.remove(key);// ������ǰ�����������Χ����
		}

	}

	// ����Χ��������¼
	private void CreateAlarmRecord(String OperateType, String childType,
			String warnState, GPSRealData rd, Enclosure ec) {
		AlarmRecord sr = CreateRecord(OperateType, childType, warnState, rd,
				ec.getEntityId());
		if (sr != null && ec != null) {
			sr.setStation(ec.getEntityId());
			sr.setLocation(ec.getName());
		}
		if (sr != null)
			baseDao.saveOrUpdate(sr);
	}

	private AlarmRecord CreateRecord(String alarmSource, String alarmType,
			String alarmState, GPSRealData rd, int stationId) {
		String hsql = "from AlarmRecord rec where rec.plateNo = ? and rec.status = ? and rec.type = ? and rec.childType = ? and station = ?";
		// �鿴�Ƿ���δ�����ı�����¼
		AlarmRecord sr = (AlarmRecord) baseDao.find(hsql,
				new Object[] { rd.getPlateNo(), AlarmRecord.STATUS_NEW,
						alarmSource, alarmType, stationId });

		if (sr == null) {
			if (TURN_OFF.equals(alarmState))
				return null;

			sr = new AlarmRecord();
			sr.setVehicleId(rd.getVehicleId());
			sr.setPlateNo(rd.getPlateNo());
			sr.setStartTime(rd.getSendTime());
			sr.setStatus(AlarmRecord.STATUS_NEW);
			sr.setEndTime(new Date());
			sr.setLatitude(rd.getLatitude());
			sr.setLongitude(rd.getLongitude());
			// sr.setLocation( MapFix.GetLocation(sr.getLongitude(),
			// sr.getLatitude()));
			sr.setVelocity(rd.getVelocity());
		} else {
			sr.setEndTime(new Date());
			double minutes = DateUtil.getSeconds(sr.getStartTime(),
					sr.getEndTime()) / 60;
			sr.setTimeSpan(minutes);
			if (alarmState.equals(TURN_OFF)) {
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

	/**
	 * �������뵽���ݿ��У��ȴ����͵�ǰ̨��������
	 * 
	 * @param alarmType
	 * @param alarmSource
	 * @param rd
	 */
	private void insertAlarm(String alarmSource, String alarmType,
			GPSRealData rd, String areaName) {
		rd.setLocation("����:" + areaName);
		this.newAlarmService.insertAlarm(alarmSource, alarmType, rd);
	}

	private List<Enclosure> getBindAreas(int vehicleId) {

		List<Enclosure> result = new ArrayList<Enclosure>();
		String hql = "from EnclosureBinding where vehicleId = ? and bindType = ?";
		List bindings = this.baseDao.query(hql, new Object[] { vehicleId,
				EnclosureBinding.BINDING_PLATFORM });
		for (Object obj : bindings) {
			EnclosureBinding eb = (EnclosureBinding) obj;
			Enclosure ec = (Enclosure) this.baseDao.load(Enclosure.class,
					eb.getEnclosureId());
			if (ec.getDeleted() == false)
				result.add(ec);
		}
		return result;

	}

	// �жϳ�����ǰλ���Ƿ���Χ����
	private void AnalyzeAreaAlarm(GPSRealData rd, Enclosure ec) {

		double lat = rd.getLatitude();
		double lng = rd.getLongitude();

		PointLatLng mp = null;
		// ��ѯ�ó����󶨵�����
		PointLatLng pointForGoogle = null;
		PointLatLng pointForBaidu = null;

		if (StringUtil.isNullOrEmpty(ec.getPoints()))
			return;

		if (Constants.MAP_BAIDU.equals(ec.getMapType())) {
			if (pointForBaidu == null) {
				pointForBaidu = MapFixService
						.fix(lat, lng, Constants.MAP_BAIDU);
			}
			mp = pointForBaidu;
		} else {

			if (pointForGoogle == null) {
				pointForGoogle = MapFixService.fix(lat, lng,
						Constants.MAP_GOOGLE);
			}
			mp = pointForGoogle;
		}

		if (Enclosure.ROUTE.equals(ec.getEnclosureType())) {
			// ·��ƫ�Ʊ���
			AnalyzeOffsetRoute(rd, ec, mp);
		} else {
			if (ec.getKeyPoint() == 1) {
				if (ec.getByTime())
					monitorKeyPointArrvie(rd, ec, mp);// ����ڹ涨��ʱ����ڵ���
				else
					monitorKeyPointLeave(rd, ec, mp); // ����ڹ涨��ʱ������뿪
			} else {
				boolean inEnclosure = IsInEnclourse(ec, mp);
				CrossBorder(ec, rd, inEnclosure);
			}
		}
	}

	// ��س����Ƿ��ڹ涨��ʱ����ڵ���
	private void monitorKeyPointArrvie(GPSRealData rd, Enclosure ec,
			PointLatLng mp) {
		Date now = new Date();
		String alarmType = AlarmRecord.TYPE_ARRIVE_NOT_ON_TIME;
		String alarmSource = AlarmRecord.ALARM_FROM_PLATFORM;
		String key = rd.getPlateNo() + "_" + ec.getEntityId() + "_" + alarmType;
		AlarmItem item = alarmMap.get(key);

		if (now.compareTo(ec.getStartDate()) >= 0
				&& now.compareTo(ec.getEndDate()) <= 0) {
			if (item == null) {
				boolean inEnclosure = IsInEnclourse(ec, mp);
				item = new AlarmItem(rd, alarmType, alarmSource);
				alarmMap.put(key, item);
			}
		} else if (now.compareTo(ec.getEndDate()) > 0) {
			if (item == null) {
				boolean inEnclosure = IsInEnclourse(ec, mp);
				if (inEnclosure == false) {
					this.insertAlarm(alarmSource, alarmType, rd, ec.getName());
					// ��һ�α���
					CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
							alarmType, TURN_ON, rd, ec);
					
					item = new AlarmItem(rd, alarmType, alarmSource);
					item.setStatus(AlarmRecord.STATUS_OLD);
					alarmMap.put(key, item);
				}
			}
			/**
			 * boolean inEnclosure = IsInEnclourse(ec, mp); if (inEnclosure) {
			 * // �رձ��� CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
			 * alarmType, TURN_OFF, rd, ec); item = new AlarmItem(key, new
			 * Date()); item.setOpen(false); alarmMap.put(key, item); }
			 */
		}

	}

	/**
	 * ��س����Ƿ��ڹ涨��ʱ������뿪
	 * 
	 * @param rd
	 * @param ec
	 * @param mp
	 */
	private void monitorKeyPointLeave(GPSRealData rd, Enclosure ec,
			PointLatLng mp) {
		Date now = new Date();
		String alarmSource = AlarmRecord.ALARM_FROM_PLATFORM;
		String alarmType = AlarmRecord.TYPE_LEAVE_NOT_ON_TIME;
		String key = rd.getPlateNo() + "_" + ec.getEntityId() + "_" + alarmType;
		AlarmItem item = alarmMap.get(key);
		if (now.compareTo(ec.getStartDate()) < 0
				&& now.compareTo(ec.getEndDate()) > 0) {
			if (item == null) {
				// �ж��Ƿ��ڹ涨��ʱ���뿪�ؼ���
				boolean inEnclosure = IsInEnclourse(ec, mp);
				if (inEnclosure) {
					this.insertAlarm(alarmSource, alarmType, rd, ec.getName());
					// ��һ�α���
					CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
							alarmType, TURN_ON, rd, ec);

					item = new AlarmItem(rd, alarmType, alarmSource);
					alarmMap.put(key, item);
				}
			} else if (item.getStatus().equals(AlarmRecord.STATUS_NEW)) {
				// �����Ѿ���,����Ҫ�رձ���
				boolean inEnclosure = IsInEnclourse(ec, mp);
				if (inEnclosure == false) {
					// �رձ���
					CreateAlarmRecord(AlarmRecord.ALARM_FROM_PLATFORM,
							alarmType, TURN_OFF, rd, ec);
					item = new AlarmItem(rd, alarmType, alarmSource);
					// item.setOpen(false);
					alarmMap.put(key, item);
				}
			}
		}

	}

	private boolean IsInEnclourse(Enclosure ec, PointLatLng mp) {
		List<PointLatLng> points = GetPoints(ec.getPoints());

		if (Enclosure.POLYGON.equals(ec.getEnclosureType())
				&& points.size() > 2) {
			if (MapFixService.IsInPolygon(mp, points)) {
				Date end = new Date();
				// TimeSpan ts = end - start;
				// logger.info("Χ����ѯ��ʱ:" + ts.TotalSeconds);
				return true;
			}
		} else if (Enclosure.CIRCLE.equals(ec.getEnclosureType())
				&& points.size() > 0) {
			PointLatLng pl = points.get(0);
			double radius = ec.getRadius();
			if (MapFixService.IsInCircle(mp, pl, radius))
				return true;
		} else if (Enclosure.RECT.equals(ec.getEnclosureType())
				&& points.size() > 1) {
			PointLatLng p1 = points.get(0);
			PointLatLng p2 = points.get(2);

			if (MapFixService.IsInRect(mp.lng, mp.lat, p1.lng, p1.lat, p2.lng,
					p2.lat))
				return true;
		}
		return false;
	}

	/**
	 * �ַ�������㣬����б���ʽ�� �ַ�����ʽҪ��112.24��31.23��112.24��31.23
	 * 
	 * @param strPoints
	 * @return
	 */
	private List<PointLatLng> GetPoints(String strPoints) {
		List<PointLatLng> results = new ArrayList<PointLatLng>();

		String[] strPts = strPoints.split(";");
		for (String strPt : strPts) {
			if (StringUtil.isNullOrEmpty(strPt) == false) {
				String[] strPoint = strPt.split(",");
				if (strPoint.length == 2) {
					PointLatLng pl = new PointLatLng(
							Double.parseDouble(strPoint[0]),
							Double.parseDouble(strPoint[1]));
					results.add(pl);
				}
			}
		}
		return results;
	}

	public IBaseDao getBaseDao() {
		return baseDao;
	}

	public void setBaseDao(IBaseDao baseDao) {
		this.baseDao = baseDao;
	}

	public boolean isAreaAlarmEnabled() {
		return areaAlarmEnabled;
	}

	public void setAreaAlarmEnabled(boolean areaAlarmEnabled) {
		this.areaAlarmEnabled = areaAlarmEnabled;
	}

	public INewAlarmService getNewAlarmService() {
		return newAlarmService;
	}

	public void setNewAlarmService(INewAlarmService newAlarmService) {
		this.newAlarmService = newAlarmService;
	}

	public IRealDataService getRealDataService() {
		return realDataService;
	}

	public void setRealDataService(IRealDataService realDataService) {
		this.realDataService = realDataService;
	}

	public DaoIbatisImpl getQueryDao() {
		return queryDao;
	}

	public void setQueryDao(DaoIbatisImpl queryDao) {
		this.queryDao = queryDao;
	}

}
