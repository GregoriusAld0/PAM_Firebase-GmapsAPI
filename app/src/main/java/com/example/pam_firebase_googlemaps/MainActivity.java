package com.example.pam_firebase_googlemaps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.Serializable;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener{

    private GoogleMap gMap;
    private Marker currentMarker;
    private LatLng currentPlace;
    private Marker selectedMarker;
    private LatLng selectedPlace;

    private FirebaseFirestore db;

    private TextView txtOrderId, txtSelectedPlace, txtCurrentPlace;
    private EditText editTextName;
    private Button btnEditOrder, btnOrder;
    FloatingActionButton fab;

    private boolean isNewOrder = true;
    boolean isPermissionGranted;

    private FusedLocationProviderClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtOrderId = findViewById(R.id.txt_orderId);
        txtCurrentPlace = findViewById(R.id.txt_currentPlace);
        txtSelectedPlace = findViewById(R.id.txt_selectedPlace);
        editTextName = findViewById(R.id.editTxt_name);
        btnEditOrder = findViewById(R.id.btn_editOrder);
        btnOrder = findViewById(R.id.btn_order);
        fab = findViewById(R.id.fab);
        checkMyPermission();

        initMap();

        mLocationClient = new FusedLocationProviderClient(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getCurrLoc();
            }
        });

        db = FirebaseFirestore.getInstance();

        btnOrder.setOnClickListener(view -> { saveOrder(); });
        btnEditOrder.setOnClickListener(view -> { updateOrder(); });
    }

    private void initMap() {
        if (isPermissionGranted == true){
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

    }

    @SuppressLint("MissingPermission")
    private void getCurrLoc() {
        mLocationClient.getLastLocation().addOnCompleteListener(task -> {

            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                gotoLocation(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void gotoLocation(double latitude, double longitude) {
        LatLng currlatLng = new LatLng(latitude, longitude);

        currentPlace = currlatLng;
        currentMarker = gMap.addMarker(new MarkerOptions().position(currentPlace));

        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPlace, 15.0f));

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(currentPlace.latitude, currentPlace.longitude, 1);
            if (addresses != null) {
                Address place = addresses.get(0);
                StringBuilder street = new StringBuilder();

                for (int i=0; i <= place.getMaxAddressLineIndex(); i++) {
                    street.append(place.getAddressLine(i)).append("\n");
                }
                txtCurrentPlace.setText(street.toString());
            }
            else {
                Toast.makeText(this, "Could not find Address!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            Toast.makeText(this, "Error get Address!", Toast.LENGTH_SHORT).show();
        }

    }

    private void checkMyPermission() {

        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                Toast.makeText(MainActivity.this, "Permission granted!", Toast.LENGTH_SHORT).show();
                isPermissionGranted = true;
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMyLocationEnabled(true);

        LatLng salatiga = new LatLng(-7.3305, 110.5084);

        selectedPlace = salatiga;
        selectedMarker = gMap.addMarker(new MarkerOptions().position(selectedPlace));

        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace, 15.0f));

        gMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        selectedPlace = latLng;
        selectedMarker.setPosition(selectedPlace);
        gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(selectedPlace.latitude, selectedPlace.longitude, 1);
            if (addresses != null) {
                Address place = addresses.get(0);
                StringBuilder street = new StringBuilder();

                for (int i=0; i <= place.getMaxAddressLineIndex(); i++) {
                    street.append(place.getAddressLine(i)).append("\n");
                }
                txtSelectedPlace.setText(street.toString());
            }
            else {
                Toast.makeText(this, "Could not find Address!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            Toast.makeText(this, "Error get Address!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrder() {
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> place = new HashMap<>();

        String name = editTextName.getText().toString();

        place.put("Current address", txtCurrentPlace.getText().toString());
        place.put("Destination address", txtSelectedPlace.getText().toString());
        place.put("currLat", currentPlace.latitude);
        place.put("currLng", currentPlace.longitude);
        place.put("destLat", selectedPlace.latitude);
        place.put("destLng", selectedPlace.longitude);

        order.put("name", name);
        order.put("createdDate", new Date());
        order.put("place", place);

        String orderId = txtOrderId.getText().toString();

        if (isNewOrder) {
            db.collection("orders")
                    .add(order)
                    .addOnSuccessListener(documentReference -> {
                        editTextName.setText("");
                        txtSelectedPlace.setText("Pilih tujuan");
                        txtOrderId.setText(documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal tambah data order", Toast.LENGTH_SHORT).show();
                    });
        }
        else {
            db.collection("orders").document(orderId)
                    .set(order)
                    .addOnSuccessListener(unused -> {
                        editTextName.setText("");
                        txtCurrentPlace.setText("");
                        txtSelectedPlace.setText("");
                        txtOrderId.setText(orderId);

                        isNewOrder = true;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal ubah data order", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateOrder() {
        isNewOrder = false;

        String orderId = txtOrderId.getText().toString();
        DocumentReference order = db.collection("orders").document(orderId);
        order.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.get("name").toString();
                    Map<String, Object> place = (HashMap<String, Object>) document.get("place");

                    editTextName.setText(name);
                    txtSelectedPlace.setText(place.get("Destination address").toString());

                    LatLng resultPlace = new LatLng((double) place.get("lat"), (double) place.get("lng"));
                    selectedPlace = resultPlace;
                    selectedMarker.setPosition(selectedPlace);
                    gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));
                }
                else {
                    isNewOrder = true;
                    Toast.makeText(this, "Document does not exist!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, "Unable to read the db!", Toast.LENGTH_SHORT).show();
            }
        });
    }


}