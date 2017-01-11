package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * General purpose dialog fragment for alert dialogs.
 * In particular useful for all kinds of editing.
 *
 * This one is only created from the ParametersActivity and PaletteActivity.
 * Thus, for callbacks cast Activity to the correct one. Provide a unique identifier.
 */
public class MyAlertDialogFragment extends DialogFragment {
    // see https://developer.android.com/reference/android/app/DialogFragment.html


    public interface DialogHandler {
        /**
         * This method initializes the view. Useful to put values into
         * the view of a dialog.
         * @param id The id of the dialog
         * @param view The view in the dialog that should be initialized
         */
        void initDialogView(String id, View view);

        /**
         * After positive button press, this method
         * @param id The id of the dialog
         * @param view The view that should be initialized
         * @return if false, the dialog is not closed (for instance to correct the input)
         */
        boolean applyDialogView(String id, View view);
    }


    public static MyAlertDialogFragment newInstance(String title, int layoutId, String id) {
        MyAlertDialogFragment frag = new MyAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("layout_id", layoutId);
        args.putString("id", id);
        frag.setArguments(args);
        return frag;
    }


    public void showDialog(Activity activity) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        Fragment prev = activity.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        show(ft, "dialog");
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        // Fetch data
        String title = getArguments().getString("title");
        int layoutId = getArguments().getInt("layout_id");
        String id = getArguments().getString("id");

        // create view
        View view = this.getActivity().getLayoutInflater().inflate(layoutId, null);

        // initialize the view
        ((DialogHandler) this.getActivity()).initDialogView(id, view);

        // finally create dialog
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel",
                        (source, whichButton) -> {
                            source.dismiss(); // fixme: Is this one needed?
                        }
                )
                .create();

        // special tweak for positive button so that the dialog does not close
        // if the handler does not return true
        dialog.setOnShowListener(source -> {
            Button button = ((AlertDialog) source).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(new View.OnClickListener() {
                // FIXME do lambdas embedded in lambdas work?
                @Override
                public void onClick(View button) {
                    if (((DialogHandler) getActivity())
                            .applyDialogView(id, view)) {
                        //Dismiss once everything is OK.
                        source.dismiss();
                    }

                    // otherwise do nothing (the handler should provide a sufficient message)
                }
            });
        });

        return dialog;
    }
}
