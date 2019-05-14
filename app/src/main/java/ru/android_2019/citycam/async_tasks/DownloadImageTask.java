package ru.android_2019.citycam.async_tasks;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

import ru.android_2019.citycam.appconfig.App;
import ru.android_2019.citycam.callbacks.DownloadCallbacks;
import ru.android_2019.citycam.connection_api.ConnectionApi;
import ru.android_2019.citycam.dao.WebcamDAO;
import ru.android_2019.citycam.model.City;
import ru.android_2019.citycam.model.Webcam;
import ru.android_2019.citycam.parsers.ResponseWebcamParser;
import ru.android_2019.citycam.webcams.Webcams;

public final class DownloadImageTask extends AsyncTask<City, Integer, List <Webcam>> {


    private DownloadCallbacks callbacks;

    public DownloadImageTask(DownloadCallbacks callbacks) {
        this.callbacks = callbacks;
    }


    @SuppressLint("WrongThread")
    @Override
    protected List<Webcam> doInBackground(City... cities) {
        City city = cities[0];
        WebcamDAO webcamDAO = App.getInstance().getWebcamDB().webcamDao();
        List <Webcam> webcams = null;
        try {
            webcams = webcamDAO.selectListWebcamsByName(city.name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(webcams != null && webcams.isEmpty()) {
            InputStream in = null;
            HttpURLConnection connection = null;
            try {
                URL url = Webcams.createNearbyUrl(city.latitude, city.longitude);
                connection = ConnectionApi.getConnection(url);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException();
                }
                in = connection.getInputStream();
                webcams = ResponseWebcamParser.listResponseWebcam(in, "UTF-8");
                if (webcams != null && !webcams.isEmpty()) {
                    for(Webcam webcam : webcams) {
                        webcam.setCityName(city.name);
                    }
                    webcamDAO.insertWebcams(webcams);
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(String.valueOf(this), "AsyncTask");
        }
        return webcams;
    }


    @Override
    protected void onPostExecute(List <Webcam> webcams) {
        Webcam webcam = webcams != null && !webcams.isEmpty() ? webcams.get(new Random().nextInt(webcams.size())) : null;
        callbacks.onPostExecute(webcam);
    }

    @Override
    protected void onProgressUpdate(Integer... percent) {
        super.onProgressUpdate(percent);
        callbacks.onProgressUpdate(percent[0]);
    }
}