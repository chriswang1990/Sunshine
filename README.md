# Sunshine
#### Android Weather App - Udacity Android course project
##### Main features:
* Call the ***OpenWeatherMap*** and ***Google Timezone API*** for weather and date data
* Use ***SQLite*** database with customized ***content provider*** for weather data storage and query
* Design GUI for hand-size device as well as tablet in both normal or land view, build a two-pane layout for tablet
* Use ***SyncAdapter*** to synchronize the weather data in backgroud, and push notification to the user when new weather data are available
* Provide setting options for user to change location and units preferrence
* Provide ***Map Location*** and ***Share*** option for user to find current location or share the weather information with others 


##### Several improvements to the original course project:
* Call the ***Google timezone API*** to get the timezone ID for the query location, fix the bug that the app is not displaying correct local date and weather for the query location
* Use Date class instead of the deprecated Time class for date, simplify the code and make date calculation more robust
* (In in two-panel mode) The today entry is auto selected when the app startup or when user change location setting
* Use the new toolbar Class instead of the deprecated ActionBar class, separate the activity and content layout XML accordingly 
* Display city name in today list item view and detail fragment to remind user for weather location
