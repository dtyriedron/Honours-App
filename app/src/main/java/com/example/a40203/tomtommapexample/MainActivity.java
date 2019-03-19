package com.example.a40203.tomtommapexample;

import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.tomtom.online.sdk.map.SingleLayoutBalloonViewAdapter;
import com.tomtom.online.sdk.map.TomtomMap;
import com.tomtom.online.sdk.map.TomtomMapCallback;
import com.tomtom.online.sdk.map.model.MapTilesType;
import com.tomtom.online.sdk.routing.OnlineRoutingApi;
import com.tomtom.online.sdk.routing.RoutingApi;
import com.tomtom.online.sdk.routing.data.FullRoute;
import com.tomtom.online.sdk.routing.data.RouteQuery;
import com.tomtom.online.sdk.routing.data.RouteQueryBuilder;
import com.tomtom.online.sdk.routing.data.RouteResult;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static java.lang.String.valueOf;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, TomtomMapCallback.OnMapLongClickListener, TextToSpeech.OnInitListener {

    private TomtomMap tomtomMap;
    private RoutingApi routingApi;
    private SearchApi searchApi;
    private TrafficApi trafficApi;
    private Route route;
    private LatLng departurePosition;
    private LatLng destinationPosition;
    private LatLng wayPointPosition;
    private Icon departureIcon;
    private Icon destinationIcon;
    private ImageButton btnSearch;
    private EditText editTextPois;
    private ArrayList<String> streetnames;
    private ArrayList<LatLng> points;
    private double[][] organisedPoints;
    int pointCount = 0;
    //map of different streetnames and their positions in the original streetname array
    Map<String, ArrayList<Integer>> map;
    ArrayList<Integer> pointPos;
    //text to speech
    TextToSpeech mTTS = null;
    private final int ACT_CHECK_TTS_DATA = 1000;
    private final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2000;
    private int permissionCount = 0;
    private String mAudioFilename = "";
    private final String mUtteranceID = "totts";
    //colours
    private int[] routeColours;
    //travelTime
    private int[] travelTime;

    final int MAX_DETOUR_TIME = 10000;
    final int QUERY_LIMIT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initTomTomServices();
        initUIViews();
        setupUIViewListeners();
        //nitColours();

        // Check to see if we have TTS voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);
    }

    @Override
    public void onMapReady(@NonNull final TomtomMap tomtomMap) {
        this.tomtomMap = tomtomMap;
        this.tomtomMap.getUiSettings().setMapTilesType(MapTilesType.VECTOR);
        this.tomtomMap.setMyLocationEnabled(true);
        this.tomtomMap.addOnMapLongClickListener(this);
        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);
        this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomViewAdapter());
        //this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute1Balloon());
        //this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute2Balloon());
    }
    public void clearMap(){
        tomtomMap.clear();
        departurePosition = null;
        destinationPosition = null;
        route = null;
        points.clear();
        streetnames.clear();
        pointPos.clear();
        map.clear();
        pointCount = 0;
        organisedPoints = null;
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(isDeparturePositionSet() && isDestinationPositionSet()){
            clearMap();
        } else {
            handleLongClick(latLng);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.tomtomMap.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void initTomTomServices() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getAsyncMap(this);
        searchApi = OnlineSearchApi.create(this);
        routingApi = OnlineRoutingApi.create(this);
        trafficApi = OnlineTrafficApi.create(this);
    }


//    private void initColours(){
//        routeColours = new int[4];
//        routeColours[0] = Color.rgb(255,237,160);
//        routeColours[1] = Color.rgb(254,178, 76);
//        routeColours[2] = Color.rgb(240, 59, 32);
//        routeColours[3] = Color.rgb(230,55,30);
//    }

    private void initUIViews() {
        departureIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_map_route_departure);
        destinationIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_map_route_destination);
        btnSearch = findViewById(R.id.btn_main_poisearch);
        editTextPois = findViewById(R.id.edittext_main_poisearch);
    }

    private void setupUIViewListeners() {
        View.OnClickListener searchButtonListener = getSearchButtonListener();
        btnSearch.setOnClickListener(searchButtonListener);
        //View.OnClickListener trafficButtonListener = getTrafficButtonListener();
        //btnTrafficList.setOnClickListener(trafficButtonListener);
    }

//    private void getLocation(){
////        Icon activeIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.ic_markedlocation);
////        Icon inactiveIcon = Icon.Factory.fromResources(MainActivity.this, R.drawable.arrow_down);
////        ChevronBuilder chevronBuilder = ChevronBuilder.create(activeIcon, inactiveIcon);
////        Chevron chevron = tomtomMap.getDrivingSettings().addChevron(chevronBuilder);
////        tomtomMap.getDrivingSettings().startTracking(chevron);
////        chevron.getLocation();
////        tomtomMap.getDrivingSettings().stopTracking();
//    }


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
                    Toast.makeText(MainActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
                }
            }
            private void processFirstResult(LatLng geocodedPosition){
                if(!isDeparturePositionSet()){
                    setAndDisplayDeparturePosition(geocodedPosition);
                } else{
                    points = new ArrayList<>();
                    streetnames = new ArrayList<>();
                    pointPos = new ArrayList<>();
                    map = new HashMap<>();
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

    private boolean isDestinationPositionSet(){
        return destinationPosition != null;
    }

    private boolean isDeparturePositionSet(){
        return departurePosition != null;
    }

    private void handleApiError(Throwable e){
        Toast.makeText(MainActivity.this, getString(R.string.api_response_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private RouteQuery createRouteQuery(LatLng start, LatLng stop, LatLng[] wayPoints){
        //return (wayPoints != null) ?
                return new RouteQueryBuilder(start, stop).withRouteType(RouteType.FASTEST);
        //new RouteQueryBuilder(start, stop).withWayPoints(wayPoints).withRouteType(RouteType.FASTEST)
    //:           new RouteQueryBuilder(start, stop).withRouteType(RouteType.FASTEST);
    }

    private void drawRoute(LatLng start, LatLng stop){
        wayPointPosition = null;
        drawRouteWithWayPoints(start, stop, null);
    }

    private void drawRouteWithWayPoints(LatLng start, LatLng stop, LatLng[] wayPoints){
        RouteQuery routeQuery = createRouteQuery(start, stop, wayPoints).withMaxAlternatives(9);
        routingApi.planRoute(routeQuery)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<RouteResult>() {
                    @Override
                    public void onSuccess(RouteResult routeResult){
                        displayRoutes(routeResult.getRoutes());
                        tomtomMap.displayRoutesOverview();
                        //createAndDisplayCustomTag1(routeResult);
                        //createAndDisplayCustomTag2(routeResult);
                    }

                    private void displayRoutes(List<FullRoute> routes) {
                        //travelTime = new int[2];
                        //Random rnd = new Random();
                        //prepare to organise the points for dijkstra

                        for (int i = 0; i<routes.size();++i) {

                            //color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//                            RouteStyle routestyle = RouteStyleBuilder.create()
//                                    .withWidth(2.0)
//                                    .withFillColor(color)
//                                    .withOutlineColor(Color.GRAY).build();
//                            route = tomtomMap.addRoute(new RouteBuilder(
//                                    routes.get(i).getCoordinates()).startIcon(departureIcon).endIcon(destinationIcon));

                            points.addAll(routes.get(i).getCoordinates());

                            //route = tomtomMap.addRoute(new RouteBuilder(routes.get(i).getCoordinates()).startIcon(departureIcon).endIcon(destinationIcon));

                        }
                        Log.w("debug", "number of routes: "+ routes.size());
                        //organisedPoints = new double[points.size()][3];
                        organisedPoints = new double[points.size()][points.size()];
                        //initialise array by setting values  0
                        for(double[] i: organisedPoints){
                            for(double j: i){
                                j=0;
                            }
                        }
                        int localCounter1;
                        int localCounter2;
                        int globalCounter1=0;
                        int globalCounter2=1;
                        int matchCounter=1;


                        for(int j=0;j<routes.size();++j){
                            localCounter1=0;
                            localCounter2=1;
                            for (int i=0; i<routes.get(j).getCoordinates().size();++i){
                                while(localCounter2<routes.get(j).getCoordinates().size() && localCounter1<routes.get(j).getCoordinates().size()){
                                //add the distance for coordinate one to coordinate two and map them to both parts fo the list
                                organisedPoints[globalCounter1][globalCounter2] = Helper.calculateDistance(routes.get(j).getCoordinates().get(localCounter1), routes.get(j).getCoordinates().get(localCounter2));
                                organisedPoints[globalCounter2][globalCounter1] = Helper.calculateDistance(routes.get(j).getCoordinates().get(localCounter1), routes.get(j).getCoordinates().get(localCounter2));
                                if((j+1)<routes.size() && localCounter1<routes.get(j+1).getCoordinates().size() && localCounter2<routes.get(j+1).getCoordinates().size()) {
                                    if (routes.get(j).getCoordinates().get(localCounter1).toString().equals(routes.get(j + 1).getCoordinates().get(localCounter1).toString())) {
                                        organisedPoints[globalCounter1][globalCounter1 + routes.get(j).getCoordinates().size()] = Helper.calculateDistance(routes.get(j).getCoordinates().get(localCounter1), routes.get(j + 1).getCoordinates().get(localCounter2));
                                        organisedPoints[globalCounter1 + routes.get(j).getCoordinates().size()][globalCounter1] = Helper.calculateDistance(routes.get(j).getCoordinates().get(localCounter1), routes.get(j + 1).getCoordinates().get(localCounter2));
                                        Log.w("cheese", "points match at: " + localCounter1 + "points compared: " + routes.get(j).getCoordinates().get(localCounter1).toString() + " and: " + routes.get(j+1).getCoordinates().get(localCounter1).toString());

                                        //change the list size to match the number of distances that have to be travelled
                                        double[][] tempList = organisedPoints.clone();
                                        organisedPoints = new double[points.size()-matchCounter][points.size()-matchCounter];
                                        for(int tempCounter=0;tempCounter<tempList.length-1;++tempCounter){
                                            for(int tempCounter2=0; tempCounter2<tempList.length-1;++tempCounter2){
                                                organisedPoints[tempCounter][tempCounter2] = tempList[tempCounter][tempCounter2];
                                            }
                                        }

                                        ++matchCounter;
                                        if(localCounter1>0) {
//                                            --localCounter1;
//                                            --localCounter2;
                                            --globalCounter1;
                                            --globalCounter2;
                                        }
                                    }
                                }
                                    Log.w("cheese", "size of route"+j+": " + routes.get(j).getCoordinates().size() + " size of i: " + localCounter1+" size of j: "+ localCounter2 + " size of k: "+globalCounter1+" size of l: "
                                            +globalCounter2+" size of the whole points list: "+ points.size() + " added dist: "+ Helper.calculateDistance(routes.get(j).getCoordinates().get(localCounter1),
                                            routes.get(j).getCoordinates().get(localCounter2)) + " due to the points:" + routes.get(j).getCoordinates().get(localCounter1) + " and: "+routes.get(j).getCoordinates().get(localCounter2));

                                //check that the ints dont go too high or there will be an out of bounds error
                                    ++localCounter1;
                                    ++localCounter2;
                                    ++globalCounter1;
                                    ++globalCounter2;

                                }
                            }
                        }

//                        for(){
////                            Log.w("cheese", "point: " +point);
////                        }


                        //compareRoutes(routes);
                        String string = "\n";

                for(double[] i: organisedPoints){
                    for(double j: i){
                         string +=j;
                         string += ", ";
                    }
                    string += "\n";
                }
                Log.w("debug2", "organised points current street: "+ string);

                        int numThreads = Runtime.getRuntime().availableProcessors();
                        Log.w("debug", "number of available threads: "+numThreads);

                        //get all the coordinates from all the routes
                        int numOfCoords = points.size();
                        //atomic integer for thread safe access
                        AtomicInteger numOfCoordsLeft = new AtomicInteger(numOfCoords);

                        //Log.w("debug", "lat: HERE: " + String.valueOf(tomtomMap.getUserLocation().getLatitude()) + " long: HERE: " + String.valueOf(tomtomMap.getUserLocation().getLongitude()));


                        Log.w("debug", "number of coords to search for: "+ numOfCoords);
                        //service pool of tasks to execute
//                        ExecutorService service = Executors.newFixedThreadPool(numOfCoords);
//
//
//                        //submit all the tasks using for loop
//                        for(int o=0;o<numOfCoords;++o) {
//                            int finalO = o;
//                            service.submit(() -> {
//                                //reverse geocode each coordinates to find streetnames
//                                searchForLatLng(points.get(numOfCoordsLeft.decrementAndGet()));
//                                Log.w("debug", "thread" + finalO + " num of coords now: "+numOfCoordsLeft);
//                            });
//                            //5 api calls a second so 1000/5 = 200 -make a call every 200ms
//                            SystemClock.sleep(200);
//                        }
//
//                        service.shutdown();
//                        try {
//                            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }




                        //get the streetname for each point and add it to an array

//                                array[j] = String.valueOf(route.getCoordinates().get(j));
//                                //array[j] = streetName;
//                                array[j] = array[j].replace("LatLng(latitude=", "");
//                                array[j] = array[j].replace("longitude=", "");
//                                array[j] = array[j].replace(")", "");
//                                Log.w("cheese", "points: "+array[j]);

//                        Log.w("cheese", "number of points: " + unsortedtrack.size());
//                        for (int i =0; i< unsortedtrack.size()/4; ++i){
//                            Log.w("cheese", String.valueOf(unsortedtrack.get(i)));
//                        }
//                        //mindistToFirstPoint(unsortedtrack, departurePosition);
//                        Log.w("cheese", "next lot:");
//                        for (int i =0; i< unsortedtrack.size()/4; ++i){
//                            Log.w("cheese2", String.valueOf(mindistToFirstPoint(unsortedtrack, unsortedtrack.get(unsortedtrack.size()/2)).get(i)));
//                        }



//                        //create 2d array made up of points and there 3 adjacent vertexes
//                        double[][] parent = new double[unsortedtrack.size()-1][3];
//
//                        //loop for every point on every route
//                        for (int tempPoint = 0; tempPoint<unsortedtrack.size()-1;++tempPoint){
//
//                            //sort the unsorted array to find the nearest edges to the current point being investigated
//                            ArrayList<LatLng> tempPoints = Helper.mindistToFirstPoint(unsortedtrack, unsortedtrack.get(tempPoint));
//
//                            //set up the edges for the tempPoint
//                            for (int tempPointEdges = 0; tempPointEdges<3; ++tempPointEdges){
//                                //at the current tempPoint input the current edge distance for each edge
//                                parent[tempPoint][tempPointEdges] = Helper.calculateDistance(tempPoints.get(tempPointEdges), unsortedtrack.get(tempPoint));
//                            }
//                        }
//
//                        double[][] adjacencyMatrix = { { 0, 4, 0, 0, 0, 0, 0, 8, 0 },
//                                { 4, 0, 8, 0, 0, 0, 0, 11, 0 },
//                                { 0, 8, 0, 7, 0, 4, 0, 0, 2 },
//                                { 0, 0, 7, 0, 9, 14, 0, 0, 0 },
//                                { 0, 0, 0, 9, 0, 10, 0, 0, 0 },
//                                { 0, 0, 4, 0, 10, 0, 2, 0, 0 },
//                                { 0, 0, 0, 14, 0, 2, 0, 1, 6 },
//                                { 8, 11, 0, 0, 0, 0, 1, 0, 7 },
//                                { 0, 0, 2, 0, 0, 0, 6, 7, 0 } };

                        ArrayList<Integer> newRoute = new ArrayList<>();
                        //calculate Dijkstra based on the points collected.
                        newRoute = CalcDijkstra.calculate(organisedPoints, 0);
//                        String printNewRoute= "";
//                        for(Integer j:newRoute){
//                            printNewRoute += j + ", ";
//                        }

                        Random rnd = new Random();
                        int color;
                        color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                        RouteStyle routestyle = RouteStyleBuilder.create()
                                .withWidth(2.0)
                                .withFillColor(color)
                                .withOutlineColor(Color.GRAY).build();

                        //new list of points that relate to current point position in the street
                        ArrayList<LatLng> newRoutePoints = new ArrayList<>();

                        //loop for all the points in the street
                        for(int j=0; j<newRoute.size();++j){
                            newRoutePoints.add(points.get(newRoute.get(j)));
                        }
                        //draw the street
                        route = tomtomMap.addRoute(new RouteBuilder(newRoutePoints).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));

//                        Log.w("debug", "new route: " + printNewRoute);
                        //clearMap();


                       // color = Color.rgb(255,255,0);




                        MainActivity.this.runOnUiThread(() -> {
                            ConnectToDB connectToDB = new ConnectToDB();
                            JSONObject json = connectToDB.roadJSON("London", "22", "33", "55", "60");
                            connectToDB.sendRequest("http://192.168.0.33/test.php", json);
                        });

                        //loop through all of the points taken from all of the routes to sort them into closest to furthest away from the starting point
//                        DisplayMap<Integer, Double> unsortedlatlngs = new HashMap<>();
//
//                        ArrayList<Integer> sortedtrack = new ArrayList<Integer>();
//                        for ( int j = 0; j < unsortedtrack.size(); j++) {
//
//                            unsortedlatlngs.put(j, calculateDistance(departurePosition, unsortedtrack.get(j)));
//                        }
//                        DisplayMap<Integer, Double> sortedlatlngs = unsortedlatlngs
//                                .entrySet()
//                                .stream()
//                                .sorted(comparingByValue())
//                                .collect(
//                                        toMap(DisplayMap.Entry::getKey, DisplayMap.Entry::getValue, (e1, e2) -> e2,
//                                                LinkedHashMap::new));

//                        Log.d("cheese", "unsorted: ");
//                        printMap(unsortedlatlngs);
//                        //sortedlatlngs.putAll(unsortedlatlngs);
//                        Log.d("cheese", "sorted: ");
//                        printMap(sortedlatlngs);
//
//                        for(int i = 0; i<sortedlatlngs.size(); ++i){
//
//                        }
//                        sortedlatlngs.values().toArray();
//
//                        ArrayList<LatLng> trackClone = unsortedtrack;
//                        unsortedtrack.clear();
//
//
//                        for (int i=0;i<sortedlatlngs.size();++i){
//                            Double j = sortedlatlngs.get(i);
//
//                            //unsortedtrack.add(sortedlatlngs.get(i), trackClone.get());
//                        }
//                        Log.d("cheese", "first element of unsortedtrack list: " + unsortedtrack.get(0));
                    }



//                    private void createAndDisplayCustomTag1(RouteResult result){
//                        Location tag1 = result.getRoutes().get(0).getLegs()[result.getRoutes().get(0).getLegs().length/2].getPoints()[0].toLocation();
//
//                        String tag1Name = String.valueOf(travelTime[0]);
//
//                        BaseMarkerBalloon markerBalloonData = new BaseMarkerBalloon();
//                        markerBalloonData.addProperty(getString(R.string.tag1_name), tag1Name);
//
//                        MarkerBuilder markerBuilder = new MarkerBuilder(new LatLng(tag1.getLatitude(), tag1.getLongitude()))
//                                .markerBalloon(markerBalloonData)
//                                .shouldCluster(false);
//                        tomtomMap.addMarker(markerBuilder);
//                    }

//                    private void createAndDisplayCustomTag2(RouteResult result){
//                        //change this
//                        Location tag2 = result.getRoutes().get(1).getLegs()[0].getPoints()[0].toLocation();
//
//                        String tag2Name = String.valueOf(travelTime[1]);
//
//                        BaseMarkerBalloon markerBalloonData = new BaseMarkerBalloon();
//                        markerBalloonData.addProperty(getString(R.string.tag2_name), tag2Name);
//
//                        MarkerBuilder markerBuilder = new MarkerBuilder(new LatLng(tag2.getLatitude(), tag2.getLongitude()))
//                                .markerBalloon(markerBalloonData)
//                                .shouldCluster(false);
//                        tomtomMap.addMarker(markerBuilder);
//                    }
                    @Override
                    public void onError(Throwable e) {
                        handleApiError(e);
                        clearMap();
                    }
                });

    }
    private void createMarkerIfNotPresent(LatLng position, Icon icon){
        com.google.common.base.Optional<Marker> optionalMarker = tomtomMap.findMarkerByPosition(position);
        if(!optionalMarker.isPresent()){
            tomtomMap.addMarker(new MarkerBuilder(position).icon(icon));
        }
    }

    public static void getResponse(String string){
        Log.w("cheese", string);
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
                    if(isWayPointPositionSet()){
                        tomtomMap.clear();
                        drawRoute(departurePosition, destinationPosition);
                    }
                    String textToSearch = editTextPois.getText().toString();
                    LatLng lngToSearch = new LatLng(Double.valueOf(textToSearch.split(",")[0]), Double.valueOf(textToSearch.split(",")[1]));
                    if(!textToSearch.isEmpty()){
                        tomtomMap.removeMarkers();
                        searchForLatLng(lngToSearch);
                        //searchAlongTheRoute(textToSearch);
                    }
                }
            }

            private boolean isRouteSet(){
                return route != null;
            }

            private boolean isWayPointPositionSet(){
                return wayPointPosition != null;
            }

        };
    }


    private void compareRoutes(List<FullRoute> routes){
        int j=0;
        //for every route
        for(int i=1;i<routes.size();++i) {
            //for every point in every route
            //Log.w("cheese", "nother route");
                while (j < routes.get(i - 1).getCoordinates().size() && j<routes.get(i).getCoordinates().size()) {
                    Log.w("cheese", "route: " + i + "num of points: " + routes.get(i).getCoordinates().size() + "current point: " + j + " " + routes.get(i).getCoordinates().get(j) +
                            "route2: " + (i - 1) + "num of points: " + routes.get(i - 1).getCoordinates().size() + "route two point: " + j + " " + routes.get(i-1).getCoordinates().get(j));
                    if (routes.get(i).getCoordinates().get(j).toString().equals(routes.get(i - 1).getCoordinates().get(j).toString())) {
                        Log.w("cheese", "this point matches the same point on another route");
                    }
                    ++j;
                    //Log.w("cheese", "hey");
                }

        }

    }

    private void collateStreetPoints(){
        //check if any streetnames match each other
        if(pointCount >0) {
            if (streetnames.get(pointCount).equals(streetnames.get(pointCount -1))) {
                //add another point to the current street's set of points
                pointPos.add(pointCount);
                //Log.w("debug", "adding another point: " + pointCount + " for: " + streetnames.get(pointCount));
                //update or add new streetname and add its points
                map.put(streetnames.get(pointCount), pointPos);
                //Log.w("debug", "Map adding1: " + streetnames.get(pointCount) + " with: "+ pointPos.size() + "points");
                //Log.w("debug2",  "size of the map: " + map.size());


                //give the street some colour
                newStreetNewColour(pointCount);
//                String string = "\n";
//                for(double[] i: organisedPoints){
//                    for(double j: i){
//                        string +=j;
//                        string += ", ";
//                    }
//                    string += "\n";
//                }
//                Log.w("debug2", "organised points current street: "+ string);

            }else{
                pointPos.clear();
                pointPos.add(pointCount);
                //put the new streetname's points into map
                map.put(streetnames.get(pointCount), pointPos);
//                map.get(streetnames.get(pointCount-1)).size()-1)
//                for(int i = 1; i<pointPos.size();++i) {
//                    organisedPoints[map.size()][pointCount] = Helper.calculateDistance(points.get(pointPos.get(i)), points.get(i-1));
//                }
                //Log.w("debug", "Map adding2: " + streetnames.get(pointCount) + " with: "+ pointPos.size() + "points");
                //Log.w("debug2",  "size of the map: " + map.size());

//                String string = "\n";

//                for(double[] i: organisedPoints){
//                    for(double j: i){
//                         string +=j;
//                         string += ", ";
//                    }
//                    string += "\n";
//                }
//                Log.w("debug2", "organised points current street: "+ string);

                //give the street some colour
                newStreetNewColour(pointCount);

//                //check if its the last streetname and therefore needs to added.
//                if((pointCount == points.size()-1)){
//                    pointPos.add(pointCount);
//                    Log.w("debug", "adding the last point: " + pointCount);
//                    map.put(streetnames.get(pointCount), pointPos);
//                    Log.w("debug", "Map adding1: "+ streetnames.get(pointCount) + " with: " + pointPos.size() + "points");
//
//                    //String key = (String) ;
//                    organisedPoints[map.size()][pointCount] = Helper.calculateDistance(points.get(pointPos.get(0)), points.get(map.get(streetnames.get(pointCount-1)).size()-1));
//                    newStreetNewColour(pointCount);
//                }

            }
//            else{
//                pointPos.add(pointCount);
//                map.put(streetnames.get(pointCount), pointPos);
//                Log.w("debug", "map adding1: " + streetnames.get(pointCount) + "with: " + pointPos.size() + "points");
//                Log.w("debug", "adding odd point: " + pointCount);
//                pointPos.clear();
//                newStreetNewColour(pointCount);
//            }
        } else {
            //add the first point to the current street's set of points
            pointPos.add(pointCount);
            //Log.w("debug", "adding first point: " + pointCount);
        }

    }

    private void newStreetNewColour(int currrentStreet){
        //Log.w("debug", "colouring in: " + streetnames.get(currrentStreet));
        //loop for every street in the route
            //new color for every street
            Random rnd = new Random();
            int color;
            color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            RouteStyle routestyle = RouteStyleBuilder.create()
                    .withWidth(2.0)
                    .withFillColor(color)
                    .withOutlineColor(Color.GRAY).build();

            //get the list of points for the current street
            ArrayList<Integer> streetPointPos = map.get(streetnames.get(currrentStreet));
            //new list of points that relate to current point position in the street
            ArrayList<LatLng> newRoutePoints = new ArrayList<>();

            //loop for all the points in the street
            for(int j=0; j<streetPointPos.size();++j){
                newRoutePoints.add(points.get(streetPointPos.get(j)));
            }
            //draw the street
            route = tomtomMap.addRoute(new RouteBuilder(newRoutePoints).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));

    }



    private void searchForLatLng(LatLng latLng){
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
                        if(response.hasResults()) {
                            //put into array of streetnames
                            streetnames.add(response.getAddresses().get(0).getAddress().getStreetName());
                            // Log.w("debug", "streetnames added: " + streetnames.get(pointCount) + " " + pointCount);

                            //add all the points up that relate top that street
                            collateStreetPoints();
                            pointCount++;
                        }
                        else{
                            Toast.makeText(MainActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void searchAlongTheRoute(final String textToSearch){

        searchApi.alongRouteSearch(new AlongRouteSearchQueryBuilder(textToSearch, route.getCoordinates(), MAX_DETOUR_TIME).withLimit(QUERY_LIMIT))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<AlongRouteSearchResponse>() {
                    @Override
                    public void onSuccess(AlongRouteSearchResponse response){
                        displaySearchResults(response.getResults());
                    }

                    private void displaySearchResults(List<AlongRouteSearchResult> results){
                        if(!results.isEmpty()){
                            for(AlongRouteSearchResult result:results){
                                createAndDisplayCustomMarker(result.getPosition(), result);
                            }
                            tomtomMap.zoomToAllMarkers();

                        } else{
                            Toast.makeText(MainActivity.this, String.format(getString(R.string.no_search_results), textToSearch), Toast.LENGTH_LONG).show();
                        }
                    }

                    private void createAndDisplayCustomMarker(LatLng position, AlongRouteSearchResult result){
                        String address = result.getAddress().getStreetName();
                        String poiName = result.getPoi().getName();

                        BaseMarkerBalloon markerBalloonData = new BaseMarkerBalloon();
                        markerBalloonData.addProperty(getString(R.string.poi_name_key), poiName);
                        markerBalloonData.addProperty(getString(R.string.address_key), address);

                        MarkerBuilder markerBuilder = new MarkerBuilder(position)
                                .markerBalloon(markerBalloonData)
                                .shouldCluster(true);
                        tomtomMap.addMarker(markerBuilder);
                    }

                    @Override
                    public void onError(Throwable e){
                        handleApiError(e);
                    }
                });
    }

   /* @NonNull
    private View.OnClickListener getTrafficButtonListener(){
        return new View.OnClickListener(){

            @Override
            public void onClick(View v){
                LatLng latLng1 = new LatLng((256/2*Math.PI)*2*(departurePosition.getLatitude()+Math.PI),
                        ((256/2*Math.PI)*2*(Math.PI - Math.log(Math.tan(Math.PI/4+departurePosition.getLongitude()/2)))));
                LatLng latLng2 = new LatLng((256/2*Math.PI)*2*(destinationPosition.getLatitude()+Math.PI),
                        ((256/2*Math.PI)*2*(Math.PI - Math.log(Math.tan(Math.PI/4+destinationPosition.getLongitude()/2)))));
                bbox = new BoundingBox(latLng1, latLng2);
                handleTrafficClick(v);
            }

            private void handleTrafficClick(View v){
                IncidentDetailsQueryBuilder query = new IncidentDetailsQueryBuilder(IncidentStyle.S1, bbox, 4, "-1")
                        .withExpandCluster(true).build();
                trafficApi.findIncidentDetails(query, incidentDetailsResultListener);
                v.setSelected(true);
            }
        };
    }

    private IncidentDetailsResultListener incidentDetailsResultListener = new IncidentDetailsResultListener() {
        @Override
        public void onTrafficIncidentDetailsResult(IncidentDetailsResponse result) {

            final List<TrafficIncident> items = new ArrayList<>();

            TrafficIncidentVisitor visitor = new TrafficIncidentVisitor() {
                @Override
                public void visit(TrafficIncidentCluster cluster) {
                    proceedWithCluster(cluster, items);
                }

                @Override
                public void visit(TrafficIncident incident) {
                    proceedWithIncident(incident, items);
                }
            };

            for (BaseTrafficIncident incident : result.getIncidents()) {
                incident.accept(visitor);
            }

            view.updateTrafficIncidentsList(items);
        }

        @Override
        public void onTrafficIncidentDetailsError(Throwable error) {
            Toast.makeText(view.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };
*/


    private SingleLayoutBalloonViewAdapter createCustomViewAdapter(){
        return new SingleLayoutBalloonViewAdapter(R.layout.marker_custom_balloon){
            @Override
            public void onBindView(View view, final Marker marker, BaseMarkerBalloon baseMarkerBalloon){
                Button btnAddWayPoint = view.findViewById(R.id.btn_balloon_waypoint);
                final TextView textViewPoiName = view.findViewById(R.id.textview_balloon_poiname);
                TextView textViewPoiAddress = view.findViewById(R.id.textview_balloon_poiaddress);
                textViewPoiName.setText(baseMarkerBalloon.getStringProperty(getApplicationContext().getString(R.string.poi_name_key)));
                textViewPoiAddress.setText(baseMarkerBalloon.getStringProperty(getApplicationContext().getString(R.string.address_key)));
                btnAddWayPoint.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        setWayPoint(marker);
                        saySomething(textViewPoiName.getText().toString().trim(), 1);
                    }
                    private void setWayPoint(Marker marker){
                        wayPointPosition = marker.getPosition();

                        tomtomMap.clearRoute();
                        drawRouteWithWayPoints(departurePosition, destinationPosition, new LatLng[] {wayPointPosition});
                        marker.deselect();
                    }
                });
            }
        };
    }

    private SingleLayoutBalloonViewAdapter createCustomRoute1Balloon() {
        return new SingleLayoutBalloonViewAdapter(R.layout.custom_tag) {
            @Override
            public void onBindView(View view, Marker marker, BaseMarkerBalloon baseMarkerBalloon) {
                final TextView textViewNameTag = view.findViewById(R.id.textview_tag_tagname);
                textViewNameTag.setText(baseMarkerBalloon.getStringProperty(getApplicationContext().getString(R.string.tag1_name)));
            }
        };
    }
    private SingleLayoutBalloonViewAdapter createCustomRoute2Balloon() {
        return new SingleLayoutBalloonViewAdapter(R.layout.custom_tag) {
            @Override
            public void onBindView(View view, Marker marker, BaseMarkerBalloon baseMarkerBalloon) {
                final TextView textViewNameTag = view.findViewById(R.id.textview_tag_tagname);
                textViewNameTag.setText(baseMarkerBalloon.getStringProperty(getApplicationContext().getString(R.string.tag2_name)));
            }
        };
    }

    //text to speech
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                int result = mTTS.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                } else {
                    saySomething("Hello World", 0);
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
        }
    }

    private void saySomething(String text, int qmode) {
        if (qmode == 1)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACT_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // data exists, so we instantiate the TTS engine
                mTTS = new TextToSpeech(this, this);
            } else {
                // data is missing, so we start the TTS installation process
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }
}
