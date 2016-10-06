package com.upenn.chriswang1990.sunshine.sync.retrofit;

import java.util.List;

/**
 * city : {"id":4930956,"name":"Boston","coord":{"lon":-71.059769,"lat":42.358429},"country":"US","population":0}
 * cod : 200
 * message : 0.3628
 * cnt : 14
 * list : [{"dt":1475683200,"temp":{"day":9.8,"min":9.8,"max":9.8,"night":9.8,"eve":9.8,"...*/
public class WeatherResponse {

    private CityBean city;

    private String cod;

    private List<ListBean> list;

    public CityBean getCity() {
        return city;
    }

    public void setCity(CityBean city) {
        this.city = city;
    }

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public List<ListBean> getList() {
        return list;
    }

    public void setList(List<ListBean> list) {
        this.list = list;
    }

    /**
     * id : 4930956
     * name : Boston
     * coord : {"lon":-71.059769,"lat":42.358429}
     * country : US
     * population : 0
     */
    public static class CityBean {
        private String name;

        private CoordBean coord;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public CoordBean getCoord() {
            return coord;
        }

        public void setCoord(CoordBean coord) {
            this.coord = coord;
        }

        /**
         * lon : -71.059769
         * lat : 42.358429
         */
        public static class CoordBean {
            private double lon;
            private double lat;

            public double getLon() {
                return lon;
            }

            public void setLon(double lon) {
                this.lon = lon;
            }

            public double getLat() {
                return lat;
            }

            public void setLat(double lat) {
                this.lat = lat;
            }
        }
    }

    /**
     * dt : 1475683200
     * temp : {"day":9.8,"min":9.8,"max":9.8,"night":9.8,"eve":9.8,"morn":9.8}
     * pressure : 1036.14
     * humidity : 100
     * weather : [{"id":800,"main":"Clear","description":"clear sky","icon":"01n"}]
     * speed : 1.78
     * deg : 29
     * clouds : 0
     */
    public static class ListBean {
        private long dt;

        private TempBean temp;
        private double pressure;
        private int humidity;
        private double speed;
        private int deg;

        private List<WeatherBean> weather;

        public long getDt() {
            return dt;
        }

        public void setDt(int dt) {
            this.dt = dt;
        }

        public TempBean getTemp() {
            return temp;
        }

        public void setTemp(TempBean temp) {
            this.temp = temp;
        }

        public double getPressure() {
            return pressure;
        }

        public void setPressure(double pressure) {
            this.pressure = pressure;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public int getDeg() {
            return deg;
        }

        public void setDeg(int deg) {
            this.deg = deg;
        }

        public List<WeatherBean> getWeather() {
            return weather;
        }

        public void setWeather(List<WeatherBean> weather) {
            this.weather = weather;
        }

        /**
         * day : 9.8
         * min : 9.8
         * max : 9.8
         * night : 9.8
         * eve : 9.8
         * morn : 9.8
         */
        public static class TempBean {
            private double min;
            private double max;

            public double getMin() {
                return min;
            }

            public void setMin(double min) {
                this.min = min;
            }

            public double getMax() {
                return max;
            }

            public void setMax(double max) {
                this.max = max;
            }
        }

        /**
         * id : 800
         * main : Clear
         * description : clear sky
         * icon : 01n
         */
        public static class WeatherBean {
            private int id;
            private String main;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getMain() {
                return main;
            }

            public void setMain(String main) {
                this.main = main;
            }
        }
    }
}
