package acr.browser.lightning.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

public class DrawableUtils {

    public static Bitmap getRoundedNumberImage(int number, int width, int height, int color, int thickness) {
        String text;

        if (number > 99) {
            text = "\u221E";
        } else {
            text = String.valueOf(number);
        }

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(Utils.dpToPx(14));
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        int radius = Utils.dpToPx(2);

        RectF outer = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRoundRect(outer, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        radius--;
        RectF inner = new RectF(thickness, thickness, canvas.getWidth() - thickness, canvas.getHeight() - thickness);
        canvas.drawRoundRect(inner, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

        canvas.drawText(String.valueOf(text), xPos, yPos, paint);

        return image;
    }

}
