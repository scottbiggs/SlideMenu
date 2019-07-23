package com.sleepfuriously.slidemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatButton;



public class SlideMenu extends AppCompatButton {

	//-------------------
	//  constants
	//-------------------

	private static final String TAG = SlideMenu.class.getSimpleName();

	/** number of DIP pixels per mm */
	private static final float DIPS_PER_MM = 6.299f;

	/**
	 * Size of this widget in density independent pixels (dip).
	 * We want 9mm for the main circle, and extra 7mm for left
	 * and right, and an extra 7mm for the top.  Does not include
	 * padding.
	 */
//	private static final int DIP_HEIGHT = (int) (DIPS_PER_MM * (9f + 7f)),
//			DIP_WIDTH = (int) (DIPS_PER_MM * (9f + 7f + 7f));

	/**
	 * Now I just want a square about 8mm per side.
	 */
	private static final int DIP_HEIGHT = (int) (DIPS_PER_MM * 8f);
	private static final int DIP_WIDTH = (int) (DIPS_PER_MM * 8f);

	//-------------------
	//  data
	//-------------------

	/** Strings to display in left & right bubbles */
	private String mLeftText, mRightText;

	/** used during onDraw() */
	private Paint mPaint;

	/** all sorts of data about the current screen */
	private DisplayMetrics mMetrics;

	/** number to multiply dip to get actual pixels */
	private float mPixelDensity;

	/**
	 * Desired final size of this widget in current screen pixels.
	 * Does not include padding.
	 */
	private int mWidth, mHeight;

	/** trash var, but declared and initialized globably for speed */
	private Rect mRect;

	/** area of this View in absolute screen coords */
	private Rect mMainRectScreenCoords;

	/**
	 * Boundaries for the option menu areas.
	 * When the user's touch is within these boundaries, the appropriate
	 * menu should display.  And when the user releases within these
	 * boundaries, that action will be taken.<br>
	 * <br>
	 * These are Rects that are the same size as this widget, just to
	 * the left and right.
	 */
	private Rect mLeftOptionRect, mRightOptionRect;

	/**
	 * Listener for callbacks when the user slides left or right.
	 */
	private OnSlideMenuListener mOnSlideMenuListener = null;

	/**
	 * Holds the position of the last touch ACTION_DOWN event.
	 */
	private float mTouchStartX, mTouchStartY;

	/**
	 * True means that the user's finger is currently over the
	 * left/right menu area.
	 */

	private boolean mLeftActive = false, mRightActive = false;


	//-------------------
	//  constructors & initializers
	//-------------------

	public SlideMenu(Context context) {
		super(context);
		init();
	}

	public SlideMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttrs(attrs);
		init();
	}

	public SlideMenu(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttrs(attrs);
		init();
	}

	/**
	 * Does all the simple initiazations.
	 */
	private void init() {

		mRect = new Rect();

		mPaint = new Paint();
		mPaint.setColor(getResources().getColor(R.color.background_color));

		// Get display metrics. They'll be useful later.
		DisplayMetrics mMetrics = getResources().getDisplayMetrics();
		mPixelDensity = mMetrics.density;

		// calculate the size we want for this widget based on current
		// screen density
		mWidth = (int) ((float) DIP_WIDTH * mPixelDensity);
		mHeight = (int) ((float) DIP_HEIGHT * mPixelDensity);

		mLeftOptionRect = new Rect();   // filled in after the layout is done
		mRightOptionRect = new Rect();
		mMainRectScreenCoords = new Rect();

		// disable built-in background
		setBackgroundResource(0);
	}


	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);

		getGlobalVisibleRect(mMainRectScreenCoords);

		// The left option area is 8mm to the left and includes the left third
		// of this widget.  Similar for the right option area.
		mLeftOptionRect.set(mMainRectScreenCoords);
		mLeftOptionRect.left -= getWidth();
		mLeftOptionRect.right -= getWidth() + (getWidth() / 3);

		mRightOptionRect.set(mMainRectScreenCoords);
		mRightOptionRect.right += getWidth();
		mRightOptionRect.left += getWidth() - (getWidth() / 3);

//		Log.d(TAG, "main rect = " + mMainRectScreenCoords);
//		Log.d(TAG, "left rect = " + mLeftOptionRect);
//		Log.d(TAG, "right rect = " + mRightOptionRect);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	/**
	 * Interprets attributes from xml file declarations of SlideMenu
	 * widgets.
	 *
	 * @param attrs		The attrs param from a constructor
	 */
	private void parseAttrs(AttributeSet attrs) {
		TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.SlideMenu);
		mLeftText = array.getString(R.styleable.SlideMenu_left_text);
		mRightText = array.getString(R.styleable.SlideMenu_right_text);
		array.recycle();
	}


	//-------------------
	//  methods
	//-------------------

	/**
	 * When the button is first touched, display the options (left and right).
	 *
	 * When the user slides their hand, illuminate/deluminate the options
	 * appropriately.
	 *
	 * When the user's hand is lifted, call the left/right interface if
	 * the hand was still in a proper position (and the option was illuminated,
	 * of course).
	 *
	 * preconditions
	 *      For the anything that uses this widget, the must call
	 *      {@link #setOnSlideMenuListener(OnSlideMenuListener)}
	 *      before any events occur. (duh!)
	 *
	 * @param   event   The MotionEvent that caused this.
	 *
	 * @return  True means the event was completely handled.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mTouchStartX = event.getRawX();
				mTouchStartY = event.getRawY();
				Log.d(TAG, "onTouch() - ACTION_DOWN, x = " + mTouchStartX + ", y = " + mTouchStartY);
				break;

			case MotionEvent.ACTION_UP:
				// Only do something if a listener exists
				if (mOnSlideMenuListener != null) {
					if (mLeftActive) {
						mOnSlideMenuListener.onSlideLeft();
						mLeftActive = false;
						mRightActive = false;
					}
					else if (mRightActive) {
						mOnSlideMenuListener.onSlideRight();
						mLeftActive = false;
						mRightActive = false;
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (isInLeftActionArea((int)event.getRawX(), (int)event.getRawY())) {
					if (mLeftActive != true) {
						mLeftActive = true;
						drawLeftOption();
					}
				}
				else if (isInRightActionArea((int)event.getRawX(), (int)event.getRawY())) {
					if (mRightActive != true) {
						mRightActive = true;
						drawRightOption();
					}
				}
				else {
					// no longer in any drawing area, undraw if necessary
					if (mLeftActive) {
						undrawLeftOption();
						mLeftActive = false;
					}
					else if (mRightActive) {
						undrawRightOption();
						mRightActive = false;
					}
				}
				break;

			default:
				return false;   // Keep processing this unknown event
		}

		return true;    // event completely handled
	}


	/**
	 * Determines if the given coordinates are within the area designated
	 * as the left option area.
	 */
	private boolean isInLeftActionArea(int x, int y) {
		return mLeftOptionRect.contains(x, y);
	}

	/**
	 * Determines if the given coordinates are within the area designated
	 * as the right option area.
	 */
	private boolean isInRightActionArea(int x, int y) {
		return mRightOptionRect.contains(x, y);
	}


	private void drawLeftOption() {
		Log.d(TAG, "left option drawn");
		// todo
	}

	private void drawRightOption() {
		Log.d(TAG, "right option drawn");
		// todo
	}

	private void undrawLeftOption() {
		Log.d(TAG, "left option UNdrawn");
		// todo
	}

	private void undrawRightOption() {
		Log.d(TAG, "right option UNdrawn");
		// todo
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		Log.v("SlideMenu onMeasure w", MeasureSpec.toString(widthMeasureSpec));
//		Log.v("SlideMenu onMeasure h", MeasureSpec.toString(heightMeasureSpec));

//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int mode = MeasureSpec.getMode(widthMeasureSpec);

		// We want the circle to be 15mm, plenty big enough for
		// a good touch, but small enough for plenty of other circles
		// to fill the screen.
		//
		// Then we want to provide room on the left and the right for our
		// sliding menu popup, which will be about the same size.


		int width = resolveSize(mWidth, widthMeasureSpec);
		int height = resolveSize(mHeight, heightMeasureSpec);

		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		// draw a background so we'll know how big the canvas is
//		canvas.drawColor(getResources().getColor(R.color.colorAccent));

		getDrawingRect(mRect);
		canvas.drawCircle(mRect.left + (mRect.width() / 2), mRect.top + (mRect.height() / 2),
				mRect.width() / 2,
				mPaint);

		// draw everything else
		super.onDraw(canvas);
	}

	//-------------------
	//  getters & setters
	//-------------------

	public String getLeftText() {
		return mLeftText;
	}

	public void setLeftText(String leftText) {
		this.mLeftText = leftText;
	}

	public String getRightText() {
		return mRightText;
	}

	public void setRightText(String rightText) {
		this.mRightText = rightText;
	}

	public OnSlideMenuListener getOnSlideMenuListener() {
		return mOnSlideMenuListener;
	}

	public void setOnSlideMenuListener(OnSlideMenuListener onSlideMenuListener) {
		this.mOnSlideMenuListener = onSlideMenuListener;
	}


	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//  interfaces
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public interface OnSlideMenuListener {

		/**
		 * This method is called when the user has chosen
		 * the left option.
		 */
		void onSlideLeft();

		/**
		 * This method is called when the user has chosen
		 * the right option.
		 */
		void onSlideRight();
	}

}
