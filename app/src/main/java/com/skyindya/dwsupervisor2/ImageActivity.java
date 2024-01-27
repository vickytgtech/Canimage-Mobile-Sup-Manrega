package com.skyindya.dwsupervisor2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageActivity extends AppCompatActivity {

    ImageView bigImage;
    PhotoViewAttacher photoViewAttacher ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        bigImage = (ImageView) findViewById(R.id.bigImage);

        int val = getIntent().getIntExtra("image",1);

        if(val == 1)
        {
            bigImage.setImageDrawable(getResources().getDrawable(R.drawable.seeplansup));
        }
        else if(val == 2)
        {
            bigImage.setImageDrawable(getResources().getDrawable(R.drawable.postreccaplan));
        }
        else
        {
            bigImage.setImageDrawable(getResources().getDrawable(R.drawable.executepostrecca));
        }

        photoViewAttacher = new PhotoViewAttacher(bigImage);

        photoViewAttacher.update();

    }
}
