package com.example.a40203.tomtommapexample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.tomtom.online.sdk.map.RouteStyle;
import com.tomtom.online.sdk.map.RouteStyleBuilder;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResponse;
import com.tomtom.online.sdk.routing.data.RouteType;
import com.tomtom.online.sdk.routing.data.TravelMode;
import com.tomtom.online.sdk.search.OnlineSearchApi;
import com.tomtom.online.sdk.search.SearchApi;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchQueryBuilder;
import com.tomtom.online.sdk.search.data.reversegeocoder.ReverseGeocoderSearchResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, TomtomMapCallback.OnMapLongClickListener{

    private static final String BUNDLE_SETTINGS = "SETTINGS";
    private static final String BUNDLE_DEPARTURE_LAT = "DEPARTURE_LAT";
    private static final String BUNDLE_DEPARTURE_LNG = "DEPARTURE_LNG";
    private static final String BUNDLE_DESTINATION_LAT = "DESTINATION_LAT";
    private static final String BUNDLE_DESTINATION_LNG = "DESTINATION_LNG";
    private static final String BUNDLE_BY_WHAT = "BY_WHAT";
    private static final String BUNDLE_ARRIVE_AT = "ARRIVE_AT";
    private static final String BUNDLE_PREPARATION_TIME = "PREPARATION_TIME";
    private static final String COUNTDOWN_MODE_PREPARATION = "countdown_mode_preparation";
    private static final String COUNTDOWN_MODE_FINISHED = "countdown_mode_finished";
    private static final int ONE_MINUTE_IN_MILLIS = 60000;
    private static final int ONE_SECOND_IN_MILLIS = 1000;
    private static final int ROUTE_RECALCULATION_DELAY = ONE_MINUTE_IN_MILLIS;

    private int preparationTime;
    private int previousTravelTime;
    private boolean isPreparationMode = false;
    private boolean isInPauseMode = false;
    private Date arriveAt;
    private TextView textViewCountDownTimerHour;
    private TextView textViewCountDownTimerMin;
    private TextView textViewCountDownTimerSec;
    private TextView textViewTravelTime;
//    private CustomSnackbar infoSnackbar;
//    private CustomSnackbar warningSnackbar;
    private AlertDialog dialogInfo;
    private AlertDialog dialogInProgress;
    private TomtomMap tomtomMap;
//    private Icon departureIcon;
//    private Icon destinationIcon;
    private TravelMode travelMode;
//    private LatLng destination;
//    private LatLng departure;
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
            findEdges(routes);
        }
    };

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
//        countDownTimer.cancel();
    }


    @Override
    public void onMapReady(@NonNull TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.setMyLocationEnabled(true);
        this.tomtomMap.addOnMapLongClickListener(this);
        showDialog(dialogInProgress);
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
        //streetnames.clear();
//        pointPos.clear();
//        map.clear();
//        pointCount = 0;
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
                            streetnames = new ArrayList<>();
//                            pointPos = new ArrayList<>();
//                            map = new HashMap<>();
                            destinationPosition = geocodedPosition;
                            tomtomMap.removeMarkers();
                            drawRoute(departurePosition, destinationPosition);
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
//                    if(isWayPointPositionSet()){
//                        tomtomMap.clear();
//                        drawRoute(departurePosition, destinationPosition);
//                    }
                    String textToSearch = editTextPois.getText().toString();
                    LatLng lngToSearch = new LatLng(Double.valueOf(textToSearch.split(",")[0]), Double.valueOf(textToSearch.split(",")[1]));
                    if(!textToSearch.isEmpty()){
                        tomtomMap.removeMarkers();
                        //searchForLatLng(lngToSearch);
                        //searchAlongTheRoute(textToSearch);
                    }
                }
            }

            private boolean isRouteSet(){
                return route != null;
            }

//            private boolean isWayPointPositionSet(){
//                return wayPointPosition != null;
//            }

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

//    private RouteQuery createRouteQuery(LatLng start, LatLng stop, LatLng[] wayPoints){
//        //return (wayPoints != null) ?
//        return new RouteQuery(start, stop).withRouteType(RouteType.FASTEST);
//        //new RouteQueryBuilder(start, stop).withWayPoints(wayPoints).withRouteType(RouteType.FASTEST)
//        //:           new RouteQueryBuilder(start, stop).withRouteType(RouteType.FASTEST);
//    }

    private void drawRoute(LatLng start, LatLng stop){
        //wayPointPosition = null;
        //drawRouteWithWayPoints(start, stop, null);
        requestRoute(start, stop);
    }

//    private void drawRouteWithWayPoints(LatLng start, LatLng stop, LatLng[] wayPoints){
//        if(!isInPauseMode) {
//            RouteQuery routeQuery = new RouteQueryBuilder(start, stop)
//                    .withMaxAlternatives(9)
//                    .build();
//            //RouteQuery routeQuery = createRouteQuery(start, stop, wayPoints).withMaxAlternatives(9);
//            routingApi.planRoute(routeQuery)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new DisposableSingleObserver<RouteResponse>() {
//                        @Override
//                        public void onSuccess(RouteResponse routeResult) {
//                            hideDialog(dialogInProgress);
//                            displayRoutes(routeResult.getRoutes());
//                            tomtomMap.displayRoutesOverview();
//                            //createAndDisplayCustomTag1(routeResult);
//                            //createAndDisplayCustomTag2(routeResult);
//                        }
//
//                        private void displayRoutes(List<FullRoute> routes) {
//                            if (!routes.isEmpty() && routes.get(0).getCoordinates() != null) {
//
//                                //get the points from all the routes
//                                for (int i = 0; i < routes.size(); ++i) {
//                                    points.addAll(routes.get(i).getCoordinates());
//                                }
//                                Log.w("debug", "number of routes: " + routes.size());
//
//                                //prepare the points for dijkstra
//                                dijkstraPoints = new double[points.size()][points.size()];
//
//
//                                int numThreads = Runtime.getRuntime().availableProcessors();
//                                Log.w("debug", "number of available threads: " + numThreads);
//                                Log.w("debug", "number of coords to search for: " + points.size());
//
//
//                                //service pool of tasks to execute
////                        ExecutorService service = Executors.newFixedThreadPool(numOfCoords);
////
////
////                        //submit all the tasks using for loop
////                        for(int o=0;o<numOfCoords;++o) {
////                            int finalO = o;
////                            service.submit(() -> {
////                                //reverse geocode each coordinates to find streetnames
////                                searchForLatLng(points.get(numOfCoordsLeft.decrementAndGet()));
////                                Log.w("debug", "thread" + finalO + " num of coords now: "+numOfCoordsLeft);
////                            });
////                            //5 api calls a second so 1000/5 = 200 -make a call every 200ms
////                            SystemClock.sleep(200);
////                        }
////
////                        service.shutdown();
////                        try {
////                            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                        }
//
//                                //set up points for dijkstra
//                                findEdges(routes);
//
//                                //new route points to display the route that dijkstra has calculated
//                                newRoute = new ArrayList<>();
//                                //calculate Dijkstra based on the points collected.
//                                CalcDijkstra.calculate(dijkstraPoints, 0);
//
//                                //put the points calculated from dijkstra into an arraylist to then display the route
//                                newRoute = CalcDijkstra.getRoute();
//
//                                //set up a random colour for the route
//                                Random rnd = new Random();
//                                int color;
//                                color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//                                RouteStyle routestyle = RouteStyleBuilder.create()
//                                        .withWidth(2.0)
//                                        .withFillColor(color)
//                                        .withOutlineColor(Color.GRAY).build();
//
//                                //new list of points that relate to current point position in the street
//                                ArrayList<LatLng> newRoutePoints = new ArrayList<>();
//
//                                //loop for all the points in the street
//                                for (int j = 0; j < newRoute.size(); ++j) {
//                                    newRoutePoints.add(points.get(newRoute.get(j)));
//                                }
//                                //draw the street
//                                route = tomtomMap.addRoute(new RouteBuilder(newRoutePoints).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));
//
//                                routeDistance = CalcDijkstra.getDistance();
//
//
//                                String printNewRoute = "";
//                                for (Integer j : newRoute) {
//                                    printNewRoute += j + ", ";
//                                }
//                                Log.w("new route", "new route: " + printNewRoute);
//                                Log.w("new route", "total distance: " + routeDistance);
//
////                        MainActivity.this.runOnUiThread(() -> {
////                            ConnectToDB connectToDB = new ConnectToDB();
////                            JSONObject json = connectToDB.roadJSON("London", "22", "33", "55", "60");
////                            connectToDB.sendRequest("http://192.168.0.33/test.php", json);
////                        });
//                            }
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            hideDialog(dialogInProgress);
//                            Toast.makeText(MapActivity.this, getString(R.string.toast_error_message_cannot_find_route), Toast.LENGTH_LONG).show();
//                            MapActivity.this.finish();
//                            handleApiError(e);
//                            clearMap();
//
//                        }
//                    });
//        }
//        else {
//            timerHandler.removeCallbacks(requestRouteRunnable);
//            timerHandler.postDelayed(requestRouteRunnable, ROUTE_RECALCULATION_DELAY);
//        }
//    }

    private void requestRoute(final LatLng departure, final LatLng destination) {
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
                                points = new ArrayList<>();
                                routes = routeResponse.getRoutes();
                                for (int i = 0; i < routes.size(); ++i) {
                                    points.addAll(routes.get(i).getCoordinates());
                                }
                                Log.w("debug", "number of routes: " + routes.size());
                                //prepare the points for dijkstra
                                dijkstraPoints = new double[points.size()][points.size()];

                                int numThreads = Runtime.getRuntime().availableProcessors();
                                Log.w("debug", "number of available threads: " + numThreads);
                                Log.w("debug", "number of coords to search for: " + points.size());


                                long startTime = System.nanoTime();
//                                findEdges(routes);
                                if(!isInPauseMode){
                                    findEdges(routes);
                                }else{
                                    timerHandler.removeCallbacks(requestFindEdgesRunnable);
                                    timerHandler.postDelayed(requestFindEdgesRunnable, ROUTE_RECALCULATION_DELAY);
                                }
                                long endTime = System.nanoTime();

                                long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
                                Log.w("timer", "time for finding edges: " + duration);
                                //set up points for dijkstra


                                //new route points to display the route that dijkstra has calculated
                                newRoute = new ArrayList<>();
                                //calculate Dijkstra based on the points collected.
//                                CalcDijkstra.calculate(dijkstraPoints, 0);
//                                requestDijkstraRunnable();
                                startTime = System.nanoTime();
                                //put the points calculated from dijkstra into an arraylist to then display the route
                                if(!isInPauseMode){
                                    CalcDijkstra.calculate(dijkstraPoints, 0);
                                }else{
                                    timerHandler.removeCallbacks(requestDijkstraRunnable);
                                    timerHandler.postDelayed(requestDijkstraRunnable, ROUTE_RECALCULATION_DELAY);
                                }


                                endTime = System.nanoTime();

                                duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
                                Log.w("timer", "time for Dijkstra: " + duration);
                                newRoute = CalcDijkstra.getRoute();
                                //new list of points that relate to current point position in the street
                                ArrayList<LatLng> newRoutePoints = new ArrayList<>();

                                //loop for all the points in the street
                                for (int j = 0; j < newRoute.size(); ++j) {
                                    newRoutePoints.add(points.get(newRoute.get(j)));
                                }
                                //draw the street
//                                route = tomtomMap.addRoute(new RouteBuilder(newRoutePoints).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));

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
//                            timerHandler.removeCallbacks(requestRouteRunnable);
//                            timerHandler.postDelayed(requestRouteRunnable, ROUTE_RECALCULATION_DELAY);
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

    private void findEdges(List<FullRoute> routes){

        //map of points and their neighbours
        Map<LatLng, ArrayList<LatLng>> pointsAndNeighbours = new LinkedHashMap<>();

        //list of points that can be adapted to our needs
        ArrayList<LatLng> pointList = new ArrayList<>();


        for(FullRoute r: routes){
            for(LatLng c: r.getCoordinates()){
                pointList.add(c);
                pointsAndNeighbours.put(c, new ArrayList<>());
            }
        }


        int numThreads = Runtime.getRuntime().availableProcessors();
        Log.w("debug", "number of available threads: " + numThreads);
        //service pool of tasks to execute
        ExecutorService service = Executors.newFixedThreadPool(pointList.size());
//        submit all the tasks using for loop
        for(int o=0;o<5;++o) {
            int finalO = o;
            service.submit(() -> {
                findNeighbours(finalO, routes, pointsAndNeighbours);
//                Log.w("debug", "thread" + finalO + " num of coords now: "+numOfCoordsLeft);
            });
        }

        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        for(int i=0;i<pointList.size();i++){
            ArrayList<LatLng> value = pointsAndNeighbours.get(pointList.get(i));
            for (int j = 0; j < value.size(); j++) {
                dijkstraPoints[i][pointList.indexOf(value.get(j))] = pointList.get(i).toLocation().distanceTo(value.get(j).toLocation());
                dijkstraPoints[pointList.indexOf(value.get(j))][i] = pointList.get(i).toLocation().distanceTo(value.get(j).toLocation());
            }
        }
    }

    private void findNeighbours(int routeIndex, List<FullRoute> routes, Map<LatLng, ArrayList<LatLng>> pointsAndNeighbours){
        for (int i = routeIndex; i < (routes.size()/4) * routeIndex+1; ++i) {
            // Log.w("points", "route: "+i + "size: "+routes.get(i).getCoordinates().size());

            //for every point within every route
            for (int j = 0; j < routes.get(i).getCoordinates().size(); j++)
            {
                ArrayList neighbours = pointsAndNeighbours.get(routes.get(i).getCoordinates().get(j));

                //if it is within the limits of the array and its not already in the map!
                if(j+1<routes.get(i).getCoordinates().size() && !neighbours.contains(routes.get(i).getCoordinates().get(j+1)))
                {
                    //add the next point as first neighbour
                    neighbours.add(routes.get(i).getCoordinates().get(j + 1));
                }
                else if((j+1>=routes.get(i).getCoordinates().size())&& !neighbours.contains(routes.get(i).getCoordinates().get(j-1)))
                {
                    //add the next point as first neighbours
                    neighbours.add(routes.get(i).getCoordinates().get(j - 1));
                }

                if(i+1<routes.size())
                {
                    //start on the next route
                    for (int y = i + 1; y < routes.size(); ++y)
                    {
                        //start at the first routes neighbour
                        for (int l = j; l < routes.get(y).getCoordinates().size(); ++l)
                        {
                            //check the current point with all the points to find matches
                            if (routes.get(i).getCoordinates().get(j).toString().equals(routes.get(y).getCoordinates().get(l).toString()))
                            {
                                //add all the neighbours of the points that match with the original point
                                if (l + 1 < routes.get(y).getCoordinates().size())
                                {
                                    //add point beyond match as a neighbour
                                    neighbours.add(routes.get(y).getCoordinates().get(l + 1));
                                    //Log.w("points", "adding point to neighbours: " + routes.get(y).getCoordinates().get(l + 1));
                                } else if((j+1>=routes.get(i).getCoordinates().size()))
                                {
                                    //if last point then get the point that links to it previously
                                    //add point previous to match as a neighbour
                                    neighbours.add(routes.get(y).getCoordinates().get(l - 1));
                                    //Log.w("points", "adding last point to neighbours: " + routes.get(y).getCoordinates().get(l - 1));
                                }

                            }
                        }
                    }
                }
                //add the point and its neighbours
                pointsAndNeighbours.put(routes.get(i).getCoordinates().get(j), neighbours);
            }
        }
    }




//                        private void setupCountDownTimer(Date departure) {
//                            if (isCountdownTimerSet()) {
//                                countDownTimer.cancel();
//                            }
//                            Date now = Calendar.getInstance().getTime();
//                            final int preparationTimeMillis = preparationTime * ONE_MINUTE_IN_MILLIS;
//                            long timeToLeave = departure.getTime() - now.getTime();
//                            countDownTimer = new CountDownTimer(timeToLeave, ONE_SECOND_IN_MILLIS) {
//                                public void onTick(long millisUntilFinished) {
//                                    updateCountdownTimerTextViews(millisUntilFinished);
//                                    if (!isPreparationMode && millisUntilFinished <= preparationTimeMillis) {
//                                        isPreparationMode = true;
//                                        setCountdownTimerColor(COUNTDOWN_MODE_PREPARATION);
//                                        if (!isInPauseMode) {
//                                            showPreparationInfoDialog();
//                                        }
//                                    }
//                                }
//
//                                public void onFinish() {
//                                    timerHandler.removeCallbacks(requestRouteRunnable);
//                                    setCountdownTimerColor(COUNTDOWN_MODE_FINISHED);
//                                    if (!isInPauseMode) {
//                                        createDialogWithCustomButtons();
//                                    }
//                                }
//                            }.start();
//                            textViewTravelTime.setText(getString(R.string.travel_time_text, formatTimeFromSecondsDisplayWithoutSeconds(previousTravelTime)));

//                        private String prepareWarningMessage(int travelDifference) {
//                            String travelTimeDifference = formatTimeFromSecondsDisplayWithSeconds(travelDifference);
//                            return getString(R.string.dialog_recalculation_info, getTimeInfoWithPrefix(travelDifference, travelTimeDifference));
//                        }
//
//                        private void showWarningSnackbar(String warningMessage) {
//                            warningSnackbar.setText(warningMessage);
//                            warningSnackbar.show();
//                        }
//
//                        private boolean isCountdownTimerSet() {
//                            return countDownTimer != null;
//                        }
//
//                        private String getTimeInfoWithPrefix(int travelDifference, String travelTimeDifference) {
//                            String prefix = (travelDifference < 0) ? "-" : "+";
//                            return prefix + travelTimeDifference;
//                        }
//
//                        private void createDialogWithCustomButtons() {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(CountdownActivity.this);
//                            builder.setMessage(getString(R.string.dialog_time_to_leave))
//                                    .setPositiveButton(R.string.dialog_on_my_way, new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            hideDialog(dialogInfo);
//                                            Intent intent = new Intent(CountdownActivity.this, SafeTravelsActivity.class);
//                                            startActivity(intent);
//                                        }
//                                    })
//                                    .setNegativeButton(R.string.dialog_whatever, new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            createDialogWithCustomLayout();
//                                        }
//                                    });
//                            hideDialog(dialogInfo);
//                            dialogInfo = builder.create();
//                            dialogInfo.setCanceledOnTouchOutside(false);
//                            showDialog(dialogInfo);
//                        }
//                        private void createDialogWithCustomLayout() {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
//                            LayoutInflater inflater = getLayoutInflater();
//
//                            builder.setView(inflater.inflate(R.layout.dialog_you_are_over_time, null))
//                                    .setPositiveButton(R.string.dialog_next_time_ill_do_better, new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            hideDialog(dialogInfo);
//                                            Intent intent = new Intent(CountdownActivity.this, MainActivity.class);
//                                            startActivity(intent);
//                                        }
//                                    });
//                            hideDialog(dialogInfo);
//                            dialogInfo = builder.create();
//                            dialogInfo.setCanceledOnTouchOutside(false);
//                            showDialog(dialogInfo);
//                        }

//                        private void showPreparationInfoDialog() {
//                            dialogInfo = createSimpleAlertDialog(getString(R.string.dialog_start_preparation_text, preparationTime));
//                            showDialog(dialogInfo);
//                        }

//                        private AlertDialog createSimpleAlertDialog(String message) {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(CountdownActivity.this);
//                            builder.setMessage(message);
//                            return builder.create();
//                        }


//                        private void setCountdownTimerColor(String state) {
//                            int color;
//                            switch (state) {
//                                case COUNTDOWN_MODE_PREPARATION:
//                                    color = R.color.color_countdown_mode_preparation;
//                                    break;
//                                case COUNTDOWN_MODE_FINISHED:
//                                    color = R.color.color_countdown_mode_finished;
//                                    break;
//                                default:
//                                    color = R.color.color_all_text;
//                                    break;
//                            }
//                            int resolvedColor = ContextCompat.getColor(CountdownActivity.this, color);
//                            textViewCountDownTimerHour.setTextColor(resolvedColor);
//                            textViewCountDownTimerMin.setTextColor(resolvedColor);
//                            textViewCountDownTimerSec.setTextColor(resolvedColor);
//                        }

//                        private void updateCountdownTimerTextViews(long millis) {
//                            long hours = TimeUnit.MILLISECONDS.toHours(millis);
//                            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
//                            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);
//                            textViewCountDownTimerHour.setText(getString(R.string.countdown_timer_hour, hours));
//                            textViewCountDownTimerMin.setText(getString(R.string.countdown_timer_min, minutes));
//                            textViewCountDownTimerSec.setText(getString(R.string.countdown_timer_sec, seconds));
//                        }

//                        private String formatTimeFromSecondsDisplayWithSeconds(long secondsTotal) {
//                            return formatTimeFromSeconds(secondsTotal, true);
//                        }
//
//                        private String formatTimeFromSecondsDisplayWithoutSeconds(long secondsTotal) {
//                            return formatTimeFromSeconds(secondsTotal, false);
//                        }
//
//                        private String formatTimeFromSeconds(long secondsTotal, boolean showSeconds) {
//                            final String TIME_FORMAT_HOURS_MINUTES = "H'h' m'min'";
//                            final String TIME_FORMAT_MINUTES = "m'min'";
//                            final String TIME_FORMAT_SECONDS = " s'sec'";
//
//                            long hours = TimeUnit.SECONDS.toHours(secondsTotal);
//                            long minutes = TimeUnit.SECONDS.toMinutes(secondsTotal) - TimeUnit.HOURS.toMinutes(hours);
//                            String timeFormat = "";
//
//                            if (hours != 0) {
//                                timeFormat = TIME_FORMAT_HOURS_MINUTES;
//                            } else {
//                                if (minutes != 0) {
//                                    timeFormat = TIME_FORMAT_MINUTES;
//                                }
//                            }
//
//                            if (showSeconds) {
//                                timeFormat += TIME_FORMAT_SECONDS;
//                            }
//                            secondsTotal = Math.abs(secondsTotal);
//                            return (String) DateFormat.format(timeFormat, TimeUnit.SECONDS.toMillis(secondsTotal));
//                        }



    private void initTomTomServices() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);
        searchApi = OnlineSearchApi.create(this);
        routingApi = OnlineRoutingApi.create(this);
//        routingApi = OnlineRoutingApi.create(getApplicationContext());

    }

//    private void initToolbarSettings() {
//        Toolbar toolbar = findViewById(R.id.custom_toolbar);
//        setSupportActionBar(toolbar);
//
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setDisplayShowHomeEnabled(true);
//            actionBar.setDisplayShowTitleEnabled(false);
//        }
//    }

    private void initActivitySettings(Bundle settings) {
        departureIcon = Icon.Factory.fromResources(MapActivity.this, R.drawable.ic_map_route_departure);
        destinationIcon = Icon.Factory.fromResources(MapActivity.this, R.drawable.ic_map_route_destination);
        btnSearch = findViewById(R.id.btn_main_poisearch);
        editTextPois = findViewById(R.id.edittext_main_poisearch);

//        textViewCountDownTimerHour = findViewById(R.id.text_view_countdown_timer_hour);
//        textViewCountDownTimerMin = findViewById(R.id.text_view_countdown_timer_minute);
//        textViewCountDownTimerSec = findViewById(R.id.text_view_countdown_timer_second);
//        ImageView imgTravelingMode = findViewById(R.id.img_countdown_by_what);
//        textViewTravelTime = findViewById(R.id.text_countdown_travel_time);
//        TextView textPreparation = findViewById(R.id.text_countdown_preparation);

        initBundleSettings(settings);
//        imgTravelingMode.setImageResource(getTravelModeIcon(travelMode));
//        textPreparation.setText(getString(R.string.preparation_indicator_info, preparationTime));
//        previousTravelTime = 0;
//
//        createWarningSnackBar();
//        createInfoSnackBar();
        createDialogInProgress();
    }

    private void initBundleSettings(Bundle settings) {
//        Long arriveAtMillis = settings.getLong(BUNDLE_ARRIVE_AT);
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(arriveAtMillis);
//        arriveAt = calendar.getTime();
        departurePosition = new LatLng(settings.getDouble(BUNDLE_DEPARTURE_LAT), settings.getDouble(BUNDLE_DEPARTURE_LNG));
        destinationPosition = new LatLng(settings.getDouble(BUNDLE_DESTINATION_LAT), settings.getDouble(BUNDLE_DESTINATION_LNG));
//        travelMode = TravelMode.valueOf(settings.getString(BUNDLE_BY_WHAT).toUpperCase());
//        preparationTime = settings.getInt(BUNDLE_PREPARATION_TIME);
    }

//    private int getTravelModeIcon(TravelMode selectedTravelMode) {
//        int iconResource;
//        switch (selectedTravelMode) {
//            case CAR:
//                iconResource = R.drawable.button_main_travel_mode_car;
//                break;
//            case TAXI:
//                iconResource = R.drawable.button_main_travel_mode_cab;
//                break;
//            case PEDESTRIAN:
//                iconResource = R.drawable.button_main_travel_mode_by_foot;
//                break;
//            default:
//                iconResource = R.drawable.button_main_travel_mode_car;
//                break;
//        }
//        return iconResource;
//    }

//    private void createWarningSnackBar() {
//        ViewGroup view = findViewById(android.R.id.content);
//        warningSnackbar = CustomSnackbar.make(view, CustomSnackbar.LENGTH_INDEFINITE, R.layout.snackbar_recalculation_warning);
//        warningSnackbar.setAction(getString(R.string.button_ok), new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                warningSnackbar.dismiss();
//            }
//        });
//        setCustomSnackbar(warningSnackbar);
//    }
//
//    private void createInfoSnackBar() {
//        ViewGroup view = findViewById(android.R.id.content);
//        infoSnackbar = CustomSnackbar.make(view, CustomSnackbar.LENGTH_LONG, R.layout.snackbar_recalculation_info);
//        infoSnackbar.setText(getString(R.string.dialog_recalculation_no_changes));
//        setCustomSnackbar(infoSnackbar);
//    }

    private void createDialogInProgress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_in_progress, null));
        dialogInProgress = builder.create();
        dialogInProgress.setCanceledOnTouchOutside(false);
    }

//    private void setCustomSnackbar(CustomSnackbar snackbar) {
//        int transparentColor = ContextCompat.getColor(CountdownActivity.this, R.color.transparent);
//        snackbar.getView().setBackgroundColor(transparentColor);
//        int paddingSnackbar = (int) getResources().getDimension(R.dimen.padding_snackbar);
//        snackbar.getView().setPadding(paddingSnackbar, paddingSnackbar, paddingSnackbar, paddingSnackbar);
//    }



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

    public static Intent prepareIntent(Context context, LatLng departure, LatLng destination, TravelMode strByWhat, long arriveAtMillis, int preparationTime) {
        Bundle settings = new Bundle();
        settings.putDouble(BUNDLE_DEPARTURE_LAT, departure.getLatitude());
        settings.putDouble(BUNDLE_DEPARTURE_LNG, departure.getLongitude());
        settings.putDouble(BUNDLE_DESTINATION_LAT, destination.getLatitude());
        settings.putDouble(BUNDLE_DESTINATION_LNG, destination.getLongitude());
        settings.putString(BUNDLE_BY_WHAT, strByWhat.toString());
        settings.putLong(BUNDLE_ARRIVE_AT, arriveAtMillis);
        settings.putInt(BUNDLE_PREPARATION_TIME, preparationTime);
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(BUNDLE_SETTINGS, settings);

        return intent;
    }
}
