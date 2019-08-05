package com.sleepfuriously.slidemenu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
		implements SlideMenu.OnSlideMenuListener {

	//-----------------------
	//  constants
	//-----------------------

	//-----------------------
	//  data
	//-----------------------
//	SlideMenu myMenu;
	SlideButton myButton;

	TextView leftTv, rightTV;

	ImageView mCartoon;
	Animation mAnim;

	//-----------------------
	//  methods
	//-----------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		myMenu = findViewById(R.id.slide_menu1);
//		myMenu.setOnSlideMenuListener(this);

		myButton = findViewById(R.id.slide_button);

		leftTv = findViewById(R.id.left_tv);
		rightTV = findViewById(R.id.right_tv);

//		leftTv.setText(myMenu.getLeftText());
//		rightTV.setText(myMenu.getRightText());

		mCartoon = findViewById(R.id.cartoon_iv);
		mAnim = AnimationUtils.loadAnimation(this, R.anim.left_lz_anim);
	}

	@Override
	public void onSlideLeft() {
		Toast.makeText(this, "left", Toast.LENGTH_SHORT).show();

		mCartoon.startAnimation(mAnim);
	}

	@Override
	public void onSlideRight() {
		Toast.makeText(this, "right", Toast.LENGTH_SHORT).show();
	}
}
