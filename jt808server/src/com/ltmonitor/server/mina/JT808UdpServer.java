package com.ltmonitor.server.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

import com.ltmonitor.app.GlobalConfig;
import com.ltmonitor.app.GpsConnection;
import com.ltmonitor.jt808.service.IJT808Server;
/**
 * ����udp��808������
 * @author DELL
 *
 */
public class JT808UdpServer implements IJT808Server {
	private static Logger logger = Logger.getLogger(JT808UdpServer.class);

	private int port;

	private NioDatagramAcceptor dataAcceptor;
	
	private Executor threadPool = Executors.newCachedThreadPool();

	private JT808ServerHandler jt808Handler;
	public JT808UdpServer()
	{

	}
	@Override
	public boolean start() {
		try {
			//��ȫ�������У���ȡ�˿�
			port = GlobalConfig.paramModel.getLocalPort();
			dataAcceptor = new NioDatagramAcceptor();
			DefaultIoFilterChainBuilder chain = dataAcceptor.getFilterChain();
			// chain.addLast("logger", new LoggingFilter());
			// chain.addLast("codec", new ProtocolCodecFilter(new
			// TextLineCodecFactory(Charset.forName("UTF-8"))));
			chain.addLast("threadPool", new ExecutorFilter(threadPool));
			dataAcceptor.setHandler(jt808Handler);
			//808Э�������
			dataAcceptor.getFilterChain().addLast("codec",
					new ProtocolCodecFilter(new JT808MessageCodecFactory()));
			DatagramSessionConfig dcfg = dataAcceptor.getSessionConfig();
			dcfg.setReadBufferSize(4096);// ���ý�������ֽ�Ĭ��2048
			dcfg.setMaxReadBufferSize(65536);
			dcfg.setReceiveBufferSize(1024);// �������뻺�����Ĵ�С
			dcfg.setSendBufferSize(1024);// ��������������Ĵ�С
			dcfg.setReuseAddress(true);// ����ÿһ�������������ӵĶ˿ڿ�������

			dataAcceptor.bind(new InetSocketAddress(port));
			logger.info("UDP�����������ɹ�!�˿ں�:" + port);
			return true;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public void Stop() {
		if (null != dataAcceptor) {
			dataAcceptor.unbind();
			dataAcceptor.getFilterChain().clear(); // ���Filter
													// chain����ֹ�´���������ʱ������������
			dataAcceptor.dispose(); // ������дһ����洢IoAccept��ͨ��spring����������������dispose��Ҳ�����´���һ���µġ����߿�����init�����ڲ����д�����
			dataAcceptor = null;
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
			return false;
		GpsConnection conn = this.getJt808Handler().getConnection(simNo);
		if(conn != null)
		{
			IoSession session = getSession(conn.getSessionId());
			return session != null && session.isConnected() ;
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
		return dataAcceptor.getManagedSessions().get(sid);
	}

	public JT808ServerHandler getJt808Handler() {
		return jt808Handler;
	}

	public void setJt808Handler(JT808ServerHandler jt808Handler) {
		this.jt808Handler = jt808Handler;
	}

}
