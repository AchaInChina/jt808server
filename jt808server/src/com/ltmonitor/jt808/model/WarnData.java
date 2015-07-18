package com.ltmonitor.jt808.model;

import java.util.Date;

/**
 * �ϴ��ı������� ���·��ı���������Ϣ ���� 
 * @author DELL
 *
 */
public class WarnData {
	//���ƺ�
	private String plateNo;
	private int plateColor;
	//������Ϣ��Դ
	private int src;
	//��������
	private int type;
	//����ʱ��
	private Date warnTime;
	//��������Id
	private long infoId;
	//��������
	private String content;
	//��������ʱ��
	private Date supervisionEndTime;
	//���켶�� 0 ���� 1 һ��
	private byte supervisionLevel;
	//����������
	private String supervisor;
	//�����˵绰
	private String supervisionTel;
	//����������
	private String supervisionEmail;
	//����������
	private int result;
	public int getSrc() {
		return src;
	}
	public void setSrc(int src) {
		this.src = src;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Date getWarnTime() {
		return warnTime;
	}
	public void setWarnTime(Date warnTime) {
		this.warnTime = warnTime;
	}
	public long getInfoId() {
		return infoId;
	}
	public void setInfoId(long infoId) {
		this.infoId = infoId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getPlateColor() {
		return plateColor;
	}
	public void setPlateColor(int plateColor) {
		this.plateColor = plateColor;
	}
	public Date getSupervisionEndTime() {
		return supervisionEndTime;
	}
	public void setSupervisionEndTime(Date supervisionEndTime) {
		this.supervisionEndTime = supervisionEndTime;
	}
	public byte getSupervisionLevel() {
		return supervisionLevel;
	}
	public void setSupervisionLevel(byte supervisionLevel) {
		this.supervisionLevel = supervisionLevel;
	}
	public String getSupervisor() {
		return supervisor;
	}
	public void setSupervisor(String supervisor) {
		this.supervisor = supervisor;
	}
	public String getSupervisionTel() {
		return supervisionTel;
	}
	public void setSupervisionTel(String supervisionTel) {
		this.supervisionTel = supervisionTel;
	}
	public String getSupervisionEmail() {
		return supervisionEmail;
	}
	public void setSupervisionEmail(String supervisionEmail) {
		this.supervisionEmail = supervisionEmail;
	}
	public String getPlateNo() {
		return plateNo;
	}
	public void setPlateNo(String plateNo) {
		this.plateNo = plateNo;
	}
	public void setResult(int result) {
		this.result = result;
	}
	public int getResult() {
		return result;
	}

}
