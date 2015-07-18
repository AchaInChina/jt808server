package com.ltmonitor.server.mina;

import java.net.InetSocketAddress;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import com.ltmonitor.app.GlobalConfig;
import com.ltmonitor.app.GpsConnection;
import com.ltmonitor.jt808.service.IJT808Server;

/**
 * ����tcp��808������
 * @author tianfei
 *
 */
public class JT808TcpServer implements IJT808Server {
	private static Logger logger = Logger.getLogger(JT808TcpServer.class);
	public static boolean isOpen = false;

//	private IJT808Server instance = null;
	
	private int port;	//����˿ں�
	private int maxOfflineTime;	//�ն������ʱʱ�� 
	public IoAcceptor dataAccepter = null;
	private JT808ServerHandler jt808Handler;
	//public static IoSession session;
	
	/* (non-Javadoc)
	 * @see tm.app.service.impl.IJT808Server#Stop()
	 */
	public void Stop()
	{
		if (null!=dataAccepter) {  
			dataAccepter.unbind();  
			dataAccepter.getFilterChain().clear();  // ���Filter chain����ֹ�´���������ʱ������������  
			dataAccepter.dispose();                 // ������дһ����洢IoAccept��ͨ��spring����������������dispose��Ҳ�����´���һ���µġ����߿�����init�����ڲ����д�����  
			dataAccepter = null;  
            //      System.exit(0);     ����������ֹͣ  
        }  
	}
	
	/**
	 * ��õ�ǰ�������б�
	 */
	public Collection<GpsConnection> getGpsConnections()
	{
		return getJt808Handler().getConnections();
	}
	
	@Override
	public boolean isOnline(String simNo)
	{
		if(simNo == null || simNo.length() == 0)
		{
			logger.error(simNo+"����Ϊ��");
			return false;
		}
		GpsConnection conn = this.getJt808Handler().getConnection(simNo);
		if(conn != null)
		{
			IoSession session = getSession(conn.getSessionId());
			if(session == null)
			{
				logger.error(simNo+"�Ҳ�����Ӧ�����ӵ�session,�ж�����");				
			}else if(session.isConnected() == false)
			{
				logger.error(simNo+"session�ر�,�ж�����");	
			}else
				//logger.error(simNo+"����");
			return session != null && session.isConnected() ;
		}else
		{
			logger.error(simNo+"û�д�����,�ж�����");
		}
		return false;
	}

	/**
	 * ���ն��·���������
	 */
	public boolean send(String simNo, byte[] msg)
	{
		GpsConnection conn = this.getJt808Handler().getConnection(simNo);
		if(conn != null)
			return send(conn.getSessionId(), msg);
		return false;
	}
	
	/**
	 * ���ն��·���������
	 */
	public  boolean send(long sessionId, byte[] msg) {
		try {
			IoSession session = getSession(sessionId);
			if (session != null && session.isConnected()) {
				WriteFuture wf = session.write(msg);
				wf.awaitUninterruptibly(1000);
				if(wf.isWritten())
					return true;
				else
				{
					Throwable tr = wf.getException();
					if(tr != null)
					{
						logger.error(tr.getMessage(), tr);
					}
						
					return false;
				}
					
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return false;
	}
	

	
	public IoSession getSession(long sid)
	{
		return dataAccepter.getManagedSessions().get(sid);
	}

	/**
	 * ����TCP����
	 */
	public boolean start() {
		port = GlobalConfig.paramModel.getLocalPort();
		try {
			dataAccepter = new NioSocketAcceptor();

			//LoggingFilter log = new LoggingFilter();
			//log.setMessageReceivedLogLevel(LogLevel.WARN);
			//dataAccepter.getFilterChain().addLast("logger", log);
			//���롢����
			dataAccepter.getFilterChain().addLast("codec",
					new ProtocolCodecFilter(new JT808MessageCodecFactory()));

			IoSessionConfig config = dataAccepter.getSessionConfig();

			config.setReadBufferSize(4096);
			config.setWriteTimeout(10000);  
	        config.setWriterIdleTime(100000);  
			config.setIdleTime(IdleStatus.BOTH_IDLE, maxOfflineTime);
			//ҵ����
			dataAccepter.setHandler(this.getJt808Handler());

			dataAccepter.bind(new InetSocketAddress(port));
			logger.info("���ݷ����������ɹ�!�˿ں�:" + port);
			isOpen = true;
		} catch (Exception e) {
			isOpen = false;
			logger.error("808����������ʧ��:" + e);
			e.printStackTrace();
		}
		return isOpen;
	}

	public void setJt808Handler(JT808ServerHandler jt808Handler) {
		this.jt808Handler = jt808Handler;
	}

	public JT808ServerHandler getJt808Handler() {
		return jt808Handler;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public int getMaxOfflineTime() {
		return maxOfflineTime;
	}

	public void setMaxOfflineTime(int maxOfflineTime) {
		this.maxOfflineTime = maxOfflineTime;
	}
}
