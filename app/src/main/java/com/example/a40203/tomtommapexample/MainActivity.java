package com.example.a40203.tomtommapexample;

import android.content.Intent;
import android.graphics.Color;
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
import com.tomtom.online.sdk.common.location.BoundingBox;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private ImageButton btnTrafficList;
    private EditText editTextPois;
    private View view;
    private BoundingBox bbox;
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

    //dijkstra
    Edge[] edges;
    Graph g;

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
        this.tomtomMap.setMyLocationEnabled(true);
        this.tomtomMap.addOnMapLongClickListener(this);
        this.tomtomMap.getMarkerSettings().setMarkersClustering(true);
        this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomViewAdapter());
        this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute1Balloon());
        this.tomtomMap.getMarkerSettings().setMarkerBalloonViewAdapter(createCustomRoute2Balloon());
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
        btnTrafficList = findViewById(R.id.btn_traffic_list);
        editTextPois = findViewById(R.id.edittext_main_poisearch);
    }

    private void setupUIViewListeners() {
        View.OnClickListener searchButtonListener = getSearchButtonListener();
        btnSearch.setOnClickListener(searchButtonListener);
        //View.OnClickListener trafficButtonListener = getTrafficButtonListener();
        //btnTrafficList.setOnClickListener(trafficButtonListener);
    }

    private void clearMap(){
        tomtomMap.clear();
        departurePosition = null;
        destinationPosition = null;
        route = null;
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
                    Toast.makeText(MainActivity.this, getString(R.string.geocode_no_results), Toast.LENGTH_SHORT).show();
                }
            }
            private void processFirstResult(LatLng geocodedPosition){
                if(!isDeparturePositionSet()){
                    setAndDisplayDeparturePosition(geocodedPosition);
                } else{
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
        RouteQuery routeQuery = createRouteQuery(start, stop, wayPoints).withMaxAlternatives(19);
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
                        Random rnd = new Random();
                        ArrayList<LatLng> unsortedtrack = new ArrayList<LatLng>();

                        int color;
                        for (int i = 0; i<routes.size();++i) {
                            color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                            RouteStyle routestyle = RouteStyleBuilder.create()
                                    .withWidth(2.0)
                                    .withFillColor(color)
                                    .withOutlineColor(Color.GRAY).build();
                            route = tomtomMap.addRoute(new RouteBuilder(
                                    routes.get(i).getCoordinates()).startIcon(departureIcon).endIcon(destinationIcon).style(routestyle));

                            unsortedtrack.addAll(routes.get(i).getCoordinates());

//                            unsortedtrack.sort() {
//                                return a.distance - b.distance;
//                            });

                            //add dijkstra

                            //travelTime[i] = routes.get(i).getSummary().getTravelTimeInSeconds();
                            Log.d("cheese", "dist: "+ String.valueOf(routes.get(i).getSummary().getDeviationDistance()));
                        }
                        Log.d("cheese", "number of points: " + unsortedtrack.size());

                        LatLng temp;

                        for(int i =0; i< unsortedtrack.size()-1;i++){
                            for(int j =1; j< unsortedtrack.size()-i;j++){
                                if(calculateDistance(departurePosition,unsortedtrack.get(j-1)) > calculateDistance(departurePosition,unsortedtrack.get(j))){
                                    temp = unsortedtrack.get(j-1);
                                    unsortedtrack.set(j-1, unsortedtrack.get(j));
                                    unsortedtrack.set(j, temp);
                                }
                            }
                        }

                        unsortedtrack.add(0,departurePosition);
                        unsortedtrack.add(unsortedtrack.size(), destinationPosition);
                        final Edge[] edges = new Edge[unsortedtrack.size()];
                        //print sorted list
                        for (int i=0; i< unsortedtrack.size();i++){
                            //Log.d("cheese", "latlng: " + String.valueOf(calculateDistance(departurePosition, unsortedtrack.get(i))));
                            //add each edge from the current index of the loop to its nearest node and calculating the distance between them
                            edges[i] = new Edge(i, unsortedtrack.indexOf(mindistToFirstPoint(unsortedtrack, unsortedtrack.get(i))), calculateDistance(departurePosition,unsortedtrack.get(i)));
                        }

                        Graph g = new Graph(edges);
                        g.calculateShortestDistances();
                        g.printResult();





                        //loop through all of the points taken from all of the routes to sort them into closest to furthest away from the starting point
//                        Map<Integer, Double> unsortedlatlngs = new HashMap<>();
//
//                        ArrayList<Integer> sortedtrack = new ArrayList<Integer>();
//                        for ( int j = 0; j < unsortedtrack.size(); j++) {
//
//                            unsortedlatlngs.put(j, calculateDistance(departurePosition, unsortedtrack.get(j)));
//                        }
//                        Map<Integer, Double> sortedlatlngs = unsortedlatlngs
//                                .entrySet()
//                                .stream()
//                                .sorted(comparingByValue())
//                                .collect(
//                                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
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

    private LatLng mindistToFirstPoint(ArrayList<LatLng> lat, LatLng firstPoint){
        LatLng minlatLng = null;
        for(int i=0; i<lat.size()-1;++i){
            if((lat.get(i) != firstPoint) && ((calculateDistance(firstPoint, lat.get(i))) < calculateDistance(firstPoint, lat.get(i+1)))){
                minlatLng = lat.get(i);
            }
        }
        return minlatLng;
    }
    private void printMap(Map map) {
        Object[] keys;
        Object[] values;
        keys = map.keySet().toArray();
        values = map.values().toArray();
        for (int i =0; i<map.size()/4;++i){
            Log.d("cheese","Key: " + keys[i] + "Value: " + values[i]);
        }
    }

    private double calculateDistance(LatLng latlng1, LatLng latlng2){
        double radlat1 = Math.PI * latlng1.getLatitude()/180;
        double radlat2 = Math.PI * latlng2.getLatitude()/180;
        double theta = latlng1.getLongitude()-latlng2.getLongitude();
        double radtheta = Math.PI * theta/180;
        double dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
        dist = Math.acos(dist);
        dist = dist*180/Math.PI;
        dist = dist * 60 * 1.1515;
        return dist;
    }
    private void createMarkerIfNotPresent(LatLng position, Icon icon){
        com.google.common.base.Optional<Marker> optionalMarker = tomtomMap.findMarkerByPosition(position);
        if(!optionalMarker.isPresent()){
            tomtomMap.addMarker(new MarkerBuilder(position).icon(icon));
        }
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
                    if(!textToSearch.isEmpty()){
                        tomtomMap.removeMarkers();
                        searchAlongTheRoute(route, textToSearch);
                    }
                }
            }

            private boolean isRouteSet(){
                return route != null;
            }

            private boolean isWayPointPositionSet(){
                return wayPointPosition != null;
            }
            private void searchAlongTheRoute(Route route, final String textToSearch){
                final Integer MAX_DETOUR_TIME = 1000;
                final Integer QUERY_LIMIT = 10;

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
                        String address = result.getAddress().getFreeformAddress();
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
        };
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
