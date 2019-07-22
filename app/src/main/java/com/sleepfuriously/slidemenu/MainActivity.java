package com.sleepfuriously.slidemenu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

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

		leftTv = findViewById(R.id.left_tv);
		rightTV = findViewById(R.id.right_tv);

		leftTv.setText(myMenu.getmLeftText());
		rightTV.setText(myMenu.getmRightText());

	}
}