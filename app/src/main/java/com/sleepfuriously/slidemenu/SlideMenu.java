package com.sleepfuriously.slidemenu;

import android.app.Activity;
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
 * This widget has three sizes:
 *     <li>Orig - The size set from the XML definition</li>
 *     <li>Additional - Dimensions to <em>add</em> to Orig to get the Full areas
 *         (kind of an intermediary between the two).</li>
 *     <li>Full - Includes the extra area for the menus</li>
 *<br>
 * And uses four rectangles:
 *      <li>Orig - Describes the original XML defined rectangle</li>
 *      <li>Full - Includes the entire clipping area for the menus, text, and anything else</li>
 *      <li>Left - Area for the left menu to become active</li>
 *      <li>Right - Area for the right menu to become active</li>
 *<br>
 * There are two pixel sizes:
 *      <li>Dp - Density Independent Pixels (aka DiP)</li>
 *      <li>Ap - Actual Pixels, the number of pixels after translating for
 *              the current screen density</li>
 *<br>
 * Also two coordinate systems are used:
 *      <li>Relative - relative to (0,0) of the Orig Rect</li>
 *      <li>Screen - based on 0,0 being top left of the device's screen</li>
 *<br>
 * Note that FullRect contains OrigRect, LeftRect, RightRect, and any text etc.<br>
 *<br>
 * Relevant variables and constants will include information in their
 * names.<br>
 *<br>
 * Once the OrigRect is touched and slid (left or right), the relevant menu will
 * appear in the LeftRect or RightRect.  It will disappear if the finger leaves the
 * region.  Only if the finger is lifted when the menu is showing will a menu
 * even fire.<br>
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

	/**
	 * The size of the original widget's side (it's always a square)
	 * in millimeters.
	 */
	private static final float ORIG_SIDE_MM = 9f;
	/** Size of the size of a side of the original square in DiP */
	private static final int ORIG_SIDE_DP =
			(int) (ORIG_SIDE_MM * DP_PER_MM);

	/** Extra height needed above the original widget (in mm) */
	private static final float ADDITIONAL_HEIGHT_MM = 9f;
	/** additional height in DiPs */
	private static final int ADDITIONAL_HEIGHT_DP =
			(int) (ADDITIONAL_HEIGHT_MM * DP_PER_MM);

	/**
	 * Extra width needed for the full rect beyond the original widget
	 * (in mm). This width is applied to the left and the right sides.
	 * Since the widget is symmetrical, these are the same.
 	 */
	private static final float
			ADDITIONAL_WIDTH_LEFT_MM = 11f;
	private static final float
			ADDITIONAL_WIDTH_RIGHT_MM = ADDITIONAL_WIDTH_LEFT_MM;

	/** same as above, but in DiPs */
	private static final int
			ADDITIONAL_WIDTH_LEFT_DP =
				(int) (ADDITIONAL_WIDTH_LEFT_MM * DP_PER_MM);
	private static final int
			ADDITIONAL_WIDTH_RIGHT_DP =
				(int) (ADDITIONAL_WIDTH_RIGHT_MM * DP_PER_MM);

	/** width of full rect in mm */
	private static final float FULL_WIDTH_MM =
			ORIG_SIDE_MM + ADDITIONAL_WIDTH_LEFT_MM + ADDITIONAL_WIDTH_RIGHT_MM;
	/** Width of Full rectangle in pixels */
	private static final int FULL_WIDTH_DP = (int) (FULL_WIDTH_MM * DP_PER_MM);

	/** height of full rect in mm */
	private static final float FULL_HEIGHT_MM =
			ORIG_SIDE_MM + ADDITIONAL_HEIGHT_MM;

	/** Height of Full rectangle in pixels */
	private static final int FULL_HEIGHT_DP = (int) (FULL_HEIGHT_MM * DP_PER_MM);

	//-------------------
	//  data
	//-------------------

		//===============
		//  general data
		//

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
	 * Desired final (Full) size of this widget in current screen density pixels
	 * (Actual Pixels). Does not include padding.
	 */
	private int mFullWidthAp, mFullHeightAp;

	/** area of the original View in absolute screen coords */
	private Rect mOrigScreenCoordsApRect;

	/** Area of the View in relative coords (top left will always be 0,0) */
	private Rect mOrigRelativeCoordsApRect;

	/**
	 * Area of the full view in coords relative to the original.
	 * Yes, that means that the left and top will be negative.
	 * This is used for setting clipping boundaries.
 	 */
	private Rect mFullRelativeApRect;

	/**
	 * Boundaries for the option menu areas.
	 * When the user's touch is within these boundaries, the appropriate
	 * menu should display.  And when the user releases within these
	 * boundaries, that action will be taken.<br>
	 */
	private Rect mLeftOptionRelativeApRect, mRightOptionRelativeApRect;

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
		mOrigWidthAp = (int) (mPixelDensity * (float) ORIG_SIDE_DP);
		mOrigHeightAp = (int) (mPixelDensity * (float) ORIG_SIDE_DP);

		mFullWidthAp = (int) (mPixelDensity * (float) FULL_WIDTH_DP);
		mFullHeightAp = (int) (mPixelDensity * (float) FULL_HEIGHT_DP);

		// These will be filled in after the layout is done drawing
		mLeftOptionRelativeApRect = new Rect();
		mRightOptionRelativeApRect = new Rect();
		mOrigRelativeCoordsApRect = new Rect();
		mOrigScreenCoordsApRect = new Rect();

		// disable built-in background
		setBackgroundResource(0);
	}


	// I'm overriding this as there are several variables that I need
	// to set, but they won't be ready until after the measuring is
	// done, which is here.
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);

		getDrawingRect(mOrigRelativeCoordsApRect);
		if (getGlobalVisibleRect(mOrigScreenCoordsApRect) == false) {
			Log.e(TAG, "Unable to get clipping coords in onWindowFocusChanged()!");
			return;
		}

		// Calculate the full rect
		mFullRelativeApRect = new Rect(mOrigRelativeCoordsApRect);
		mFullRelativeApRect.top = mFullRelativeApRect.bottom - mFullHeightAp;
		mFullRelativeApRect.left -= (int) ((float)ADDITIONAL_WIDTH_LEFT_DP * mPixelDensity);
		mFullRelativeApRect.right += (int) ((float)ADDITIONAL_WIDTH_RIGHT_DP * mPixelDensity);

		// test to make sure I did the calculations right
		// todo: figure out the rounding errors that happen here
//		if (mFullRelativeApRect.width() != mFullWidthAp) {
//			Log.e(TAG, "widths don't match in onWindowFocusChanged!");
//			// todo: exit more gracefully
//			((Activity)getContext()).finish();
//		}

		// calculate option menu areas
		calcLeftOptionRect(mOrigRelativeCoordsApRect, mLeftOptionRelativeApRect);
		calcRightOptionRect(mOrigRelativeCoordsApRect, mRightOptionRelativeApRect);
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
	 * Calculates the location of the left options menu rect based on the
	 * given rect.
	 *
	 * @param startRect         The Rect to start with.
	 *
	 * @param leftOptionRect    Will be filled in with the correct values
	 *                          for the left options menu area for the given
	 *                          starting rect.
	 */
	private void calcLeftOptionRect(Rect startRect, Rect leftOptionRect) {

		// The left option area is 8mm to the left and includes the left third
		// of the original rect.
		leftOptionRect.set(startRect);
		leftOptionRect.left -= getWidth();
		leftOptionRect.right -= getWidth() + (getWidth() / 3);
	}

	/**
	 * Calculates the location of the left options menu rect based on the
	 * given rect.
	 *
	 * @param startRect         The Rect to start with.
	 *
	 * @param rightOptionRect    Will be filled in with the correct values
	 *                          for the left options menu area for the given
	 *                          starting rect.
	 */
	private void calcRightOptionRect(Rect startRect, Rect rightOptionRect) {

		// The right option area is 8mm to the right and includes the right third
		// of the original rect.
		rightOptionRect.set(startRect);
		rightOptionRect.right += getWidth();
		rightOptionRect.left += getWidth() - (getWidth() / 3);
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
		return mLeftOptionRelativeApRect.contains(x, y);
	}

	/**
	 * Determines if the given coordinates are within the area designated
	 * as the right option area.
	 */
	private boolean isInRightActionArea(int x, int y) {
		return mRightOptionRelativeApRect.contains(x, y);
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
		canvas.drawRect(mLeftOptionRelativeApRect, mLeftPaint);
	}

	private void drawRightOption(Canvas canvas) {
		canvas.drawRect(mRightOptionRelativeApRect, mRightPaint);
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

		// first things first: increase the clip rect
		if (mFullRelativeApRect != null) {
			canvas.clipRect(mFullRelativeApRect, Region.Op.REPLACE);
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
			drawLeftOption(canvas, mLeftOptionRelativeApRect);
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
