package at.searles.fractview.main;

import android.os.Handler;

/**
 * Class used for updating the view on a regular basis
 */
class DrawerProgressTask implements Runnable {

    private static final long PROGRESS_UPDATE_MILLIS = 500; // TODO move to res. update the progress bar every ... ms.

    private final Handler handler;

    private CalculatorWrapper parent;
    private boolean destroyed = false;

    DrawerProgressTask(CalculatorWrapper parent) {
        this.parent = parent;
        this.handler = new Handler();
    }

    @Override
    public void run() {
        if(destroyed) {
            return;
        }

        parent.updateProgress();
        if(parent.isRunning()) {
            handler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
        }
    }

    public void destroy() {
        destroyed = true;
    }
}
