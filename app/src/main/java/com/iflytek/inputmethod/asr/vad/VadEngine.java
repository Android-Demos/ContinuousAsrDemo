package com.iflytek.inputmethod.asr.vad;



import com.iflytek.inputmethod.asr.vad.VadChecker.VadData;

import android.util.Log;

/**
 * @author yjzhao
 *
 */
public class VadEngine{
	
	private final static String LOG_TAG = "VocalPassEngine";

	private static VadEngine instance;
	private VadChecker mvader = new VadChecker();

	private VadEngine()
	{
	}
	public static VadEngine getInstance(){
		if(instance == null){
			instance = new VadEngine();
		}
		return instance;
	}
	public void reset()
	{
		mvader.initialize();
		mvader.reset();
	}
	public int vadCheck(byte[] byData, int len)
	{
		VadData d = new VadData();
		mvader.checkVAD(byData, len, d);
		//Log.d(LOG_TAG,"VAD check state = "+ d.status +"  time = " + (new Date().getTime()-begin.getTime()));
		boolean bError = false;
		switch (d.status) {
		case VadChecker.Status.ivVAD_OK: 
			break;
		case VadChecker.Status.ivVAD_INVARG:
		case VadChecker.Status.ivVAD_INVCALL:
		case VadChecker.Status.ivVAD_INSUFFICIENTBUFFER:
		case VadChecker.Status.ivVAD_BUFFERFULL:
			bError = true;
			break;
		case VadChecker.Status.ivVAD_FINDSTART:
			break;
		case VadChecker.Status.ivVAD_FINDPAUSE:
			break;
		case VadChecker.Status.ivVAD_FINDSTARTANDPAUSE:
			break;
		case VadChecker.Status.ivVAD_FINDEND: 
		case VadChecker.Status.ivVAD_FINDSTARTANDEND:
			break;
		}

		if (bError) {
			return 0;
		}else
		{
			if(d.status == VadChecker.Status.ivVAD_FINDSTARTANDEND
					|| VadChecker.Status.ivVAD_FINDEND == d.status)
			{
				Log.d(LOG_TAG, "-------Check vad get endpoint-------");
				return -1;
			}else
			{
				return d.volumeLevel;
			}
		}
	}
}
