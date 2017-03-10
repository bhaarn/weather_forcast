package com.wipro.weatherforecast.helpers;


public class Helper {

    public static final String API_KEY = "241817a301f7c2413afe2caa0f180b7a";

    public static String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }
}
