package com.ltmonitor.jt808.service;

import com.ltmonitor.jt808.protocol.T808Message;

public interface IMessageProcessService {

	/**
	 * ���ն˷�����������Ϣ����ͨ��Ӧ��
	 * 
	 * @param msgFromTerminal  �ն���Ϣ
	 */
	public abstract void processMsg(T808Message msgFromTerminal);

}