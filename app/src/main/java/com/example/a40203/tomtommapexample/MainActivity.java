package com.example.a40203.tomtommapexample;

import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.common.base.Optional;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.BaseMarkerBalloon;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.Marker;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.Route;
import com.tomtom.online.sdk.map.RouteBuilder;
import com.tomtom.online.sdk.map.RouteStyle;
import com.tomtom.online.sdk.map.RouteStyleBuilder;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.map.model.MapTilesType;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteType;
import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;
import com.tomtom.online.sdk.search.data.alongroute.AlongRouteSearchQueryBuilder;
import com.tomtom.online.sdk.search.data.alongroute.AlongRouteSearchResponse;
import com.tomtom.online.sdk.search.data.alongroute.AlongRouteSearchResult;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchQueryBuilder;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchResponse;
import com.tomtom.online.sdk.traffic.OnlineTrafficApi;
import com.tomtom.online.sdk.traffic.TrafficApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static java.lang.String.valueOf;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;
import android.Manifest;
        import android.app.DialogFragment;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.location.Location;
        import android.os.Bundle;
        import android.os.Handler;
        import android.support.annotation.NonNull;
        import android.support.v4.app.ActivityCompat;
        import android.support.v7.app.ActionBar;
        import android.support.v7.app.AppCompatActivity;
        import android.support.v7.widget.Toolbar;
        import android.text.Editable;
        import android.text.TextWatcher;
        import android.text.format.DateFormat;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.AutoCompleteTextView;
        import android.widget.Button;
        import android.widget.ImageButton;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.google.android.gms.location.LocationRequest;
        import com.tomtom.online.sdk.common.location.LatLng;
        import com.tomtom.online.sdk.common.permission.AndroidPermissionChecker;
        import com.tomtom.online.sdk.common.permission.PermissionChecker;
        import com.tomtom.online.sdk.location.LocationSource;
        import com.tomtom.online.sdk.location.LocationSourceFactory;
        import com.tomtom.online.sdk.location.LocationUpdateListener;
        import com.tomtom.online.sdk.routing.data.TravelMode;
        import com.tomtom.online.sdk.search.OnlineSearchApi;
        import com.tomtom.online.sdk.search.SearchApi;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchQueryBuilder;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResponse;
        import com.tomtom.online.sdk.search.data.fuzzy.FuzzySearchResult;
        import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderFullAddress;
        import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchQueryBuilder;
        import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchResponse;

        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Locale;
        import java.util.Map;

        import io.reactivex.android.schedulers.AndroidSchedulers;
        import io.reactivex.observers.DisposableSingleObserver;
        import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements LocationUpdateListener {

    private static final int PREPARATION_FIRST_OPT = 0;
    private static final int PREPARATION_SECOND_OPT = 5;
    private static final int PREPARATION_THIRD_OPT = 10;

    private static final LatLng DEFAULT_DEPARTURE_LATLNG = new LatLng(52.376368, 4.908113);
    private static final LatLng DEFAULT_DESTINATION_LATLNG = new LatLng(52.3076865, 4.767424099999971);
    private static final String TIME_PICKER_DIALOG_TAG = "TimePicker";
    private static final String LOG_TAG = "MainActivity";
    private static final int ARRIVE_TIME_AHEAD_HOURS = 5;
    private static final int AUTOCOMPLETE_SEARCH_DELAY_MILLIS = 600;
    private static final int AUTOCOMPLETE_SEARCH_THRESHOLD = 3;
    private static final String TIME_24H_FORMAT = "HH:mm";
    private static final String TIME_12H_FORMAT = "hh:mm";
    private static final int SEARCH_FUZZY_LVL_MIN = 2;
    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private Calendar calArriveAt;
    private SearchApi searchApi;
    private LocationSource locationSource;
    private TextView textViewArriveAtHour;
    private TextView textViewArriveAtAmPm;
    private TravelMode travelModeSelected = TravelMode.CAR;
    private long arrivalTimeInMillis;
    private AutoCompleteTextView atvDepartureLocation;
    private AutoCompleteTextView atvDestinationLocation;
    private Handler searchTimerHandler = new Handler();
    private Runnable searchRunnable;
    private ArrayAdapter<String> searchAdapter;
    private List<String> searchAutocompleteList;
    private Map<String, LatLng> searchResultsMap;
    private LatLng latLngCurrentPosition;
    private LatLng latLngDeparture;
    private LatLng latLngDestination;
    private ImageButton buttonByWhatTaxi;
    private ImageButton buttonByWhatCar;
    private ImageButton buttonByWhatOnFoot;
    private Button buttonPreparationFirst;
    private Button buttonPreparationSecond;
    private Button buttonPreparationThird;
    private int preparationTimeSelected = PREPARATION_FIRST_OPT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //showHelpOnFirstRun();
        initTomTomServices();
        //initToolbarSettings();
        initSearchFieldsWithDefaultValues();
        initWhereSection();
        //initByWhenSection();
        //initByWhatSection();
        //initPreparationSection();
        initStartSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //resetDaysInArriveAt();
        PermissionChecker checker = AndroidPermissionChecker.createLocationChecker(this);
        if(!checker.ifNotAllPermissionGranted()) {
            locationSource.activate();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (latLngCurrentPosition == null) {
            latLngCurrentPosition = new LatLng(location);
            locationSource.deactivate();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int selectedItem = item.getItemId();
//        if (selectedItem == R.id.toolbar_menu_help) {
//            showHelpActivity();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

//    public Calendar getCalArriveAt() {
//        return calArriveAt;
//    }

    private void initTomTomServices() {
        searchApi = OnlineSearchApi.create(this);
    }

//    private void initToolbarSettings() {
//        Toolbar toolbar = findViewById(R.id.custom_toolbar);
//        setSupportActionBar(toolbar);
//
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(false);
//            actionBar.setDisplayShowHomeEnabled(false);
//            actionBar.setDisplayShowTitleEnabled(false);
//        }
//    }

    private void initSearchFieldsWithDefaultValues() {
        atvDepartureLocation = findViewById(R.id.atv_main_departure_location);
        atvDestinationLocation = findViewById(R.id.atv_main_destination_location);
        initLocationSource();
        initDepartureWithDefaultValue();
        initDestinationWithDefaultValue();
    }

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

//    private void showHelpOnFirstRun() {
//        String sharedPreferenceName = getString(R.string.shared_preference_name);
//        String sharedPreferenceIsFirstRun = getString(R.string.shared_preference_first_run);
//        Boolean isFirstRun = getSharedPreferences(sharedPreferenceName, MODE_PRIVATE)
//                .getBoolean(sharedPreferenceIsFirstRun, true);
//        if (isFirstRun) {
//            showHelpActivity();
//            getSharedPreferences(sharedPreferenceName, MODE_PRIVATE).edit()
//                    .putBoolean(sharedPreferenceIsFirstRun, false).apply();
//        }
//    }

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

//    private void initByWhenSection() {
//        setArriveAtCalendar(ARRIVE_TIME_AHEAD_HOURS);
//        textViewArriveAtHour = findViewById(R.id.text_view_main_arrive_at_hour);
//        textViewArriveAtAmPm = findViewById(R.id.text_view_main_arrive_at_am_pm);
////        setTimerDisplay();
////        textViewArriveAtHour.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                DialogFragment timePickerFragment = new TimePickerFragment();
////                timePickerFragment.show(getFragmentManager(), TIME_PICKER_DIALOG_TAG);
////            }
////        });
//    }

//    private void setArriveAtCalendar(int aheadHours) {
//        calArriveAt = Calendar.getInstance();
//        calArriveAt.add(Calendar.HOUR, aheadHours);
//    }
//
//    public void setTimerDisplay() {
//        String tvArriveAtHourString = (String) DateFormat.format(getUserPreferredHourPattern(), calArriveAt.getTimeInMillis());
//        textViewArriveAtHour.setText(tvArriveAtHourString);
//        setTvArriveAtAmPm(DateFormat.is24HourFormat(getApplicationContext()), calArriveAt.get(Calendar.AM_PM));
//    }

//    private String getUserPreferredHourPattern() {
//        return DateFormat.is24HourFormat(getApplicationContext()) ? TIME_24H_FORMAT : TIME_12H_FORMAT;
//    }
//
//    private void setTvArriveAtAmPm(boolean is24HourFormat, int indicator) {
//        if (is24HourFormat) {
//            textViewArriveAtAmPm.setVisibility(View.INVISIBLE);
//        } else {
//            textViewArriveAtAmPm.setVisibility(View.VISIBLE);
//            String strAmPm = (indicator == Calendar.AM) ? getString(R.string.main_am_value) : getString(R.string.main_pm_value);
//            textViewArriveAtAmPm.setText(strAmPm);
//        }
//    }
//
//    private void initByWhatSection() {
//        buttonByWhatCar = findViewById(R.id.button_main_car);
//        buttonByWhatTaxi = findViewById(R.id.button_main_taxi);
//        buttonByWhatOnFoot = findViewById(R.id.button_main_on_foot);
//        buttonByWhatCar.setSelected(true);
//
//        buttonByWhatCar.setOnClickListener(setByWhatButtonListener(TravelMode.CAR));
//        buttonByWhatTaxi.setOnClickListener(setByWhatButtonListener(TravelMode.TAXI));
//        buttonByWhatOnFoot.setOnClickListener(setByWhatButtonListener(TravelMode.PEDESTRIAN));
//    }

//    private View.OnClickListener setByWhatButtonListener(final TravelMode travelMode) {
//        return new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                deselectByWhatButtons();
//                v.setSelected(true);
//                travelModeSelected = travelMode;
//            }
//        };
//    }

//    private void deselectByWhatButtons() {
//        buttonByWhatTaxi.setSelected(false);
//        buttonByWhatOnFoot.setSelected(false);
//        buttonByWhatCar.setSelected(false);
//    }

//    private void initPreparationSection() {
//        buttonPreparationFirst = findViewById(R.id.button_main_preparation_first);
//        buttonPreparationSecond = findViewById(R.id.button_main_preparation_second);
//        buttonPreparationThird = findViewById(R.id.button_main_preparation_third);
//        deselectPreparationButtons();
//        selectPreparationButton(buttonPreparationFirst);
//
//        buttonPreparationFirst.setOnClickListener(setPreparationButtonListener(PREPARATION_FIRST_OPT));
//        buttonPreparationSecond.setOnClickListener(setPreparationButtonListener(PREPARATION_SECOND_OPT));
//        buttonPreparationThird.setOnClickListener(setPreparationButtonListener(PREPARATION_THIRD_OPT));
//    }

//    private View.OnClickListener setPreparationButtonListener(final int preparationTimeInMinutes) {
//        return new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                deselectPreparationButtons();
//                selectPreparationButton(v);
//                preparationTimeSelected = preparationTimeInMinutes;
//            }
//        };
//    }

//    private void deselectPreparationButtons() {
//        int elevationButtonNormal = (int) getResources().getDimension(R.dimen.main_elevation_button_normal);
//        buttonPreparationFirst.setSelected(false);
//        buttonPreparationFirst.setElevation(elevationButtonNormal);
//        buttonPreparationSecond.setSelected(false);
//        buttonPreparationSecond.setElevation(elevationButtonNormal);
//        buttonPreparationThird.setSelected(false);
//        buttonPreparationThird.setElevation(elevationButtonNormal);
//    }
//
//    private void selectPreparationButton(View preparationButton) {
//        preparationButton.setSelected(true);
//        int elevationButtonPressed = (int) getResources().getDimension(R.dimen.main_elevation_button_pressed);
//        preparationButton.setElevation(elevationButtonPressed);
//    }

    private void initStartSection() {
        Button buttonStart = findViewById(R.id.button_main_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //long currentTimeInMillis = getCurrentTimeInMillis();
                //arrivalTimeInMillis = getArrivalTimeInMillis();

                if (departureFiledIsEmpty()) {
                    initDepartureWithDefaultValue();
                } else if (destinationFieldIsEmpty()) {
                    initDestinationWithDefaultValue();
                }

//                if (currentTimeInMillis >= arrivalTimeInMillis) {
//                    calArriveAt.add(Calendar.DAY_OF_MONTH, 1);
//                    arrivalTimeInMillis = getArrivalTimeInMillis();
//                }

                Intent intent = MapActivity.prepareIntent(MainActivity.this,
                        latLngDeparture,
                        latLngDestination,
                        travelModeSelected,
                        arrivalTimeInMillis,
                        preparationTimeSelected);
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

//    private long getCurrentTimeInMillis() {
//        Calendar calendar = Calendar.getInstance();
//        return calendar.getTimeInMillis();
//    }

//    private long getArrivalTimeInMillis() {
//        return calArriveAt.getTimeInMillis();
//    }

//    private void resetDaysInArriveAt() {
//        Calendar calendar = Calendar.getInstance();
//        calArriveAt.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
//    }

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






//    //OnMapReadyCallback
//    //TomtomMapCallback.OnMapLongClickListener
//        //TextToSpeech.OnInitListener
//
//    private TomtomMap tomtomMap;
//    private RoutingApi routingApi;
//
//    //private TrafficApi trafficApi;
//
////    private LatLng wayPointPosition;
//
//    private String streetName;
//    //new list of points that relate to current point position in the street
//    private ArrayList<LatLng> newRoutePoints;
//
//    int pointCount = 0;
//    //map of different streetnames and their positions in the original streetname array
//    Map<String, ArrayList<Integer>> map;
//    ArrayList<Integer> pointPos;
//    //text to speech
//    TextToSpeech mTTS = null;
//    private final int ACT_CHECK_TTS_DATA = 1000;
//    private final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2000;
//    private int permissionCount = 0;
//    private String mAudioFilename = "";
//    private final String mUtteranceID = "totts";
//    //colours
//    private int[] routeColours;
//    //travelTime
//    private int[] travelTime;
//
//    final int MAX_DETOUR_TIME = 10000;
//    final int QUERY_LIMIT = 100;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        initTomTomServices();
//        initUIViews();
//        setupUIViewListeners();
//        //nitColours();
//
//        // Check to see if we have TTS voice data
////        Intent ttsIntent = new Intent();
////        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
////        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);
//    }
//
////    @Override
////    public void onMapReady(@NonNull final TomtomMap tomtomMap) {
////        this.tomtomMap = tomtomMap;
////        this.tomtomMap.getUiSettings().setMapTilesType(MapTilesType.VECTOR);
////        this.tomtomMap.setMyLocationEnabled(true);
//////        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);
////        //this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomViewAdapter());
////        //this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute1Balloon());
////        //this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute2Balloon());
////    }
//
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        this.tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
//
//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//
//    }
//
//    private void initTomTomServices() {
////        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
////        mapFragment.getAsyncMap(this);
////        searchApi = OnlineSearchApi.create(this);
////        routingApi = OnlineRoutingApi.create(this);
//        //trafficApi = OnlineTrafficApi.create(this);
//    }
//
//    private void initUIViews() {
//
//    }
//
//    private void setupUIViewListeners() {
////        View.OnClickListener searchButtonListener = getSearchButtonListener();
////        btnSearch.setOnClickListener(searchButtonListener);
//        //View.OnClickListener trafficButtonListener = getTrafficButtonListener();
//        //btnTrafficList.setOnClickListener(trafficButtonListener);
//    }
//
////    private void getLocation(){
//////        Icon activeIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_markedlocation);
//////        Icon inactiveIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.arrow_down);
//////        ChevronBuilder chevronBuilder = ChevronBuilder.create(activeIcon, inactiveIcon);
//////        Chevron chevron = tomtomMap.getDrivingSettings().addChevron(chevronBuilder);
//////        tomtomMap.getDrivingSettings().startTracking(chevron);
//////        chevron.getLocation();
//////        tomtomMap.getDrivingSettings().stopTracking();
////    }
//
//
//    public static void getResponse(String string){
//        Log.w("cheese", string);
//    }
//
//    public String getStreetName() {
//        return streetName;
//    }
//
//
//    //apply different speed factors to the distances between points in the distances list
//    private static double factorSpeed(double distance){
//        Random rand = new Random();
//
//        //the percentage of speed that the car is decreased by in various situations
//        double speedBumpDecrease = 0.5;
//        double trafficLightDecrease = 0.5;
//        double pedestrianDecrease = 0.9;
//
//        boolean speedBump;
//        boolean trafficLight;
//        boolean pedestrian;
//
//            //the chance of the car encountering each speed factor
//            speedBump = rand.nextDouble() <=0.05;
//            trafficLight = rand.nextDouble() <= 0.3;
//            pedestrian = rand.nextDouble() <= 0.1;
//
//            if(speedBump){
//                distance = distance * speedBumpDecrease;
//            }
//            if(trafficLight){
//                distance = distance * trafficLightDecrease;
//            }
//            if(pedestrian){
//                distance = distance * pedestrianDecrease;
//            }
//
//        return distance;
//    }
//
//
////    private void collateStreetPoints(){
////        //check if any streetnames match each other
////        if(pointCount >0) {
////            if (streetnames.get(pointCount).equals(streetnames.get(pointCount -1))) {
////                //add another point to the current street's set of points
////                pointPos.add(pointCount);
////                //Log.w("debug", "adding another point: " + pointCount + " for: " + streetnames.get(pointCount));
////                //update or add new streetname and add its points
////                map.put(streetnames.get(pointCount), pointPos);
////                //Log.w("debug", "Map adding1: " + streetnames.get(pointCount) + " with: "+ pointPos.size() + "points");
////                //Log.w("debug2",  "size of the map: " + map.size());
////
////
////                //give the street some colour
////                //debugStreets(pointCount);
//////                String string = "\n";
//////                for(double[] i: organisedPoints){
//////                    for(double j: i){
//////                        string +=j;
//////                        string += ", ";
//////                    }
//////                    string += "\n";
//////                }
//////                Log.w("debug2", "organised points current street: "+ string);
////
////            }else{
////                pointPos.clear();
////                pointPos.add(pointCount);
////                //put the new streetname's points into map
////                map.put(streetnames.get(pointCount), pointPos);
//////                map.get(streetnames.get(pointCount-1)).size()-1)
//////                for(int i = 1; i<pointPos.size();++i) {
//////                    organisedPoints[map.size()][pointCount] = Helper.calculateDistance(points.get(pointPos.get(i)), points.get(i-1));
//////                }
////                //Log.w("debug", "Map adding2: " + streetnames.get(pointCount) + " with: "+ pointPos.size() + "points");
////                //Log.w("debug2",  "size of the map: " + map.size());
////
//////                String string = "\n";
////
//////                for(double[] i: organisedPoints){
//////                    for(double j: i){
//////                         string +=j;
//////                         string += ", ";
//////                    }
//////                    string += "\n";
//////                }
//////                Log.w("debug2", "organised points current street: "+ string);
////
////                //give the street some colour
////                //debugStreets(pointCount);
////
//////                //check if its the last streetname and therefore needs to added.
//////                if((pointCount == points.size()-1)){
//////                    pointPos.add(pointCount);
//////                    Log.w("debug", "adding the last point: " + pointCount);
//////                    map.put(streetnames.get(pointCount), pointPos);
//////                    Log.w("debug", "Map adding1: "+ streetnames.get(pointCount) + " with: " + pointPos.size() + "points");
//////
//////                    //String key = (String) ;
//////                    organisedPoints[map.size()][pointCount] = Helper.calculateDistance(points.get(pointPos.get(0)), points.get(map.get(streetnames.get(pointCount-1)).size()-1));
//////                    debugStreets(pointCount);
//////                }
////
////            }
//////            else{
//////                pointPos.add(pointCount);
//////                map.put(streetnames.get(pointCount), pointPos);
//////                Log.w("debug", "map adding1: " + streetnames.get(pointCount) + "with: " + pointPos.size() + "points");
//////                Log.w("debug", "adding odd point: " + pointCount);
//////                pointPos.clear();
//////                debugStreets(pointCount);
//////            }
////        } else {
////            //add the first point to the current street's set of points
////            pointPos.add(pointCount);
////            //Log.w("debug", "adding first point: " + pointCount);
////        }
////
////    }
//
//
//
////    private void debugStreets(LatLng p1, LatLng p2){
////        ///new color for every street
////        Random rnd = new Random();
////        int color;
////        color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
////        RouteStyle routestyle = RouteStyleBuilder.create()
////                .withWidth(2.0)
////                .withFillColor(color)
////                .withOutlineColor(Color.GRAY).build();
////
////        //new list of points that relate to current point position in the street
////        ArrayList<LatLng> newRoutePoints = new ArrayList<>();
////        newRoutePoints.add(p1);
////        newRoutePoints.add(p2);
////
////        //draw the street
////        route = tomtomMap.addRoute(new RouteBuilder(newRoutePoints).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));
////
////    }
//
//
////    public void setStreetName(LatLng latLng){
////        searchApi.reverseGeocoding(new ReverseGeocoderSearchQueryBuilder(latLng.getLatitude(), latLng.getLongitude()))
////                .subscribeOn(Schedulers.io())
////                .observeOn(AndroidSchedulers.mainThread())
////                .subscribe(new DisposableSingleObserver<ReverseGeocoderSearchResponse>() {
////                    @Override
////                    public void onSuccess(ReverseGeocoderSearchResponse response){
////                        processResponse(response);
////                    }
////
////                    @Override
////                    public void onError(Throwable e){
////                        handleApiError(e);
////                    }
////
////                    private void processResponse(ReverseGeocoderSearchResponse response){
////                        if(response.hasResults()) {
////                            //set the streetname
////                            streetName = response.getAddresses().get(0).getAddress().getStreetName();
////                            response.toString();
//////                            //put into array of streetnames
//////                            streetnames.add(response.getAddresses().get(0).getAddress().getStreetName());
//////                            // Log.w("debug", "streetnames added: " + streetnames.get(pointCount) + " " + pointCount);
//////
//////                            //add all the points up that relate top that street
//////                            //collateStreetPoints();
//////                            pointCount++;
////                        }
////                        else{
////                            Toast.makeText(MainActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
////                        }
////                    }
////                });
////    }
//
////    //text to speech
////    public void onInit(int status) {
////        if (status == TextToSpeech.SUCCESS) {
////            if (mTTS != null) {
////                int result = mTTS.setLanguage(Locale.US);
////                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
////                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
////                } else {
////                    saySomething("Hello World", 0);
////                }
////            }
////        } else {
////            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
////        }
////    }
////
////    private void saySomething(String text, int qmode) {
////        if (qmode == 1)
////            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
////        else
////            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
////    }
////
////    @Override
////    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
////        if (requestCode == ACT_CHECK_TTS_DATA) {
////            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
////                // data exists, so we instantiate the TTS engine
////                mTTS = new TextToSpeech(this, this);
////            } else {
////                // data is missing, so we start the TTS installation process
////                Intent installIntent = new Intent();
////                installIntent.setAction(
////                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
////                startActivity(installIntent);
////            }
////        }
////    }
////
////    @Override
////    protected void onDestroy() {
////        if (mTTS != null) {
////            mTTS.stop();
////            mTTS.shutdown();
////        }
////
////        super.onDestroy();
////    }
//}
