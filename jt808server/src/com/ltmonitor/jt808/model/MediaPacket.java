package com.ltmonitor.jt808.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ltmonitor.jt808.protocol.JT_0200;
import com.ltmonitor.jt808.protocol.JT_0801;
import com.ltmonitor.jt808.protocol.T808Message;
import com.ltmonitor.jt808.protocol.T808MessageHeader;
import com.ltmonitor.util.DateUtil;

public class MediaPacket {
	//����Ψһkey
	private String key;
	
	private int totalNum;
	
	private Date updateDate = new Date();
	/**
	 * �ְ��ͷְ��ŵ�ӳ���ϵ
	 */
	private Map<Integer, byte[]> packets = new HashMap<Integer, byte[]>();
	
	private int mediaId;
	
	private T808Message t808Message;
	
	private JT_0200 position;
	/**
	 * �ش�����
	 */
	private int retransCount;
	
	private Date createDate = new Date();
	
	public MediaPacket(T808Message msg)
	{
		T808MessageHeader header = msg.getHeader();
		this.totalNum = header.getMessageTotalPacketsCount();
		
		// ����ǵ�һ���ְ�����������;
		JT_0801 mediaData = (JT_0801) msg.getMessageContents();
		this.mediaId = mediaData.getMultimediaDataId();
		packets.put((int)header.getMessagePacketNo(), mediaData.getMultimediaData());
		this.position = mediaData.getPosition();
		this.t808Message = msg;
	}
	/**
	 * �Ƿ���յ����еķְ�
	 * @return
	 */
	public boolean isComplete()
	{
		return packets.size() == this.totalNum; 
	}
	
	public List<byte[]> getWholePacket()
	{
		if(isComplete() == false)
			return null;
		List<byte[]> result = new ArrayList<byte[]>();
		for(int m = 1; m <= this.totalNum; m++)
		{
			byte[] data = packets.get(m);
			result.add(data);
		}
		return result;
	}
	/**
	 * �õ���Ҫ�ش��ķְ����
	 * @return
	 */
	public ArrayList<Short> getNeedReTransPacketNo()
	{
		ArrayList<Short> result = new ArrayList<Short>();
		if(this.totalNum > this.packets.size())
		{
			for(int m = 1; m <= this.totalNum;m++)
			{
				if(packets.containsKey(m) == false)
					result.add((short)m);
			}
		}
		return result;
	}
	
	public boolean containPacket(int packetNo)
	{
		return packets.containsKey(packetNo);
	}
	
	public void addPacket(int packetNo, byte[] packetData)
	{
		packets.put(packetNo, packetData);
		this.updateDate = new Date();//�����ϴ�ʱ��
	}
	//�õ��ش��İ���
	public List<Integer> getNeedReUploadPacketNo()
	{
		List<Integer> result = new ArrayList<Integer>();
		for(int m = 1; m <= totalNum; m++)
		{
			if(packets.containsKey(m) == false)
				result.add(m);
		}
		return result;
	}
	
	public String toString()
	{
		double seconds = DateUtil.getSeconds(createDate, updateDate);
		StringBuilder sb = new StringBuilder();
		sb.append(this.t808Message.getPlateNo()).append(this.t808Message.getSimNo())
		.append(",�ܰ���:").append(this.totalNum)
		.append(",�յ�����:").append(this.packets.size())
		.append("�ش���:").append(this.retransCount)
		.append("��ʱ:").append(seconds).append("��");
		return sb.toString();
	}

	public int getTotalNum() {
		return totalNum;
	}

	public void setTotalNum(int totalNum) {
		this.totalNum = totalNum;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public Map<Integer, byte[]> getPackets() {
		return packets;
	}

	public void setPackets(Map<Integer, byte[]> packets) {
		this.packets = packets;
	}

	public int getMediaId() {
		return mediaId;
	}

	public void setMediaId(int mediaId) {
		this.mediaId = mediaId;
	}

	public JT_0200 getPosition() {
		return position;
	}

	public void setPosition(JT_0200 position) {
		this.position = position;
	}
	public T808Message getT808Message() {
		return t808Message;
	}
	public void setT808Message(T808Message t808Message) {
		this.t808Message = t808Message;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int getRetransCount() {
		return retransCount;
	}
	public void setRetransCount(int retransCount) {
		this.retransCount = retransCount;
	}
	
	

}
