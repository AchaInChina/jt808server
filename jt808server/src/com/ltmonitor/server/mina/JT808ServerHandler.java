package com.ltmonitor.server.mina;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;
import com.ltmonitor.app.GpsConnection;
import com.ltmonitor.entity.StringUtil;
import com.ltmonitor.jt808.protocol.T808Message;
import com.ltmonitor.jt808.service.IMessageProcessService;
import com.ltmonitor.service.IRealDataService;

/**
 * JT808Oҵ������
 * @author tianfei
 *
 */
public class JT808ServerHandler extends IoHandlerAdapter {
	private Logger logger = Logger.getLogger(JT808ServerHandler.class);

	private IMessageProcessService messageProcessService;
	//�ն����Ӽ���
	private static ConcurrentMap<String, GpsConnection> connctionMap = new ConcurrentHashMap<String, GpsConnection>();
	
	private IRealDataService realDataService;

	public Collection<GpsConnection> getConnections() {
		return connctionMap.values();
	}

	public void exceptionCaught(IoSession session, Throwable e)
			throws Exception {
		//this.logger.error(getSimNo(session) + "ͨѶʱ�����쳣��" + e.getMessage(), e);
		this.logger.error(getSimNo(session) + "ͨѶʱ�����쳣��" + e.getMessage());
	}

	private String getSimNo(IoSession session) {
		return "" + session.getAttribute("simNo");
	}

	public GpsConnection getConnection(String simNo) {
		if (simNo.length() > 11)
			simNo = simNo.substring(1);
		GpsConnection conn = connctionMap.get(simNo);
		return conn;
	}

	private GpsConnection getConnection(long sessionId, T808Message msg) {
		if (msg == null || msg.getSimNo() == null) {
			logger.error("����Ŀ���Ϣ:");
			return null;
		}
		GpsConnection conn = connctionMap.get(msg.getSimNo());
		if (conn == null) {
			conn = new GpsConnection(msg.getSimNo(), sessionId);
			connctionMap.put(msg.getSimNo(), conn);
		} else if (conn.getSessionId() != sessionId) {
			// �µ�����
			//logger.error(msg.getSimNo() + "�����µ�����");
			conn.setSessionId(sessionId);
		}
		conn.setOnlineDate(new Date());
		conn.setSessionId(sessionId);

		return conn;
	}

	public void messageReceived(IoSession session, Object message)
			throws Exception {
		// tm.platform.server.LocalServer.session = session;
		T808Message tm = (T808Message) message;

		// T808Manager.putMsg(tm);
		session.setAttribute("simNo", tm.getSimNo());
		GpsConnection conn = getConnection(session.getId(), tm);
		if (conn != null) {
			conn.setConnected(true);
			//��Ϣ����
			messageProcessService.processMsg(tm);
			conn.setPlateNo(tm.getPlateNo()); // �������ӵĳ��ƺ�
			if(conn.getPackageNum() == Integer.MAX_VALUE)
			{
				conn.setPackageNum(0);
				conn.setErrorPacketNum(0);
				conn.setErrorPacketNum(0);
			}

			conn.setPackageNum(conn.getPackageNum() + 1);

			if (tm.getErrorMessage() != null) {
				// �յ���������İ�
				conn.setErrorPacketNum(conn.getErrorPacketNum() + 1);
			}
			if (tm.getHeader() != null
					&& tm.getHeader().getMessageType() == 0x0200) {
				conn.setPositionPackageNum(conn.getPositionPackageNum() + 1);
			}
		}

	}

	public void messageSent(IoSession session, Object message) throws Exception {
		this.logger.info("SimNo:" + session.getAttribute("simNo") + "�·�����ͳɹ�!");
	}

	public void sessionClosed(IoSession session) throws Exception {

		String simNo = "" + session.getAttribute("simNo");
		if (StringUtil.isNullOrEmpty(simNo) == false) {
			GpsConnection conn = connctionMap.get(simNo);
			if (conn != null) {
				// connctionMap.remove(simNo);
				conn.setConnected(false);
				conn.setDisconnectTimes(conn.getDisconnectTimes() + 1);
			}
		}
		session.close(true);
		this.logger.info("�뱾�ط������Ͽ�����, SimNo:" + simNo);
	}

	public void sessionCreated(IoSession session) throws Exception {
		// ���������ӱ�����ʱ�˷��������ã�����϶���sessionOpened(IoSession
		// session)����֮ǰ�����ã���������Զ�Socket����һЩ�������
		IoSessionConfig cfg1 = session.getConfig();
		if (cfg1 instanceof SocketSessionConfig) {
			SocketSessionConfig cfg = (SocketSessionConfig) session.getConfig();
			// ((SocketSessionConfig) cfg).setReceiveBufferSize(4096);
			cfg.setReceiveBufferSize(2 * 1024 * 1024);
			cfg.setReadBufferSize(2 * 1024 * 1024);
			cfg.setKeepAlive(true);
			// if (session.== TransportType.SOCKET) {
			// ((SocketSessionConfig) cfg).setKeepAlive(true);
			((SocketSessionConfig) cfg).setSoLinger(0);
			((SocketSessionConfig) cfg).setTcpNoDelay(true);
			((SocketSessionConfig) cfg).setWriteTimeout(1000);
		}

	}

	public void sessionIdle(IoSession session, IdleStatus idle)
			throws Exception {
		String simNo = getSimNo(session);
		this.logger.info(simNo + "����ʱ�������ϵͳ���ر�����");
		session.close(true);
	}

	public void sessionOpened(IoSession session) throws Exception {
	}

	public void setMessageProcessService(
			IMessageProcessService messageProcessService) {
		this.messageProcessService = messageProcessService;
	}

	public IMessageProcessService getMessageProcessService() {
		return messageProcessService;
	}

	public IRealDataService getRealDataService() {
		return realDataService;
	}

	public void setRealDataService(IRealDataService realDataService) {
		this.realDataService = realDataService;
	}
}
