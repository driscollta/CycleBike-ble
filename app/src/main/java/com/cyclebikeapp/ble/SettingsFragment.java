package com.cyclebikeapp.ble;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragment {
    private PreferenceChangedListener prefsChangedListener;

    public SettingsFragment() {
        // Required empty public constructor
        //Log.e(this.getClass().getName(), "new Settings Fragment");

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();

        return fragment;
    }
    private class PreferenceChangedListener implements
                                            SharedPreferences.OnSharedPreferenceChangeListener {

        @SuppressWarnings("deprecation")
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //Log.e(this.getClass().getName(), " onSharedPreferenceChanged()" + key);
            setSummary(key);
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Log.e(this.getClass().getName(), " onActivityCreated()");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        initSummary();
        prefsChangedListener = new PreferenceChangedListener();
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsChangedListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    private void initSummary() {
        String[] prefsKeys = new String[]{"tcx_idle_default","distance_default","unit_default","activity_file_default","trackpoint_density_default"};
        for (String key:prefsKeys){
            //Log.e(this.getClass().getName(), "initializing summary for " + key);
            setSummary(key);
        }
    }
    private void setSummary(String key) {
        Preference mPref = findPreference(key);
        // Set summary to be the user-description for the selected value
        if (mPref != null) {
            if (mPref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) mPref;
                mPref.setSummary(listPref.getEntry());
            }
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
