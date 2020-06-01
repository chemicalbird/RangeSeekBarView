package com.android.rangeview;

import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;

public class ViewHelper {

    public static int revertGravity(int gravity) {
        int rightGravity;
        int verticalG = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int horizontalG = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (horizontalG == Gravity.RIGHT) {
            rightGravity = verticalG | Gravity.LEFT;
        } else if (horizontalG == Gravity.LEFT) {
            rightGravity = verticalG | Gravity.RIGHT;
        } else {
            rightGravity = gravity;
        }

        return rightGravity;
    }

    public static int getDefiningColor(Drawable drawable) {
        if (drawable instanceof GradientDrawable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ColorStateList colorList = ((GradientDrawable) drawable).getColor();
                if (colorList != null) {
                    return colorList.getDefaultColor();
                }
            }
        } else if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable)drawable).getColor();
        }

        return -1;
    }
}
