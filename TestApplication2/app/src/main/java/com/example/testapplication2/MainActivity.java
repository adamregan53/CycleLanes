package com.example.testapplication2;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static com.example.testapplication2.R.raw.galwaycyclelanes;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, RoutingListener {

    //data members
    private GoogleMap mMap;//GoogleMap Object
    SearchView searchView;

    //current and destination location objects
    Location myLocation = null;//used to store location of device
    Location destinationLocation = null;
    protected LatLng start = null;//coordinates of start
    protected LatLng end = null;//coordinates of end
    protected LatLng setMapStart=null;

    //to get location permissions.
    private final static int LOCATION_REQUEST_CODE = 23;//used for location permission
    boolean locationPermission = false;//initialise location permission as false

    //polyline object
    private List<Polyline> polylines = null;//stores coordinates for routes
    private List<Address> addressList = null;//stores address for geocoder object

    //routeCoordinates array
    private List<LatLng> routeCoordinates=null;
    private List<LatLng> jsonBikeLanes=null;

    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {//This acts as the main method for the android application
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//R is used to locate files in the res folder such as XML, JSON and other text formats


        requestPermision();//calls method to request location data

        //initialise the google map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);//gets the map fragment from the activity_main.xml. It then uses R to locate the ID of the fragment which looks like "android:id="@+id/map" in the file
        mapFragment.getMapAsync(this);//calls the map Object


    }

    private void requestPermision() {//this method gets the permission from the manifest file
        //if the location permission is false, ask user to share their location. Note that the manifest file will save that choice by altering location permission in the phone settings
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            locationPermission = true;//else if the location permission is already granted, set the value to true
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //if permission granted.
                    locationPermission = true;
                    getMyLocation();//if location permission is granted call the method to get the users location

                } else {

                }
                return;
            }
        }
    }

    private void getMyLocation() {//this method gets the location of the users device
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
            
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {//uses method in the google map api to track changes in location

                myLocation=location;
                LatLng ltlng=new LatLng(location.getLatitude(),location.getLongitude());



            }

        });




        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {//uses an onclick listener to get coordinates of the
            @Override
            public void onMapClick(LatLng latLng) {

                end=latLng;

                mMap.clear();

                start=new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
                //start route finding
                Findroutes(start,end);
            }
        });





    }//end getMyLocation()




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getMyLocation();
        getCycleLaneData();
        try {
            parseJSONData();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

    }

    private void getCycleLaneData() {
        try {
            GeoJsonLayer layer=new GeoJsonLayer(mMap, galwaycyclelanes, getApplicationContext());
            GeoJsonLineStringStyle lineStringStyle = layer.getDefaultLineStringStyle();
            lineStringStyle.setColor(Color.RED);
            lineStringStyle.setPolygonStrokeWidth(2);
            layer.addLayerToMap();
            //parseJSONData();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //parse json and get coordinates of bike lanes
    public void parseJSONData() throws JSONException, IOException, ParseException {
        double lat;
        double lng;
        LatLng bikeLaneCoordinates=null;
        jsonBikeLanes=new ArrayList<>();

        JSONParser parser=new JSONParser();
        String jsonStr=readJSONFromResource(galwaycyclelanes);
        JSONObject reader=new JSONObject(jsonStr);
        JSONArray features=reader.getJSONArray("features");

        for(int i=0;i<features.length();i++){
            JSONObject position =(JSONObject) features.get(i);
            JSONObject geometry= (JSONObject) position.get("geometry");
            JSONArray coordinates=geometry.getJSONArray("coordinates");
            String type= (String) geometry.get("type");


            JSONArray latLngCoordinates;

            if(type.equals("MultiLineString")){

                for(int j=0;j<coordinates.length();j++){
                    JSONArray mutliCoordinates = (JSONArray) coordinates.get(j);

                    for(int k=0;k<mutliCoordinates.length();k++){
                        latLngCoordinates= (JSONArray) mutliCoordinates.get(k);
                        lat = (double) latLngCoordinates.get(0);
                        lng = (double) latLngCoordinates.get(1);
                        bikeLaneCoordinates=new LatLng(lat,lng);
                        jsonBikeLanes.add(bikeLaneCoordinates);//add to arraylist of json coordinates

                    }
                }
            }
            else if(type.equals("LineString")){

                for(int j=0;j<coordinates.length();j++){
                    latLngCoordinates= (JSONArray) coordinates.get(j);
                    lat = (double) latLngCoordinates.get(0);
                    lng = (double) latLngCoordinates.get(1);
                    bikeLaneCoordinates=new LatLng(lat,lng);
                    jsonBikeLanes.add(bikeLaneCoordinates);//add to arraylist of json coordinates

                }
            }

        }
        Log.i(TAG, "parseJSONData: "+jsonBikeLanes.toString());
    }

    //read in galwaycyclelanes geojson from raw resources folder and convert to string
    public String readJSONFromResource(int fileID) throws IOException {
        InputStream is = getResources().openRawResource(galwaycyclelanes);
        Writer writer = new StringWriter();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line = reader.readLine();
        while(line !=null){
            writer.write(line);
            line = reader.readLine();
        }
        String jsonStr= writer.toString();
        return jsonStr;
    }




    // function to find Routes.
    public void Findroutes(LatLng Start, LatLng End)
    {
        if(Start==null || End==null) {
            Toast.makeText(MainActivity.this,"Unable to get location",Toast.LENGTH_LONG).show();
        }
        else
        {

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.BIKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key("AIzaSyBar81Cy8YtRv1fpXNVxUy4taLgI8Pn238")
                    .build();
            routing.execute();

        }
    }

    //Routing call back functions.
    @Override
    public void onRoutingFailure(RouteException e) {
        View parentLayout = findViewById(android.R.id.content);
        Snackbar snackbar= Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG);
        snackbar.show();
//        Findroutes(start,end);
    }

    @Override
    public void onRoutingStart() {
        Toast.makeText(MainActivity.this,"Finding Route...",Toast.LENGTH_LONG).show();//shows text on screen when the route is calculated
    }

    //If Route finding success..
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        try {
            if (polylines != null) {
                polylines.clear();
            }
            PolylineOptions polyOptions = new PolylineOptions();
            LatLng polylineStartLatLng = null;
            LatLng polylineEndLatLng = null;

            
            polylines = new ArrayList<>();
            routeCoordinates=new ArrayList<>();
            //add route(s) to the map using polyline
            for (int i = 0; i < route.size(); i++) {

                if (i == shortestRouteIndex) {
                    polyOptions.color(getResources().getColor(R.color.colorPrimary));
                    polyOptions.width(7);
                    polyOptions.addAll(route.get(shortestRouteIndex).getPoints());
                    Polyline polyline = mMap.addPolyline(polyOptions);
                    polylineStartLatLng = polyline.getPoints().get(0);
                    int k = polyline.getPoints().size();
                    polylineEndLatLng = polyline.getPoints().get(k - 1);
                    polylines.add(polyline);

                    for (LatLng point: route.get(i).getPoints()) {
                        routeCoordinates.add(point);
                    }

                    double routeLat=routeCoordinates.get(i).latitude;
                    double routeLng=routeCoordinates.get(i).longitude;

                    for(int j=0;j<jsonBikeLanes.size();j++){
                        double jsonLat=jsonBikeLanes.get(j).latitude;
                        double jsonLng=jsonBikeLanes.get(j).longitude;

                    }



                } else {

                }


            }

            getCycleLaneData();

            Log.i(TAG, "coordinates: "+routeCoordinates);
            Log.i(TAG, "coordinates: "+routeCoordinates.get(0));

            mMap.moveCamera(CameraUpdateFactory.newLatLng(route.get(0).getLatLgnBounds().getCenter()));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(polylineStartLatLng);
            builder.include(polylineEndLatLng);
            LatLngBounds bounds=builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            //Add Marker on route starting position
            MarkerOptions startMarker = new MarkerOptions();
            startMarker.position(polylineStartLatLng );
            startMarker.title("My Location");
            mMap.addMarker(startMarker);

            //Add Marker on route ending position
            MarkerOptions endMarker = new MarkerOptions();
            endMarker.position(polylineEndLatLng);
            endMarker.title("Destination");
            mMap.addMarker(endMarker);
        }catch(Exception e){

        }
    }

    @Override
    public void onRoutingCancelled() {
        Findroutes(start,end);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Findroutes(start,end);

    }

}