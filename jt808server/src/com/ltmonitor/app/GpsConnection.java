package com.ltmonitor.app;

import java.util.Date;
/**
 * ���Ӳ�����
 * @author tianfei
 *
 */
public class GpsConnection {
	//����Id
	private long sessionId;
	//�ն�Sim����
	private String simNo;
	//���ƺ�
	private String plateNo;
	//����ʱ��
	private Date createDate;
	//��������ʱ��
	private Date onlineDate;
	//�յ��İ�������
	private int packageNum;
	//��λ������
	private int positionPackageNum;
	//�Ͽ�����
	private int disconnectTimes;
	//�������
	private int errorPacketNum;
	//�Ƿ�������
	private boolean connected;
	//�Ƿ�λ
	private boolean located;
	
	
	public GpsConnection(String _simNo, long sId)
	{
		setSimNo(_simNo);
		setSessionId(sId);
		setCreateDate(new Date());
		setOnlineDate(new Date());
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public String getSimNo() {
		return simNo;
	}

	public void setSimNo(String simNo) {
		this.simNo = simNo;
	}

	public String getPlateNo() {
		return plateNo;
	}

	public void setPlateNo(String plateNo) {
		this.plateNo = plateNo;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getOnlineDate() {
		return onlineDate;
	}

	public void setOnlineDate(Date onlineDate) {
		this.onlineDate = onlineDate;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public int getErrorPacketNum() {
		return errorPacketNum;
	}

	public void setErrorPacketNum(int errorPacketNum) {
		this.errorPacketNum = errorPacketNum;
	}

	public int getDisconnectTimes() {
		return disconnectTimes;
	}

	public void setDisconnectTimes(int disconnectTimes) {
		this.disconnectTimes = disconnectTimes;
	}

	public int getPositionPackageNum() {
		return positionPackageNum;
	}

	public void setPositionPackageNum(int positionPackageNum) {
		this.positionPackageNum = positionPackageNum;
	}

	public int getPackageNum() {
		return packageNum;
	}

	public void setPackageNum(int packageNum) {
		this.packageNum = packageNum;
	}

	public boolean isLocated() {
		return located;
	}

	public void setLocated(boolean located) {
		this.located = located;
	}

}
