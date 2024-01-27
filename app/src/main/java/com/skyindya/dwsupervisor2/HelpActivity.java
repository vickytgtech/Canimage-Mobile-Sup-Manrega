package com.skyindya.dwsupervisor2;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

public class HelpActivity extends AppCompatActivity {

    ImageView step1image,step2image,step3image;
    Activity act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        try
        {
            androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ImageView imageView = new ImageView(actionBar.getThemedContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.actionlogo);
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT, Gravity.LEFT
                    | Gravity.CENTER_VERTICAL);
            layoutParams.rightMargin = 40;
            imageView.setLayoutParams(layoutParams);
            actionBar.setCustomView(imageView);

            act = this;
            step1image = (ImageView) findViewById(R.id.step1image);
            step2image = (ImageView) findViewById(R.id.step2image);
            step3image = (ImageView) findViewById(R.id.step3image);

            step1image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(act,ImageActivity.class);
                    i.putExtra("image",1);
                    startActivity(i);
                }
            });

            step2image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(act,ImageActivity.class);
                    i.putExtra("image",2);
                    startActivity(i);
                }
            });

            step3image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(act,ImageActivity.class);
                    i.putExtra("image",3);
                    startActivity(i);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
