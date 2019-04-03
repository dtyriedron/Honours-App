package com.example.a40203.tomtommapexample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.Icon;
import com.tomtom.online.sdk.map.MapFragment;
import com.tomtom.online.sdk.map.Marker;
import com.tomtom.online.sdk.map.MarkerBuilder;
import com.tomtom.online.sdk.map.OnMapReadyCallback;
import com.tomtom.online.sdk.map.Route;
import com.tomtom.online.sdk.map.RouteBuilder;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResponse;
import com.tomtom.online.sdk.routing.data.TravelMode;
import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchQueryBuilder;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, TomtomMapCallback.OnMapLongClickListener{

    private static final String BUNDLE_SETTINGS = "SETTINGS";
    private static final String BUNDLE_DEPARTURE_LAT = "DEPARTURE_LAT";
    private static final String BUNDLE_DEPARTURE_LNG = "DEPARTURE_LNG";
    private static final String BUNDLE_DESTINATION_LAT = "DESTINATION_LAT";
    private static final String BUNDLE_DESTINATION_LNG = "DESTINATION_LNG";
    private static final int ONE_MINUTE_IN_MILLIS = 60000;
    private static final int ROUTE_RECALCULATION_DELAY = ONE_MINUTE_IN_MILLIS;

    private boolean isInPauseMode = false;
    private AlertDialog dialogInfo;
    private AlertDialog dialogInProgress;
    private TomtomMap tomtomMap;
    private TravelMode travelMode;
    private CountDownTimer countDownTimer;
    private Handler timerHandler = new Handler();
    private RoutingApi routingApi;

    //mainact stuff
    private ArrayList<LatLng> points;
    private double[][] dijkstraPoints;
    private LatLng departurePosition;
    private LatLng destinationPosition;
    private Icon departureIcon;
    private Icon destinationIcon;
    private SearchApi searchApi;
    private ImageButton btnSearch;
    private EditText editTextPois;
    private ArrayList<String> streetnames;
    private double routeDistance;
    private ArrayList<Integer> newRoute;
    private Route route;
    private List<FullRoute> routes;

    private Runnable requestRouteRunnable = new Runnable() {
        @Override
        public void run() {
            requestRoute(departurePosition, destinationPosition);
        }
    };
    private Runnable requestDijkstraRunnable = new Runnable() {
        @Override
        public void run() {
            CalcDijkstra.calculate(dijkstraPoints, 0);
        }
    };
    private Runnable requestFindEdgesRunnable = new Runnable() {
        @Override
        public void run() {
            FindEdgesForDijkstra.calculate(routes, dijkstraPoints);
        }
    };

    public static void getResponse(String reposnseString) {
        Log.w("cheese", reposnseString);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initTomTomServices();
//        initToolbarSettings();
        Bundle settings = getIntent().getBundleExtra(BUNDLE_SETTINGS);
        initActivitySettings(settings);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInPauseMode = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!dialogIsShowing(dialogInfo)) {
            isInPauseMode = false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timerHandler.removeCallbacks(requestRouteRunnable);
        timerHandler.removeCallbacks(requestDijkstraRunnable);
        timerHandler.removeCallbacks(requestFindEdgesRunnable);
        countDownTimer.cancel();
    }


    @Override
    public void onMapReady(@NonNull TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.setMyLocationEnabled(true);
        this.tomtomMap.addOnMapLongClickListener(this);
        requestRoute(departurePosition, destinationPosition);

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(isDeparturePositionSet() && isDestinationPositionSet()){
            clearMap();
        } else {
            handleLongClick(latLng);
        }
    }
    public void clearMap(){
        tomtomMap.clear();
        departurePosition = null;
        destinationPosition = null;
        route = null;
        points.clear();
        dijkstraPoints = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private boolean isDestinationPositionSet(){
        return destinationPosition != null;
    }

    private boolean isDeparturePositionSet(){
        return departurePosition != null;
    }

    private void handleLongClick(@NonNull LatLng latLng){
        searchApi.reverseGeocoding(new ReverseGeocoderSearchQueryBuilder(latLng.getLatitude(), latLng.getLongitude()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<ReverseGeocoderSearchResponse>() {
                    @Override
                    public void onSuccess(ReverseGeocoderSearchResponse response){
                        processResponse(response);
                    }

                    @Override
                    public void onError(Throwable e){
                        handleApiError(e);
                    }

                    private void processResponse(ReverseGeocoderSearchResponse response){
                        if(response.hasResults()){
                            processFirstResult(response.getAddresses().get(0).getPosition());
                        }
                        else{
                            Toast.makeText(MapActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
                        }
                    }
                    private void processFirstResult(LatLng geocodedPosition){
                        if(!isDeparturePositionSet()){
                            setAndDisplayDeparturePosition(geocodedPosition);
                        } else{
//                            if (!isInPauseMode) {
                                streetnames = new ArrayList<>();
    //                            pointPos = new ArrayList<>();
    //                            map = new HashMap<>();
                                destinationPosition = geocodedPosition;
                                tomtomMap.removeMarkers();
                                requestRoute(departurePosition, destinationPosition);
//                            }
                        }
                    }

                    private void setAndDisplayDeparturePosition(LatLng geocodedPosition){
                        departurePosition = geocodedPosition;
                        createMarkerIfNotPresent(departurePosition, departureIcon);
                    }
                });
    }

    @NonNull
    private View.OnClickListener getSearchButtonListener(){
        return new View.OnClickListener(){

            @Override
            public void onClick(View v){
                handleSearchClick(v);
            }

            private void handleSearchClick(View v){
                if(isRouteSet()){
                    Optional<CharSequence> description = Optional.fromNullable(v.getContentDescription());
                    if(description.isPresent()){
                        editTextPois.setText(description.get());
                        v.setSelected(true);
                    }
                    String textToSearch = editTextPois.getText().toString();
                    //LatLng lngToSearch = new LatLng(Double.valueOf(textToSearch.split(",")[0]), Double.valueOf(textToSearch.split(",")[1]));
                    if(!textToSearch.isEmpty()){
                        tomtomMap.removeMarkers();
                    }
                }
            }

            private boolean isRouteSet(){
                return route != null;
            }


        };
    }

    private void createMarkerIfNotPresent(LatLng position, Icon icon){
        com.google.common.base.Optional<Marker> optionalMarker = tomtomMap.findMarkerByPosition(position);
        if(!optionalMarker.isPresent()){
            tomtomMap.addMarker(new MarkerBuilder(position).icon(icon));
        }
    }

    private void handleApiError(Throwable e){
        Toast.makeText(MapActivity.this, getString(R.string.api_response_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private void requestRoute(final LatLng departure, final LatLng destination) {
        showDialog(dialogInProgress);
        if (!isInPauseMode) {
            RouteQuery routeQuery = new RouteQueryBuilder(departure, destination)
                    .withMaxAlternatives(4)
                    .build();

            routingApi.planRoute(routeQuery)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<RouteResponse>() {
                        @Override
                        public void onSuccess(RouteResponse routeResponse) {
                            if (routeResponse.hasResults()) {
                                Log.w("bed", "departure: "+ departure + " destination: "+ destination);
                                points = new ArrayList<>();
                                routes = routeResponse.getRoutes();
                                for (int i = 0; i < routes.size(); ++i) {
                                    points.addAll(routes.get(i).getCoordinates());
                                }
                                Log.w("debug", "number of routes: " + routes.size());
                                //prepare the points for dijkstra
                                dijkstraPoints = new double[points.size()][points.size()];
                                if(!isInPauseMode){
                                    FindEdgesForDijkstra.calculate(routes, dijkstraPoints);
                                }else{
                                    timerHandler.removeCallbacks(requestFindEdgesRunnable);
                                    timerHandler.postDelayed(requestFindEdgesRunnable, ROUTE_RECALCULATION_DELAY);
                                }

                                //setupForDB(points);


                                //new route points to display the route that dijkstra has calculated
                                newRoute = new ArrayList<>();;
                                //put the points calculated from dijkstra into an arraylist to then display the route
                                if(!isInPauseMode){
                                    CalcDijkstra.calculate(dijkstraPoints, 0);
                                }else{
                                    timerHandler.removeCallbacks(requestDijkstraRunnable);
                                    timerHandler.postDelayed(requestDijkstraRunnable, ROUTE_RECALCULATION_DELAY);
                                }

                                newRoute = CalcDijkstra.getRoute();
                                //new list of points that relate to current point position in the street
                                ArrayList<LatLng> newRoutePoints = new ArrayList<>();

                                //loop for all the points in the street
                                for (int j = 0; j < newRoute.size(); ++j) {
                                    newRoutePoints.add(points.get(newRoute.get(j)));
                                }

                                routeDistance = CalcDijkstra.getDistance();


                                String printNewRoute = "";
                                for (Integer j : newRoute) {
                                    printNewRoute += j + ", ";
                                }
                                Log.w("new route", "new route: " + printNewRoute);
                                Log.w("new route", "total distance: " + routeDistance);

                                hideDialog(dialogInProgress);
                                displayRouteOnMap(newRoutePoints);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            hideDialog(dialogInProgress);
                            Toast.makeText(MapActivity.this, getString(R.string.toast_error_message_cannot_find_route), Toast.LENGTH_LONG).show();
                            MapActivity.this.finish();
                            handleApiError(e);
                            clearMap();
                            MapActivity.this.finish();
                        }
                        private void displayRouteOnMap(List<LatLng> coordinates) {
                            RouteBuilder routeBuilder = new RouteBuilder(coordinates)
                                    .startIcon(departureIcon)
                                    .endIcon(destinationIcon)
                                    .isActive(true);
                            tomtomMap.clear();
                            tomtomMap.addRoute(routeBuilder);
                            tomtomMap.displayRoutesOverview();
                        }
                    });
        }
        else {
            timerHandler.removeCallbacks(requestRouteRunnable);
            timerHandler.postDelayed(requestRouteRunnable, ROUTE_RECALCULATION_DELAY);
        }
    }

    private void setupForDB(ArrayList<LatLng> points){
        setStreetName(points);
    }

    public void setStreetName(ArrayList<LatLng> points){
        //map of streetnames and a point related to them
        Map<String, LatLng> streetnames = new LinkedHashMap<>();
        ConnectToDB connectToDB = new ConnectToDB();

        for (int i=0; i<points.size(); i++){
            int finalI = i;
            searchApi.reverseGeocoding(new ReverseGeocoderSearchQueryBuilder(points.get(i).getLatitude(), points.get(i).getLongitude()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DisposableSingleObserver<ReverseGeocoderSearchResponse>() {
                        @Override
                        public void onSuccess(ReverseGeocoderSearchResponse response){
                            processResponse(response);
                        }

                        @Override
                        public void onError(Throwable e){
                            handleApiError(e);
                        }

                        private void processResponse(ReverseGeocoderSearchResponse response){
                            if(response.hasResults()) {
                                //set the streetname
                                //streetName = response.getAddresses().get(0).getAddress().getStreetName();
                                streetnames.put(response.getAddresses().get(0).getAddress().getStreetName(), points.get(finalI));
                                Log.w("server", "streetnames size: "+streetnames.size());

                                Log.w("server", "streetname: "+ response.getAddresses().get(0).getAddress().getStreetName());
                                String whole = streetnames.get(response.getAddresses().get(0).getAddress().getStreetName()).toString();
                                String part1 = whole.split("=", 2)[1];
                                part1 = part1.replace(")", " ");
                                String part11= part1.split(",")[0];
                                String part2 = part1.split("=")[1];
                                String lat = part11+","+part2;


                                if(!response.getAddresses().get(0).getAddress().getStreetName().equals("")){
                                    JSONObject json = connectToDB.roadJSON(response.getAddresses().get(0).getAddress().getStreetName(), lat);
                                    connectToDB.sendRequest("http://192.168.1.99/test.php", json);
                                }

                            }
                            else{
                                Toast.makeText(MapActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            SystemClock.sleep(200);
        }




    }




    private void initTomTomServices() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);
        searchApi = OnlineSearchApi.create(this);
        routingApi = OnlineRoutingApi.create(this);

    }


    private void initActivitySettings(Bundle settings) {
        departureIcon = Icon.Factory.fromResources(MapActivity.this, R.drawable.ic_map_route_departure);
        destinationIcon = Icon.Factory.fromResources(MapActivity.this, R.drawable.ic_map_route_destination);
        btnSearch = findViewById(R.id.btn_main_poisearch);
        editTextPois = findViewById(R.id.edittext_main_poisearch);

        initBundleSettings(settings);

        createDialogInProgress();
    }

    private void initBundleSettings(Bundle settings) {
        departurePosition = new LatLng(settings.getDouble(BUNDLE_DEPARTURE_LAT), settings.getDouble(BUNDLE_DEPARTURE_LNG));
        destinationPosition = new LatLng(settings.getDouble(BUNDLE_DESTINATION_LAT), settings.getDouble(BUNDLE_DESTINATION_LNG));
    }

    private void createDialogInProgress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_in_progress, null));
        dialogInProgress = builder.create();
        dialogInProgress.setCanceledOnTouchOutside(false);
    }

    private void hideDialog(Dialog dialog) {
        if (dialogIsShowing(dialog)) {
            dialog.dismiss();
        }
    }

    private void showDialog(Dialog dialog) {
        if (!dialogIsShowing(dialog)) {
            dialog.show();
        }
    }

    private boolean dialogIsShowing(Dialog dialog) {
        return dialog != null && dialog.isShowing();
    }

    public static Intent prepareIntent(Context context, LatLng departure, LatLng destination) {
        Bundle settings = new Bundle();
        settings.putDouble(BUNDLE_DEPARTURE_LAT, departure.getLatitude());
        settings.putDouble(BUNDLE_DEPARTURE_LNG, departure.getLongitude());
        settings.putDouble(BUNDLE_DESTINATION_LAT, destination.getLatitude());
        settings.putDouble(BUNDLE_DESTINATION_LNG, destination.getLongitude());
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(BUNDLE_SETTINGS, settings);

        return intent;
    }
}
