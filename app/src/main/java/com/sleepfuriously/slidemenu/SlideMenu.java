package com.sleepfuriously.slidemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;



public class SlideMenu extends AppCompatButton {

	//-------------------
	//  constants
	//-------------------

	private static final String TAG = SlideMenu.class.getSimpleName();

	//-------------------
	//  data
	//-------------------

	/** Strings to display in left & right bubbles */
	private String mLeftText, mRightText;


	//-------------------
	//  constructors
	//-------------------

	public SlideMenu(Context context) {
		super(context);
	}

	public SlideMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttrs(attrs);
	}

	public SlideMenu(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttrs(attrs);
	}


	//-------------------
	//  methods
	//-------------------

	private void parseAttrs(AttributeSet attrs) {
		TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.SlideMenu);
		mLeftText = array.getString(R.styleable.SlideMenu_left_text);
		mRightText = array.getString(R.styleable.SlideMenu_right_text);
		array.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = super.getWidth();
		int height = super.getHeight();

		// This widget will be a circle. So make the smaller equal to the larger.
		if (width < height) {
			width = height;
		}
		else {
			height = width;
		}
		// fulfill our contract!
		setMeasuredDimension(width, height);
	}

	//-------------------
	//  getters & setters
	//-------------------

	public String getmLeftText() {
		return mLeftText;
	}

	public void setmLeftText(String mLeftText) {
		this.mLeftText = mLeftText;
	}

	public String getmRightText() {
		return mRightText;
	}

	public void setmRightText(String mRightText) {
		this.mRightText = mRightText;
	}


}
