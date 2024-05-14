package com.example.homely;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.database.PropertyName;

public class SensorData implements Parcelable {
    private double humidity;
    private double temperature;

    public SensorData() {}

    public SensorData(double humidity, double temperature) {
        this.humidity = humidity;
        this.temperature = temperature;
    }

    protected SensorData(Parcel in) {
        humidity = in.readDouble();
        temperature = in.readDouble();
    }

    public static final Creator<SensorData> CREATOR = new Creator<SensorData>() {
        @Override
        public SensorData createFromParcel(Parcel in) {
            return new SensorData(in);
        }

        @Override
        public SensorData[] newArray(int size) {
            return new SensorData[size];
        }
    };

    @PropertyName("Humidity")
    public double getHumidity() {
        return humidity;
    }
    @PropertyName("Temperature")
    public double getTemperature() {
        return temperature;
    }

    @PropertyName("Humidity")
    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }
    @PropertyName("Temperature")
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(humidity);
        dest.writeDouble(temperature);
    }
}
