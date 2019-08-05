package com.sleepfuriously.slidemenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import org.w3c.dom.Attr;

/**
 * This is my 2nd try at making this custom widget.  I'm overriding
 * RelativeLayout and making it a compound view instead of a custom
 * widget.
 *
 * Why? because it'll be easier to do animations this way.  Or so I'm thinking.
 */
public class SlideButton extends RelativeLayout {

    //----------------------
    //  constants
    //----------------------

    private static final String TAG = SlideButton.class.getSimpleName();


    //----------------------
    //  data
    //----------------------

    private Context mCtx;

    //----------------------
    //  methods
    //----------------------

    public SlideButton(Context context) {
        super(context);
        init (context, null);
    }

    public SlideButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init (context, attrs);
    }

    public SlideButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init (context, attrs);
    }


    private void init(Context ctx, AttributeSet attrs) {

        mCtx = ctx;
        parseAttrs(attrs);

        // not sure how this is different from from just simply doing inflate(this, layout, ...)
//        LayoutInflater.from(mCtx).inflate(R.layout.landing_zone, this, true);
        inflate(mCtx, R.layout.landing_zone, this);

        // todo: get individual widgets
    }


    private void parseAttrs(AttributeSet attrs) {
        // todo
    }



}
