package com.icloudoor.clouddoor;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.icloudoor.clouddoor.SetGestureDrawLineView.SetGestureCallBack;

public class SetGestureContentView extends ViewGroup {
	
	private int baseNum = 6;
	private int[] screenDispaly;
	
	/**
	 * ÿ��������Ŀ��
	 */
	private int blockWidth;
	
	private List<SetGesturePoint> list;
	private Context context;
	private boolean isVerify;
	private SetGestureDrawLineView getstureDrawLine;
	
	/**
	 * ����9��ImageView����������ʼ��
	 * @param context
	 * @param isVerify �Ƿ�ΪУ����������
	 * @param passWord �û���������
	 * @param callBack ���ƻ�����ϵĻص�
	 */
	public SetGestureContentView(Context context, boolean isVerify, String passWord, SetGestureCallBack callBack){
		super(context);
		screenDispaly = SetGestureUtil.getScreenDispaly(context);
		blockWidth = screenDispaly[0]/3;
		this.list = new ArrayList<SetGesturePoint>();
		this.context = context;
		this.isVerify = isVerify;
		// ���9��ͼ��
		addChild();
		// ��ʼ��һ�����Ի��ߵ�view
		getstureDrawLine = new SetGestureDrawLineView(context, list, isVerify, passWord, callBack);
	}
	
	private void addChild(){
		for (int i = 0; i < 9; i++) {
			ImageView image = new ImageView(context);
			image.setBackgroundResource(R.drawable.sign_gray);
			this.addView(image);
			invalidate();
			// �ڼ���
			int row = i / 3;
			// �ڼ���
			int col = i % 3;
			// ������ÿ������
			int leftX = col*blockWidth+blockWidth/baseNum;
			int topY = row*blockWidth+blockWidth/baseNum;
			int rightX = col*blockWidth+blockWidth-blockWidth/baseNum;
			int bottomY = row*blockWidth+blockWidth-blockWidth/baseNum;
			SetGesturePoint p = new SetGesturePoint(leftX, rightX, topY, bottomY, image,i+1);
			this.list.add(p);
		}
	}
	
	public void setParentView(ViewGroup parent){
		// �õ���Ļ�Ŀ��
		int width = screenDispaly[0];
		LayoutParams layoutParams = new LayoutParams(width, width);
		this.setLayoutParams(layoutParams);
		getstureDrawLine.setLayoutParams(layoutParams);
		parent.addView(getstureDrawLine);
		parent.addView(this);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		for (int i = 0; i < getChildCount(); i++) {
			//�ڼ���
			int row = i/3;
			//�ڼ���
			int col = i%3;
			View v = getChildAt(i);
			v.layout(col*blockWidth+blockWidth/baseNum, row*blockWidth+blockWidth/baseNum, 
					col*blockWidth+blockWidth-blockWidth/baseNum, row*blockWidth+blockWidth-blockWidth/baseNum);
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// ��������ÿ����view�Ĵ�С
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.measure(widthMeasureSpec, heightMeasureSpec);
        }
	}
	
	/**
	 * ����·��delayTimeʱ�䳤
	 * @param delayTime
	 */
	public void clearDrawlineState(long delayTime) {
		getstureDrawLine.clearDrawlineState(delayTime);
	}
}