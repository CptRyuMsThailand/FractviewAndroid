package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 *
 */
public class FavoritesActivity extends Activity {
	// TODO: Copy collection to clipboard
	// TODO: Import collection from clipboard

	// fixme menus import from clipboard
	static final String[] options = {"Delete", "Copy To Clipboard"};

	List<FavoriteEntry> entries;
	SharedPreferences persistent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

		// read bookmark-map from shared preferences
		// fixme what do the other modes do?
		persistent = getSharedPreferences("favorites", Context.MODE_PRIVATE);

		/*manageOldEntries();*/

		for(Map.Entry<String, ?> entry : persistent.getAll().entrySet()) {
			try {
				entries.add(FavoriteEntry.fromJSON(entry.getKey(), new JSONObject((String) entry.getValue())));
			} catch (JSONException e) {
				e.printStackTrace();
				// FIXME Error message in this case
			}
		}

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		final FavoritesAdapter adapter = new FavoritesAdapter(this, entries);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// get bookmark
				FavoriteEntry fav = entries.get(index);

				Intent data = new Intent();
				data.putExtra("fractal", fav.fractal());
				setResult(1, data);
				finish();
			}
		});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, final View view, final int index, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(FavoritesActivity.this);

				builder.setTitle("Choose an option");
				builder.setItems(options, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavoriteEntry entry = entries.get(index);

						switch (which) {
							case 0: {
								// delete fav
								// first from internal DSs
								entries.remove(index);

								// then from shared prefs
								SharedPreferences.Editor editor = persistent.edit();
								editor.remove(entry.title());
								editor.apply();

								// and update view.
								adapter.notifyDataSetChanged(); // tell that sth changed.
							}
							break;
							case 1: {
								// copy to clipboard
								ClipboardHelper.copyFractal(view.getContext(), entry.fractal());
							}
							break;
						}
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// just dismiss it.
						dialogInterface.dismiss();
					}
				});

				builder.show();

				return true;
			}
		});

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				FavoritesActivity.this.finish();
			}
		});
	}

	// FIXME Remove after some time
	/*@Deprecated
	private void manageOldEntries() {
		// show menu
		// old bookmark entries found
		// copy to clipboard
		// delete them

		CharSequence options[] = new CharSequence[] {
				"Delete old bookmarks",
				"Copy old bookmarks to clipboard"};

		SharedPreferences oldPreferences = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);

		final Map<String, ?> oldEntries = oldPreferences.getAll();

		if(oldEntries.isEmpty()) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Unfortunately old bookmarks cannot be supported");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// the user clicked on colors[which]
				if(which == 0) {
					SharedPreferences.Editor editor = getSharedPreferences("bookmarks", Context.MODE_PRIVATE).edit();
					editor.clear();
					editor.apply();
				} else if(which == 1) {
					StringBuilder sb = new StringBuilder();

					for(Map.ProgramAsset<String, ?> entry : oldEntries.entrySet()) {
						sb.append(entry.getKey()).append("\n");
						sb.append(String.valueOf(entry.getValue()));

						sb.append("\n\n");
					}

					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("fractview_old_bookmarks", sb.toString());
					clipboard.setPrimaryClip(clip);
				}
			}
		});

		builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				// Do nothing.
				dialogInterface.dismiss();
			}
		});

		builder.show();
	}*/

}