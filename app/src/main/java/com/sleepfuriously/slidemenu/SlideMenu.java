package com.sleepfuriously.slidemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatButton;


/**
 * <h2>Dev notes:</h2>
 *
 * This widget has several rectangles:
 *     <li>Orig - The size set from the XML definition. It's actually a square.</li>
 *     <li>Clip - The largest possible dimensions of anything this widget may draw</li>
 *     <li>LZ (Landing Zone) - Defines the rectangle to activate a menu</li>
 *     <li>LeftLz & RightLz - Distinguishes between the left and right landing zones</li>
 *<br>
 * There are three sizes:
 * 		<li>mm - millimeters, yes actual size from user's pov!</li>
 *      <li>Dp - Density Independent Pixels (aka DiP)</li>
 *      <li>Ap - Actual Pixels, the number of pixels after translating for
 *              the current screen density</li>
 *<br>
 * Also two coordinate systems are used:
 *      <li>Relative - relative to (0,0) of the Orig Rect</li>
 *      <li>Screen - based on 0,0 being top left of the device's screen</li>
 *<br>
 * And there are three slop distances, called Clip Additional. These define the extra
 * area of the clipping region that's not otherwise specified (like for text and other
 * animations).  It's actually the slop that makes it look good.
 * 		<li>ClipAdditionalHeightTop</li>
 * 		<li>ClipAdditionalLeft</li>
 * 		<li>ClipAdditionalRight</li>
 *
 * <h3>How it Works</h3>
 *
 * Once the OrigRect is touched and slid (left or right), the relevant menu will
 * appear in the LeftLz or RightLz.  It will disappear if the finger leaves the
 * region.  Only if the finger is lifted when the menu is showing will a menu
 * event fire.<br>
 *<br>
 * Most of the constants deal with sizes in terms of millimeters.  This is
 * useful for designers, but hard on programmers.  Thus many variables and
 * constants will have both.  The suffix will distinguish which is which
 * (furthermore mm vars will be <code>floats</code> while dp vars will
 * always be <code>ints</code>--at least that's what I'm striving for).
 */
public class SlideMenu extends AppCompatButton {

	//-------------------
	//  constants
	//-------------------

	private static final String TAG = SlideMenu.class.getSimpleName();

	/** number of DiPs per mm */
	private static final float DP_PER_MM = 6.299f;

	/** mms wide a circle's stroke should be */
	private static final float CIRCLE_STROKE_WIDTH_MM = 1f;
	private static final int CIRCLE_STROKE_WIDTH_DP = (int) (CIRCLE_STROKE_WIDTH_MM * DP_PER_MM);
	// todo: should make a variable so that the stroke width is properly set for screen density

	/**
	 * The size of the original widget's side (it's always a square)
	 * in millimeters.
	 */
	private static final float ORIG_SIDE_MM = 9f;
	/** Size of the size of a side of the original square in DiP */
//	private static final int ORIG_SIDE_DP =
//			(int) (ORIG_SIDE_MM * DP_PER_MM);

		//===============
		//  landing zone constants
		//

	/** width and height of landing zones */
	private static final float
			LZ_WIDTH_MM = 12f,
			LZ_HEIGHT_MM = 11f;


		//===============
		//  clipping constants
		//

	/** Extra height needed above the original widget (in mm) */
	private static final float CLIP_ADDITIONAL_HEIGHT_MM = 9f;

	/**
	 * Extra width needed for the full rect beyond the original widget
	 * (in mm). This width is applied to the left and the right sides.
	 * Since the widget is symmetrical, these are the same.
 	 */
	private static final float
			CLIP_ADDITIONAL_WIDTH_LEFT_MM = 11f;
	private static final float
			CLIP_ADDITIONAL_WIDTH_RIGHT_MM = CLIP_ADDITIONAL_WIDTH_LEFT_MM;

	/** width of clip rect in mm */
	private static final float CLIP_WIDTH_MM =
			ORIG_SIDE_MM + CLIP_ADDITIONAL_WIDTH_LEFT_MM + CLIP_ADDITIONAL_WIDTH_RIGHT_MM;

	/** height of clip rect in mm */
	private static final float CLIP_HEIGHT_MM =
			ORIG_SIDE_MM + CLIP_ADDITIONAL_HEIGHT_MM;


	//-------------------
	//  data
	//-------------------

		//===============
		//  general data
		//

	/**
	 * When TRUE, it's the first time this widget is drawn (good time to do some
	 * initiazation).
	 */

	private boolean mFirstTime = true;

	/** Strings to display in left & right bubbles */
	private String mLeftText, mRightText;

	/** Paint for the main body of the widget. Used during onDraw() */
	private Paint mOrigPaint;

	/** Paint for the left option menu */
	private Paint mLeftPaint;

	/** Paint for the right option menu */
	private Paint mRightPaint;

	/** all sorts of data about the current screen */
	private DisplayMetrics mMetrics;

	/** number to multiply DiP to get actual pixels (derived from DisplayMetrics) */
	private float mPixelDensity;

	/** only used in onDraw(), but declared and initialized globally for speed */
	private Rect mTmpRect;

	/**
	 * Listener for callbacks when the user slides left or right.
	 */
	private OnSlideMenuListener mOnSlideMenuListener = null;

	/**
	 * Holds the position of the last touch ACTION_DOWN event.
	 */
	private float mTouchStartRelativeX, mTouchStartRelativeY;

	/**
	 * True means that the user's finger is currently over the
	 * left/right menu area.
	 */
	private boolean mLeftActive = false, mRightActive = false;

		//===============
		//  size and position data
		//

	/** Width & height of original clipping square in current screen density pixels */
	private int mOrigWidthAp, mOrigHeightAp;

	/**
	 * Clipping size of this widget in current screen density pixels
	 * (Actual Pixels). ALL drawing will be within these boundaries
	 */
	private int mClipWidthAp, mClipHeightAp;

	/** area of the original View in absolute screen coords */
	private Rect mOrigScreenCoordsApRect;

	/** Area of the View in relative coords (top left will always be 0,0) */
	private Rect mOrigRelativeCoordsApRect;

	/** Describes the clipping rect in relative screen coords */
	private Rect mClipRelativeApRect;

	/**
	 * Boundaries for the menu landing zones.
	 * When the user's touch is within these boundaries, the appropriate
	 * menu should display.  And when the user releases within these
	 * boundaries, that action will be taken.<br>
	 */
	private Rect mLeftLzRelativeApRect, mRightLzRelativeApRect;

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

		mFirstTime = true;

		// Force onDraw() to be called every time invalide() is called.
		// This is a standard thing any time onDraw() is overridden.
		setWillNotDraw(false);

		mTmpRect = new Rect();

		// todo: make this look much prettier
		mOrigPaint = new Paint();
		mOrigPaint.setColor(getResources().getColor(R.color.background_color));
		mOrigPaint.setStyle(Paint.Style.STROKE);
		mOrigPaint.setStrokeWidth(CIRCLE_STROKE_WIDTH_DP);


		// todo: make prettier
		mLeftPaint = new Paint();
		mLeftPaint.setColor(getResources().getColor(R.color.left_option_color));
		mRightPaint = new Paint();
		mRightPaint.setColor(getResources().getColor(R.color.right_option_color));

		// Get display metrics. They'll be useful later.
		mMetrics = getResources().getDisplayMetrics();
		mPixelDensity = mMetrics.density;

		// calculate the sizes we want for this widget based on current
		// screen density
		mOrigWidthAp = mmToPixels(ORIG_SIDE_MM);
		mOrigHeightAp = mmToPixels(ORIG_SIDE_MM);

		mClipWidthAp = mmToPixels(CLIP_WIDTH_MM);
		mClipHeightAp = mmToPixels(CLIP_HEIGHT_MM);


		// These will be filled in after the layout is done drawing
		mLeftLzRelativeApRect = new Rect();
		mRightLzRelativeApRect = new Rect();
		mOrigRelativeCoordsApRect = new Rect();
		mOrigScreenCoordsApRect = new Rect();

		// disable built-in background
		setBackgroundResource(0);
	}


	/**
	 * Initializes all sorts of variables that can't be initialized until the last
	 * minute (because they need info that won't be ready until the last minute).
	 */
	private void firstTimeInit() {

		getDrawingRect(mOrigRelativeCoordsApRect);
		if (getGlobalVisibleRect(mOrigScreenCoordsApRect) == false) {
			Log.e(TAG, "Unable to get clipping coords in onWindowFocusChanged()!");
			return;
		}

		// calc landing zones (relative coords to Orig)
		int lzWidth = mmToPixels(LZ_WIDTH_MM);
		int lzHeight = mmToPixels(LZ_HEIGHT_MM);

		mLeftLzRelativeApRect.bottom = mOrigRelativeCoordsApRect.bottom;
		mLeftLzRelativeApRect.top = mOrigRelativeCoordsApRect.bottom - lzHeight;
		mLeftLzRelativeApRect.right = mOrigRelativeCoordsApRect.left;
		mLeftLzRelativeApRect.left = mOrigRelativeCoordsApRect.left - lzWidth;

		mRightLzRelativeApRect.bottom = mOrigRelativeCoordsApRect.bottom;
		mRightLzRelativeApRect.top = mOrigRelativeCoordsApRect.bottom - lzHeight;
		mRightLzRelativeApRect.left = mOrigRelativeCoordsApRect.right;
		mRightLzRelativeApRect.right = mOrigRelativeCoordsApRect.right + lzWidth;

		// calc clipping rect
		mClipRelativeApRect = new Rect(mOrigRelativeCoordsApRect);
		mClipRelativeApRect.top = mClipRelativeApRect.bottom - mClipHeightAp;

		int centerX = (mOrigRelativeCoordsApRect.right - mOrigRelativeCoordsApRect.left) / 2;
		int halfWidth = mClipWidthAp / 2;

		mClipRelativeApRect.right =  centerX + halfWidth;
		mClipRelativeApRect.left = centerX - halfWidth;
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
	 * Converts millimeters to current screen pixels.
	 *
	 * preconditions:
	 * 		mPixelDensity	Properly set for this screen
	 *
	 * @param mm	The number of millimeters
	 *
	 * @return	The closest number of pixels for this screen to display the
	 * 			requested millimeters.
	 */
	private int mmToPixels(float mm) {
		return (int) (mPixelDensity * DP_PER_MM * mm);
	}


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
				mTouchStartRelativeX = event.getRawX();
				mTouchStartRelativeY = event.getRawY();
				Log.d(TAG, "onTouch() - ACTION_DOWN, x = " + mTouchStartRelativeX + ", y = " + mTouchStartRelativeY);

				// todo: change the paint!
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
				invalidate();
				break;

			case MotionEvent.ACTION_MOVE:
				if (isInLeftActionArea((int)event.getX(), (int)event.getY())) {
					if (mLeftActive != true) {
						mLeftActive = true;
						invalidate();
					}
				}
				else if (isInRightActionArea((int)event.getX(), (int)event.getY())) {
					if (mRightActive != true) {
						mRightActive = true;
						invalidate();
					}
				}
				else {
					// no longer in any drawing area, undraw if necessary
					if (mLeftActive) {
						mLeftActive = false;
						invalidate();   // force redraw (hope it's enough)
					}
					else if (mRightActive) {
						mRightActive = false;
						invalidate();
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
		return mLeftLzRelativeApRect.contains(x, y);
	}

	/**
	 * Determines if the given coordinates are within the area designated
	 * as the right option area.
	 */
	private boolean isInRightActionArea(int x, int y) {
		return mRightLzRelativeApRect.contains(x, y);
	}


	/**
	 * Draws the left option menu.  Should only be called from onDraw() or
	 * one of its children as it needs a Canvas.<br>
	 *<br>
	 * preconditions:
	 *<li>      mLeftPaint      initialized
	 *
	 * @param canvas    Standard Canvas to draw on.
	 *
	 * @param startRect The starting rect of the widget (NOT the left
	 *                  options menu area--the regular area).
	 *                  I think this should be in <em>screen coords</em>. todo: test this!
	 */
	private void drawLeftOption(Canvas canvas, Rect startRect) {
		canvas.drawRect(mLeftLzRelativeApRect, mLeftPaint);
	}

	private void drawRightOption(Canvas canvas) {
		canvas.drawRect(mRightLzRelativeApRect, mRightPaint);
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

		int mode = MeasureSpec.getMode(widthMeasureSpec);

		// Size (as far as the other widgets are concerned) is
		// just the original size.
		int width = resolveSize(mOrigWidthAp, widthMeasureSpec);
		int height = resolveSize(mOrigHeightAp, heightMeasureSpec);

		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		// draw everything else todo: is this necessary?
		super.onDraw(canvas);

		if (mFirstTime) {
			firstTimeInit();
			mFirstTime = false;
		}

		// first things first: increase the clip rect
		if (mClipRelativeApRect != null) {
			canvas.clipRect(mClipRelativeApRect, Region.Op.REPLACE);
		}

		// draw a background so we'll know how big the canvas is
		canvas.drawColor(getResources().getColor(R.color.colorAccent));

		getDrawingRect(mTmpRect);

		// draw the circle for this button
		float x = mTmpRect.left + ((float)(mTmpRect.width()) / 2f);	// center within mTmpRect
		float y = mTmpRect.top + ((float)(mTmpRect.height()) / 2f);

		float radius = ((float)mTmpRect.width()) / 2f;
		radius -= ((float)CIRCLE_STROKE_WIDTH_DP) / 2f;	// Make sure the circle's stroke stays inside
														// the bounds of Orig area
		canvas.drawCircle(x, y, radius, mOrigPaint);

		// Now draw any option menu
		if (mLeftActive) {
//			drawLeftOption(canvas, mTmpRect);
			drawLeftOption(canvas, mLeftLzRelativeApRect);
		}
		else if (mRightActive) {
			drawRightOption(canvas);
		}

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
