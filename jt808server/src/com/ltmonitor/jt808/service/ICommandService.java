package com.ltmonitor.jt808.service;

import com.ltmonitor.dao.IBaseDao;
import com.ltmonitor.entity.TerminalCommand;
import com.ltmonitor.jt808.protocol.T808Message;


public interface ICommandService {

	public abstract IBaseDao getBaseDao();

	public abstract void setBaseDao(IBaseDao value);

	public abstract ICommandHandler getOnRecvCommand();

	public abstract void setOnRecvCommand(ICommandHandler value);

	public abstract int getInterval();

	public abstract void setInterval(int value);

	// ������������̣߳��Զ�������������͸��ն�
	public abstract void Start();

	public abstract void Stop();

	public abstract void ParseCommand();

	// ������Ϣ����ˮ��������״̬
	public abstract TerminalCommand UpdateStatus(String GpsId, int SN,
			String status);

	public abstract void UpdateCommand(TerminalCommand tc);

	public abstract TerminalCommand getCommandBySn(int sn);

	/**
	 * ����·���ĳ������
	 * @param cmdType
	 * @return
	 */
	TerminalCommand getLatestCommand(int cmdType, String simNo);
	/**
	 * �¼��ϱ�ʱ������Id�õ��¼�����
	 * @param eventId
	 * @return
	 */
	String getEventContent(int eventId);

	// ���ԷǷ������ʽ���н�����������¼��ʱȷ����ʽ��ȷ
	//public abstract T808Message Parse(TerminalCommand tc);

}