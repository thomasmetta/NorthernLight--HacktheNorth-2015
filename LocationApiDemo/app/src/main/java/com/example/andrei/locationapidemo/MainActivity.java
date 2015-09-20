package com.example.andrei.locationapidemo;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.OnClick;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private EditText mNameField;
    private EditText mMessageEditText;
    private EditText mCityEditText;
    private Button mSubmitButton;

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    private boolean mRequestLocationUpdates = false;

    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 10000;
    private static int FATEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;

    private TextView lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Firebase.setAndroidContext(this);

        final Button button = (Button) findViewById(R.id.startChat);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ChatMainActivity.class);
                startActivity(intent);
            }
        });

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        btnShowLocation = (Button) findViewById(R.id.buttonShowLocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.buttonLocationUpdates);

        mNameField = (EditText) findViewById(R.id.personEditText);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mCityEditText = (EditText) findViewById(R.id.cityEditText);
        mSubmitButton = (Button) findViewById(R.id.submitButton);



        mSubmitButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                String name = mNameField.getText().toString();
                                                Toast.makeText(MainActivity.this, "Submitted", Toast.LENGTH_LONG).show();

                                                new Connection().execute();



                                            }
                                        }
        );


        if(checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
        }

        btnShowLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePeriodLocationUpdates();
            }
        });


    }


    private class Connection extends AsyncTask {

        @Override
        protected Object doInBackground(Object... arg0) {

            try {
                run();
            } catch (Exception ex) {
                Log.e("foo","shit happened");
                Log.e("foo", ex.toString());
            }
            return null;
        }

    }


    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/x-markdown; charset=utf-8");


    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    public void run() throws Exception {


  //      Response response = client.newCall(request).execute();


   //     if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);


 //       System.out.println(response.body().string());


//        Response response = client.newCall(request).execute();
//        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        sendName(client, mNameField.getText().toString());



//        System.out.println(response.body().string());
    }



    @Override
     protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();
        if(mGoogleApiClient.isConnected() && mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void sendName(final OkHttpClient client, String name) {
        RequestBody formBody = new FormEncodingBuilder()
                .add("name", mNameField.getText().toString())
                .build();
        Request request = new Request.Builder()
                .url("https://secret-depths-3946.herokuapp.com/api/v1/users")
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

                Log.d("foo", "not working");

            }

            @Override
            public void onResponse(Response response) throws IOException {
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(response.body().string());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String id = null;
                try {
                    id = jObject.getString("user_id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String address = mCityEditText.getText().toString();
                sendDestination(client, id, address);
                Log.d("foo", "sending from sendName");
                Log.d("foo", "id: " + id + " address: " + address);
            }
        });
    }

    private void sendDestination(final OkHttpClient client, String id, String address) {

        RequestBody formBody = new FormEncodingBuilder()
                .add("user_id", id)
                .add("address", address)
                .build();
        Request request = new Request.Builder()
                .url("https://secret-depths-3946.herokuapp.com/api/v1/destinations")
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

                Log.d("foo", "not working second time");

            }

            @Override
            public void onResponse(Response response) throws IOException {

                Log.d("foo", "receive from sendDes" + response.toString());
                String message = mMessageEditText.getText().toString();
                double latitude = mLastLocation.getLatitude();
                double longitude = mLastLocation.getLongitude();

                sendPost(client, message, latitude, longitude);


            }
        });

        RequestBody formBody1 = new FormEncodingBuilder()
                .add("user_id", id)
                .add("latitude", String.valueOf(mLastLocation.getLatitude()))
                .add("longtitude", String.valueOf(mLastLocation.getLongitude()))
                .build();
        Request request1 = new Request.Builder()
                .url("https://secret-depths-3946.herokuapp.com/api/v1/locations")
                .post(formBody1)
                .build();
        client.newCall(request1).enqueue(new Callback() {
            @Override
            public void onFailure(Request request1, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {



            }
        });


    }



    private void sendPost(OkHttpClient client, String message, double latitdue, double longitude) {

        RequestBody formBody = new FormEncodingBuilder()
                .add("message", message)
                .add("latitude", String.valueOf(mLastLocation.getLatitude()))
                .add("longitude", String.valueOf(mLastLocation.getLongitude()))
                .build();
        Request request = new Request.Builder()
                .url("https://secret-depths-3946.herokuapp.com/api/v1/posts")
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

                Log.d("foo", "not working third time");

            }

            @Override
            public void onResponse(Response response) throws IOException {

                Log.d("foo", "receive from sendDes" + response.toString());


            }
        });




    }






    private void displayLocation() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longtitude = mLastLocation.getLongitude();


        } else {
            lblLocation.setText("Couldn't get the location. Make sure location is enabled on the device");
        }
    }

    private void togglePeriodLocationUpdates() {
        if(!mRequestLocationUpdates) {
            btnStartLocationUpdates.setText(getString(R.string.btn_stop_location_updates));

            mRequestLocationUpdates = true;

            startLocationUpdates();
        } else {
            btnStartLocationUpdates.setText(getString(R.string.btn_start_location_updates));

            mRequestLocationUpdates = false;

            stopLocationUpdates();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(), "This device is not supported", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        displayLocation();

        if(mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Location changed!", Toast.LENGTH_SHORT).show();

        displayLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode());
    }


}
