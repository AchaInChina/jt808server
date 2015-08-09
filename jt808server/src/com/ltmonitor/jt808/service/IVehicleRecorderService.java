package com.ltmonitor.jt808.service;

import com.ltmonitor.jt808.protocol.T808Message;

/**
 * �г���¼�Ƿ���
 * @author tianfei
 *
 */
public interface IVehicleRecorderService {
	
	/**
	 * ֹͣ����
	 */
	public void Stop();

	/**
	 * �����ý�����ݰ�
	 * 
	 * @param msg
	 */
	public void processMediaMsg(T808Message msg);

	public void setMessageSender(IMessageSender messageSender);

}
