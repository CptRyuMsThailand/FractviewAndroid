package at.searles.fractview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.meelan.Tree;
import at.searles.meelan.Value;

/**
 * Created by searles on 11.06.17.
 */

public class Commons {

    public static interface KeyAction {
        void apply(String key);
    }

    public static String timestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        return simpleDateFormat.format(new Date());
    }

    public static String fancyTimestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        return simpleDateFormat.format(new Date());
    }

    public static String duration(long ms) {
        StringBuilder sb = new StringBuilder();

        double seconds = ms / 1000.;

        long minutes = ms / 60000;

        if(minutes > 0) {
            seconds -= minutes * 60;
            sb.append(minutes + ":");
        }

        sb.append(String.format("%02.3f", seconds));

        return sb.toString();
    }


    /**
     * Matrices to convert coordinates into value that is
     * independent from the bitmap-size. Normized always
     * contains the square -1,-1 - 1-1 with 0,0 in the middle
     * but also keeps the ratio of the image.
     */
    public static Matrix bitmap2norm(int width, int height) {
        float m = Math.min(width, height);

        Matrix ret = new Matrix();

        ret.setValues(new float[]{
                2f / m, 0f, -width / m,
                0f, 2f / m, -height / m,
                0f, 0f, 1f
        });

        return ret;
    }

    public static float normX(float bitmapX, int width, int height) {
        float m = Math.min(width, height);
        return bitmapX * 2f / m - width / m;
    }

    public static float normY(float bitmapY, int width, int height) {
        float m = Math.min(width, height);
        return bitmapY * 2f / m - height / m;
    }

    /**
     * Inverse of bitmap2norm
     */
    public static Matrix norm2bitmap(int width, int height) {
        float m = Math.min(width, height);

        Matrix ret = new Matrix();

        ret.setValues(new float[]{
                m / 2f, 0f, width / 2f,
                0f, m / 2f, height / 2f,
                0f, 0f, 1f
        });

        return ret;
    }

    public static float bitmapX(float normX, int width, int height) {
        float m = Math.min(width, height);
        return normX * m / 2f + width / 2f;
    }

    public static float bitmapY(float normY, int width, int height) {
        float m = Math.min(width, height);
        return normY * m / 2f + height / 2f;
    }

    public static boolean isUI() {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }

    public static void uiRun(Runnable runnable) {
        if(isUI()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    public static Scale toScale(Tree init) throws CompileException {
        if(init instanceof Tree.Vec) {
            Tree.Vec vec = (Tree.Vec) init;

            if(vec.size() == 3) {
                double[] scale = new double[3];

                for(int i = 0; i < 3; ++i) {
                    Tree child = vec.get(i);

                    if(child instanceof Value.Int) {
                        scale[2 * i] = ((Value.Int) child).value;
                        scale[2 * i + 1] = 0;
                    } else if(child instanceof Value.Real) {
                        scale[2 * i] = ((Value.Real) child).value;
                        scale[2 * i + 1] = 0;
                    } else if(child instanceof Value.CplxVal) {
                        scale[2 * i] = ((Value.CplxVal) child).value.re();
                        scale[2 * i + 1] = ((Value.CplxVal) child).value.im();
                    } else {
                        throw new CompileException("Components of scale must be complex numbers");
                    }
                }

                return new Scale(scale);
            }
        }

        throw new CompileException("Scale must be vector of 3 numbers");
    }

    public static byte[] toPNG(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
        return byteArrayBitmapStream.toByteArray();
    }

    public static Bitmap fromPNG(byte[] data) {
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * Converts a term to a palette
     * @param t
     * @return
     * @throws CompileException
     */
    public static Palette toPalette(Object t) throws CompileException {
        if(t == null) {
            throw new CompileException("Missing list value");
        }

        int w, h;
        int colors[];

        if(t instanceof Tree.Vec) {
            Tree.Vec vec = (Tree.Vec) t;

            if(vec.size() > 0) {
                Tree element = vec.get(0);

                if(element instanceof Tree.Vec) {
                    // two dimensional palette.
                    h = vec.size();
                    w = ((Tree.Vec) element).size();

                    colors = new int[w * h];

                    int y = 0;

                    for(Tree row : vec) {
                        if(row instanceof Tree.Vec) {
                            if(((Tree.Vec) row).size() == w) {
                                for(int x = 0; x < w; ++x) {
                                    Tree color = ((Tree.Vec) row).get(x);

                                    if(color instanceof Value.Int) {
                                        colors[x + y * w] = ((Value.Int) color).value;
                                    } else {
                                        throw new CompileException("invalid entry in palette");
                                    }
                                }
                            }
                        } else {
                            throw new CompileException("invalid row in palette");
                        }

                        y++;
                    }
                } else if(element instanceof Value.Int) {
                    // just one row.
                    h = 1;
                    w = vec.size();

                    colors = new int[w];

                    for(int x = 0; x < w; ++x) {
                        Tree color = vec.get(x);

                        if(color instanceof Value.Int) {
                            colors[x] = ((Value.Int) color).value;
                        } else {
                            throw new CompileException("invalid entry in palette row");
                        }
                    }
                } else {
                    throw new CompileException("invalid row in palette");
                }
            } else {
                throw new CompileException("palette must not be empty");
            }
        } else if(t instanceof Value.Int) {
            w = h = 1;
            colors = new int[1];
            colors[0] = ((Value.Int) t).value;
        } else {
            throw new CompileException("invalid palette");
        }

        return new Palette(w, h, colors);
    }


    public static <A> Map<String, A> merge(Map<String, A> primary, Map<String, A> secondary) {
        Map<String, A> merged = new HashMap<>();

        merged.putAll(secondary);
        merged.putAll(primary);

        return merged;
    }



    /**
     * creates a new bitmap with size 64x64 containing the center of the current image
     * @return
     */
    public static Bitmap createIcon(Bitmap original, int iconSize) {
        // FIXME Move somewhere else!
        // create a square icon. Should  only contain central square.
        Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(icon);

        float scale = ((float) iconSize) / Math.min(original.getWidth(), original.getHeight());

        float w = original.getWidth();
        float h = original.getHeight();

        Matrix transformation = new Matrix();
        transformation.setValues(new float[]{
                scale, 0, (iconSize - scale * w) * .5f,
                0, scale, (iconSize - scale * h) * .5f,
                0, 0, 1,
        });

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(original, transformation, paint);

        return icon;
    }
}
