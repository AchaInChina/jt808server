package com.ltmonitor.jt808.service;

import java.util.Collection;
import com.ltmonitor.app.GpsConnection;
import com.ltmonitor.jt808.protocol.T808Message;

/**
 * JT808�ⲿ�ӿ�
 * @author tianfei
 *
 */
public interface IT808Manager {

	/**
	 * ����808Server����
	 */
	public abstract boolean StartServer();


	boolean Send(T808Message msg);

	/**
	 * ֹͣ����
	 */
	public void StopServer();

	/**
	 * 
	 * @return
	 */
	public Collection<GpsConnection> getGpsConnections();


	int getListenPort();


	void setListenPort(int listenPort);

	//public void processMsg(T808Message msgFromTerminal);

}