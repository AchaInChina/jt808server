package com.ltmonitor.server.mina;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * ����mina��ת���ͻ���
 * 
 * @author DELL
 * 
 */
public class MinaClient {

	public static ConcurrentLinkedQueue<byte[]> upQueue = new ConcurrentLinkedQueue();

	public static ConcurrentLinkedQueue<byte[]> downQueue = new ConcurrentLinkedQueue();

	private int port = 8899;

	private String serverIp;

	private ConnectFuture cf;

	private NioSocketConnector connector;

	private IoSession session;

	//private Thread sendMessageThread;

	private static Logger logger = Logger.getLogger(MinaClient.class);

	private boolean isStop = false;
	
	private String simNo;
	
	private Date updateTime = new Date();

	public MinaClient() {

	}

	public MinaClient(String _simNo, String _serverIp, int _port) {
		this.simNo = _simNo;
		this.setServerIp(_serverIp);
		this.setPort(_port);
	}
	
	public void addUpQueue(byte[] bytes)
	{
		upQueue.add(bytes);
	}
	

	public void addDownQueue(byte[] bytes)
	{
		downQueue.add(bytes);
	}
	
	
	public boolean isConnected()
	{
		return session != null && session.isConnected();
	}
	
	public boolean connect() {
		try {
			// ����һ��socket����
			connector = new NioSocketConnector();
			// ��ȡ��������
			DefaultIoFilterChainBuilder chain = connector.getFilterChain();

			ProtocolCodecFilter filter = new ProtocolCodecFilter(
					new ByteArrayCodecFactory());
			// ��ӱ�������� �������롢��������
			chain.addLast("objectFilter", filter);
			// ��Ϣ���Ĵ�����
			connector.setHandler(new MinaClientHandler(simNo));
			// �������ӳ�ʱʱ��
			connector.setConnectTimeoutCheckInterval(30);
			// ���ӷ�������֪���˿ڡ���ַ
			cf = connector.connect(new InetSocketAddress(getServerIp(), getPort()));
			// �ȴ����Ӵ������
			cf.awaitUninterruptibly();
			session = cf.getSession();
			//cf.getSession().getCloseFuture().awaitUninterruptibly();
			
			
			return true;
		} catch (Exception e) {
			logger.error("808ת���ͻ�������ʧ��:"+e.getMessage());
		}
		return false;
	}

	public void send(byte[] messageBytes) {
		//if (session == null || session.isConnected() == false)
			//return;
		//JT808TransferQueue.add(messageBytes);
		
		connectAndSend(messageBytes);
	}

	private void connectAndSend(byte[] tm )
	{
		try {
			if(isConnected() == false)
			{
				boolean res = connect(); //����������
				if(res == false)
				{
					this.downQueue.clear();
					this.upQueue.clear();
				}
			}
			
			this.sendData(tm);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			
			if(isConnected() == false)
				connect(); //����������
		}
	}

	public boolean sendData(byte[] messageBytes) {
		if (session != null && session.isConnected()) {
			WriteFuture wf = session.write(messageBytes);
			wf.awaitUninterruptibly(1000);
			if (wf.isWritten())
				return true;
			else {
				Throwable tr = wf.getException();
				if (tr != null) {
					logger.error(tr.getMessage(), tr);
				}

				return false;
			}
		}

		return false;
	}

	public void stop() {
		isStop = true; //�ر��߳���������
		try {
			if (connector != null && session != null) {
				session.getCloseFuture().join();
				connector.dispose();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	public static void main(String[] args) {

	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getSimNo() {
		return simNo;
	}

	public void setSimNo(String simNo) {
		this.simNo = simNo;
	}
}