package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import at.searles.fractview.editors.EditableDialogFragment;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.PresetFractals;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.utils.Pair;

public class ParameterActivity extends Activity implements EditableDialogFragment.Callback {

	// TODO: Load from sample
	// TODO: Load (like in palette)
	// TODO: Save (like in palette)

	public static final int PROGRAM_ACTIVITY_RETURN = 100;

	static final CharSequence[] scaleOptions = { "Reset to Default", "Center on Origin", "Orthogonalize", "Straighten" };
	static final CharSequence[] boolOptions = { "Reset to Default" };
	static final CharSequence[] intOptions = { "Reset to Default" };
	static final CharSequence[] realOptions = { "Reset to Default" };
	static final CharSequence[] cplxOptions = { "Reset to Default", "Set to Center" };
	static final CharSequence[] colorOptions = { "Reset to Default" };
	static final CharSequence[] exprOptions = { "Reset to Default" };
	static final CharSequence[] paletteOptions = { "Reset to Default" };

	static final int BOOL = 0;
	static final int ELEMENT = 1;
	static final int SCALE = 2;

	Fractal fb;
	ParameterAdapter adapter; // List adapter for parameters
	ListView listView;

	@Override
	public void apply(int resourceCode, Object o) {
		// TODO Should this method move into the adapter?
		if(o == null) {
			// There was an error in the input
			Toast.makeText(this, "ERROR: Bad input", Toast.LENGTH_LONG).show();
			return;
		}

		// The resourceCode is the position in the element list
		Pair<String, Fractal.Type> p = adapter.elements.get(resourceCode);

		switch(p.b) {
			case Scale: {
				Scale sc = (Scale) o;
				fb.setScale(sc);
			} break;
			case Int: {
				fb.setInt(p.a, (Integer) o);
			} break;
			case Real: {
				fb.setReal(p.a, (Double) o);
			} break;
			case Cplx: {
				fb.setCplx(p.a, (Cplx) o);
			} break;
			case Expr: {
				// This one is more complicated.
				// Compiling is one here and not in the dialog because I cannot simply
				// pass a Tree as a parcel in case I modify it accordinly.

				// store id in case of error.
				// If backup is null, then the original was used.
				String backup = fb.isDefaultValue(p.a) ? null : (String) fb.get(p.a).b;

				try {
					fb.setExpr(p.a, (String) o);
					fb.compile();

					// compiling was fine...
					adapter.notifyDataSetChanged();
				} catch(CompileException e) { // this includes parsing exceptions now
					Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

					// there was an error. Restore expr for id to original state
					if (backup == null) {
						// back to original
						fb.reset(p.a);
					} else {
						// back to old value
						fb.setExpr(p.a, backup);
					}

					// TODO Collect these. This is code duplication

					// and reopen dialog.
					EditableDialogFragment ft = EditableDialogFragment.newInstance(
							resourceCode, "Error in Expression!", false,
							EditableDialogFragment.Type.Name).setInitVal(o);

					ft.show(getFragmentManager(), "dialog");
				}
			} break;
			case Color: {
				fb.setColor(p.a, ((Integer) o) | 0xff000000);
			} break;
			default:
				// bool and palette is not her
				throw new IllegalArgumentException("No such type");
		}
	}

	/*@Override
	public boolean applyScale(Scale sc) {
		fb.setScale(sc);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyInt(int i) {
		fb.setInt(currentEditId, i);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyReal(double d) {
		fb.setReal(currentEditId, d);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyColor(int color) {
		fb.setColor(currentEditId, color);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyCplx(Cplx c) {
		Log.d("cplx edit callback: " , "re = " + c.re() + ", im = " + c.im());

		fb.setCplx(currentEditId, c);
		adapter.notifyDataSetChanged();
		return true;
	}

	@Override
	public boolean applyExpr(String data) {
		// store id in case of error
		String resetValue = fb.isDefaultValue(currentEditId) ? null : fb.expr(currentEditId);

		try {
			fb.setExpr(currentEditId, data);
			fb.compile();

			adapter.notifyDataSetChanged();
			return true;
		} catch(CompileException e) { // this includes parsing exceptions now
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

			if (resetValue == null) {
				fb.resetExpr(currentEditId);
			} else {
				fb.setExpr(currentEditId, resetValue);
			}

			return false;
		}
	}*/

	private void showOptionsDialog(CharSequence[] options, DialogInterface.OnClickListener listener) {
		// show these simple dialogs to reset or center values.
		AlertDialog.Builder builder = new AlertDialog.Builder(ParameterActivity.this);

		builder.setTitle("Select an Option");
		builder.setItems(options, listener);

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		});

		builder.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.parameter_layout);

		if(savedInstanceState == null) {
			Intent intent = getIntent();
			this.fb = intent.getParcelableExtra("fractal");
		} else {
			this.fb = savedInstanceState.getParcelable("fractal");
		}

		if(this.fb == null) {
			throw new IllegalArgumentException("fb is null!");
		}

		// need to extract all external values from FB. Hence parse it [compiling not necessary]

		try {
			this.fb.parse();
		} catch (CompileException e) {
			throw new IllegalArgumentException("could not compile fractal: " + e.getMessage() + ". this is a bug.");
		}

		adapter = new ParameterAdapter(this);

		listView = (ListView) findViewById(R.id.parameterListView);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Element 'position' was selected
				Pair<String, Fractal.Type> p = adapter.elements.get(position);

				switch(p.b) {
                /*case Label: {
                    // this should not happen...
                } return;*/
					case Scale: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Scale", false, EditableDialogFragment.Type.Scale)
								.setInitVal(fb.scale());

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Expr: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Expression", false, EditableDialogFragment.Type.Name)
								.setInitVal(fb.get(p.a).b);

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Bool: {
						boolean newValue = !(Boolean) fb.get(p.a).b;

						fb.setBool(p.a, newValue);
						((CheckedTextView) view).setChecked(newValue);

						adapter.notifyDataSetChanged();

						return;
					}
					case Int: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Integer Value", false, EditableDialogFragment.Type.Int)
								.setInitVal(fb.get(p.a).b);

						ft.show(getFragmentManager(), "dialog");
					} return;
					case Real: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Real Value", false, EditableDialogFragment.Type.Real)
								.setInitVal(fb.get(p.a).b);

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Cplx: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Complex Value", false, EditableDialogFragment.Type.Cplx)
								.setInitVal(fb.get(p.a).b);

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Color: {
						EditableDialogFragment ft = EditableDialogFragment.newInstance(
								position, "Edit Color", false, EditableDialogFragment.Type.Color)
								.setInitVal(fb.get(p.a).b);

						ft.show(getFragmentManager(), "dialog");
					}
					return;
					case Palette: {
						// start new activity
						Palette value = (Palette) fb.get(p.a).b;

						Intent i = new Intent(ParameterActivity.this, PaletteActivity.class);

						i.putExtra("palette", new PaletteActivity.PaletteWrapper(value));
						i.putExtra("id", p.a); // label should also be in here.

						startActivityForResult(i, PaletteActivity.PALETTE_ACTIVITY_RETURN);
					}
					return;
					default:
						throw new IllegalArgumentException();
				}
			}
		});

		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Pair<String, Fractal.Type> p = adapter.elements.get(position);

				switch (p.b) {
					case Scale: {
						showOptionsDialog(scaleOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								Scale original = fb.scale();
								switch (which) {
									case 0: // Reset
										fb.resetScale();
										break;
									case 1: // Origin
										fb.setScale(new Scale(original.xx, original.xy, original.yx, original.yy, 0, 0));
										break;
									case 2: // Ratio

										// Step 1: make x/y-vectors same length
										double xx = original.xx;
										double xy = original.xy;

										double yx = original.yx;
										double yy = original.yy;

										double lenx = Math.sqrt(xx * xx + xy * xy);
										double leny = Math.sqrt(yx * yx + yy * yy);

										double mlen = Math.max(lenx, leny);

										xx *= mlen / lenx;
										xy *= mlen / lenx;
										yx *= mlen / leny;
										yy *= mlen / leny;

										double vx = (xx + yx) / 2;
										double vy = (xy + yy) / 2;

										double ax = vx + vy;
										double ay = vx - vy;

										// fixme find proper orientation
										fb.setScale(new Scale(ax, ay, -ay, ax, original.cx, original.cy));

										break;
									case 3: // Straighten
										double xlen = Math.hypot(original.xx, original.xy);
										double ylen = Math.hypot(original.yx, original.yy);
										fb.setScale(new Scale(xlen, 0, 0, ylen, original.cx, original.cy));
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Expr: {
						showOptionsDialog(exprOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});
						return true;
					}
					case Bool: {
						showOptionsDialog(boolOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										// must update it in the interface
										((CheckedTextView) view).setChecked((Boolean) fb.get(p.a).b);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Int: {
						showOptionsDialog(intOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Real: {
						showOptionsDialog(realOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Cplx: {
						showOptionsDialog(cplxOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
									case 1: // Center
										fb.setCplx(p.a, new Cplx(fb.scale().cx, fb.scale().cy));
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Color: {
						showOptionsDialog(colorOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
					case Palette: {
						showOptionsDialog(paletteOptions, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								switch (which) {
									case 0: // Reset
										fb.reset(p.a);
										break;
								}

								adapter.notifyDataSetChanged();
							}
						});

						return true;
					}
				}

				return false;
			}
		});

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(0);
				finish();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent data = new Intent();
				data.putExtra("parameters", fb);
				setResult(1, data);
				finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable("fractal", fb);

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	void setNewSourceCode(String sourceCode) {
		try {
			// in first attempt preserve arguments
			final Fractal newFractal = fb.copyNewSource(sourceCode, true);

			newFractal.parse();
			newFractal.compile();

			this.fb = newFractal;
			adapter.init();
			adapter.notifyDataSetChanged();

			Toast.makeText(this, "Keeping parameters", Toast.LENGTH_SHORT).show();
		} catch(CompileException e) {
			Toast.makeText(this, "Resetting parameters due to errors: " + e.getMessage(), Toast.LENGTH_LONG).show();

			try {
				final Fractal newFractal = fb.copyNewSource(sourceCode, false);

				newFractal.parse();
				newFractal.compile();

				this.fb = newFractal;
				adapter.init();
				adapter.notifyDataSetChanged();
			} catch(CompileException e2) {
				e.printStackTrace();
				Toast.makeText(this, e2.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO I should also be able to use this one for dialogs?

		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PaletteActivity.PALETTE_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				PaletteActivity.PaletteWrapper wrapper = data.getParcelableExtra("palette");
				String id = data.getStringExtra("id");

				fb.setPalette(id, wrapper.p);
				adapter.notifyDataSetChanged();
			}
		} else if (requestCode == PROGRAM_ACTIVITY_RETURN) {
			if (resultCode == 1) { // = "Ok"
				String sourceCode = data.getExtras().getString("source");
				setNewSourceCode(sourceCode);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.parameters_activity_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_reset: {
				// confirm that reset is what you want.
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setMessage("Reset Parameters to Defaults?");
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						fb.resetAll();
						adapter.notifyDataSetChanged(); // something changed...
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// do nothing
					}
				});

				builder.show();

				return true;
			}
			case R.id.action_edit_source: {
				Intent i = new Intent(this, EditProgramActivity.class);
				i.putExtra("source", this.fb.sourceCode());
				startActivityForResult(i, PROGRAM_ACTIVITY_RETURN);
			} return true;
		}

		return false;
	}

	// create new dialog with fractal editor in it.
				/*FractalEditor fractalEditor = new FractalEditor("Edit FractalBuilder");

				SettingsEditor<Fractal> editor = new SettingsEditor.Adapter<Fractal>(
						fractalEditor, new SettingsEditor.OnConfirmedListener<Fractal>() {
					@Override
					public boolean confirmed(final Fractal fractal) {
						bitmapFragment.edit(new Runnable() {
							@Override
							public void run() {
								bitmapFragment.fractalBuilder.set(fractal);
							}
						}, "edit"); // FIXME String
						return true;
					}
				});

				editor.set(bitmapFragment.fractal());
				DialogEditFragment.createDialog(this, editor);*/

	class ParameterAdapter extends BaseAdapter implements ListAdapter {

		final LayoutInflater inflater;
		final ArrayList<Pair<String, Fractal.Type>> elements;

		ParameterAdapter(Context context) {
			inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
			// fill elements with elements :)
			elements = new ArrayList<>();
			init();
		}

		/**
		 * Fill elements-list with content.
		 */
		void init() {
			elements.clear();

			// First add scale.
			elements.add(new Pair<>("Scale", Fractal.Type.Scale));

			for(String id : fb.parameterIds()) {
				elements.add(new Pair<>(id, fb.get(id).a));
			}
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public int getCount() {
			return elements.size();
		}

		@Override
		public String getItem(int position) {
			return elements.get(position).a;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			// labels + one for each type.
			// fixme more of them (should contain a preview/checkbox for bools)
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			Fractal.Type t = elements.get(position).b;
			return t == Fractal.Type.Bool ? BOOL : t == Fractal.Type.Scale ? SCALE : ELEMENT;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			int viewType = getItemViewType(position);

			Pair<String, Fractal.Type> e = elements.get(position);

			switch(viewType) {
				case BOOL: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_checked, parent, false);

					CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);

					if(!fb.isDefaultValue(e.a)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					text1.setText(e.a);
					text1.setChecked((Boolean) fb.get(e.a).b);
				} break;
				case ELEMENT: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText(e.a);

					// if not isDefaultValue set bold.
					if(!fb.isDefaultValue(e.a)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}

					TextView text2 = (TextView) view.findViewById(android.R.id.text2);
					switch(e.b) {
						case Expr: text2.setText("Expression"); break;
						case Int: text2.setText("Integer Number"); break;
						case Real: text2.setText("Real Number"); break;
						case Cplx: text2.setText("Complex Number"); break;
						case Color: text2.setText("Color"); break;
						case Palette: text2.setText("Palette"); break;
					}
				} break;
				case SCALE: {
					if (view == null)
						view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
					TextView text1 = (TextView) view.findViewById(android.R.id.text1);
					text1.setText("Scale"); // FIXME there is only one. Thus use a string.

					if(!fb.scale().equals(PresetFractals.INIT_SCALE)) {
						//Log.d("PA", e.label + " is not default");
						text1.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						//Log.d("PA", e.label + " is default");
						text1.setTypeface(Typeface.DEFAULT);
					}
				}
			}


			return view;
		}
	}
}