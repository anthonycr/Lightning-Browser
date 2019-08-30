package acr.browser.lightning.utils;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import acr.browser.lightning.R;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class DrawableUtils {

    private DrawableUtils() {}

    /**
     * Creates a white rounded drawable with an inset image of a different color.
     *
     * @param context     the context needed to work with resources.
     * @param drawableRes the drawable to inset on the rounded drawable.
     * @return a bitmap with the desired content.
     */
    @NonNull
    public static Bitmap createImageInsetInRoundedSquare(Context context,
                                                         @DrawableRes int drawableRes) {
        final Bitmap icon = ThemeUtils.getBitmapFromVectorDrawable(context, drawableRes);

        final Bitmap image = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(image);
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        final int radius = Utils.dpToPx(2);

        final RectF outer = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRoundRect(outer, radius, radius, paint);

        final Rect dest = new Rect(Math.round(outer.left + radius), Math.round(outer.top + radius), Math.round(outer.right - radius), Math.round(outer.bottom - radius));
        canvas.drawBitmap(icon, null, dest, paint);

        return image;
    }

    /**
     * Creates a rounded square of a certain color with
     * a character imprinted in white on it.
     *
     * @param character the character to write on the image.
     * @param width     the width of the final image.
     * @param height    the height of the final image.
     * @param color     the background color of the rounded square.
     * @return a valid bitmap of a rounded square with a character on it.
     */
    @NonNull
    public static Bitmap createRoundedLetterImage(@NonNull Character character,
                                                  int width,
                                                  int height,
                                                  int color) {
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setColor(color);
        Typeface boldText = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        paint.setTypeface(boldText);
        paint.setTextSize(Utils.dpToPx(14));
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        int radius = Utils.dpToPx(2);

        RectF outer = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRoundRect(outer, radius, radius, paint);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

        paint.setColor(Color.WHITE);
        canvas.drawText(character.toString(), xPos, yPos, paint);

        return image;
    }

    /**
     * Hashes a character to one of four colors:
     * blue, green, red, or orange.
     *
     * @param character the character to hash.
     * @param app       the application needed to get the color.
     * @return one of the above colors, or black something goes wrong.
     */
    @ColorInt
    public static int characterToColorHash(@NonNull Character character, @NonNull Application app) {
        int smallHash = Character.getNumericValue(character) % 4;
        switch (Math.abs(smallHash)) {
            case 0:
                return ContextCompat.getColor(app, R.color.bookmark_default_blue);
            case 1:
                return ContextCompat.getColor(app, R.color.bookmark_default_green);
            case 2:
                return ContextCompat.getColor(app, R.color.bookmark_default_red);
            case 3:
                return ContextCompat.getColor(app, R.color.bookmark_default_orange);
            default:
                return Color.BLACK;
        }
    }


    public static int mixColor(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24 |
            (startR + (int) (fraction * (endR - startR))) << 16 |
            (startG + (int) (fraction * (endG - startG))) << 8 |
            (startB + (int) (fraction * (endB - startB)));
    }

}
