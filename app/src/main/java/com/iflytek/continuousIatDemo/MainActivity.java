package com.iflytek.continuousIatDemo;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.continuousIat.R;
import com.iflytek.inputmethod.asr.vad.VadEngine;
import com.iflytek.util.IflyRecorder;
import com.iflytek.util.IflyRecorderListener;

public class MainActivity extends Activity implements OnClickListener,IflyRecorderListener {

	private static final String TAG = "DwaDemo";
	private SpeechRecognizer mIat;

	private RecordView volView;
	private ImageView waitView;
	private ImageView cancelView;

	private WakeLock mWakeLock;
	private Animation rotateAnimation;

	private EditText mEdit;
	private TextView mClear;
	private Toast mToast = null;
	private final int SAMPLE_RATE = 16000;

	/* 录音临时保存队列 */
	private ConcurrentLinkedQueue<byte[]> mRecordQueue = null;

	/* 端点检测引擎 */
	private VadEngine vadengine = null;

	/* 是否写入数据 */
	private boolean isRunning = true;

	/* 是否用户主动结束 */
	private boolean isUserEnd = false;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 初始化UI
		initUI();
		// 初始化录音机
		initRecorder();
		SpeechUtility.createUtility(this, "appid=" + getString(R.string.app_id));
		mIat = SpeechRecognizer.createRecognizer(this, new InitListener() {
			@Override
			public void onInit(int i) {

			}
		});
		vadengine = VadEngine.getInstance();
		vadengine.reset();
		mRecordQueue = new ConcurrentLinkedQueue<byte[]>();
	}
	// 初始化录音机
	private void initRecorder() {
		// 对三星手机的优化
		if(Build.MANUFACTURER.equalsIgnoreCase("samsung")){
			IflyRecorder.getInstance().initRecoder(SAMPLE_RATE,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					MediaRecorder.AudioSource.VOICE_RECOGNITION);
		} else {
			IflyRecorder.getInstance().initRecoder(SAMPLE_RATE,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					MediaRecorder.AudioSource.MIC);
		}
	}
	private void initUI() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		mEdit = (EditText)findViewById(R.id.edit_text);
		findViewById(R.id.tx_clear).setOnClickListener(this);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				"wakeLock");
		mToast = new Toast(this);
		rotateAnimation = new RotateAnimation(0f, 360f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		LinearInterpolator lin = new LinearInterpolator();
		rotateAnimation.setInterpolator(lin);
		rotateAnimation.setDuration(500);
		FrameLayout layout = (FrameLayout) findViewById(R.id.framelayout);
		volView = new RecordView(this);
		LayoutParams par = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		layout.addView(volView, 0, par);
		volView.setVisibility(View.GONE);
		waitView = (ImageView) findViewById(R.id.image_load);
		waitView.setVisibility(View.GONE);
		cancelView = (ImageView) findViewById(R.id.image_cancel);
		cancelView.setVisibility(View.GONE);
		
		findViewById(R.id.image_cancel).setOnClickListener(this);
		findViewById(R.id.btn_recognize).setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mWakeLock.acquire();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mWakeLock.release();
	}

	@Override
	protected void onDestroy() {
		if (mIat != null && isRunning)
			mIat.cancel();

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_recognize:
			if (isConnectNetwork(this)) {
				clickToStart();
			}else {
				Toast.makeText(MainActivity.this,"未检测到网络",Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.image_cancel:
			clickToCancel();
			break;
		case R.id.tx_clear:
			mEdit.setText("");
			break;
		default:
			break;
		}
	}

	private void clickToStart() {
		mRecordQueue.clear();
		// 重置启动标志位
		setIsRunning(true);
		isUserEnd = false;
		volView.startRecording();
		((Button) findViewById(R.id.btn_recognize)).setBackgroundDrawable(getResources().getDrawable(R.drawable.mic_p));
		cancelView.setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.btn_recognize)).setEnabled(false);

		// 开始录音
		IflyRecorder.getInstance().startRecoder(this);
		setParam();
		mIat.startListening(listener);

		new Thread(myRunner).start();// 开启上传数据的线程

	}

	private void clickToCancel() {
		((Button) findViewById(R.id.btn_recognize)).setBackgroundDrawable(getResources().getDrawable(R.drawable.mic_n));
		waitView.setVisibility(View.GONE);
		volView.setVisibility(View.GONE);
		cancelView.setVisibility(View.GONE);

		isUserEnd = true;
		if (mIat != null)
			mIat.cancel();
		IflyRecorder.getInstance().stopRecorder();
		((Button) findViewById(R.id.btn_recognize)).setEnabled(true);
	}

	private RecognizerListener listener = new RecognizerListener() {

		@Override
		public void onEndOfSpeech() {
			Log.d(TAG, "onEndOfSpeech");
		}

		@Override
		public void onVolumeChanged(int i, byte[] bytes) {
			Log.d(TAG, "volume:" + i);
			volView.setVolume(i);
		}

		@Override
		public void onBeginOfSpeech() {
			Log.d(TAG, "onBeginOfSpeech");
		}

		@Override
		public void onError(SpeechError error) {
			if(error.getErrorCode() == 10118) {
				//报错10118 检测到未说话，从新开始一次
				setParam();
				mIat.startListening(listener);
				setIsRunning(true);
			}else {
				clickToCancel();
				Log.e("iii",error.toString());
				Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
			
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			String textPre = mEdit.getText().toString();
			
			mEdit.setText(textPre+text);
			mEdit.setSelection(mEdit.getText().toString().length());
			if(isLast){
				setParam();
				mIat.startListening(listener);
				setIsRunning(true);
			}
		}
	};

	int volume;
	private Runnable myRunner = new Runnable() {
		public void run() {
			while (!isUserEnd) { // 条件是否用户主动结束
				if (getIsRunning()) {
					byte[] data = mRecordQueue.poll();
					if (data == null) {
						Log.d(TAG, "no---no data");
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
					volume = vadengine.vadCheck(data, data.length);
					if (volume >= 0) {
						Log.d(TAG, "no----volume");
						mIat.writeAudio(data, 0, data.length);
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// 检测到端点
					else {
						Log.d(TAG, "no----checked");
						// 重置引擎、停止监听等待返回结果
						vadengine.reset();
						mIat.stopListening();
						setIsRunning(false);
					}
				} else {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};


	/**
	 * 返回当前会话状态
	 * 
	 * @return
	 */
	protected synchronized boolean getIsRunning() {

		return isRunning;
	}

	/**
	 * 设置当前会话状态
	 * 
	 * @return
	 */
	protected synchronized void setIsRunning(boolean trueOrfalse) {
		isRunning = trueOrfalse;
	}

	public static boolean isConnectNetwork(Context context) {
		// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
		try {
			ConnectivityManager connectivity = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				// 获取网络连接管理的对象
				NetworkInfo info = connectivity.getActiveNetworkInfo();
				if (info != null && info.isConnected()) {
					// 判断当前网络是否已经连接
					if (info.getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			Log.d("error", e.toString());
		}
		return false;
	}
	
	@Override
	public void OnReceiveBytes(byte[] data, int length) {
		// record data
		if (length > 0) {
			byte[] temp = new byte[length];
			System.arraycopy(data, 0, temp, 0, length);
			if (data == null || data.length == 0)
				return;
			// 不断的填充数据
//			Log.d(TAG, "get----data");
			mRecordQueue.add(temp);
		}
	}
	/**
	 * 参数设置
	 *
	 * @return
	 */
	public void setParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS,  "2000");
		// 关闭sdk内部录音，使用writeAudio接口传入音频
		mIat.setParameter(SpeechConstant.AUDIO_SOURCE,  "-1");
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS,  "5000");
		// 设置音频保存路径，保存音频格式仅为pcm，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
//        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/test"+System.currentTimeMillis()/1000+".pcm");
	}
}