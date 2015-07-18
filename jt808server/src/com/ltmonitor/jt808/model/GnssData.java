package com.ltmonitor.jt808.model;

import java.util.Date;

/**
 * ��λ����
 * ������ ��λ���ݽ��� ����������
 * @author DELL
 * 
 */
public class GnssData {
	//���ƺ�
	private String plateNo;
	//������ɫ
	private int plateColor;
	// ��γ��
	private int latitude;

	private int longitude;

	// GPS�ٶ�
	private int gpsSpeed;

	// �г���¼���ٶ�
	private int recSpeed;

	// �����
	private int totalMileage;
	// ����
	private int direction;

	// ����
	private int altitude;

	// ����״̬
	private int vehicleState;
	// ����״̬
	private int alarmState;
	// ��λʱ��
	private Date PosTime;

	// �Ƿ��ƫ
	private int PosEncrypt;

	public GnssData() {
		plateNo = "��A53251";
		plateColor = 1;
		// ��λ����
		this.PosEncrypt = 0;
		PosTime = new Date();
		this.longitude = 121123456;
		this.latitude = 34123456;
		this.gpsSpeed = 20;
		this.recSpeed = 23;
		this.totalMileage = 123456;
		this.direction = 361;
		this.altitude = 256;
		this.vehicleState = 32;
		this.alarmState = 121;
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}

	public int getLongitude() {
		return longitude;
	}

	public void setGpsSpeed(int gpsSpeed) {
		this.gpsSpeed = gpsSpeed;
	}

	public int getGpsSpeed() {
		return gpsSpeed;
	}

	public void setRecSpeed(int recSpeed) {
		this.recSpeed = recSpeed;
	}

	public int getRecSpeed() {
		return recSpeed;
	}

	public void setTotalMileage(int totalMileage) {
		this.totalMileage = totalMileage;
	}

	public int getTotalMileage() {
		return totalMileage;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getDirection() {
		return direction;
	}

	public void setAltitude(int altitude) {
		this.altitude = altitude;
	}

	public int getAltitude() {
		return altitude;
	}

	public void setVehicleState(int vehicleState) {
		this.vehicleState = vehicleState;
	}

	public int getVehicleState() {
		return vehicleState;
	}

	public void setAlarmState(int alarmState) {
		this.alarmState = alarmState;
	}

	public int getAlarmState() {
		return alarmState;
	}

	public void setPosTime(Date posTime) {
		PosTime = posTime;
	}

	public Date getPosTime() {
		return PosTime;
	}

	public void setPosEncrypt(int posEncrypt) {
		PosEncrypt = posEncrypt;
	}

	public int getPosEncrypt() {
		return PosEncrypt;
	}

	public String getPlateNo() {
		return plateNo;
	}

	public void setPlateNo(String plateNo) {
		this.plateNo = plateNo;
	}

	public int getPlateColor() {
		return plateColor;
	}

	public void setPlateColor(int plateColor) {
		this.plateColor = plateColor;
	}
}
