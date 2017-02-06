package at.searles.fractview;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import at.searles.fractview.fractal.Fractal;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * Created by searles on 24.01.17.
 */

public class AssetsHelper {
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);

    /**
     * Try to read content of assets-folder
     * @param am The asset manager that should be used
     * @param filename The filename to be read
     * @return The content of the file as a string, null in case of an error
     */
    public static String readSourcecode(AssetManager am, String filename) {
        BufferedReader reader = null;
        String program = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(am.open(filename)));

            StringBuilder sb = new StringBuilder();

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                sb.append(mLine).append("\n");
            }

            program = sb.toString();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    Log.e("PF", "close failed!");
                    e.printStackTrace();
                }
            }
        }

        return program;
    }

    /**
     * Reads an icon from assets
     * @param am Asset Manager to access it
     * @param iconFilename Filename of the icon.
     * @return null if there is no such file. The error message is logged
     */
    public static Bitmap readIcon(AssetManager am, String iconFilename) {
        if(iconFilename == null) return null;

        Bitmap icon = null;
        InputStream is = null;

        try {
            is = am.open(iconFilename);
            icon = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Could not read icon", Toast.LENGTH_LONG).show();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return icon;
    }

    // Create a list of assets and icons that come with it.
    // Read private entries
    public static ProgramAsset e(AssetManager am, String title, String iconFilename, String description, String sourceFilename) {
        String sourceCode = AssetsHelper.readSourcecode(am, sourceFilename);
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        if(sourceCode == null/* || icon == null*/) {
            throw new IllegalArgumentException("bad asset: " + title);
        }

        return new ProgramAsset(title, icon, description, sourceCode);
    }

    // And now for the presets.
    public static class ProgramAsset implements FractalEntry {
        public final String title;
        public final Bitmap icon;
        public final String description;
        public final String source;

        private ProgramAsset(String title, Bitmap icon, String description, String source) {
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.source = source;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public Bitmap icon() {
            return icon;
        }

        @Override
        public String description() {
            return description;
        }
    }

    private static ArrayList<ProgramAsset> _ENTRIES = null;

    public static synchronized ArrayList<ProgramAsset> entries(AssetManager am) {
        if (_ENTRIES == null) {
            // create entries.
            _ENTRIES = new ArrayList<>();

            _ENTRIES.add(e(am, "Default", "Default.png", "Basic fractal with bailout and lake coloring", "Default.fv"));
            _ENTRIES.add(e(am, "Lake", "Lake.png", "Draws only the lake of a fractal, thus useful for bounded fractals like Duck or Newton", "Lake.fv"));
            _ENTRIES.add(e(am, "Julia Map", "JuliaMap.png", "Variation of \"Default\" that shows a map of julia sets.", "JuliaMap.fv"));

            _ENTRIES.add(e(am, "Cczcpaczcp", "Cczcpaczcp.png", "Default with a built-in special formula by Mark R Eggleston, called Cczcpaczcp", "Cczcpaczcp.fv"));
            _ENTRIES.add(e(am, "Branching", "Branching.png", "\"Default\" with an addend for average coloring methods for polynom formulas", "Branching.fv"));
            _ENTRIES.add(e(am, "Fold", "Fold.png", "\"Default\" with a more general addend (fold), also suitable for stripe coloring methods of non-polynomial fractals", "Fold.fv"));
            _ENTRIES.add(e(am, "Two Folds", "TwoFold.png", "\"Default\" with two fold functions", "TwoFold.fv"));
            _ENTRIES.add(e(am, "Orbit Trap", "OrbitTrap.png", "\"Default\" with an orbit trap", "OrbitTrap.fv"));
            _ENTRIES.add(e(am, "Min/Max Trap", "MinMaxOrbitTrap.png", "Picks the maximum distance to the orbit trap", "MinMaxOrbitTrap.fv"));

            // _ENTRIES.add(e(am, "Newton", "Newton.png", "Newton method for root finding fractals", "Newton.fv"));
            _ENTRIES.add(e(am, "Secant", "Secant.png", "Secant method for root finding fractals", "Secant.fv"));

            _ENTRIES.add(e(am, "Lyapunov", "Lyapunov.png", "Lyapunov fractals", "Lyapunov.fv"));

            _ENTRIES.add(e(am, "Pendulum (3 Magnets)", "Pendulum3.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum3.fv"));

            _ENTRIES.add(e(am, "Complex Function", "ComplexFn.png", "Drawing of Complex function (Color Wheel method by default)", "ComplexFn.fv"));
        }

        return _ENTRIES;
    }

    // Create a list of assets and icons that come with it.
    // Read private entries
    private static ParametersAsset e(AssetManager am, String title, String iconFilename, String description, Fractal.Parameters parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        /*if(icon == null) {
            throw new IllegalArgumentException("bad asset: " + title);
        }*/

        return new ParametersAsset(title, icon, description, null, parameters);
    }

    public static ParametersAsset e(AssetManager am, String title, String iconFilename, String description, Scale scale, Fractal.Parameters parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        /*if(icon == null) {
            throw new IllegalArgumentException("bad asset: " + title);
        }*/

        return new ParametersAsset(title, icon, description, scale, parameters);
    }

    // And now for the presets.
    public static class ParametersAsset implements FractalEntry {
        public final String title;
        public final Bitmap icon;
        public final String description;
        public final Scale scale; // may be null
        public final Fractal.Parameters parameters;

        private ParametersAsset(String title, Bitmap icon, String description, Scale scale, Fractal.Parameters parameters) {
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.scale = scale;
            this.parameters = parameters;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public Bitmap icon() {
            return icon;
        }

        @Override
        public String description() {
            return description;
        }
    }

    private static ArrayList<ParametersAsset> _PARAMETER_ENTRIES = null;

    public static synchronized ArrayList<ParametersAsset> parameterEntries(AssetManager am) {
        if (_PARAMETER_ENTRIES == null) {
            // create entries.
            _PARAMETER_ENTRIES = new ArrayList<>();

            _PARAMETER_ENTRIES.add(e(am, "Wikipedia", "DefaultMBWiki.png", "Parameters from the Wikipedia - Mandelbrot - Entry",
                    new Fractal.Parameters()
                            .add("bailoutvalue", Fractal.Type.Expr, "i + smooth_i")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + value * (0.42 / 28))")
                            .add("laketransfer", Fractal.Type.Expr, "0")
                            .add("lakepalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000000}}))
                            .add("bailoutpalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}})))
            );

            _PARAMETER_ENTRIES.add(e(am, "Mandelbrot Set", "Mandelbrot.png", "Mandelbrot SetBurning Ship Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Burning Ship", "BurningShip.png", "Burning Ship Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(abs z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Celtic", "Celtic.png", "Celtic Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "rabs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Tricorn", "Tricorn.png", "Tricorn Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(conj z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Buffalo", "Buffalo.png", "Buffalo Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "abs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Mandel^3", "Mandel3.png", "Mandelbrot Set to the power of 3",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z^3 + p")
                            .add("max_power", Fractal.Type.Real, 3)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Mandel^4", "Mandel3.png", "Mandelbrot Set to the power of 3",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z^4 + p")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Lambda", "Lambda.png", "Lambda Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "p (1 - z) z")
                            .add("mandelinit", Fractal.Type.Expr, "0.5")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Phoenix Julia Set", "Phoenix.png", "Julia Set of the Phoenix Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p.x) + zlast p.y")
                            .add("mandelinit", Fractal.Type.Expr, "c")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(0.5666, -0.5))
            ));

            _PARAMETER_ENTRIES.add(e(am, "Glynn", "Glynn.png", "Glynn fractal (a julia set of mandel^1.6",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z ^ 1.75 + p")
                            .add("max_power", Fractal.Type.Real, 1.75)
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(-0.4))
            ));

            _PARAMETER_ENTRIES.add(e(am, "Magnet1", "Magnet1.png", "Magnet 1 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((sqr z + p - 1) / (2z + p - 2))")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Magnet2", "Magnet2.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((z^3 + 3(p - 1)z + (p - 1)(p - 2)) / (3 sqr z + 3(p - 2)z + (p - 1)(p - 2) + 1))")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "ZtanZ", "Magnet2.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z tan z + c")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Duck fractal", "Duck.png", "A nice variation for newton",
                    new Scale(2, 0, 0, 2, 0, -1.5),
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "log(iabs z + p)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Newton of z^3 + p", "newton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^3 + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Newton of sinh z + p", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(sinh z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Nova of z^6 - 1", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^6 - 1, z) + p")
                            .add("mandelinit", Fractal.Type.Expr, "1")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Nova z - a f(z)/f'(z) + p with a=2 and f=z^4 + p", "SinHNewton.png", "Mandelinit should be \"(p(a n-a)/(a-n))^/n\", provided p does not depend on z.",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z - 2 (z^4 + p) / derive(z^4 + p, z) + p")
                            .add("mandelinit", Fractal.Type.Expr, "{ def a=2; def n=4; def p=c; (p(a n-a)/(a-n))^/n}")
            ));

            // Stop Fractal Types.

            // Next: Branching Specialities

            _PARAMETER_ENTRIES.add(e(am, "Curvature Inequality", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("addend", Fractal.Type.Expr, "arcnorm((znext - z) / (z - zlast))")
                            //.add("interpolate_smooth_i", Fractal.Type.Bool, true)
            ));

            _PARAMETER_ENTRIES.add(e(am, "Triange Inequality", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("addend", Fractal.Type.Expr, "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /rad(z - znext)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "re foldvalue")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing p * cosh z", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "p * cosh z")
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("bailout", Fractal.Type.Real, 64)
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /rad(z - znext)) + foldvalue")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Branching", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Branching p * sinh z", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "p * sinh z")
                            .add("mandelinit", Fractal.Type.Expr, "I")
                            .add("bailout", Fractal.Type.Real, 64)
                            .add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Lake Branching", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue }")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Lake Branching Newton z^3 + p", "SinHNewton.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^3 + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
                            .add("foldfn", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue }")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Two-Fold Branching", "twobranch.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue2 }")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Two-Fold Branching Magnet 1", "twobranch.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((sqr z + p - 1) / (2z + p - 2))")
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue2 }")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Two-Fold Branching Magnet 2", "twobranch.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((z^3 + 3(p - 1)z + (p - 1)(p - 2)) / (3 sqr z + 3(p - 2)z + (p - 1)(p - 2) + 1))")
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue2 }")
            ));


            // Next: Orbit Traps

            _PARAMETER_ENTRIES.add(e(am, "Steiner Circles Trap", null, "Orbit Trap of 6 Steiner Circles",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")
            ));

            // Next Lyapunov Fractals

            _PARAMETER_ENTRIES.add(e(am, "Zirkony Zity", "ZirkonyZity.png", "Part of Lyapunov fractal called Zirkony Zity",
                    new Scale(0.45, 0, 0, -0.3, 3.05, 3.7),
                    new Fractal.Parameters()
                            .add("lyastring", Fractal.Type.Expr, "[a,a,a,a,a,a,b,b,b,b,b,b]")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Domain Coloring", "DomainColoring.png", "Domain Coloring for Complex Functions",
                    new Fractal.Parameters()
                            .add("transfer", Fractal.Type.Expr, "arcnorm z : (0.6 fract (log rad z / log 2) + 0.0667)")
            ));

            // TODO: Replace newton/secant by OnlyLake
            // TODO: Add solve2 and solve3
            // TODO: nova!
            // TODO: pendulum with arbitrary many magnets that all have the same arguments

        }

        return _PARAMETER_ENTRIES;
    }
}