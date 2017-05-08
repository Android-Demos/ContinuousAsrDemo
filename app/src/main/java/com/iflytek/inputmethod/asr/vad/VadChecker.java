package com.iflytek.inputmethod.asr.vad;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;



import android.util.Log;

public class VadChecker {
	static {
		System.loadLibrary("vadLib");
	}
	public static class VadData
	{
		public int startByte;		//????????
		public int endByte;			//????????
		public int status;			//??????
		public int volumeLevel;		//??????С
	}
	public static class Status
	{
		public final static  int ivVAD_OK 					= 0;	// 0
		public final static  int ivVAD_INVARG 				= 1;	// 1
		public final static  int ivVAD_INVCALL 				= 2;	// 2
		public final static  int ivVAD_INSUFFICIENTBUFFER 	= 3;	// 3
		public final static  int ivVAD_BUFFERFULL 			= 4;	// 4
		public final static  int ivVAD_FINDSTART 			= 5;	// 5
		public final static  int ivVAD_FINDPAUSE 			= 6;	// 6
		public final static  int ivVAD_FINDSTARTANDPAUSE 	= 7;	// 7
		public final static  int ivVAD_FINDEND 				= 8;	// 8
		public final static  int ivVAD_FINDSTARTANDEND 		= 9;	// 9
		
		static ConcurrentHashMap<String, String> statusStringMap = new  ConcurrentHashMap<String, String>();
		static 
		{
			statusStringMap.put("0", "ivVAD_OK");
			statusStringMap.put("1", "ivVAD_INVARG");
			statusStringMap.put("2", "ivVAD_INVCALL");
			statusStringMap.put("3", "ivVAD_INSUFFICIENTBUFFER");
			statusStringMap.put("4", "ivVAD_BUFFERFULL");
			statusStringMap.put("5", "ivVAD_FINDSTART");
			statusStringMap.put("6", "ivVAD_FINDPAUSE");
			statusStringMap.put("7", "ivVAD_FINDSTARTANDPAUSE");
			statusStringMap.put("8", "ivVAD_FINDEND");
			statusStringMap.put("9", "ivVAD_FINDSTARTANDEND");
		}
		public static String GetStatusString(int status)
		{
			return statusStringMap.get(""+status);
		}
	}
	
	int		hVadHandle = 0;
	public boolean  initialize()
	{
		Date d = new Date();
		Log.d("vadLib", "native_initialize enter");
		hVadHandle = native_initialize();
		Log.d("vadLib", "native_initialize leave "+ (new Date().getTime()-d.getTime()));
		
		if(0 != hVadHandle)
		{
			native_setParam(hVadHandle, 2, 800);
		}
		
		return hVadHandle  != 0;
	}
	public void uninitialize()
	{
		Date d = new Date();
		Log.d("vadLib", "native_uninitialize enter");
		if(hVadHandle != 0)
			native_uninitialize(hVadHandle);
		Log.d("vadLib", "native_uninitialize leave "+ (new Date().getTime()-d.getTime()));
		hVadHandle = 0;
	}
	public void reset()
	{
		if(0 == hVadHandle)
		{
			return;
		}
		Date d = new Date();
		Log.d("vadLib", "native_reset enter");
		native_reset(hVadHandle);
		Log.d("vadLib", "native_reset leave "+ (new Date().getTime()-d.getTime()));
	}
	public int  checkVAD(byte[] byData, int len, VadData vad)
	{
		Date d = new Date();
		Log.d("vadLib", "checkVAD enter");
		int nRet = native_appendData(hVadHandle, byData, len, vad);
		Log.d("vadLib", "checkVAD leave "+ (new Date().getTime()-d.getTime()));
		return nRet;
	}
	public int  setParam( int paramID, int paramValue)
	{
		Date d = new Date();
		Log.d("vadLib", "setParam enter");
		int nRet =  native_setParam(hVadHandle, paramID, paramValue);
		Log.d("vadLib", "setParam leave "+ (new Date().getTime()-d.getTime()));
		return nRet;
	}
	
	public static native int  native_initialize();
	public static native void native_uninitialize(int handle);
	public static native void native_reset(int handle);
	public static native int  native_appendData(int handle, byte[] pData, int len, VadData vad);
	public static native int  native_setParam(int handle, int paramID, int paramValue);

}
