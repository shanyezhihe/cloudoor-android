package com.icloudoor.clouddoor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

public class SetGestureDrawLineView extends View {
	//�������
	private int mov_x;
	private int mov_y;	
	private Paint paint;
	private Canvas canvas;
	private Bitmap bitmap;
	private List<SetGesturePoint> list;
	private List<Pair<SetGesturePoint, SetGesturePoint>> linelist;  //��¼��������
	private Map<String, SetGesturePoint> autoCheckPointMap;  //�Զ�ѡ�е������
	private boolean isDrawEnable = true;
	
	private int[] screenDispaly;
	
	private SetGesturePoint currentPoint;
	private SetGestureCallBack GestureCallBack;  //�û���ͼ�Ļص�
	
	private StringBuilder passWordSb;  //�û���ǰ���Ƶ�ͼ������
	
	private boolean isVerify;
	private String passWord; //�û������passWord
	
	public SetGestureDrawLineView(Context context, List<SetGesturePoint> list, boolean isVerify,
			String passWord, SetGestureCallBack callBack){
		super(context);		
		screenDispaly = SetGestureUtil.getScreenDispaly(context);
		paint = new Paint(Paint.DITHER_FLAG);// ����һ������
		bitmap = Bitmap.createBitmap(screenDispaly[0], screenDispaly[0], Bitmap.Config.ARGB_8888);
		canvas = new Canvas();
		canvas.setBitmap(bitmap);
		paint.setStyle(Style.STROKE);// ���÷����
		paint.setStrokeWidth(10);// �ʿ�5����
		paint.setColor(Color.rgb(245, 142, 33));// ����Ĭ��������ɫ
		paint.setAntiAlias(true);// ����ʾ���
		
		this.list = list;
		this.linelist = new ArrayList<Pair<SetGesturePoint, SetGesturePoint>>();
		
		initAutoCheckPointMap();
		this.GestureCallBack = callBack; 
		
		// ��ʼ�����뻺��
		this.isVerify = isVerify;
		this.passWordSb = new StringBuilder();
		this.passWord = passWord;
	}
	
	private void initAutoCheckPointMap() {
		autoCheckPointMap = new HashMap<String, SetGesturePoint>();
		autoCheckPointMap.put("1,3", getGesturePointByNum(2));
		autoCheckPointMap.put("1,7", getGesturePointByNum(4));
		autoCheckPointMap.put("1,9", getGesturePointByNum(5));
		autoCheckPointMap.put("2,8", getGesturePointByNum(5));
		autoCheckPointMap.put("3,7", getGesturePointByNum(5));
		autoCheckPointMap.put("3,9", getGesturePointByNum(6));
		autoCheckPointMap.put("4,6", getGesturePointByNum(5));
		autoCheckPointMap.put("7,9", getGesturePointByNum(8));
	}
	private SetGesturePoint getGesturePointByNum(int num) {
		for (SetGesturePoint point : list) {
			if (point.getNum() == num) {
				return point;
			}
		}
		return null;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// super.onDraw(canvas);
		canvas.drawBitmap(bitmap, 0, 0, null);
	}
	
	// �����¼�
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isDrawEnable == false) {
			// ���ڲ��������
			return true;
		}
		paint.setColor(Color.rgb(245, 142, 33));// ����Ĭ��������ɫ
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mov_x = (int) event.getX();
			mov_y = (int) event.getY();
			// �жϵ�ǰ�����λ���Ǵ����ĸ���֮��
			currentPoint = getPointAt(mov_x, mov_y);
			if (currentPoint != null) {
				currentPoint.setPointState(SetGestureConstants.POINT_STATE_SELECTED);
				passWordSb.append(currentPoint.getNum());
			}
			// canvas.drawPoint(mov_x, mov_y, paint);// ����
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			clearScreenAndDrawList();

			// �õ���ǰ�ƶ�λ���Ǵ����ĸ�����
			SetGesturePoint pointAt = getPointAt((int) event.getX(),
					(int) event.getY());
			// ����ǰ�û���ָ���ڵ����֮ǰ
			if (currentPoint == null && pointAt == null) {
				return true;
			} else {// �����û�����ָ�ƶ����˵���
				if (currentPoint == null) {// ���жϵ�ǰ��point�ǲ���Ϊnull
					// ���Ϊ�գ���ô����ָ�ƶ����ĵ㸳ֵ��currentPoint
					currentPoint = pointAt;
					// ��currentPoint���������ѡ��Ϊtrue;
					currentPoint.setPointState(SetGestureConstants.POINT_STATE_SELECTED);
					passWordSb.append(currentPoint.getNum());
				}
			}
			if (pointAt == null
					|| currentPoint.equals(pointAt)
					|| SetGestureConstants.POINT_STATE_SELECTED == pointAt
							.getPointState()) {
				// ����ƶ�������Բ�����򣬻��ߵ�ǰ����ĵ��뵱ǰ�ƶ����ĵ��λ����ͬ�����ߵ�ǰ����ĵ㴦��ѡ��״̬
				// ��ô�Ե�ǰ�ĵ�����Ϊ��㣬����ָ�ƶ�λ��Ϊ�յ㻭��
				canvas.drawLine(currentPoint.getCenterX(),
						currentPoint.getCenterY(), event.getX(), event.getY(),
						paint);// ����
			} else {
				// �����ǰ����ĵ��뵱ǰ�ƶ����ĵ��λ�ò�ͬ
				// ��ô��ǰǰ�������Ϊ��㣬�����ƶ����ĵ��λ�û���
				canvas.drawLine(currentPoint.getCenterX(),
						currentPoint.getCenterY(), pointAt.getCenterX(),
						pointAt.getCenterY(), paint);// ����
				pointAt.setPointState(SetGestureConstants.POINT_STATE_SELECTED);

				// �ж��Ƿ��м����Ҫѡ��
				SetGesturePoint betweenPoint = getBetweenCheckPoint(currentPoint,
						pointAt);
				if (betweenPoint != null
						&& SetGestureConstants.POINT_STATE_SELECTED != betweenPoint
								.getPointState()) {
					// �����м�㲢��û�б�ѡ��
					Pair<SetGesturePoint, SetGesturePoint> pair1 = new Pair<SetGesturePoint, SetGesturePoint>(
							currentPoint, betweenPoint);
					linelist.add(pair1);
					passWordSb.append(betweenPoint.getNum());
					Pair<SetGesturePoint, SetGesturePoint> pair2 = new Pair<SetGesturePoint, SetGesturePoint>(
							betweenPoint, pointAt);
					linelist.add(pair2);
					passWordSb.append(pointAt.getNum());
					// �����м��ѡ��
					betweenPoint.setPointState(SetGestureConstants.POINT_STATE_SELECTED);
					// ��ֵ��ǰ��point;
					currentPoint = pointAt;
				} else {
					Pair<SetGesturePoint, SetGesturePoint> pair = new Pair<SetGesturePoint, SetGesturePoint>(
							currentPoint, pointAt);
					linelist.add(pair);
					passWordSb.append(pointAt.getNum());
					// ��ֵ��ǰ��point;
					currentPoint = pointAt;
				}
			}
			invalidate();
			break;
		case MotionEvent.ACTION_UP:// ����ָ̧���ʱ��
			if (isVerify) {
				// ��������У��
				// �����Ļ�����е��ߣ�ֻ���ϼ������汣�����
				if (passWord.equals(passWordSb.toString())) {
					// �����û����Ƶ����������봫���������ͬ
					GestureCallBack.checkedSuccess();
				} else {
					// �û����Ƶ������봫������벻ͬ��
					GestureCallBack.checkedFail();
				}
			} else {
				GestureCallBack.onGestureCodeInput(passWordSb.toString());
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * ָ��ʱ��ȥ������Ƶ�״̬
	 * @param delayTime �ӳ�ִ��ʱ��
	 */
	public void clearDrawlineState(long delayTime) {
		if (delayTime > 0) {
			// ���ƺ�ɫ��ʾ·��
			isDrawEnable = false;
			drawErrorPathTip();
		}
		new Handler().postDelayed(new clearStateRunnable(), delayTime);
	}
	
	/**
	 * �������״̬���߳�
	 */
	final class clearStateRunnable implements Runnable {
		public void run() {
			// ����passWordSb
			passWordSb = new StringBuilder();
			// ��ձ����ļ���
			linelist.clear();
			// ���»��ƽ���
			clearScreenAndDrawList();
			for (SetGesturePoint p : list) {
				p.setPointState(SetGestureConstants.POINT_STATE_NORMAL);
			}
			invalidate();
			isDrawEnable = true;
		}
	}
	
	/**
	 * ͨ�����λ��ȥ�����������������ǰ������ĸ�Point�����
	 * 
	 * @param x
	 * @param y
	 * @return ���û���ҵ����򷵻�null�������û���ǰ�ƶ��ĵط����ڵ����֮��
	 */
	private SetGesturePoint getPointAt(int x, int y) {

		for (SetGesturePoint point : list) {
			// ���ж�x
			int leftX = point.getLeftX();
			int rightX = point.getRightX();
			if (!(x >= leftX && x < rightX)) {
				// ���Ϊ�٣���������һ���Ա�
				continue;
			}

			int topY = point.getTopY();
			int bottomY = point.getBottomY();
			if (!(y >= topY && y < bottomY)) {
				// ���Ϊ�٣���������һ���Ա�
				continue;
			}

			// ���ִ�е��⣬��ô˵����ǰ����ĵ��λ���ڱ��������λ������ط�
			return point;
		}

		return null;
	}
	
	private SetGesturePoint getBetweenCheckPoint(SetGesturePoint pointStart, SetGesturePoint pointEnd) {
		int startNum = pointStart.getNum();
		int endNum = pointEnd.getNum();
		String key = null;
		if (startNum < endNum) {
			key = startNum + "," + endNum;
		} else {
			key = endNum + "," + startNum;
		}
		return autoCheckPointMap.get(key);
	}

	/**
	 * �����Ļ�����е��ߣ�Ȼ�󻭳������������
	 */
	private void clearScreenAndDrawList() {
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		for (Pair<SetGesturePoint, SetGesturePoint> pair : linelist) {
			canvas.drawLine(pair.first.getCenterX(), pair.first.getCenterY(),
					pair.second.getCenterX(), pair.second.getCenterY(), paint);// ����
		}
	}
	
	/**
	 * У�����/���λ��Ʋ�һ����ʾ
	 */
	private void drawErrorPathTip() {
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		paint.setColor(Color.rgb(154, 7, 21));// ����Ĭ����·��ɫ
		for (Pair<SetGesturePoint, SetGesturePoint> pair : linelist) {
			pair.first.setPointState(SetGestureConstants.POINT_STATE_WRONG);
			pair.second.setPointState(SetGestureConstants.POINT_STATE_WRONG);
			canvas.drawLine(pair.first.getCenterX(), pair.first.getCenterY(),
					pair.second.getCenterX(), pair.second.getCenterY(), paint);// ����
		}
		invalidate();
	}
	
	public interface SetGestureCallBack {

		/**
		 * �û�����/��������������
		 */
		public abstract void onGestureCodeInput(String inputCode);

		/**
		 * �����û����Ƶ������봫���������ͬ
		 */
		public abstract void checkedSuccess();

		/**
		 * �����û����Ƶ������봫������벻��ͬ
		 */
		public abstract void checkedFail();
	}
}