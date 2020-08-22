package com.sirelon.marsroverphotos.utils.transformers;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

public class HorizontalFlipTransformation implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {

        page.setTranslationX(-position * page.getWidth());
        page.setCameraDistance(12000);

        if (position < 0.5 && position > -0.5) {
            page.setVisibility(View.VISIBLE);
        } else {
            page.setVisibility(View.INVISIBLE);
        }


        if (position < -1) {     // [-Infinity,-1)
            page.setAlpha(0);

        } else if (position <= 0) {    // [-1,0]
            page.setAlpha(1);
            page.setRotationY(180 * (1 - Math.abs(position) + 1));

        } else if (position <= 1) {    // (0,1]
            page.setAlpha(1);
            page.setRotationY(-180 * (1 - Math.abs(position) + 1));

        } else {
            page.setAlpha(0);
        }
    }
}