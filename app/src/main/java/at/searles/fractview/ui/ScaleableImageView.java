package at.searles.fractview.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import at.searles.fractview.BitmapFragment;
import at.searles.fractview.MultiTouchController;
import at.searles.math.Scale;

public class ScaleableImageView extends ImageView {

	/**
	 * Scale factor on double tapping
	 */
	public static final float SCALE_ON_DOUBLE_TAB = 3f;

	public static final float LEFT_UP_INDICATOR_LENGTH = 40f;

	/**
	 * The grid is painted from two kinds of lines. These are the paints
	 */
	private static final Paint[] GRID_PAINTS = new Paint[]{new Paint(), new Paint(), new Paint()};
	private static final Paint BOUNDS_PAINT = new Paint();
	private static final Paint TEXT_PAINT = new Paint(); // for error messages

	static {
		GRID_PAINTS[0].setColor(0xffffffff);
		GRID_PAINTS[0].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[0].setStrokeWidth(5f);

		GRID_PAINTS[1].setColor(0xff000000);
		GRID_PAINTS[1].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[1].setStrokeWidth(3f);

		GRID_PAINTS[2].setColor(0xffffffff);
		GRID_PAINTS[2].setStyle(Paint.Style.STROKE);
		GRID_PAINTS[2].setStrokeWidth(1f);

		BOUNDS_PAINT.setColor(0xaa000000); // semi-transparent black
		BOUNDS_PAINT.setStyle(Paint.Style.FILL_AND_STROKE);

		TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
		TEXT_PAINT.setTextSize(96); // fixme hardcoded...
	}


	/**
	 * To save the state of this view over rotation etc...
	 */
	static class ViewState extends BaseSavedState {

		/**
		 * Should the grid be shown or not?
		 */
		boolean showGrid = false;
		boolean rotationLock = false;
		boolean confirmZoom = false;
		boolean deactivateZoom = false;

		ViewState(Parcelable in) {
			super(in);
		}

		private ViewState(Parcel in) {
			super(in);
			this.showGrid = in.readInt() == 1;
			this.rotationLock = in.readInt() == 1;
			this.confirmZoom = in.readInt() == 1;
			this.deactivateZoom = in.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);

			dest.writeInt(showGrid ? 1 : 0);
			dest.writeInt(rotationLock ? 1 : 0);
			dest.writeInt(confirmZoom ? 1 : 0);
			dest.writeInt(deactivateZoom ? 1 : 0);
		}

		public static final Creator<ViewState> CREATOR = new Creator<ViewState>() {
			@Override
			public ViewState createFromParcel(Parcel in) {
				return new ViewState(in);
			}

			@Override
			public ViewState[] newArray(int size) {
				return new ViewState[size];
			}
		};
	}

	private boolean showGrid; // FIXME shouldn't I rather have one state object?
	private boolean rotationLock;
	private boolean confirmZoom;
	private boolean deactivateZoom;

	// Here, we also have some gesture control
	// Scroll-Events are handled as multitouch scale-events
	// double tab zooms at tabbed position

	// FIXME: Change structure so that bitmap Fragment is not needed.
	// FIXME: bitmap fragment is needed for the following reasons:
	// FIXME: first, matrices for view transformations
	// FIXME: second, setting Scale
	// FIXME: and third, checking whether it is still running.
	// FIXME: and 4: setting scale.

	// Reason 3: Why do I need to know whether it is still running?

	// === The following fields are not preserved over rotation ===
	private BitmapFragment bitmapFragment;

	private Matrix view2bitmap = new Matrix();
	private Matrix bitmap2view = new Matrix();

	/**
	 * We use this one to store the last transformation
	 * to also apply it to the picture if it was not updated yet.
	 */
	private LinkedList<Matrix> lastScale = new LinkedList<>();

	/**
	 * To detect gestures (3 finger drag etc...)
	 */
	private GestureDetector detector;

	/**
	 * Multitouch object that always reacts to finger input
	 */
	private MultiTouch multitouch;

	public ScaleableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTouch();
	}

	/**
	 * Toggle show-grid flag
	 * @param showGrid if true, the grid will be shown.
     */
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
		invalidate();
	}

	public boolean getShowGrid() {
		return showGrid;
	}

	public void setRotationLock(boolean rotationLock) {
		this.rotationLock = rotationLock;
		// does not change the view.
	}

	public boolean getRotationLock() {
		return rotationLock;
	}

	public void setConfirmZoom(boolean confirmZoom) {
		this.confirmZoom = confirmZoom;
	}

	public boolean getConfirmZoom() {
		return confirmZoom;
	}

	public void setDeactivateZoom(boolean deactivateZoom) {
		multitouch.cancel();
		this.deactivateZoom = deactivateZoom;
	}

	public boolean getDeactivateZoom() {
		return deactivateZoom;
	}

	public boolean backButtonAction() {
		// cancel selection if it exists
		if(multitouch != null && multitouch.controller != null) {
			multitouch.cancel();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		//begin boilerplate code that allows parent classes to save state
		Parcelable superState = super.onSaveInstanceState();

		ViewState vs = new ViewState(superState);
		vs.showGrid = this.showGrid;
		vs.rotationLock = this.rotationLock;
		vs.confirmZoom = this.confirmZoom;
		vs.deactivateZoom = this.deactivateZoom;

		return vs;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		//begin boilerplate code so parent classes can restore state
		if(!(state instanceof ViewState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		ViewState vs = (ViewState)state;
		super.onRestoreInstanceState(vs.getSuperState());
		//end

		setShowGrid(vs.showGrid);
		setRotationLock(vs.rotationLock);
		setConfirmZoom(vs.confirmZoom);
		setDeactivateZoom(vs.deactivateZoom);
	}

	/**
	 * If the view measurements are the ones in the arguments, what would be the width?
	 * In case the image is flipped then this one returns the scaled height.
	 * @param viewWidth
	 * @param viewHeight
     * @return
     */
	private float scaledBitmapWidth(float viewWidth, float viewHeight) {
		if(bitmapFragment == null) return viewWidth;

		float bitmapWidth = bitmapFragment.width(); // fixme: bitmapFragment requires drawer in the background.
		float bitmapHeight = bitmapFragment.height();

		/* Just some thoughts:
		The scaled rectangle of the bitmap should fit into the view.
		So, the ratio is the min-ratio.
		 */

		if(flipBitmap(viewWidth, viewHeight)) {
			float ratio = Math.min(viewWidth / bitmapHeight, viewHeight / bitmapWidth);
			return bitmapHeight * ratio;
		} else {
			float ratio = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
			return bitmapWidth * ratio;
		}
	}

	private float scaledBitmapHeight(float viewWidth, float viewHeight) {
		if(bitmapFragment == null) return viewHeight;

		float bitmapWidth = bitmapFragment.width(); // fixme: bitmapFragment requires drawer in the background.
		float bitmapHeight = bitmapFragment.height();

		if(flipBitmap(viewWidth, viewHeight)) {
			float ratio = Math.min(viewWidth / bitmapHeight, viewHeight / bitmapWidth);
			return bitmapWidth * ratio;
		} else {
			float ratio = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
			return bitmapHeight * ratio;
		}
	}

	/**
	 * Should the bitmap be rotated by 90 degrees to maximize
	 * the filled fractal area?
	 * @param viewWidth
	 * @param viewHeight
     * @return
     */
	private boolean flipBitmap(float viewWidth, float viewHeight) {
		if(bitmapFragment == null) return false;

		float bitmapWidth = bitmapFragment.width(); // fixme: bitmapFragment requires drawer in the background.
		float bitmapHeight = bitmapFragment.height();

		// maximize filled area
		if(bitmapWidth > bitmapHeight) {
			return viewWidth < viewHeight;
		} else {
			return bitmapWidth < bitmapHeight && viewWidth > viewHeight;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d("SIV", "onMeasure called now");

		int width;
		int height;

		float vw = MeasureSpec.getSize(widthMeasureSpec);
		float vh = MeasureSpec.getSize(heightMeasureSpec);

		if(bitmapFragment == null || bitmapFragment.getBitmap() == null) {
			setMeasuredDimension((int) vw, (int) vh);
			return;
		}

		float bw = bitmapFragment.width(); // fixme: bitmapFragment requires drawer in the background.
		float bh = bitmapFragment.height();

		if(vw > vh) {
			// if width of view is bigger, match longer side to it
			RectF viewRect = new RectF(0f, 0f, vw, vh);
			RectF bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
			bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);
		} else {
			RectF viewRect = new RectF(0f, 0f, vw, vh);
			RectF bitmapRect = new RectF(0f, 0f, Math.min(bw, bh), Math.max(bw, bh));
			bitmap2view.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);
		}

		// Check orientation
		if(flipBitmap(vw, vh)) {
			// fixme create this one directly
			// Turn centerImageMatrix by 90 degrees
			Matrix m = new Matrix();
			m.postRotate(90f);
			m.postTranslate(bh, 0);

			bitmap2view.preConcat(m);
		}

		bitmap2view.invert(view2bitmap);

		setImageMatrix(multitouch.viewMatrix());

		width = (int) vw;
		height = (int) vh;

		Log.d("SIV", "dimensions are " + width + " x " + height + ", matrices are " + bitmap2view + ", " + view2bitmap);

		//MUST CALL THIS
		setMeasuredDimension(width, height);
	}

	/**
	 * The following method should be called whenever the imageview changes its size
	 * or the bitmap is changed.
	 */
	/*public void initMatrices() {
		// Set initMatrix + inverse to current view-size
		float vw = getWidth();
		float vh = getHeight();

		// Right after creation, this view might not
		// be inside anything and therefore have size 0.
		if(vw <= 0 && vh <= 0) return; // do nothing.

		// fixme put into scale.
		float bw = bitmapFragment.width();
		float bh = bitmapFragment.height();

		RectF viewRect = new RectF(0f, 0f, vw, vh);
		RectF bitmapRect;

		if(vw > vh) {
			// if width of view is bigger, match longer side to it
			bitmapRect = new RectF(0f, 0f, Math.max(bw, bh), Math.min(bw, bh));
		} else {
			bitmapRect = new RectF(0f, 0f, Math.min(bw, bh), Math.max(bw, bh));
		}

		defaultMatrix.setRectToRect(bitmapRect, viewRect, Matrix.ScaleToFit.CENTER);

		// Check orientation
		if(vw > vh ^ bw > bh) {
			// Turn centerImageMatrix by 90 degrees
			Matrix m = new Matrix();
			m.postRotate(90f);
			m.postTranslate(bh, 0);

			defaultMatrix.preConcat(m);
		}

		currentImageMatrix = defaultMatrix;

		setImageMatrix(defaultMatrix);
	}*/

	private boolean cannotDrawImage = false;

	@Override
	public void invalidate() {
		cannotDrawImage = false;
		super.invalidate();
	}

	@Override
	public void onDraw(@NotNull Canvas canvas) {
		/*if(bitmapFragment != null && getDrawable() != null) {
			// fixme this is ugly...
			BitmapDrawable bd = (BitmapDrawable) getDrawable();
			if(bd.getBitmap() == null) setImageBitmap(bitmapFragment.getBitmap());
		}*/

		// draw image
		if(!cannotDrawImage) {
			try {
				super.onDraw(canvas);
			} catch (RuntimeException ex) {
				// The image might be too large to be drawn.
				Toast.makeText(getContext(), "Image cannot be shown (but it is still rendered!): " + ex.getMessage(), Toast.LENGTH_LONG).show();

				this.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));

				cannotDrawImage = true;
				deactivateZoom = true;
			}
		} else {
			return;
		}

		// remove bounds
		float w = getWidth(), h = getHeight();

		float bw = scaledBitmapWidth(getWidth(), getHeight());
		float bh = scaledBitmapHeight(getWidth(), getHeight());

		float cx = w / 2.f;
		float cy = h / 2.f;

		if(showGrid) {
			float minlen = Math.min(bw, bh) / 2.f;

			for (int i = 0; i < GRID_PAINTS.length; ++i) {
				Paint gridPaint = GRID_PAINTS[i];

				// outside grid
				canvas.drawLine(0, cy - minlen, w, cy - minlen, gridPaint);
				canvas.drawLine(0, cy + minlen, w, cy + minlen, gridPaint);
				canvas.drawLine(cx - minlen, 0, cx - minlen, h, gridPaint);
				canvas.drawLine(cx + minlen, 0, cx + minlen, h, gridPaint);

				// inside cross
				canvas.drawLine(0, h / 2.f, w, h / 2.f, gridPaint);
				canvas.drawLine(w / 2.f, 0, w / 2.f, h, gridPaint);

				// and a circle inside
				canvas.drawCircle(w / 2.f, h / 2.f, minlen, gridPaint);

				// and also draw quaters with thinner lines
				if(i != 0) {
					canvas.drawLine(0, cy - minlen / 2.f, w, cy - minlen / 2.f, gridPaint);
					canvas.drawLine(0, cy + minlen / 2.f, w, cy + minlen / 2.f, gridPaint);
					canvas.drawLine(cx - minlen / 2.f, 0, cx - minlen / 2.f, h, gridPaint);
					canvas.drawLine(cx + minlen / 2.f, 0, cx + minlen / 2.f, h, gridPaint);

				}
			}
		}

		// draw in total 4 transparent rectangles to indicate the drawing area
		canvas.drawRect(-1, -1, w, cy - bh / 2.f, BOUNDS_PAINT); // top
		canvas.drawRect(-1, -1, cx - bw / 2.f, h, BOUNDS_PAINT); // left
		canvas.drawRect(-1, cy + bh / 2.f, w, h, BOUNDS_PAINT);  // bottom
		canvas.drawRect(cx + bw / 2.f, -1, w, h, BOUNDS_PAINT);  // right

		if(flipBitmap(w, h)) {
			// draw an indicator in the left upper corner of the bitmap
			// which is in this case
			for (int i = 0; i < GRID_PAINTS.length; ++i) {
				Paint gridPaint = GRID_PAINTS[i];

				// three lines in total
				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
						gridPaint
				);

				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						gridPaint
				);

				canvas.drawLine(
						w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
						w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
						gridPaint
				);
			}
		}
	}

	public void setBitmapFragment(BitmapFragment bitmapFragment) {
		this.bitmapFragment = bitmapFragment;
	}

	/*public void updateBitmap() {
		setImageBitmap(bitmapFragment.getBitmap());
		this.requestLayout(); // size might have changed. Force it to relayout
	}*/



	/*@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}*/

	/**
	 * Called when the bitmap is updated according to the last transformation or
	 * when a post-edit happend that did not restart the drawing. This is necessary
	 * because I might be scaling the image while it is modified according to a
	 * pending change.
	 */
	public void removeLastScale() {
		Log.d("SIV", "remove last scale");
		if(!lastScale.isEmpty()) {
			Matrix l = lastScale.removeLast();
			// update the viewMatrix.
			setImageMatrix(multitouch.viewMatrix());
			invalidate();
		}
	}

	/**
	 * Called when a scale is commited
	 */
	public void combineLastScales() {
		if(lastScale.size() >= 2) {
			// more than or eq two, because one scale is currently waiting for
			// its preview to be finished (which is then handled by removeLastScale)
			Matrix l1 = lastScale.removeLast();
			Matrix l0 = lastScale.removeLast();

			l0.preConcat(l1);

			lastScale.addLast(l0);
		}
	}

	/**
	 * Initialize objects used for multitouch and other events.
	 */
	void initTouch() {
		this.detector = new GestureDetector(getContext(), gestureListener);
		this.multitouch = new MultiTouch();

		// and I need a touchevent
	}

	@Override
	public boolean onTouchEvent(@NotNull MotionEvent event) {
		// gesture detector handles scroll
		// no action without bitmap fragment.
		// or if deactivateZoom is set.
		if(bitmapFragment == null || deactivateZoom) {
			return false;
		}

		boolean ret = false;

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_CANCEL: multitouch.cancel(); ret = true; break;
			case MotionEvent.ACTION_DOWN: multitouch.initDown(event); ret = true; break;
			case MotionEvent.ACTION_POINTER_DOWN: multitouch.down(event); ret = true; break;
			case MotionEvent.ACTION_POINTER_UP: multitouch.up(event); ret = true; break;
			case MotionEvent.ACTION_UP: multitouch.finalUp(event); ret = true; break;
		}

		ret |= detector.onTouchEvent(event);

		return ret || super.onTouchEvent(event);
	}

	/**
	 * Map point from view coordinates to norm
	 * @param p Immutable point to modify
	 * @return returns the mapped point.
	 */
	PointF norm(PointF p) {
		float[] pts = new float[]{p.x, p.y};

		view2bitmap.mapPoints(pts);
		bitmapFragment.bitmap2norm.mapPoints(pts);
		return new PointF(pts[0], pts[1]);
	}

	/** Inverse of norm
	 */
	void invNormAll(float[] src, float[] dst) {
		bitmapFragment.norm2bitmap.mapPoints(dst, src);
		bitmap2view.mapPoints(dst);
	}

	/**
	 * Transform the image using matrix n
	 * @param n
	 */
	void addScale(Matrix n) {
		Log.d("SIV", "Adding a new scale: " + n);
		// and use it for lastScale.
		lastScale.addFirst(n); // add at the end (?)

		// the inverted one will be applied to the current scale
		Matrix m = new Matrix();
		n.invert(m);

		float[] values = new float[9];
		m.getValues(values);

		final Scale sc = Scale.fromMatrix(values);

		// update scale and restart calculation.
		bitmapFragment.setScaleRelative(sc);

		// If there are multiple scales pending, then
		// we combine them into one. This is important for the preview.
		// In the preview-listener-method, the last scale is removed anyways.
		combineLastScales();
	}

	final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onDown(MotionEvent motionEvent) {
			// must be true, otherwise no touch events.
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			if(confirmZoom) {
				// not active when 'confirm zoom' is set.
				return false;
			}

			if(multitouch.isScrollEvent) {
				multitouch.cancel();
			}

			int index = event.getActionIndex();

			final PointF p = norm(new PointF(event.getX(index), event.getY(index)));

			Matrix m = new Matrix();
			m.setValues(new float[]{
					SCALE_ON_DOUBLE_TAB, 0, p.x * (1 - SCALE_ON_DOUBLE_TAB),
					0, SCALE_ON_DOUBLE_TAB, p.y * (1 - SCALE_ON_DOUBLE_TAB),
					0, 0, 1
			});

			addScale(m);

			return true;
		}

		/*@Override
		public void onShowPress(MotionEvent motionEvent) {

		}*/

		@Override
		public boolean onSingleTapUp(MotionEvent motionEvent) {
			if(confirmZoom && multitouch.controller != null) {
				multitouch.confirm();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean onScroll(MotionEvent startEvt, MotionEvent currentEvt, float vx, float vy) {
			multitouch.scroll(currentEvt);
			return true;
		}

		/*@Override
		public void onLongPress(MotionEvent motionEvent) {

		}

		@Override
		public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
			return false;
		}*/
	};


	class MultiTouch {
		/**
		 * Controller for the image. It is possible to use only one, I guess,
		 * but scaling and everything is rather difficult.
		 */
		MultiTouchController controller = null;

		boolean isScrollEvent = false;

		/**
		 * returns the current view-matrix
		 * @return
		 */
		Matrix viewMatrix() {
			Matrix m;

			if(isScrollEvent) {
				// fixme avoid creating a new matrix all the time
				m = controller.getMatrix();
			} else {
				// fixme avoid creating a new matrix all the time
				m = new Matrix();
			}

			for(Matrix l : lastScale) {
				m.preConcat(l);
			}

			m.preConcat(bitmapFragment.bitmap2norm);
			m.postConcat(bitmapFragment.norm2bitmap);

			m.postConcat(bitmap2view);

			return m;
		}

		/**
		 * Cancel entire action
		 */
		void cancel() {
			controller = null;
			isScrollEvent = false;
			setImageMatrix(viewMatrix());
		}

		/**
		 * Call this on the first finger-down.
		 * @param event
		 */
		void initDown(MotionEvent event) {
			if(controller == null) {
				controller = new MultiTouchController(rotationLock);
			} else if(!confirmZoom) {
				throw new IllegalArgumentException("");
			}

			// do not display the point-dragging view while dragging...
			// fixme interactiveView.setVisibility(View.INVISIBLE);
			down(event); // scrollEvent might be false here.
		}

		void down(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			PointF p = new PointF(event.getX(index), event.getY(index));
			controller.addPoint(id, norm(p));
		}


		/**
		 *
		 * @param event
		 * @return
		 */
		void finalUp(MotionEvent event) {
			if(!isScrollEvent) {
				cancel();
				// no dragging here...
			} else {
				up(event);

				if (!confirmZoom) {
					confirm();
				}
			}
		}

		void up(MotionEvent event) {
			int index = event.getActionIndex();
			int id = event.getPointerId(index);

			controller.removePoint(id);
		}

		boolean confirm() {
			if(controller == null) {
				return false;
			}

			if(!controller.isDone()) {
				throw new IllegalStateException("action up but not done...");
			}

			// next for the preferences
			// convert it to a Scale

			// fetch controller-matrix
			Matrix n = controller.getMatrix();

			// and edit the bitmap fragment.
			addScale(n);

			// thanks for all that work, dear controller.
			isScrollEvent = false;
			controller = null;

			// the matrix of the image view will be updated
			// after we receive the first call to the update-method.

			// for now, we will set the latest view matrix.
			setImageMatrix(viewMatrix());

			return true;
		}

		void scroll(MotionEvent event) {
			isScrollEvent = true;

			for(int index = 0; index < event.getPointerCount(); ++index) {
				PointF pos = new PointF(event.getX(index), event.getY(index));
				int id = event.getPointerId(index);

				controller.movePoint(id, norm(pos));
			}

			/*
			Original:
			m = controller.getMatrix();
			m.postConcat(currentImageMatrix);
			// ie, imagematrix = bitmap2view * m
			 */

			ScaleableImageView.this.setImageMatrix(viewMatrix());
			ScaleableImageView.this.invalidate();
		}
	}
}
