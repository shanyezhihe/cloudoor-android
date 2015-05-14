package com.icloudoor.clouddoor;

import com.icloudoor.clouddoor.TakePicDisplayUtil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TakePicMaskView extends ImageView {

	private Paint mLinePaint;
	private Paint mAreaPaint;
	private Rect mCenterRect = null;
	private Context mContext;


	public TakePicMaskView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initPaint();
		mContext = context;
		Point p	= TakePicDisplayUtil.getScreenMetrics(mContext);
		widthScreen = p.x;
		heightScreen = p.y;
	}

	private void initPaint(){
		//�����м�͸��������α߽��Paint
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setColor(Color.RED);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setStrokeWidth(100f);
		mLinePaint.setAlpha(0);

		//����������Ӱ����
		mAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mAreaPaint.setColor(Color.GRAY);
		mAreaPaint.setStyle(Style.FILL);
		mAreaPaint.setAlpha(50);
		
		
		
	}
	public void setCenterRect(Rect r){
		this.mCenterRect = r;
		postInvalidate();  //����ˢ��
	}
	public void clearCenterRect(Rect r){
		this.mCenterRect = null;
	}

	int widthScreen, heightScreen;
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		if(mCenterRect == null)
			return;
		//����������Ӱ����
		canvas.drawRect(0, 0, widthScreen, mCenterRect.top, mAreaPaint);    //�Ϸ���Ӱ����
		canvas.drawRect(0, mCenterRect.bottom + 1, widthScreen, heightScreen, mAreaPaint);  //�·���Ӱ����
		canvas.drawRect(0, mCenterRect.top, mCenterRect.left - 1, mCenterRect.bottom  + 1, mAreaPaint);  //�����Ӱ����
		canvas.drawRect(mCenterRect.right + 1, mCenterRect.top, widthScreen, mCenterRect.bottom + 1, mAreaPaint);  //�Ҳ���Ӱ���� 

		//����Ŀ��͸������
		canvas.drawRect(mCenterRect, mLinePaint);
		super.onDraw(canvas);
	}



}