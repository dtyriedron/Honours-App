package com.example.a40203.tomtommapexample;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.ImageButton;
import android.widget.Toast;

import com.tomtom.online.sdk.common.location.LatLng;

import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchQueryBuilder;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import android.Manifest;
        import android.content.Context;
        import android.content.pm.PackageManager;
        import android.location.Location;
        import android.os.Handler;
        import android.support.v4.app.ActivityCompat;
        import android.text.Editable;
        import android.text.TextWatcher;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.AutoCompleteTextView;
        import android.widget.Button;
        import com.google.android.gms.location.LocationRequest;
        import com.tomtom.online.sdk.common.permission.AndroidPermissionChecker;
        import com.tomtom.online.sdk.common.permission.PermissionChecker;
        import com.tomtom.online.sdk.location.LocationSource;
        import com.tomtom.online.sdk.location.LocationSourceFactory;
        import com.tomtom.online.sdk.location.LocationUpdateListener;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQueryBuilder;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResponse;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResult;
        import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderFullAddress;

public class MainActivity extends AppCompatActivity implements LocationUpdateListener {

    private static final LatLng DEFAULT_DEPARTURE_LATLNG = new LatLng(55.95004, -3.19295);
    private static final LatLng DEFAULT_DESTINATION_LATLNG = new LatLng(55.94487, -3.18419);
    private static final String LOG_TAG = "MainActivity";
    private static final int AUTOCOMPLETE_SEARCH_DELAY_MILLIS = 600;
    private static final int AUTOCOMPLETE_SEARCH_THRESHOLD = 3;
    private static final int SEARCH_FUZZY_LVL_MIN = 2;
    private static final int PERMISSION_REQUEST_LOCATION = 0;

    //TomTom's search api used in the text boxes to reverse geocode and search for places
    private SearchApi searchApi;
    //current location
    private LocationSource locationSource;
    //text views for view fuzzy search suggestions
    private AutoCompleteTextView atvDepartureLocation;
    private AutoCompleteTextView atvDestinationLocation;
    //used to cause less inconvenience when typing a search (delay for suggestions)
    private Handler searchTimerHandler = new Handler();
    //run search on new thread
    private Runnable searchRunnable;
    //arrylist to store current search suggestions
    private ArrayAdapter<String> searchAdapter;
    private List<String> searchAutocompleteList;
    //store the results of the search in a map
    private Map<String, LatLng> searchResultsMap;
    //stores current position
    private LatLng latLngCurrentPosition;
    //points stored for departure and destination
    private LatLng latLngDeparture;
    private LatLng latLngDestination;

    //initialize at start of activty
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initTomTomServices();
        initSearchFieldsWithDefaultValues();
        initWhereSection();
        initStartSection();
    }

    //when the activity returned from a switch
    @Override
    protected void onResume() {
        super.onResume();
        PermissionChecker checker = AndroidPermissionChecker.createLocationChecker(this);
        if(!checker.ifNotAllPermissionGranted()) {
            locationSource.activate();
        }
    }

    //update location
    @Override
    public void onLocationChanged(Location location) {
        if (latLngCurrentPosition == null) {
            latLngCurrentPosition = new LatLng(location);
            locationSource.deactivate();
        }
    }

    //initialize search api
    private void initTomTomServices() {
        searchApi = OnlineSearchApi.create(this);
    }

    //pre set route locations to act as a hint
    private void initSearchFieldsWithDefaultValues() {
        atvDepartureLocation = findViewById(R.id.atv_main_departure_location);
        atvDestinationLocation = findViewById(R.id.atv_main_destination_location);
        initLocationSource();
        initDepartureWithDefaultValue();
        initDestinationWithDefaultValue();
    }

    //
    private void initLocationSource() {
        PermissionChecker permissionChecker = AndroidPermissionChecker.createLocationChecker(this);
        if(permissionChecker.ifNotAllPermissionGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }
        LocationSourceFactory locationSourceFactory = new LocationSourceFactory();
        locationSource = locationSourceFactory.createDefaultLocationSource(this, this,  LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(2000)
                .setInterval(5000));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION:
                if(grantResults.length >= 2 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    locationSource.activate();
                }
                else {
                    Toast.makeText(this, R.string.location_permissions_denied, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void initDepartureWithDefaultValue() {
        latLngDeparture = DEFAULT_DEPARTURE_LATLNG;
        setAddressForLocation(latLngDeparture, atvDepartureLocation);
    }

    private void initDestinationWithDefaultValue() {
        latLngDestination = DEFAULT_DESTINATION_LATLNG;
        setAddressForLocation(latLngDestination, atvDestinationLocation);
    }

    private void initWhereSection() {
        searchAutocompleteList = new ArrayList<>();
        searchResultsMap = new HashMap<>();
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, searchAutocompleteList);
        ImageButton btnDepartureClear = findViewById(R.id.button_departure_clear);
        ImageButton btnDestinationClear = findViewById(R.id.button_destination_clear);

        setTextWatcherToAutoCompleteField(atvDepartureLocation, btnDepartureClear);
        setClearButtonToAutocompleteField(atvDepartureLocation, btnDepartureClear);
        setTextWatcherToAutoCompleteField(atvDestinationLocation, btnDestinationClear);
        setClearButtonToAutocompleteField(atvDestinationLocation, btnDestinationClear);
    }

    private void setTextWatcherToAutoCompleteField(final AutoCompleteTextView autoCompleteTextView, final ImageButton imageButton) {
        autoCompleteTextView.setAdapter(searchAdapter);
        autoCompleteTextView.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTimerHandler != null) {
                    searchTimerHandler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (s.length() > 0) {
                    imageButton.setVisibility(View.VISIBLE);
                    if (s.length() >= AUTOCOMPLETE_SEARCH_THRESHOLD) {
                        searchRunnable = new Runnable() {
                            @Override
                            public void run() {
                                searchAddress(s.toString(), autoCompleteTextView);
                            }
                        };
                        searchAdapter.clear();
                        searchTimerHandler.postDelayed(searchRunnable, AUTOCOMPLETE_SEARCH_DELAY_MILLIS);
                    }
                } else {
                    imageButton.setVisibility(View.INVISIBLE);
                }
            }
        });
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                if (autoCompleteTextView == atvDepartureLocation) {
                    latLngDeparture = searchResultsMap.get(item);
                } else if (autoCompleteTextView == atvDestinationLocation) {
                    latLngDestination = searchResultsMap.get(item);
                }
                hideKeyboard(view);
            }
        });
    }

    private void searchAddress(final String searchWord, final AutoCompleteTextView autoCompleteTextView) {
        searchApi.search(new FuzzySearchQueryBuilder(searchWord)
                .withLanguage(Locale.getDefault().toLanguageTag())
                .withTypeAhead(true)
                .withMinFuzzyLevel(SEARCH_FUZZY_LVL_MIN)) //.build()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<FuzzySearchResponse>() {
                    @Override
                    public void onSuccess(FuzzySearchResponse fuzzySearchResponse) {
                        if (!fuzzySearchResponse.getResults().isEmpty()) {
                            searchAutocompleteList.clear();
                            searchResultsMap.clear();
                            if (autoCompleteTextView == atvDepartureLocation && latLngCurrentPosition != null) {
                                String currentLocationTitle = getString(R.string.main_current_position);
                                searchAutocompleteList.add(currentLocationTitle);
                                searchResultsMap.put(currentLocationTitle, latLngCurrentPosition);
                            }
                            for (FuzzySearchResult result : fuzzySearchResponse.getResults()) {
                                String addressString = result.getAddress().getFreeformAddress();
                                searchAutocompleteList.add(addressString);
                                searchResultsMap.put(addressString, result.getPosition());
                            }
                            searchAdapter.clear();
                            searchAdapter.addAll(searchAutocompleteList);
                            searchAdapter.getFilter().filter("");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setAddressForLocation(LatLng location, final AutoCompleteTextView autoCompleteTextView) {
        searchApi.reverseGeocoding(new ReverseGeocoderSearchQueryBuilder(location.getLatitude(), location.getLongitude())) //.build()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<ReverseGeocoderSearchResponse>() {
                    @Override
                    public void onSuccess(ReverseGeocoderSearchResponse reverseGeocoderSearchResponse) {
                        List addressesList = reverseGeocoderSearchResponse.getAddresses();
                        if (!addressesList.isEmpty()) {
                            String address = ((ReverseGeocoderFullAddress) addressesList.get(0)).getAddress().getFreeformAddress();
                            autoCompleteTextView.setText(address);
                            autoCompleteTextView.dismissDropDown();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_error_message_error_getting_location, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, getString(R.string.toast_error_message_error_getting_location, e.getLocalizedMessage()), e);
                    }
                });
    }

    private void setClearButtonToAutocompleteField(final AutoCompleteTextView autoCompleteTextView, final ImageButton imageButton) {
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteTextView.setText("");
                autoCompleteTextView.requestFocus();
                imageButton.setVisibility(View.GONE);
            }
        });
    }


    private void initStartSection() {
        Button buttonStart = findViewById(R.id.button_main_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (departureFiledIsEmpty()) {
                    initDepartureWithDefaultValue();
                } else if (destinationFieldIsEmpty()) {
                    initDestinationWithDefaultValue();
                }

                Intent intent = MapActivity.prepareIntent(MainActivity.this,
                        latLngDeparture,
                        latLngDestination);
                startActivity(intent);
            }
        });
    }

    private boolean textViewIsEmpty(AutoCompleteTextView textView) {
        return textView.getText().toString().isEmpty();
    }

    private boolean departureFiledIsEmpty() {
        return textViewIsEmpty(atvDepartureLocation);
    }

    private boolean destinationFieldIsEmpty() {
        return textViewIsEmpty(atvDestinationLocation);
    }

    private void hideKeyboard(View view) {
        InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (in != null) {
            in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        }
    }

    private abstract class BaseTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
    }
}
