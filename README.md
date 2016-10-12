# Sunshine
#### Android Weather App
##### Main features:
* Call the ***OpenWeatherMap*** and ***Google Timezone API*** for weather and date data with ***RxJava***, ***Retrofit2*** and ***GSON*** Libraries
* Use ***SQLite*** database with customized ***content provider*** for weather data storage and query
* Use ***CursorAdapter*** and ***Loader*** to load and display required data from database
* Implement the Main Fragment Layout with ***RecyclerView*** and ***SwipeRefreshLayout*** for best perfermance
* Proper ***error handling*** for all error cases including network, server, invalid input, etc., and give users suggetions when errors happen
* Design GUI for hand-size device as well as tablet in both normal or land view, build a ***two-pane layout*** for tablet
* Use ***SyncAdapter*** to synchronize the weather data in backgroud, also push ***notification*** to the user when new weather data are available
* Provide setting options for user to change location and units preferrence
* Provide ***Map Location*** and ***Share*** option for user to find current location or share the weather information with others
* Build the complete ***android test suite*** for database
