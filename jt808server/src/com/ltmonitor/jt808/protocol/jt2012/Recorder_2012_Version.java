package com.ltmonitor.jt808.protocol.jt2012;

import com.ltmonitor.entity.StringUtil;

/** 
 �ɼ�ִ�б�׼�汾
 
*/
public class Recorder_2012_Version implements IRecorderDataBlock_2012
{
	/** 
	 ������
	 
	*/
	public final byte getCommandWord()
	{
		return 0x00;
	}

	/** 
	 ���ݿ鳤��
	 
	*/
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ushort getDataLength()
	public final short getDataLength()
	{
		return 2;
	}

	public final byte[] WriteToBytes()
	{
		byte[] bytes = null;
		return bytes;
	}
	/** 
	 �޸ĵ��� ��Ĭ��Ϊ 00H��
	 
	*/
	private String privateModifiedOrder;
	public final String getModifiedOrder()
	{
		return privateModifiedOrder;
	}
	public final void setModifiedOrder(String value)
	{
		privateModifiedOrder = value;
	}

	/** 
	 �汾�� ��Ĭ��Ϊ 03��
	 
	*/
	private String privateRecardVersion;
	public final String getRecardVersion()
	{
		return privateRecardVersion;
	}
	public final void setRecardVersion(String value)
	{
		privateRecardVersion = value;
	}


	/** 
	 ��������
	 
	 @param bytes
	*/
	public final void ReadFromBytes(byte[] bytes)
	{
		byte year = bytes[0];
		byte nub = bytes[1];
		String yearTo2 = Integer.toBinaryString(year);
		if (yearTo2.length() < 8)
		{
			yearTo2 = StringUtil.leftPad(yearTo2, 8, '0');
		}
		//����BCD���ȡ���
		int years = Integer.parseInt(yearTo2, 2);
		String y = (new Integer(years)).toString();
		//����16��������ȡ����
		String n = Integer.toHexString(nub);
		setRecardVersion("20" + y);
		setModifiedOrder(n);
	}
}

