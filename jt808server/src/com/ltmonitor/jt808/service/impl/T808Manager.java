package com.ltmonitor.jt808.service.impl;

import java.util.Collection;
import java.util.Date;
import org.apache.log4j.Logger;
import com.ltmonitor.app.GlobalConfig;
import com.ltmonitor.app.GpsConnection;
import com.ltmonitor.app.ServiceLauncher;
import com.ltmonitor.entity.PlatformState;
import com.ltmonitor.entity.TerminalCommand;
import com.ltmonitor.jt808.protocol.T808Message;
import com.ltmonitor.jt808.service.IAckService;
import com.ltmonitor.jt808.service.ICommandHandler;
import com.ltmonitor.jt808.service.ICommandService;
import com.ltmonitor.jt808.service.IJT808Server;
import com.ltmonitor.jt808.service.IMediaService;
import com.ltmonitor.jt808.service.IMessageSender;
import com.ltmonitor.jt808.service.IT808Manager;
import com.ltmonitor.jt808.service.ITransferGpsService;
import com.ltmonitor.server.mina.IJT808TransferService;
import com.ltmonitor.server.mina.JT808TransferQueue;


/**
 * �ṩ�ⲿ����808ת��ƽ̨�Ľӿ�
 * ��������StartServer����
 * @author tianfei
 * 
 */
public class T808Manager implements IT808Manager {

	private static Logger logger = Logger.getLogger(T808Manager.class);
	/**
	 * gps�������ļ����˿�
	 */
	private int listenPort;
	/**
	 * �Ƿ�����ת��
	 */
	private boolean enable808Tranasfer = false;
	/**
	 * ת������������808�������Ķ˿�
	 */
	private int third808ServerPort = 0;
	/**
	 * ת������������808������ip��ַ
	 */
	private String third808ServerIp;
	
	private IJT808Server jt808Server;
	
	//private static ConcurrentLinkedQueue<T808Message> dataQueue = new ConcurrentLinkedQueue();

	/**
	 *  ��ˮ�ż�������ÿ���·��Զ�����1
	 */
	private static int serialNo = 0;
	/**
	 * �û����ն��·�ָ��Ĵ������
	 */
	private ICommandService commandService;

	//private Thread processRealDataThread;
	/**
	 * Ӧ�����
	 */
	private IAckService ackService;
	/**
	 * ��ý���ϴ����ݵĴ������
	 */
	private IMediaService mediaService;
	
	/**
	 *  ����ת������,��Ҫ����809ת��
	 */
	private ITransferGpsService transferGpsService;
	/**
	 * ����808Э���ת���ͻ���
	 */
	private IJT808TransferService jt808TransferService;

	
	public static int getSerialNo() {
		return serialNo++;
	}

	public Collection<GpsConnection> getGpsConnections() {
		return getJt808Server().getGpsConnections();
	}

	// ���ϼ�ƽ̨��������
	private  boolean send(String simNo, byte[] msg) {
		// ����ʹ������·��������
		boolean res = getJt808Server().send(simNo, msg);
		return res;
	}

	// ��������
	public boolean Send(T808Message msg) {
		msg.getHeader().setMessageSerialNo((short) getSerialNo());
		// msg.setPacketDescr(strMsg);
		boolean res = send(msg.getSimNo(), msg.WriteToBytes());
		GlobalConfig.putMsg(msg);
		return res;
	}
	
	/**
	 * ����JT808������
	 */
	public boolean StartServer() {
		// ����������
		boolean res = getJt808Server().start();
		if (res) {
			JT808TransferQueue.transferEnabled = enable808Tranasfer;
			//����������808ת������
			if(this.enable808Tranasfer)
			{
				//��ʼת��
				JT808TransferQueue.port = (this.third808ServerPort);
				JT808TransferQueue.serverIp = (this.third808ServerIp);
			    this.jt808TransferService.startTransfer();
			}
			// ��������������������ݿ��ж�ȡ������н���
			//getCommandService().setBaseDao(ServiceLauncher.getBaseDao());
			//����ƽ̨״̬
			try {
				PlatformState ps = ServiceLauncher.getBaseService().getPlatformState();
				ps.setGpsServerDate(new Date());
				ps.setGpsServerState(PlatformState.STATE_START);
				ServiceLauncher.getBaseService().saveOrUpdate(ps);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			//�����ն��·��������
			getCommandService().Start();
		    this.commandService.setOnRecvCommand(new ICommandHandler(){
		    	/**
		    	 * ���ն��·�����
		    	 */
				@Override
				public boolean OnRecvCommand(T808Message tm, TerminalCommand tc) {
					if(getJt808Server().isOnline(tc.getSimNo()) == false)
					{
						tc.setStatus(TerminalCommand.STATUS_OFFLINE);
						return false;
					}else
					{
						boolean res = Send(tm);
						tc.setSN(tm.getHeader().getMessageSerialNo());
						tc.setStatus(res ? TerminalCommand.STATUS_PROCESSING : 
							TerminalCommand.STATUS_FAILED);
						return res;
					}				
				}
		    });
			//Ӧ����񣬵���Server������Ӧ�����ݰ�
			ackService.setMessageSender(new IMessageSender(){
				@Override
				public void Send808Message(T808Message tm) {
					Send(tm);					
				}				
			});
			//ý�����
			mediaService.setMessageSender(new IMessageSender(){
				@Override
				public void Send808Message(T808Message tm) {
					Send(tm);					
				}				
			});
			
			//if(transferGpsService.sta)
			//����809����ת������
			transferGpsService.start();
			//processRealDataThread.start();
		}
		return res;
	}
	


	/**
	 * ֹͣ����
	 */
	public void StopServer() {
		try {
			PlatformState ps = ServiceLauncher.getBaseService().getPlatformState();
			ps.setGpsServerDate(new Date());
			ps.setGpsServerState(PlatformState.STATE_STOP);
			ServiceLauncher.getBaseService().saveOrUpdate(ps);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		try {
			getJt808Server().Stop();
			transferGpsService.stop();
			if(jt808TransferService != null)
			   this.jt808TransferService.stop();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	
	public void setJt808Server(IJT808Server jt808Server) {
		this.jt808Server = jt808Server;
	}

	public IJT808Server getJt808Server() {
		return jt808Server;
	}

	public void setCommandService(ICommandService commandService) {
		this.commandService = commandService;
	}

	public ICommandService getCommandService() {
		return commandService;
	}

	public void setAckService(IAckService ackService) {
		this.ackService = ackService;
	}

	public IAckService getAckService() {
		return ackService;
	}

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public ITransferGpsService getTransferGpsService() {
		return transferGpsService;
	}

	public void setTransferGpsService(ITransferGpsService transferGpsService) {
		this.transferGpsService = transferGpsService;
	}

	public IMediaService getMediaService() {
		return mediaService;
	}

	public void setMediaService(IMediaService mediaService) {
		this.mediaService = mediaService;
	}

	public boolean isEnable808Tranasfer() {
		return enable808Tranasfer;
	}

	public void setEnable808Tranasfer(boolean enable808Tranasfer) {
		this.enable808Tranasfer = enable808Tranasfer;
	}

	public int getThird808ServerPort() {
		return third808ServerPort;
	}

	public void setThird808ServerPort(int third808ServerPort) {
		this.third808ServerPort = third808ServerPort;
	}

	public String getThird808ServerIp() {
		return third808ServerIp;
	}

	public void setThird808ServerIp(String third808ServerIp) {
		this.third808ServerIp = third808ServerIp;
	}

	public IJT808TransferService getJt808TransferService() {
		return jt808TransferService;
	}

	public void setJt808TransferService(IJT808TransferService jt808TransferService) {
		this.jt808TransferService = jt808TransferService;
	}

}
