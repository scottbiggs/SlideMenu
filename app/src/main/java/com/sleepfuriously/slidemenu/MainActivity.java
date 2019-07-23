package com.sleepfuriously.slidemenu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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
	SlideMenu myMenu;

	TextView leftTv, rightTV;

	//-----------------------
	//  methods
	//-----------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		myMenu = findViewById(R.id.slide_menu1);
		myMenu.setOnSlideMenuListener(this);

		leftTv = findViewById(R.id.left_tv);
		rightTV = findViewById(R.id.right_tv);

		leftTv.setText(myMenu.getLeftText());
		rightTV.setText(myMenu.getRightText());

	}

	@Override
	public void onSlideLeft() {
		Toast.makeText(this, "left", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSlideRight() {
		Toast.makeText(this, "right", Toast.LENGTH_SHORT).show();
	}
}
