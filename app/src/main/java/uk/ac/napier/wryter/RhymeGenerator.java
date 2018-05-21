package uk.ac.napier.wryter;


import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class RhymeGenerator extends Fragment {

    private Toast toast;

    private ListView mListView;
    private EditText mEditText;

    public RhymeGenerator() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rhyme_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Making toasts display a little above the bottom navigation bar
        toast = Toast.makeText(getContext(), null, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 800);

        mListView = (ListView) getView().findViewById(R.id.fragment_rhyme_list);
        mEditText = (EditText) getView().findViewById(R.id.fragment_rhyme_editText);

        //Grabs words that rhymes with the text and lists them if there are any
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String rhyme = mEditText.getText().toString();

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:

                            if (mEditText.getText().toString().contains(" ")){ //Check to see the user only inputted one word before searching rhymes
                                toast.setText("Please make sure it's only one word");
                                toast.show();
                                return false;
                            }

                            new APITask().execute(rhyme);
                            return true;

                        default:

                            break;
                    }
                }

                return false;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_rhyme_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        int targetId = R.id.menu_rhyme_new;

        Intent intent = new Intent(getContext(), MainActivity.class);
        RhymeGenerator fragment = new RhymeGenerator();

        //If the menu button that's pressed is the new button, start the writing activity
        if (itemId == targetId) {
            getFragmentManager().beginTransaction().replace(R.id.main_frameLayout, fragment).commit();
        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        mEditText.setText(null); //Clearing the textif the user navigates somewhere else
    }

    /**
     * Class that handles web API tasks
     */
    class APITask extends AsyncTask<String, Void, String> {

        private ProgressDialog loading = new ProgressDialog(getContext());

        @Override
        protected void onPreExecute() {
            //Loading gif
            loading.setMessage("Loading");
            loading.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String s1 = strings[0];

            //Grabs the text from the API site
            try {
                URL url = new URL("https://api.datamuse.com/words?rel_rhy=" + s1);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ERROR", e.getMessage(), e);

                //Letting user know it could be an internet connection issue
                toast.setText("Please make sure you have a working internet connection");
                toast.show();
                getFragmentManager().beginTransaction().replace(R.id.main_frameLayout, new RhymeGenerator()).commit(); //Restarts the fragment in order to let the user input again
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            //Stop loading gif
            if (loading.isShowing()) {
                loading.dismiss();
            }

            //Getting rid of the components when the list of rhymes appears
            mEditText.setVisibility(View.GONE);

            //Listing the rhymes if there are any
            if (s != null) { //Checking to make sure if data was grabbed
                ArrayList<String> rhymes = Features.getRhymes(s);

                if (rhymes == null || rhymes.size() == 0) { //Check to see if there were any rhymes at all
                    toast.setText("No rhymes found");
                    toast.show();
                    getFragmentManager().beginTransaction().replace(R.id.main_frameLayout, new RhymeGenerator()).commit(); //Restarts the fragment in order to let the user input again
                    return;
                } else {
                    RhymeAdapter ra = new RhymeAdapter(getContext(), R.layout.item_rhyme, rhymes);
                    mListView.setAdapter(ra);
                }
            }
        }
    }
}
