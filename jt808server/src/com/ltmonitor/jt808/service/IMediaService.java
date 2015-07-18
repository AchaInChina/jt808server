package com.ltmonitor.jt808.service;

import com.ltmonitor.jt808.protocol.T808Message;

public interface IMediaService {

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

	public void setTransferGpsService(ITransferGpsService transferGpsService);

}