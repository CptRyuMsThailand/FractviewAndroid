package at.searles.fractview;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

        LinkedList<List<Integer>> p = new LinkedList<List<Integer>>();

        int w = 0, h = 0;

        if(t instanceof Tree.Vec) {
            for(Tree arg : (Tree.Vec) t) {
                List<Integer> row = new LinkedList<Integer>();

                if(arg instanceof Tree.Vec) {
                    for(Tree item : (Tree.Vec) arg) {
                        if(item instanceof Value.Int) {
                            row.add(((Value.Int) item).value);
                        } else {
                            throw new CompileException("int was expected here");
                        }
                    }
                } else if(arg instanceof Value.Int) {
                    row.add(((Value.Int) arg).value);
                } else {
                    throw new CompileException("int was expected here");
                }

                if(row.isEmpty()) {
                    throw new CompileException("no empty row allowed in palette");
                }

                if(w < row.size()) w = row.size();


                p.add(row);
                h++;
            }
        } else if(t instanceof Value.Int) {
            w = h = 1;
            p.add(Collections.singletonList(((Value.Int) t).value));
        }

        // p now contains lists of lists, h and w contain width and height.
        int[][] argbs = new int[h][w];

        int y = 0;

        for(List<Integer> row : p) {
            int x = 0;
            while(x < w) {
                // we circle around if a row is incomplete.
                for(int rgb : row) {
                    argbs[y][x] = rgb;
                    x++;
                    if(x >= w) break;
                }
            }
            y++;
        }

        return new Palette(argbs);
    }

    public static void writePaletteToParcel(Palette p, Parcel parcel) {
        parcel.writeInt(p.width());
        parcel.writeInt(p.height());

        for (int y = 0; y < p.height(); ++y) {
            for (int x = 0; x < p.width(); ++x) {
                parcel.writeInt(p.argb(x, y));
            }
        }
    }

    public static Palette readPalette(Parcel in) {
        int w = in.readInt();
        int h = in.readInt();

        int data[][] = new int[h][w];

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                data[y][x] = in.readInt();
            }
        }

        return new Palette(data);
    }

    public static <A> Map<String, A> merge(Map<String, A> primary, Map<String, A> secondary) {
        Map<String, A> merged = new HashMap<>();

        merged.putAll(secondary);
        merged.putAll(primary);

        return merged;
    }


    public static class PaletteWrapper implements Parcelable {

        //public final String label;
        public final Palette p;

        PaletteWrapper(/*String label,*/ Palette p) {
            //this.label = label;
            this.p = p;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            //parcel.writeString(label);
            writePaletteToParcel(p, parcel);
        }

        public static final Creator<PaletteWrapper> CREATOR
                = new Creator<PaletteWrapper>() {
            public PaletteWrapper createFromParcel(Parcel in) {
                return new PaletteWrapper(in);
            }

            public PaletteWrapper[] newArray(int size) {
                return new PaletteWrapper[size];
            }
        };

        /**
         * Now, writeParcel in reverse
         * @param parcel The palette in a parcel
         */
        private PaletteWrapper(Parcel parcel) {
            //label = parcel.readString();
            p = readPalette(parcel);
        }
    }
}
