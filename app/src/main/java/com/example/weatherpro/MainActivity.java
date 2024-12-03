package com.example.weatherpro;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.location.Location;
import android.location.Geocoder;
import android.location.Address;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.weatherpro.api.WeatherService;
import com.example.weatherpro.model.City;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends ComponentActivity {

    private SearchView searchViewCity;
    private TextView textViewCityName;
    private WeatherService weatherService;
    private static final String API_KEY = "6251fc213963cb8f3ad9068e5ae173dd";

    private ListView suggestionListView;
    private List<String> suggestionList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        ScrollView sv = (ScrollView)findViewById(R.id.scrollViewMain);
        sv.scrollTo(0, 200);

        searchViewCity = findViewById(R.id.searchViewCity);
        textViewCityName = findViewById(R.id.textViewCityName);
        suggestionListView = findViewById(R.id.suggestionListView);

        // Инициализация списка и адаптера для подсказок
        suggestionList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestionList);
        suggestionListView.setAdapter(adapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherService = retrofit.create(WeatherService.class);

        setCurrentLocation();
        setupSearchView();

        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = suggestionList.get(position); // Получаем выбранный город
            searchViewCity.setQuery(selectedCity, false); // Устанавливаем выбранный город в SearchView
            suggestionListView.setVisibility(View.GONE); // Скрываем список подсказок
        });
    }

    //Получение текущей геолокации
    private void setCurrentLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation().addOnCompleteListener(task ->  {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> adresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                        String cityName = adresses.get(0).getLocality();
                        textViewCityName.setText(cityName);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Не удалось получить местоположение", Toast.LENGTH_SHORT).show();
                }
        });
    }

    // Настройка SearchView для получения подсказок городов
    private void setupSearchView()
    {
        searchViewCity.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                textViewCityName.setText(query);
                fetchCitySuggestions(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 1) {
                    fetchCitySuggestions(newText);
                } else {
                    //suggestionList.clear();
                    //adapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    // Получение подсказок городов через OpenWeatherAPI
    private void fetchCitySuggestions(String query)
    {
        weatherService.getCities(query, 5, API_KEY).enqueue(new Callback<List<City>>() {
            @Override
            public void onResponse(Call<List<City>> call, Response<List<City>> response) {
               if (response.isSuccessful() && response.body() != null) {
                   List<City> cities = response.body();
                   showSuggestions(cities);
               } else {
                   Toast.makeText(MainActivity.this, "Ошибка получения данных", Toast.LENGTH_SHORT).show();
               }
            }

            @Override
            public void onFailure(Call<List<City>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuggestions(List<City> cities)
    {suggestionList.clear(); // Очистить старые подсказки
        for (City city : cities) {
            suggestionList.add(city.getName() + ", " + city.getCountry()); // Добавить новые подсказки
        }
        adapter.notifyDataSetChanged(); // Уведомить адаптер об изменениях

        if (suggestionList.isEmpty()) {
            suggestionListView.setVisibility(View.GONE); // Скрыть, если подсказок нет
        } else {
            suggestionListView.setVisibility(View.VISIBLE); // Показать, если есть подсказки
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setCurrentLocation();
        } else {
            Toast.makeText(this, "Разрешение на доступ к геолокации отклонено", Toast.LENGTH_SHORT).show();
        }
    }
}